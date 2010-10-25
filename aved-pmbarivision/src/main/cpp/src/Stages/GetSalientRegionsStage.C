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

#include "MessagePassing/Mpimessage.H" 
#include "Stages/GetSalientRegionsStage.H"
#include "PipelineControl/PipelineController.H"
#include "Image/BitObject.H"
#include "Neuro/WTAwinner.H"
#include "Neuro/NeuroSimEvents.H"
#include "Media/MediaSimEvents.H"
#include "Media/SimFrameSeries.H"
#include "Raster/Raster.H"
#include "Image/ColorOps.H"
#include "Image/fancynorm.H"
#include "Simulation/SimEventQueue.H"
#include "Simulation/SimEvents.H"
#include "Util/StringConversions.H"
#include "Data/MbariOpts.H"
#include "Parallel/pvisionTCP-defs.H"
#include "Util/Timer.H"
#include "Image/Image.H"
#include "Image/ColorOps.H"
#include "Image/DrawOps.H"
#include "Image/ShapeOps.H"
#include "Image/FilterOps.H"
#include "Image/MathOps.H"
#include "Image/Transforms.H"
#include "Image/ImageSet.H"
#include "Raster/RasterFileFormat.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "Image/colorDefs.H"
#include "Stages/SalientWinner.H"
#include "Utils/Const.H"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <iostream>
#include <string>
#include <iostream>

using namespace std;

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////
GetSalientRegionsStage::GetSalientRegionsStage(MPI_Comm mastercomm, const char *name, \
                                               const int argc, const char **argv,
                                               nub::soft_ref<Beowulf> &beo,
                                               nub::soft_ref<WinnerTakeAll> &wta,
					       nub::soft_ref<SimEventQueue> &seq,
                                               const LevelSpec &levelSpec,
                                               const float boringmv,
                                               const SimTime &boringDelay,
                                               const MaxNormType &maxNormType,
                                               const VisualCortexWeights &wts)
  :Stage(mastercomm,name),
   itsArgc(argc),
   itsArgv(argv),
   itsBeo(beo),
   itsWta(wta),
   itsSeq(seq),
   itsLevelSpec(levelSpec),
   itsMaxNormType(maxNormType),
   itsBoringmv(boringmv),
   itsBoringDelay(boringDelay),
   itsWeights(wts),
   itsTestGrayscale(true),
   itsGrayscaleCompute(false)
{
 
}
GetSalientRegionsStage::~GetSalientRegionsStage()
{
}

void GetSalientRegionsStage::initStage()
{
  if(!itsBeo->started())
    itsBeo->start();     
}

void GetSalientRegionsStage::runStage()
{
  int exit = 0;  
  Image< PixRGB<byte> > *img; 	
  MPI_Status status;
  MPI_Request request;
  int framenum = -1;
   
  LINFO("Running stage %s", Stage::name());    

  do {		

    framenum = receiveData((void**)&img, RGBBYTEIMAGE, Stages::CP_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &status, &request);
    Stages::stageID id = static_cast<Stages::stageID>(status.MPI_SOURCE);
    LDEBUG("%s received frame: %d MSG_DATAREADY from Source: %d", Stage::name(), framenum, status.MPI_SOURCE);
    
    switch (status.MPI_TAG)		{			
    case(Stage::MSG_EXIT):				
      LDEBUG("%s received MSG_EXIT from %s", Stage::name(), Stages::stageName(id));
      exit = 1;
      break;									
    case(Stage::MSG_DATAREADY):
        
      // get saliency winners and send list of winners to the Stages::UE_STAGE
      if(framenum != -1) {
        MPE_Log_event(5,0,"");	 

        // test for grayscale or color image. this is later used to
        // remove the r/g b/w color map computations for speedup
	if(itsTestGrayscale == true) {
	  itsGrayscaleCompute = isGrayscale(*img);
	  itsTestGrayscale = false;
	}

        // send the image to the beowulf worker nodes
        sendImage(*img, framenum);

        // get the winners back
        std::list<WTAwinner> winners = getWinners(*img, framenum);
	std::list<SalientWinner> salwinners;

        // initialize salwinners list for sending through mpi message to US_STAGE
        if(winners.size() > 0) {
          std::list<WTAwinner>::iterator i;
          
          for (i = winners.begin(); i != winners.end(); ++i)
            salwinners.push_back(SalientWinner(i->p, i->sv));


          // send winner list to Stages::UE_STAGE
          sendSalientWinnerList(salwinners, framenum, Stages::UE_STAGE, MSG_DATAREADY, Stage::mastercomm());
          salwinners.clear();
        }
        else
        {
          // send empty winner list to Stages::UE_STAGE
          sendSalientWinnerList(salwinners, -1, Stages::UE_STAGE, MSG_DATAREADY, Stage::mastercomm());
        }
         
        delete img;	
	winners.clear();
	MPE_Log_event(6,0,"");	
      }
      break;
    default:
      LDEBUG("%s received frame: %d  MSG: %d from Source: %d", Stage::name(), framenum,
             status.MPI_TAG, id);			
      break;		
    }
  }
  while (!exit && !probeMasterForExit());	      
  
}

