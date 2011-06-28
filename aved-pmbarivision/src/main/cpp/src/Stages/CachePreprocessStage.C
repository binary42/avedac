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

#include "Stages/CachePreprocessStage.H"
#include "PipelineControl/PipelineController.H"
#include "MessagePassing/Mpimessage.H"
#include "Image/FilterOps.H"
#include "Image/MbariImage.H"
#include "Data/MbariMetaData.H"

#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <iostream>
#include <fstream>
#include <sstream>
#include <signal.h>

using namespace std;


CachePreprocessStage::CachePreprocessStage(MPI_Comm mastercomm, 
                                           const char *name,
                                           nub::soft_ref<InputFrameSeries> &ifs,
                                           nub::soft_ref<MbariResultViewer> &rv,
                                           const FrameRange framerange,
                                           const std::string& inputFileStem )
  :Stage(mastercomm,name),
   itsifs(ifs),
   itsFrameRange(framerange),
   itsAvgCache(ImageCacheAvg< PixRGB<byte> > (DetectionParametersSingleton::instance()->itsParameters.itsSizeAvgCache)),
   itsInputFileStem(inputFileStem),
   itsRv(rv)
{
  preload();	
}

CachePreprocessStage::~CachePreprocessStage()
{
}

void CachePreprocessStage::runStage()
{
  DetectionParameters detectionParms = DetectionParametersSingleton::instance()->itsParameters;
  Image< PixRGB<byte> > img, img2runsaliency; 
  MPI_Status s;
  MPI_Request r;	
  int curFrame = itsFrameRange.getFirst();
  int countFrameDist = 1;  
  int cacheFrameNum;
  uint numFrameDist = detectionParms.itsSaliencyFrameDist;
  string tc;
  MbariImage< PixRGB<byte> > mbariImg(itsInputFileStem);
  
  LINFO("Running stage %s", Stage::name());  
  
  do {

    while ( curFrame <=  itsFrameRange.getLast() )
      {
        MPE_Log_event(1,0,"");
        
        cacheFrameNum = curFrame - itsFrameRange.getFirst();
        
        // get image from cache or load 
        if (cacheFrameNum < itsAvgCache.size()) {
          // we have cached this guy already
          img = itsAvgCache[cacheFrameNum];
        }
        else
          {	
            if (itsifs->frame() >= itsFrameRange.getLast()) {
	        LERROR("Less input frames than necessary for sliding average - "
		     "using all the frames for caching.");
		break;
	    }	
            itsifs->updateNext();
            img = itsifs->readRGB();

	   // if there is little deviation do not add to the average cache
           if (stdev(luminance(img)) <= 5.0f){
             LINFO("No standard deviation in frame %d. Is this frame all black ? Not including this image in the average cache", itsifs->frame());
             itsAvgCache.push_back(itsAvgCache.mean());
           }
           else
             itsAvgCache.push_back(img);
 
	   // Get the MBARI metadata from the frame if it exists
            mbariImg.updateData(img, curFrame);
            tc = mbariImg.getMetaData().getTC();			
            
            if(tc.length() > 0) 
              LINFO("Caching frame %06d timecode: %s", curFrame, tc.c_str());
            else
              LINFO("Caching frame %06d", curFrame);
          }
        
 	// Get the saliency input image
        if ( detectionParms.itsSaliencyInputType == SIDiffMean) {
            if (detectionParms.itsSizeAvgCache > 1)
              img2runsaliency = itsAvgCache.clampedDiffMean(img);
            else
              LFATAL("ERROR - must specify an imaging cache size "
                      "to use the DiffMean option. Try setting the"
                      "--mbari-cache-size option to something > 1");
        }
        else if (detectionParms.itsSaliencyInputType == SIRaw) {
             img2runsaliency = img;
        }
        else {
             img2runsaliency = itsAvgCache.clampedDiffMean(img);
        } 

        itsRv->output(img, curFrame, "Input"); 
        itsRv->output(img2runsaliency, curFrame, "Saliency_input"); 
        MPE_Log_event(2,0,"");		
        
        //every frame send diffed data to segment stage
        LDEBUG("%s sending message MSG_DATAREADY to: %s", Stage::name(), stageName(Stages::SG_STAGE));        
        if(sendRGBByteImage(img, curFrame, Stages::SG_STAGE,  Stage::MSG_DATAREADY, Stage::mastercomm()) == -1) {
          LINFO("Error sendingByteImage to %s", stageName(Stages::SG_STAGE));
          break;		
        }	
        
        //every numFrameDist send frames to GSR stage
        --countFrameDist;
        if (countFrameDist == 0) {
          countFrameDist = numFrameDist;
          LDEBUG("%s sending message MSG_DATAREADY to: %s", Stage::name(), stageName(Stages::GSR_STAGE));
          if(sendRGBByteImage(img2runsaliency, curFrame, Stages::GSR_STAGE,  Stage::MSG_DATAREADY, Stage::mastercomm()) == -1) {
            LINFO("Error sendingRGBByteImage");
            break;
          }          
        }
        curFrame++;
       	
      } //end while ( curFrame <=  itsFrameRange.getLast() )
  }// end do
    while (!probeMasterForExit());

    int flag = 1;
    //GSR, CP may be pending on message from CP stage; send EXIT message to interrupt
    MPI_Isend(&flag, 1, MPI_INT, Stages::SG_STAGE, Stage::MSG_EXIT, Stage::mastercomm(), &r);
    MPI_Isend(&flag, 1, MPI_INT, Stages::GSR_STAGE, Stage::MSG_EXIT, Stage::mastercomm(), &r);
}

void CachePreprocessStage::shutdown() {
}

void CachePreprocessStage::preload() {
    MbariImage< PixRGB<byte> > mbariImg(itsInputFileStem);
    Image< PixRGB<byte> > img;
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

    // pre-load a few frames to get a valid average
    while (itsAvgCache.size() < dp.itsSizeAvgCache) {
        if (itsifs->frame() >= itsFrameRange.getLast()) {
            LERROR("Less input frames than necessary for sliding average - "
                    "using all the frames for caching.");
            break;
        }
        itsifs->updateNext();
        img = itsifs->readRGB();

        // get the standard deviation in the input image
        // if there is little deviation do not add to the average cache
        if (stdev(luminance(img)) <= 5.0f) {
            LINFO("No standard deviation in frame %d. Is this frame all black ? Not including this image in the average cache", itsifs->frame());
            itsAvgCache.push_back(itsAvgCache.mean());
        } else
            itsAvgCache.push_back(img);

        mbariImg.updateData(img, itsifs->frame());
        if (mbariImg.getMetaData().getTC().length() > 0)
            LINFO("Caching frame %06d timecode: %s", itsifs->frame(), mbariImg.getMetaData().getTC().c_str());
        else
            LINFO("Caching frame %06d", itsifs->frame());
    }
}
