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
#include "Image/MathOps.H"
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
                                           const std::string& inputFileStem,
                                           const Dims &dims)
  :Stage(mastercomm,name),
   itsifs(ifs),
   itsFrameRange(framerange),
   itsAvgCache(ImageCacheAvg< PixRGB<byte> > (DetectionParametersSingleton::instance()->itsParameters.itsSizeAvgCache)),
   itsInputFileStem(inputFileStem),
   itsRv(rv),
   itsDims(dims)
{
  preload();	
}

CachePreprocessStage::~CachePreprocessStage()
{
}

void CachePreprocessStage::runStage()
{
  DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
  Image< PixRGB<byte> > img, img2runsaliency; 
  Image< byte > img2segment; 
  MPI_Status s;
  MPI_Request r;	
  int curFrame = itsFrameRange.getFirst();
  int countFrameDist = 1;  
  int cacheFrameNum;
  uint numFrameDist = dp.itsSaliencyFrameDist;
  float minStdDev = dp.itsMinStdDev;
  string tc;
  float stddev= 0.f;
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

	        // store rgb image in cache; sometimes this fails due to NFS error so retry a few times
                int ntrys = 0;
                do {
                    img = itsifs->readRGB();
                 } while (!img.initialized() && ntrys++ < 3);
        
                if (ntrys == 3)
                     LERROR("Error reading frame %06d after 3 tries", itsifs->frame());

                if (dp.itsMinStdDev > 0.f) {
                    stddev = stdev(luminance(img));
                    LINFO("Standard deviation in frame %d:  %f", itsifs->frame(), stddev);

                    // if there is little deviation do not add to the average cache
                    if (stdev(luminance(img)) <= minStdDev && itsAvgCache.size() > 0) {
                        LINFO("No standard deviation in frame %d too low. Is this frame all black ? Not including this image in the average cache", itsifs->frame());
                        itsAvgCache.push_back(itsAvgCache.mean());
                    } else
                        itsAvgCache.push_back(img);
                } else {
                    itsAvgCache.push_back(img);
                }
 
	   // Get the MBARI metadata from the frame if it exists
            mbariImg.updateData(img, curFrame);
            tc = mbariImg.getMetaData().getTC();			
            
            if(tc.length() > 0) 
              LINFO("Caching frame %06d timecode: %s", curFrame, tc.c_str());
            else
              LINFO("Caching frame %06d", curFrame);
          }
        
	// Create the binary image to segment
 	if (dp.itsSegmentAlgorithmInputType == SAILuminance) {
            img2segment = luminance(img);
	}
        else if (dp.itsSegmentAlgorithmInputType == SAIDiffMean) {
          img2segment = maxRGB(itsAvgCache.clampedDiffMean(img));
	}
        else {
          img2segment = maxRGB(itsAvgCache.clampedDiffMean(img));
	}

 	itsRv->output(img2segment, curFrame, "Segment_input");

 	// Get the saliency input image
        if ( dp.itsSaliencyInputType == SIDiffMean) {
            if (dp.itsSizeAvgCache > 1)
              img2runsaliency = rescale(itsAvgCache.clampedDiffMean(img),itsDims);
            else
              LFATAL("ERROR - must specify an imaging cache size "
                      "to use the --mbari-saliency-input-image=DiffMean option. Try setting the"
                      "--mbari-cache-size option to something > 1");
        }
        else if (dp.itsSaliencyInputType == SIRaw) {
             img2runsaliency = rescale(img, itsDims);
        }
        else {
             img2runsaliency = rescale(img, itsDims);
        } 

        itsRv->output(img, curFrame, "Input");
        itsRv->output(img2runsaliency, curFrame, "Saliency_input"); 
        MPE_Log_event(2,0,"");		
        
        //every frame send diffed data to segment stage
        LDEBUG("%s sending message MSG_DATAREADY to: %s", Stage::name(), stageName(Stages::SG_STAGE));        
        if(sendByteImage(img2segment, curFrame, Stages::SG_STAGE,  Stage::MSG_DATAREADY, Stage::mastercomm()) == -1) {
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
 
	// store rgb image in cache; sometimes this fails due to NFS error so retry a few times
        int ntrys = 0;
        do {
             img = itsifs->readRGB();
         } while (!img.initialized() && ntrys++ < 3);
        
        if (ntrys == 3)
               LERROR("Error reading frame %06d after 3 tries", itsifs->frame());

        // get the standard deviation in the input image
        // if there is little deviation do not add to the average cache
        if (dp.itsMinStdDev > 0.f) {
            float imgstdev = stdev(luminance(img));
            if (imgstdev <= dp.itsMinStdDev && itsAvgCache.size() > 0) {
                LINFO("Standard deviation %f in frame %d. Is this frame all black ? Not including this image in the average cache", imgstdev, itsifs->frame());
                itsAvgCache.push_back(itsAvgCache.mean());
            } else
                itsAvgCache.push_back(img);

        } else {
            itsAvgCache.push_back(img);
        }
        
        mbariImg.updateData(img, itsifs->frame());

        if (mbariImg.getMetaData().getTC().length() > 0)
            LINFO("Caching frame %06d timecode: %s", itsifs->frame(), mbariImg.getMetaData().getTC().c_str());
        else
            LINFO("Caching frame %06d", itsifs->frame());
    }
}