void GetSalientRegionsStage::shutdown()
{
  int flag=1;  
  MPI_Request request;  
     
  //Stages::UE_STAGE may be pending on message from GSR stage; send EXIT message to interrupt 
  MPI_Isend( &flag, 1, MPI_INT, Stages::UE_STAGE, Stage::MSG_EXIT, Stage::mastercomm(), &request );	      

  if(itsBeo->started())
    itsBeo->stop();
}

// ######################################################################
        
std::list<WTAwinner> GetSalientRegionsStage::getSalientWinnersNew(const Image< PixRGB<byte> > &img, 
							     int framenum,
							     int sml,
							     Image<float> &sm,
							     Image<float> &intensity,
							     Image<float> &color,
							     Image<float> &orientation)
{
  Point2D<int> currwin(-1,-1);
  int numSpots = 0;
  std::list<WTAwinner> winners;   
  SimStatus status = SIM_CONTINUE;
  DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    
  // get the standard deviation in the input image
  // if there is no deviation, this image is uniform and
  // will have no saliency so return empty winners    
  if (stdev(luminance(img)) == 0.f) {
    LINFO("######no standard deviation in luminance so no winners will be found in frame %d ####", framenum );
    return winners;
  }

  // initialize the max time to simulate
  const SimTime simMaxEvolveTime = itsSeq->now() + SimTime::MSECS(p.itsMaxEvolveTime);
  
  rutz::shared_ptr<SimEventAttentionGuidanceMapOutput>
    agm(new SimEventAttentionGuidanceMapOutput(NULL, sm));
    itsSeq->post(agm); 
  
  while (status == SIM_CONTINUE) {
 
     itsWta->evolve(*itsSeq); 
 
    // switch to next time step:
    status = itsSeq->evolve();
    
    LINFO("######Evolve time now: %f secs", itsSeq->now().secs());
    
    if (SeC<SimEventWTAwinner> e = itsSeq->check<SimEventWTAwinner > (0)) {
      WTAwinner newwin = e->winner();
      LINFO("##### winner #%d found at [%d; %d] with %f voltage frame: %d#####",
	    numSpots, newwin.p.i, newwin.p.j, newwin.sv, framenum);
      
      // if a boring event detected, and not keeping boring WTA points then break simulation
      if (newwin.boring && p.itsKeepWTABoring == false) { 
	rutz::shared_ptr<SimEventBreak>
	  e(new SimEventBreak(0, "Boring event detected"));
	itsSeq->post(e);
      } else {
	winners.push_back(newwin);
	++numSpots;
	// scan channels, finding the max      
	float mx = -1.0F; int bestindex = -1;
	for (uint i = 0; i < 3; ++i)      {      	
	  if(currwin.i != -1) {
	    float curr_val = 0.0f;
	    switch(i) {
	    case(0):        			
	      curr_val = intensity.getVal(currwin);
	      break;
	    case(1):
	      curr_val = color.getVal(currwin);
	      break;
	    case(2):
	      curr_val = orientation.getVal(currwin);		
	      break;
	    default:
	      curr_val =  0.0f;
	      break;
        }		          
	    // do we have a new max?
	    if (curr_val >= mx) { mx = curr_val; bestindex = i; }
	  }
	}
	
	//mask max object
	if(bestindex > -1) {
	  Image<float> winMapNormalized;
	  switch(bestindex) {
	  case(0):
	    LINFO("Segmenting object around (%d,%d) in intensity", newwin.p.i, newwin.p.j);
	    winMapNormalized = intensity;
	    break;
	  case(1):
	    LINFO("Segmenting object around (%d,%d) in color", newwin.p.i, newwin.p.j);
	    winMapNormalized = color;
          break;
	  case(2):
	    LINFO("Segmenting object around (%d,%d) in orientation", newwin.p.i, newwin.p.j);
	    winMapNormalized = orientation;		
	    break;
	  default:
	    LINFO("Segmenting object around (%d,%d) in orientation", newwin.p.i, newwin.p.j);
	    winMapNormalized = orientation;		
	    break;
	  }
	  inplaceNormalize(winMapNormalized, 0.0F, 1.0F);
	  Image<byte> objectMask = segmentObjectClean(winMapNormalized, currwin);
	  inplaceSetValMask(sm,objectMask, 0.0F);
	}
      }
    }
    else {
    LINFO("######No winner found time now %f", itsSeq->now().secs());
    }
      if (numSpots >= p.itsMaxWTAPoints) {
          LINFO("#### found maximum number of saliency spots %d", p.itsMaxWTAPoints);
	rutz::shared_ptr<SimEventBreak>
	  e(new SimEventBreak(0, "##### found maximum number of salient spots #####"));
	itsSeq->post(e);
      }

      if (itsSeq->now().msecs() >= simMaxEvolveTime.msecs()) {
	LINFO("##### time limit reached time now:%f sec  max evolve time:%f sec #####", itsSeq->now().secs(), simMaxEvolveTime.secs());
	rutz::shared_ptr<SimEventBreak>
	  e(new SimEventBreak(0, "##### time limit reached #####"));
	itsSeq->post(e);
      } 
  }

  // print final memory allocation stats
  LINFO("Simulation terminated. Found %d numspots", numSpots);

  return winners;
}
 
