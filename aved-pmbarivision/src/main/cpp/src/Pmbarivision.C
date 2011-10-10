/*
 * Copyright 2010 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1 
 * (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This is a program to automate detection and tracking of events in underwater 
 * video. This is based on modified version from Dirk Walther's 
 * work that originated at the 2002 Workshop  Neuromorphic Engineering 
 * in Telluride, CO, USA. 
 * 
 * This code requires the The iLab Neuromorphic Vision C++ Toolkit developed
 * by the University of Southern California (USC) and the iLab at USC. 
 * See http://iLab.usc.edu for information about this project. 
 *  
 * This work would not be possible without the generous support of the 
 * David and Lucile Packard Foundation
 */ 

/*!@file Pmbarivision.C a program to automate detection of events 
  in underwater video. Based on the single threaded Mbarivision.C, but designed to
  operate on a Beowulf cluster using the MPI message passing libraries.
 */

#include "MessagePassing/Mpidef.H"
#include "Channels/ChannelOpts.H"
#include "Component/GlobalOpts.H"
#include "Component/JobServerConfigurator.H"
#include "Neuro/NeuroOpts.H"
#include "Neuro/VisualCortex.H"
#include "Neuro/VisualCortexWeights.H"
#include "Neuro/WinnerTakeAllConfigurator.H"
#include "Neuro/ShapeEstimatorModes.H"
#include "Neuro/AttentionGuidanceMap.H"
#include "Component/ModelManager.H"
#include "Data/MbariOpts.H"
#include "Stages/SegmentStage.H"
#include "Stages/GetSalientRegionsStage.H"
#include "Stages/CachePreprocessStage.H"
#include "Stages/UpdateEventsStage.H"
#include "Stages/Stage.H"
#include "MessagePassing/Mpimessage.H"
#include "PipelineControl/PipelineController.H"
#include "Parallel/pvisionTCP-defs.H"
#include "Utils/Version.H"
#include "Beowulf/BeowulfOpts.H"
#include "Media/MediaOpts.H"
#include "Media/FrameRange.H"
#include "Stages/Stage.H"
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>

// ! Initializes model components
void initModelComponents(int argc, const char **argv);

// ! Local variables
const int foaSizeRatio = 19;
const int circleRadiusRatio = 40; 

using namespace std;

