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
#include "Stages/SegmentStage.H"
#include "PipelineControl/PipelineController.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/Segmentation.H"
#include "Image/FilterOps.H"
#include "Image/MathOps.H"
#include "Image/ColorOps.H"
#include "Image/Transforms.H"
#include "Image/ImageCache.H"
#include "Image/MbariImage.H"

#include "Raster/RasterFileFormat.H"
#include "MessagePassing/Mpidef.H"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////
SegmentStage::SegmentStage(MPI_Comm mastercomm, const char *name, 
	               nub::soft_ref<InputFrameSeries> &ifs,
	               nub::soft_ref<MbariResultViewer> &rv,
                       const FrameRange framerange,
                       const std::string& inputFileStem)
  :Stage(mastercomm,name), 
   itsbwAvgCache(ImageCacheAvg< byte > (DetectionParametersSingleton::instance()->itsParameters.itsSizeAvgCache)),
   itsifs(ifs),
   itsRv(rv),
   itsFrameRange(framerange),
   itsAvgCache(ImageCacheAvg< PixRGB<byte> > (DetectionParametersSingleton::instance()->itsParameters.itsSizeAvgCache)),
   itsInputFileStem(inputFileStem)
{	
    preload();
}

SegmentStage::~SegmentStage()
{
   
}
void SegmentStage::runStage()
{	
  int exit = 0;
  int flag = 1;
  MPI_Status status;	
  MPI_Request request;
  Image< PixRGB<byte> > *img;
  Image< byte > img2segment;

  int framenum = -1;	
  DetectionParameters detectionParms = DetectionParametersSingleton::instance()->itsParameters;
  Segmentation segmentation;
	
  LINFO("Running stage %s", Stage::name()); 
	
  do {   
    framenum = receiveData((void**)&img, RGBBYTEIMAGE, Stages::CP_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &status, &request);
    Stages::stageID id = static_cast<Stages::stageID>(status.MPI_SOURCE);				
    LDEBUG("%s received frame: %d MSG_DATAREADY from: %s", Stage::name(), framenum, Stages::stageName(Stages::CP_STAGE));
            
    switch (status.MPI_TAG)		{			
    case(Stage::MSG_EXIT):
      LDEBUG("%s received MSG_EXIT from %s", Stage::name(), Stages::stageName(id));
      exit = 1;
      break;									
    case(Stage::MSG_DATAREADY):
      MPE_Log_event(3,0,"");

      if(framenum != -1)  { 
          	// Create the binary image to segment
 	if (detectionParms.itsSegmentAlgorithmInputType == SAIMaxRGB) {
          img2segment = maxRGB(itsAvgCache.absDiffMean(*img));
	}
        else if (detectionParms.itsSegmentAlgorithmInputType == SAILuminance) {
	  img2segment = luminance(*img);
	}
        else {
          img2segment = maxRGB(itsAvgCache.absDiffMean(*img));
	}

 	itsRv->output(img2segment, framenum, "Segment_input"); 

        itsbwAvgCache.push_back(img2segment);

	 // create a binary image for the segmentation
        Image<byte> bitImg;
	const Image <PixRGB <byte> > background = itsAvgCache.mean();

        //  Run selected segmentation algorithm
        if (detectionParms.itsSegmentAlgorithm == SABackgroundCanny)
            bitImg = segmentation.runBackgroundCanny(img2segment, segmentAlgorithmType(SABackgroundCanny));
        else if (detectionParms.itsSegmentAlgorithm == SAHomomorphicCanny)
            bitImg = segmentation.runHomomorphicCanny(img2segment, segmentAlgorithmType(SAHomomorphicCanny));
        else if (detectionParms.itsSegmentAlgorithm == SAAdaptiveThreshold)
            bitImg = segmentation.runAdaptiveThreshold(img2segment, segmentAlgorithmType(SAAdaptiveThreshold));
        else if (detectionParms.itsSegmentAlgorithm == SAExtractForegroundBW) {
            bitImg = segmentation.runGraphCut(img2segment, background, segmentAlgorithmType(SAExtractForegroundBW));
	} 
	else if (detectionParms.itsSegmentAlgorithm == SABinaryAdaptive) { 
	if (detectionParms.itsSizeAvgCache > 1) { 
	  bitImg = segmentation.runBinaryAdaptive(itsbwAvgCache.clampedDiffMean(img2segment),
						  img2segment, detectionParms.itsTrackingMode);
           }
        else 
	  bitImg = segmentation.runBinaryAdaptive(img2segment,
						  img2segment, detectionParms.itsTrackingMode);
	} 
	else {
	  if (detectionParms.itsSizeAvgCache > 1)
	    bitImg = segmentation.runBinaryAdaptive(itsbwAvgCache.clampedDiffMean(img2segment), img2segment,
						    detectionParms.itsTrackingMode);
	  else
	    bitImg = segmentation.runBinaryAdaptive(img2segment, img2segment,
						    detectionParms.itsTrackingMode);
	}
	//send byte image to UpdateEvents stage to start work
        sendByteImage(bitImg, framenum, Stages::UE_STAGE, MSG_DATAREADY, Stage::mastercomm());
          
        delete img;	
        MPE_Log_event(4,0,"");        
      } 
      break;
    default:
      LDEBUG("%s received %d frame: %d from: %s", Stage::name(), framenum, status.MPI_TAG, stageName(id));
      break;
    }
  }
  while (!exit  && !probeMasterForExit());	  
   
  //UE may be pending on message from CP stage; send EXIT message to interrupt 
  MPI_Isend( &flag, 1, MPI_INT, Stages::UE_STAGE, Stage::MSG_EXIT, Stage::mastercomm(), &request );
}
void SegmentStage::shutdown()
{
}
void SegmentStage::preload()
{
  Image< PixRGB<byte> > img;
  DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

  // pre-load a few frames to get a valid average
  while(itsAvgCache.size() < dp.itsSizeAvgCache) {
    if (itsifs->frame() >= itsFrameRange.getLast())
      {
        LERROR("Less input frames than necessary for sliding average - "
               "using all the frames for caching.");
        break;
      }
    itsifs->updateNext();
    img = itsifs->readRGB();
	   // get the standard deviation in the input image
           // if there is little deviation do not add to the average cache
    	   if (stdev(luminance(img)) <= 5.f && itsAvgCache.size() > 0){ 
    		itsAvgCache.push_back(itsAvgCache.mean()); 
	   }
	   else {
    		itsAvgCache.push_back(img);
	   } 
         
    LINFO("Caching frame %06d", itsifs->frame());
  } 
}