list<WTAwinner> GetSalientRegionsStage::getWinners(const Image< PixRGB<byte> > &img, int framenum)
{ 
  std::list<WTAwinner> winners;   
  Image<float> cmap[NBCMAP];       // array of conspicuity maps
  int32 cmapframe[NBCMAP];         // array of cmap frame numbers  
  for (int i = 0; i < NBCMAP; i ++) cmapframe[i] = -1;
  int sml = itsLevelSpec.mapLevel();                      // pyramid level of saliency map
 
  Image<float> sm(img.getWidth() >> sml, img.getHeight() >> sml, ZEROS); // saliency map
  Image<float> color = sm;
  Image<float> intensity = sm;
  Image<float> ori = sm;

  int reccmaps = 0;
  int numcmaps = NBCMAP;	
  Timer masterclock;                // master clock for simulations
  masterclock.reset();
  float mi,ma;  
  
     // remove the r/g b/w color map computations if gray scale image
    if (itsGrayscaleCompute == true)
      itsWeights.chanCw = 0.f;

    // if no channel weight for intensity, color, or orientation
    // remove computation
    if (itsWeights.chanIw == 0.f )
      numcmaps -= 1;

    if (itsWeights.chanOw == 0.f )
      numcmaps -= 4;
   
    if (itsWeights.chanCw == 0.f)
      numcmaps -= 2;

    // receive conspicuity maps:
    while(reccmaps < numcmaps) {
      reccmaps += receiveCMAPS(itsBeo, cmap, cmapframe);
      usleep(500);
    };     
              
    if (itsWeights.chanOw != 0.f ) {
      // add all the orientation channels together, max normalize and weight
      for(int i = 3;i < NBCMAP; i++) {
          LDEBUG("sml: %d image: %dx%d ori: %dx%d cmap: %dx%d", sml, img.getWidth(), img.getHeight(), ori.getWidth(), ori.getHeight(), cmap[i].getWidth(), cmap[i].getHeight());
	ori += cmap[i];
      }
      ori = maxNormalize(ori, 0.0f, 0.0f, itsMaxNormType);
      getMinMax(ori, mi, ma);
      LDEBUG("Orientation final range [%f .. %f]", mi, ma);    	
      ori *= itsWeights.chanOw;
      getMinMax(ori, mi, ma);
      LDEBUG("Orientation final %f weighted range [%f .. %f]", itsWeights.chanOw, mi, ma);
    }
         
    // add in the r/g b/w color map computations if a color image
    if (itsWeights.chanCw != 0.f) {
      // add all the color channels together, max normalize and weight    
      color = cmap[1] + cmap[2];
      color = maxNormalize(color, 0.0f, 0.0f, itsMaxNormType);
      getMinMax(color, mi, ma);
      LDEBUG("Color final range [%f .. %f]", mi, ma);    	
      color *= itsWeights.chanCw;
      getMinMax(color, mi, ma);
      LDEBUG("Color final %f weighted range [%f .. %f]", itsWeights.chanCw, mi, ma);
    
    }
    
    if (itsWeights.chanIw != 0.f ) {
      // maxnormalize and weight intensity channel
      intensity = cmap[0];    
      maxNormalize(intensity,0.0f, 0.0f, itsMaxNormType);
      getMinMax(intensity, mi, ma);
      LDEBUG("Intensity final range [%f .. %f]", mi, ma);
      intensity *= itsWeights.chanIw;
      getMinMax(intensity, mi, ma);
      LDEBUG("Intensity final %f weighted range [%f .. %f]", itsWeights.chanIw, mi, ma);            	
   
    }
    // build our current saliency map
    Image<float> sminput = ori + color + intensity;
    
    getMinMax(sminput, mi, ma);         
    LDEBUG("Raw output range is [%f .. %f]", mi, ma);     
    sminput = maxNormalize(sminput, 0.f, 2.f, itsMaxNormType);
    
    // output is     now typically in the (0.0..8.0) range;
    // typical images are in (0..4) range; we want input current in nA
    //sminput *= 1e-9F;
    //getMinMax(sminput, mi, ma);
    //LINFO("Salmap input range is [%f .. %f] nA", mi * 1.0e9F, ma * 1.0e9F);
    
    // inject saliency map input into saliency map:
    if (sminput.initialized()) 
      sm = sm * 0.7F + sminput * 0.3F;         	 
    
    int minframe = -1;  
    // check for accumulated delay in pvision slaves
    for(int i=1; i < NBCMAP; i++) {
      LDEBUG("cmap: %d ", cmapframe[i]);             
      if(cmapframe[i] != -1 && (cmapframe[i] < minframe) || minframe == -1) 
        minframe = cmapframe[i];
    }
    
    // this is the frame number that corresponds to the oldest frame
    if( (framenum - minframe) > 10)
      LINFO("ERROR: SENDING FRAMES TOO FAST framenum: %d minframe:%d", framenum, minframe);
    
    winners = getSalientWinnersNew(img, framenum, sml, sm, intensity,color,ori);
  
  return winners;
}