extern "C" {
	
  /* Prototypes
     /* Creates next stage according to id and returns object
     * TODO: refactor this into a factory pattern in Stage*/
  Stage *createStage(MPI_Comm master, Stages::stageID id, int argc, const char **argv,  ModelManager &manager);

  int main( int argc, char **argv )
  {	
    int id, size;
    char processorName[MPI_MAX_PROCESSOR_NAME];
    int nameLen;
    Controller *c = 0;
    Stage *s = 0;

    MPI_Init( &argc, &argv );
    MPI_Comm_rank( MPI_COMM_WORLD, &id );
    MPI_Comm_size( MPI_COMM_WORLD, &size );	 
    MPI_Get_processor_name(processorName, &nameLen);

    LINFO("Process %d of %d is on %s\n", id, size, processorName);
    MPE_Init_log();
		
    // Abort if don't have exactly the number of stages plus one for the controller requested 
    if(size != ( Stages::getNumStages() + 1 )) {
      fprintf(stderr, "ERROR, Need exactly %d processes to run and requested %d. Adjust the number of processes to %d.\n", Stages::getNumStages()+1, size, Stages::getNumStages()+1);
      MPI_Abort( MPI_COMM_WORLD, 1);            
    }

    char name[128];
    sprintf(name, "MBARI Automated Video Event Detection Program - slave %s", processorName);
    ModelManager manager(name);
 
    if(id == 0) 	{
      // make first process the Controller
      c = new Controller(MPI_COMM_WORLD);

      // initialize logging states
      MPE_Describe_state(1,2,"CachePreprocess", "purple:vlines3");
      MPE_Describe_state(3,4,"Segment", "white:vlines3");
      MPE_Describe_state(5,6,"GetSalientRegions", "orange:vlines3");
      MPE_Describe_state(7,8,"UpdateEvents", "red:vlines3");
      MPE_Describe_state(20,21,"total time", "yellow:vlines3");
    }
    else  {
      // all other processes are stages, indexed from 1
      Stages::stageID sID = static_cast<Stages::stageID>(id);
      s = createStage(MPI_COMM_WORLD, sID, argc, (const char **)argv, manager);
    } 
 
    MPE_Start_log();

    //wait for all other processes to get to this point
    MPI_Barrier(MPI_COMM_WORLD);	 

    //now run stages
    if(s !=0 ) s->run();
    if(c !=0 ) c->run();

    //wait for all other processes to get to this point
    MPI_Barrier(MPI_COMM_WORLD);	 
 
    if(s != 0) delete s;
    if(c != 0) delete c; 

    //close log and clean up
    MPE_Finish_log("cpilog");
  
    MPI_Finalize( ); 
 
    if(manager.started()) manager.stop();
 
    LINFO( "Done !!");
    return 0;
  }

  Stage *createStage(MPI_Comm master, Stages::stageID id, int argc, const char **argv,  ModelManager &manager)
  {
    // turn down log messages until after initialzation
    MYLOGVERB = LOG_NOTICE;

    Stage *s = 0; 

    LINFO("Creating stage:%s id: %d", Stages::stageName(id), id);

    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
    nub::soft_ref<WinnerTakeAllConfigurator>  wtac(new WinnerTakeAllConfigurator(manager));
    manager.addSubComponent(wtac);
    nub::soft_ref<JobServerConfigurator>
            jsc(new JobServerConfigurator(manager));
    manager.addSubComponent(jsc);
    nub::soft_ref<SimEventQueueConfigurator>
            seqc(new SimEventQueueConfigurator(manager));
    manager.addSubComponent(seqc);
    nub::soft_ref<SaliencyMapStd> sm(new SaliencyMapStd(manager));
    manager.addSubComponent(sm);  
    nub::soft_ref<AttentionGuidanceMapStd> agm(new AttentionGuidanceMapStd(manager));
    manager.addSubComponent(agm);  
    nub::soft_ref<InputFrameSeries> ifs(new InputFrameSeries(manager));
    manager.addSubComponent(ifs);			
    nub::soft_ref<OutputFrameSeries> ofs(new OutputFrameSeries(manager));
    manager.addSubComponent(ofs);
    nub::soft_ref<OutputFrameSeries> evtofs(new OutputFrameSeries(manager));
    manager.addSubComponent(evtofs);
  
    // Get the binary directory of this executable
    string exe(argv[0]);
    size_t found = exe.find_last_of("/\\");
   
    nub::soft_ref<MbariResultViewer> rv(new MbariResultViewer(manager, evtofs, ofs, exe.substr(0,found)));
    manager.addSubComponent(rv);
    
    nub::soft_ref<DetectionParametersModelComponent> detectionParmsModel(new DetectionParametersModelComponent(manager));
    manager.addSubComponent(detectionParmsModel);

    // Request mbari specific option aliases
    REQUEST_OPTIONALIAS_MBARI(manager);

    // Request toolkit option aliases
    REQUEST_OPTIONALIAS_NEURO(manager);
     
    // add a brain here so we can get the command options
    nub::soft_ref<StdBrain> brain(new StdBrain(manager));
    manager.addSubComponent(brain); 

    // initialize brain defaults 
    manager.setOptionValString(&OPT_UseRandom, "true");
    manager.setOptionValString(&OPT_SVdisplayFOA, "true");
    manager.setOptionValString(&OPT_SVdisplayPatch, "false");
    manager.setOptionValString(&OPT_SVdisplayFOALinks, "false");
    manager.setOptionValString(&OPT_SVdisplayAdditive, "true");
    manager.setOptionValString(&OPT_SVdisplayTime, "false");
    manager.setOptionValString(&OPT_SVdisplayBoring, "false");

    // disable model manager option for all display-related options for
    // all stages because Xdisplay option are not enabled in parallel code.
    manager.setOptionValString(&OPT_MRVdisplayOutput,"false");
    manager.setOptionValString(&OPT_MRVdisplayResults,"false");
    
    //create master Beowulf component
    nub::soft_ref<Beowulf> beowulf(new Beowulf(manager, "Beowulf", "Beowulf", true));
    beowulf->exportOptions(OPTEXP_ALL);

    // get reference to the SimEventQueue
    nub::soft_ref<SimEventQueue> seq = seqc->getQ();

    // parse the command line
    if(manager.parseCommandLine(argc, argv, "", 0, -1) == false) 
 	LFATAL("Invalid command line argument. Aborting program now !");

    // set the range to be the same as the input frame range
    FrameRange fr = ifs->getModelParamVal<FrameRange>("InputFrameRange");
    ofs->setModelParamVal(string("OutputFrameRange"), fr);
 
    // get image dimensions and set a few parameters that depend on it
    detectionParmsModel->reset(&dp);

    // get the dimensions of the input frames
    Dims dims = ifs->peekDims();
    float scaleW = 1.0f;
    float scaleH = 1.0f;

    // store rgb image in cache; sometimes this fails due to NFS error so retry a few times 
    int ntrys = 0; 
    do { 
        dims = ifs->peekDims();
    } while (!dims.isEmpty() && ntrys++ < 3); 

    if (ntrys == 3) 
      LERROR("Error reading frame dimensions");

    // if the user has selected to retain the original dimensions in the events
    // get the scaling factors, and unset the resizing in the input frame series
    if (dp.itsSaveOriginalFrameSpec) {
      // get a reference to our original frame source
      const nub::ref<FrameIstream> ref = ifs->getFrameSource();
      const Dims origDims = ref->peekDims();
      scaleW = (float) origDims.w() / (float) dims.w();
      scaleH = (float) origDims.h() / (float) dims.h();
      ifs->setModelParamVal(string("InputFrameDims"), Dims(0,0), MC_RECURSE);
      ifs->peekDims();
    }

    int foaRadius;
    const string foar = manager.getOptionValString(&OPT_FOAradius);
    convertFromString(foar, foaRadius);
 
    // calculate the foa size based on the image size if set to defaults
    // A zero foa radius indicates to set defaults from input image dims
    if (foaRadius == 0) {
        foaRadius = dims.w() / foaSizeRatio;
        char str[256];
        sprintf(str, "%d", foaRadius);
        manager.setOptionValString(&OPT_FOAradius, str);
    }
		  
    // start all our ModelComponent instances  
    manager.start();
 
    nub::soft_ref<WinnerTakeAll> wta = wtac->getWTA();

    // initialize detection parameters
    DetectionParametersSingleton::initialize(dp, dims, foaRadius);

    // is this a a gray scale sequence ? if so disable computing the color channels
    // to save computation time. This assumes the color channel has no weight !
    if (dp.itsColorSpaceType == SAColorGray )   {
        string search = "C";
        string source = manager.getOptionValString(&OPT_VisualCortexType);
        size_t pos = source.find(search);
        if (pos != string::npos) {
            string replace = source.erase(pos, 1);
            manager.setOptionValString(&OPT_VisualCortexType, replace);
        }
    }

    // get level spec and norm type 
    const string ls = manager.getOptionValString(&OPT_LevelSpec).c_str();
    LevelSpec levelSpec;convertFromString(ls, levelSpec);
    const string mn = manager.getOptionValString(&OPT_MaxNormType).c_str();
    MaxNormType normType; convertFromString(mn, normType);
    
    LDEBUG("MaxNormType: %d LevelSpec sml %d deltamin %d deltamax"
            " %d levelmin %d levelmax %d maxdepth %d", normType,
                                            levelSpec.mapLevel(),
                                            levelSpec.delMin(),
                                            levelSpec.delMax(),
                                            levelSpec.levMin(),
                                            levelSpec.levMax(),
                                            levelSpec.levMax() + levelSpec.delMax() + 1
                                            );

    // get the boring delay and mv
    const string bd = manager.getOptionValString(&OPT_BrainBoringDelay).c_str();
    SimTime boringDelay;  convertFromString(bd, boringDelay); 
    
    const string bmv = manager.getOptionValString(&OPT_BrainBoringSMmv).c_str();
    float boringmv;  convertFromString(bmv, boringmv);

    // get the frame range
    FrameRange frameRange = FrameRange::fromString(manager.getOptionValString(&OPT_InputFrameRange));
    string inputFileStem = manager.getOptionValString(&OPT_InputFrameSource).c_str();

    string vcweight = manager.getOptionValString(&OPT_VisualCortexType).c_str();
    bool isbeo = false, issurp = false, isthreaded = false;
    const VisualCortexWeights wts =
        VisualCortexWeights::fromString(vcweight,
                                        &isbeo, &issurp, &isthreaded);

    // get the shape estimator mode
    const string semode = manager.getOptionValString(&OPT_ShapeEstimatorMode).c_str();
    ShapeEstimatorMode sem; convertFromString(semode, sem); 

    switch(id)	{
    case(Stages::CP_STAGE):            
      s = (Stage *)new CachePreprocessStage(master, Stages::stageName(id), 
                                            ifs,
					    rv,
                                            frameRange,
                                            inputFileStem,
                                            dims);
      break;
    case(Stages::SG_STAGE):
      s = (Stage *)new SegmentStage(master, Stages::stageName(id),
					    ifs,
					    rv,
                                            frameRange,
                                            inputFileStem);
      break;		
    case(Stages::GSR_STAGE):
      s = (Stage *)new GetSalientRegionsStage(master, Stages::stageName(id), 
                                              argc, argv,
                                              beowulf,
					      wta,
					      sm,
					      agm,
					      seq,
					      sem,
					      foaRadius, 
					      levelSpec,
                                              boringmv,
                                              boringDelay,
                                              normType,
                                              wts, scaleW, scaleH);
      break;
    case(Stages::UE_STAGE):    
      s = (Stage *)new UpdateEventsStage(master, Stages::stageName(id), 
                                         ifs,
                                         rv,
                                         inputFileStem,
                                         frameRange);
      break;
    default:
      return 0;
      break;
    }
    return s;
  }
}
// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
