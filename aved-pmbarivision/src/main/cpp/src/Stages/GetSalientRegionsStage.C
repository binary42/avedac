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
#include "Raster/Raster.H"
#include "Image/ColorOps.H"
#include "Image/fancynorm.H"
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
                                               const LevelSpec &levelSpec,
                                               const MaxNormType &maxNormType,
                                               const VisualCortexWeights &wts,
                                               bool &fastBeoSaliency)
  :Stage(mastercomm,name),
   itsArgc(argc),
   itsArgv(argv),
   itsBeo(beo),
   itsLevelSpec(levelSpec),
   itsMaxNormType(maxNormType),
   itsWeights(wts),
   itsFastBeoSaliency(fastBeoSaliency)
{

}
GetSalientRegionsStage::~GetSalientRegionsStage()
{
}

void GetSalientRegionsStage::initStage()
{
  if(itsFastBeoSaliency == true && !itsBeo->started())
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
      if(framenum != -1)	{
        MPE_Log_event(5,0,"");	 
        
        if(itsFastBeoSaliency == true)
          sendImage(*img, framenum);
	
        std::list<WTAwinner> winners = getWinners(*img, framenum);
		
        if(winners.size() > 0) {
          // initialize salwinners list for sending through mpi message to US_STAGE
          std::list<SalientWinner> salwinners;		
          std::list<WTAwinner>::iterator i;
          
          for (i = winners.begin(); i != winners.end(); ++i)
            salwinners.push_back(SalientWinner(i->p, i->sv)); 
          
          // send winner list to Stages::UE_STAGE
          sendSalientWinnerList(salwinners, framenum, Stages::UE_STAGE, MSG_DATAREADY, Stage::mastercomm());
          salwinners.clear();
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

  if(itsFastBeoSaliency == true && itsBeo->started())
    itsBeo->stop();
}


std::list<WTAwinner> GetSalientRegionsStage::getWinners(const Image< PixRGB<byte> > &img, int framenum)
{
  Point2D<int> winner(-1,-1);
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
  Timer masterclock;                // master clock for simulations
  masterclock.reset();
  float mi,ma,;

  if(itsFastBeoSaliency == true) {
    // receive conspicuity maps:
    while(reccmaps < NBCMAP) {
      reccmaps += receiveCMAPS(itsBeo, cmap, cmapframe);
      usleep(500);      
    };     
              
    // add all the orientation channels together, max normalize and weight
    for(int i = 3;i < NBCMAP; i++)
      ori += cmap[i];     	
    
    ori = maxNormalize(ori, 0.0f, 0.0f, itsMaxNormType);
    getMinMax(ori, mi, ma);
    LDEBUG("Orientation final range [%f .. %f]", mi, ma);    	
    ori *= itsWeights.chanOw;
    getMinMax(ori, mi, ma);
    LDEBUG("Orientation final weighted range [%f .. %f]", mi, ma);
    
    // add all the color channels together, max normalize and weight    
    color = cmap[1] + cmap[2];
    color = maxNormalize(color, 0.0f, 0.0f, itsMaxNormType);
    getMinMax(color, mi, ma);
    LDEBUG("Color final range [%f .. %f]", mi, ma);    	
    color *= itsWeights.chanCw;
    getMinMax(color, mi, ma);
    LDEBUG("Color final weighted range [%f .. %f]", mi, ma);
    
    // maxnormalize and weight intensity channel
    intensity = cmap[0];    
    maxNormalize(intensity,0.0f, 0.0f, itsMaxNormType);
    getMinMax(intensity, mi, ma);
    LDEBUG("Intensity final range [%f .. %f]", mi, ma);
    intensity *= itsWeights.chanIw;
    getMinMax(intensity, mi, ma);
    LDEBUG("Intensity final weighted range [%f .. %f]", mi, ma);            	
    
    // build our current saliency map
    Image<float> sminput = ori + color + intensity;
    
    getMinMax(sminput, mi, ma);         
    LDEBUG("Raw output range is [%f .. %f]", mi, ma);     
    sminput = maxNormalize(sminput, 0.f, 2.f, itsMaxNormType);
    
    // output is now typically in the (0.0..8.0) range;
    // typical images are in (0..4) range; we want input current in nA
    sminput *= 1e-9F;
    getMinMax(sminput, mi, ma);
    LINFO("Salmap input range is [%f .. %f] nA", mi * 1.0e9F, ma * 1.0e9F);      
    
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
    
    winners = getSalientWinnersMaxFast(sml, sm,intensity,color,ori);
  }
  else {
    LINFO("------->UNSUPPORTED FUNCTION FIX THIS-----");
    // DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    //winners = getSalientWinners(itsArgc, itsArgv, img,p.itsMaxEvolveTime,p.itsMaxWTAPoints);
  }
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
  Point2D<int> currwin;
  WTAwinner newwin = WTAwinner::NONE();
  SimTime boringDelay = SimTime::MSECS(200);
  WTAwinner lastwinner = WTAwinner::NONE();
  std::list<WTAwinner> winners;
  Timer masterclock;                // master clock for simulations
  masterclock.reset();

  DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;

  do {
    
    // initialize a new winner
    findMax(sm, currwin, maxval);
    
    //rescale coordinate system
    if (currwin.i != -1) {
      newwin =   WTAwinner::buildFromSMcoords(currwin, sml, true,
                                              masterclock.getSimTime(),
                                              maxval, false);
      
      // if the last covert attention shift was slower than boring Delay(msecs)
      // or the SM voltage was less than 3mV, mark the covert attention shift as boring:
      if (lastwinner.t > SimTime::ZERO() &&
          (newwin.t - lastwinner.t > boringDelay || newwin.sv < 0.003 ) )
        newwin.boring = true;                                   
      
      if (newwin.isValid()) {
        LINFO("##### winner #%d found at (%d,%d) with %f voltage at %fms %s %d #####",
              numSpots, newwin.p.i, newwin.p.j, maxval, newwin.t.msecs(),
              newwin.boring ? "[boring] ":"");
        winners.push_back(newwin);	   
      }
      
      numSpots++;
      lastwinner = newwin;

       // if a boring event detected, and not keeping boring WTA points then break simulation
      if(newwin.boring && p.itsKeepWTABoring == false)
        break;     	
      
    }// end if currwin.i != -1
    
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
    
    // find up to itsMaxNumSalSpots and evolve up to maxEvolveTime to make a list
    if (masterclock.getSimTime().secs() > p.itsMaxEvolveTime || numSpots >= p.itsMaxWTAPoints)    {
      LINFO("##### time limit reached or found maximum number of salient spots %d#####", p.itsMaxWTAPoints);
      break;
    }
    
  }while(1);    
  return winners;
}

void GetSalientRegionsStage::sendImage(const Image< PixRGB<byte> >& img, int framenum)
{
  // buffer to send messages to nodes
  TCPmessage smsg;                  
  
  // prescale image if needed
  int prescale = itsLevelSpec.levMin();
  Image<PixRGB<byte> > ima2;
  if(prescale > 0)
    ima2 = decY(decX(img,1<<prescale),1<<prescale);
  else
    ima2 = img;
  
  // compute luminance and send it off:
  Image<byte> lum = luminance(ima2);
  
  // first, send off luminance to orientation slaves:
  smsg.reset(framenum, BEO_ORI0); smsg.addImage(lum); itsBeo->send(smsg);
  smsg.setAction(BEO_ORI45); itsBeo->send(smsg);
  smsg.setAction(BEO_ORI90); itsBeo->send(smsg);
  smsg.setAction(BEO_ORI135); itsBeo->send(smsg);
  
  // finally, send to luminance slave:
  smsg.setAction(BEO_LUMINANCE); itsBeo->send(smsg);
  
  // compute RG and BY and send them off:
  Image<byte> r, g, b, y; getRGBY(ima2, r, g, b, y, (byte)25);
  smsg.reset(framenum, BEO_REDGREEN);
  smsg.addImage(r); smsg.addImage(g); itsBeo->send(smsg);
  smsg.reset(framenum, BEO_BLUEYELLOW);
  smsg.addImage(b); smsg.addImage(y); itsBeo->send(smsg);
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
      //LINFO("received %d/%d from %d", rframe, raction, rnode);
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