list<WTAwinner> GetSalientRegionsStage::getSalientWinnersMaxFast(int sml,
                                                                 Image<float> &sm,
                                                                 Image<float> &intensity,
                                                                 Image<float> &color,
                                                                 Image<float> &orientation)
{	
  int numSpots = 0;
  float maxval;
  Point2D<int> currwin(-1,-1);
  WTAwinner newwin = WTAwinner::NONE(); 
  WTAwinner lastwinner = WTAwinner::NONE();
  std::list<WTAwinner> winners;
  Timer masterclock;                // master clock for simulations
  masterclock.reset(); 

  DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters; 
  
  //itsWta->input(sm);

  do {
    
    // initialize a new winner
    findMax(sm, currwin, maxval);
    //itsWta->doEvolve(masterclock.getSimTime(), currwin);
    //maxval = itsWta->getMaxVal();
 
    //rescale coordinate system
    if (currwin.i != -1) {
      newwin =   WTAwinner::buildFromSMcoords(currwin, sml, true,
                                              masterclock.getSimTime(),
                                              maxval, false);
      
      // if the last covert attention shift was slower than boring Delay(msecs)
      // or the SM voltage was less than (default 3mV), mark the covert attention shift as boring:
      if (lastwinner.t > SimTime::ZERO() &&
          (newwin.t - lastwinner.t > itsBoringDelay || 
              newwin.sv < itsBoringmv * 0.001) )
        newwin.boring = true;                                   
      
      if (newwin.isValid()) {
        LINFO("##### winner #%d found at (%d,%d) with %f mV maxval %f mV at %fms %s #####",
              numSpots, newwin.p.i, newwin.p.j, (newwin.sv/0.001), (maxval/0.001), newwin.t.msecs(),
              newwin.boring ? "[boring] ":"" );
        winners.push_back(newwin);	   
      }
      else {
	LINFO("##### winner #%d invalid !! #####", numSpots);
      }
      
      numSpots++;
      lastwinner = newwin;

       // if a boring event detected, and not keeping boring WTA points then break simulation
      if(newwin.boring == true && p.itsKeepWTABoring == false)
        break;
    
    // scan channels, finding the max      
    float mx = -1.0F; int bestindex = -1;
    for (uint i = 0; i < 3; ++i)      {      	
      if(currwin.i != -1) {
        float curr_val = 0.0f;
        switch(i) {
        case(0):        			
          curr_val = intensity.getVal(currwin);
          break;
        case(1):
          curr_val = color.getVal(currwin);
          break;
        case(2):
          curr_val = orientation.getVal(currwin);		
          break;
        default:
          curr_val =  0.0f;
          break;
        }		          
        // do we have a new max?
        if (curr_val >= mx) { mx = curr_val; bestindex = i; }
      }
    }
    
    //mask max object
    if(bestindex > -1) {
      Image<float> winMapNormalized;
      switch(bestindex) {
      case(0):
        LINFO("Segmenting object around (%d,%d) in intensity", newwin.p.i, newwin.p.j);
        winMapNormalized = intensity;
        break;
      case(1):
        LINFO("Segmenting object around (%d,%d) in color", newwin.p.i, newwin.p.j);
        winMapNormalized = color;
        break;
      case(2):
        LINFO("Segmenting object around (%d,%d) in orientation", newwin.p.i, newwin.p.j);
        winMapNormalized = orientation;		
        break;
      default:
        LINFO("Segmenting object around (%d,%d) in orientation", newwin.p.i, newwin.p.j);
        winMapNormalized = orientation;		
        break;
      }
      inplaceNormalize(winMapNormalized, 0.0F, 1.0F);
      Image<byte> objectMask = segmentObjectClean(winMapNormalized, currwin);
      inplaceSetValMask(sm,objectMask, 0.0F);
    }
    else
      break;

    } 
    else { // end if currwin.i != -1
      LINFO("##### No valid winner found #####");
    }
     
    // find up to itsMaxNumSalSpots and evolve up to maxEvolveTime to make a list
    if (masterclock.getSimTime().msecs() > p.itsMaxEvolveTime || numSpots >= p.itsMaxWTAPoints)    {
      LINFO("##### time limit reached or found maximum number of salient spots %d#####", p.itsMaxWTAPoints);
      break;
    }
    
      LINFO("##### time %f ####  max time %f ", masterclock.getSimTime().secs(), p.itsMaxEvolveTime/1000.0);
  }while(masterclock.getSimTime().msecs() < p.itsMaxEvolveTime);    
  return winners;
}

