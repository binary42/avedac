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
#include "Neuro/NeuroOpts.H"
#include "Neuro/VisualCortex.H"
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
const int maxSizeFactor = 200;
const float maxEvolveTime = 0.5F;
const uint maxNumSalSpots = 20;
// get image dimensions and set a few paremeters that depend on it
const int minSizeRatio = 10000;
const int maxDistRatio = 40;
const int foaSizeRatio = 19;
const int circleRadiusRatio = 40;
string inputFilterScript; 

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
    int size = 0;
    Stage *s = 0;
    bool displayResults = false;	
    bool displayOutput = false;
    // by default we only use the fast beo version, but for comparing
    // the results of the fast and slow beo, we can change this to false which will use the slower
    // saliency computation.     
    bool fastBeoSaliency = true;
    char *scratchdir = NULL;

    LINFO("Creating stage:%s id: %d", Stages::stageName(id), id);

    DetectionParameters detectionParms = DetectionParametersSingleton::instance()->itsParameters ;  
  
    nub::soft_ref<InputFrameSeries> ifs(new InputFrameSeries(manager));
    manager.addSubComponent(ifs);			
    nub::soft_ref<OutputFrameSeries> ofs(new OutputFrameSeries(manager));
    manager.addSubComponent(ofs);  
    nub::soft_ref<DetectionParametersModelComponent> dp(new DetectionParametersModelComponent(manager));
    manager.addSubComponent(dp);
    nub::soft_ref<OutputFrameSeries> evtofs(new OutputFrameSeries(manager));
    manager.addSubComponent(evtofs);

    // Get the binary directory of this executable
    string exe(argv[0]);
    size_t found = exe.find_last_of("/\\");
   
    nub::soft_ref<MbariResultViewer> rv(new MbariResultViewer(manager, evtofs, ofs, exe.substr(0,found)));
    manager.addSubComponent(rv);

    // Request mbari specific option aliases
    REQUEST_OPTIONALIAS_MBARI(manager);

    // Request toolkit option aliases
    REQUEST_OPTIONALIAS_NEURO(manager);
 
    // add a brain here so we can get the command options
    nub::ref<StdBrain> brain(new StdBrain(manager));
    manager.addSubComponent(brain);
    manager.setOptionValString(&OPT_MRVsaveNonInterestingEvents,"true");
    
    //create master Beowulf component
    nub::soft_ref<Beowulf> beowulf(new Beowulf(manager, "Beowulf", "Beowulf", true));
    beowulf->exportOptions(OPTEXP_ALL);
 
    // parse the command line
    if(manager.parseCommandLine(argc, argv, "", 0, -1) == false) 
 	LFATAL("Invalid command line argument. Aborting program now !");
 
    // set the range to be the same as the input frame range
    FrameRange fr = ifs->getModelParamVal<FrameRange>("InputFrameRange");
    ofs->setModelParamVal(string("OutputFrameRange"), fr);
 
    // remove the brain - we don't use a brain here, but we needed to add it 
    // as a simple way to advertise the command options that are then passed to
    // getSalientWinners() where the StdBrain is actually created and used
    manager.removeSubComponent(*brain);        
   
    // get image dimensions and set a few paremeters that depend on it
    const Dims dims = ifs->peekDims();	
    manager.setOptionValString(&OPT_InputFrameDims, convertToString(dims));
    const int circleRadius = dims.w() / circleRadiusRatio;
    const int maxDist = dims.w() / maxDistRatio;
    const int foaSize = dims.w() / foaSizeRatio;
    char str[256]; sprintf(str,"%d",foaSize);	
    manager.setOptionValString(&OPT_FOAradius,str);
    const int minSize = foaSize;
    const int maxSize = minSize * maxSizeFactor;	
	
    // initialize derived detection parameters
    detectionParms.itsMaxDist = maxDist; //pixels

    if (detectionParms.itsMinEventArea == DEFAULT_MIN_EVENT_AREA)
    detectionParms.itsMinEventArea = minSize; //sq pixels
    if (detectionParms.itsMaxEventArea == DEFAULT_MAX_EVENT_AREA)
    detectionParms.itsMaxEventArea = maxSize; //sq pixels
	
    // calculate cost parameter from other parameters
    float maxDistFloat = (float) maxDist;
    float maxAreaDiff = pow((double)maxDistFloat,2) / (double)4.0;
    detectionParms.itsMaxCost = (float) maxDist/2*maxAreaDiff;
    if(detectionParms.itsTrackingMode == TMKalmanFilter)
      detectionParms.itsMaxCost = pow((double)maxDistFloat,2) + pow((double)maxAreaDiff,2);
		
    // disable model manager option for all stages because Xdisplay options not enabled in parallel code.
    manager.setOptionValString(&OPT_MRVdisplayOutput,"false");
    manager.setOptionValString(&OPT_MRVdisplayResults,"false"); 
  
    // start all our ModelComponent instances  
    manager.start();
     
    // initialize detection parameters
    dp->reset(&detectionParms);
    DetectionParametersSingleton::initialize(detectionParms);
  
    // get the frame range
    FrameRange frameRange = FrameRange::fromString(manager.getOptionValString(&OPT_InputFrameRange));
    std::string inputFileStem = manager.getOptionValString(&OPT_InputFrameSource).c_str();
  
    switch(id)	{
    case(Stages::CP_STAGE):            
      s = (Stage *)new CachePreprocessStage(master, Stages::stageName(id), 
                                            ifs, 
                                            detectionParms,
                                            frameRange,
                                            inputFileStem);
      break;
    case(Stages::SG_STAGE):
      s = (Stage *)new SegmentStage(master, Stages::stageName(id), detectionParms);
      break;		
    case(Stages::GSR_STAGE):	     
      s = (Stage *)new GetSalientRegionsStage(master, Stages::stageName(id), 
                                              argc, argv,
                                              beowulf,
                                              maxEvolveTime,
                                              maxNumSalSpots,
                                              fastBeoSaliency);
      break;
    case(Stages::UE_STAGE):    
      s = (Stage *)new UpdateEventsStage(master, Stages::stageName(id),
                                         detectionParms,
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