void GetSalientRegionsStage::sendImage(const Image< PixRGB<byte> >& img, int framenum)
{
  // buffer to send messages to nodes
  TCPmessage smsg;                  

  // remove the r/g b/w color map computations if gray scale image
  if (itsGrayscaleCompute == true)
    itsWeights.chanCw = 0.f;

  // compute luminance and send it off:
  Image<byte> lum = luminance(img);
     
  if (itsWeights.chanOw != 0.f ){
    // first, send off luminance to orientation slaves:
    smsg.reset(framenum, BEO_ORI0); smsg.addImage(lum); itsBeo->send(smsg);
    smsg.setAction(BEO_ORI45); itsBeo->send(smsg);
    smsg.setAction(BEO_ORI90); itsBeo->send(smsg);
    smsg.setAction(BEO_ORI135); itsBeo->send(smsg);
  }
     
  if (itsWeights.chanIw != 0.f) {
    // finally, send to luminance slave:
    smsg.setAction(BEO_LUMINANCE); itsBeo->send(smsg);
  }

 if (itsWeights.chanCw != 0.f) {
   // compute RG and BY and send them off:
   Image<byte> r, g, b, y; getRGBY(img, r, g, b, y, (byte)25);
   smsg.reset(framenum, BEO_REDGREEN);
   smsg.addImage(r); smsg.addImage(g); itsBeo->send(smsg);
   smsg.reset(framenum, BEO_BLUEYELLOW);
   smsg.addImage(b); smsg.addImage(y); itsBeo->send(smsg);
 }
}
// ######################################################################
int GetSalientRegionsStage::receiveCMAPS(nub::soft_ref<Beowulf>& beo, Image<float> *cmap,
                                         int32 *cmapframe)
{ 
  //TODO: Add timeout for cmap receive
  TCPmessage rmsg;      // buffer to receive messages from nodes
  int32 rframe, raction, rnode = -1, recnb=0, reccmaps=0 ; // receive from any node

  while(beo->receive(rnode, rmsg, rframe, raction) )
    {
      LDEBUG("received %d/%d from %d", rframe, raction, rnode);
      switch(raction & 0xffff)
        {
        case BEO_CMAP: // ##############################
          {
            // get the map:
            Image<float> ima = rmsg.getElementFloatIma();            
            
            // the map number is stored in the high 16 bits of the
            // raction field:
            int32 mapn = raction >> 16;
            if (mapn < 0 || mapn >= NBCMAP) {
              LERROR("Bogus cmap number ignored");
  	      break;	      
            }
               
            // here is a totally asynchronous system example: we
            // just update our current value of a given cmap if
            // the one we just received is more recent than the
            // one we had so far:
            if (cmapframe[mapn] < rframe) { 
              cmap[mapn] = ima; 
              cmapframe[mapn] = rframe; 
              reccmaps++;
              LDEBUG("rframe: %d  mapnum: %d reccmaps: %d", rframe, mapn, reccmaps);
            }	      
          }
          
          break;
        default: // ##############################
          LERROR("Bogus action %d -- IGNORING.", raction);
	  break;
        }
      // limit number of receives, so we don't hold CPU too long:
      recnb ++; if (recnb > 3) break;
    }
    
  return reccmaps;
}
