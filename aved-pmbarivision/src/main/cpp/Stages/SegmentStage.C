// stage.cpp: implementation of the stage class.
//
//////////////////////////////////////////////////////////////////////
#include "MessagePassing/Mpimessage.H"
#include "Stages/SegmentStage.H"
#include "PipelineControl/PipelineController.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "Image/FilterOps.H"
#include "Image/MathOps.H"
#include "Image/ColorOps.H"
#include "Image/Transforms.H"
#include "Image/ImageCache.H"
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
                           const DetectionParameters &detectionParms)
  :Stage(mastercomm,name),
   itsbwAvgCache(ImageCacheAvg< byte > (detectionParms.itsSizeAvgCache))
{				
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
  Image< PixRGB<byte> > *img2segment; 
  BitObject obj;
  int framenum = -1;	
  DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
	
  LINFO("Running stage %s", Stage::name());
   	
  do {   
    framenum = receiveData((void**)&img2segment, RGBBYTEIMAGE, Stages::CP_STAGE, MPI_ANY_TAG, Stage::mastercomm(), &status, &request);
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
        Image<byte> bwImg;
              
        // create bw and binary versions of the img					
        if(dp.itsTrackingMode == TMKalmanFilter)
          bwImg = maxRGB(*img2segment);
        else
          bwImg = luminance(*img2segment);
	LINFO("2) DEBUG %06d", framenum);;
        itsbwAvgCache.push_back(bwImg);
            
        // create a binary image for the segmentation
        Image<byte> bitImg;

        //  Run selected segmentation algorithm
	  if (dp.itsSegmentAlgorithm == SABackgroundCanny)          
            bitImg = itsSegmentation.runBackgroundCanny(itsbwAvgCache.clampedDiffMean(bwImg), segmentAlgorithmType(SABackgroundCanny));
	  else if (dp.itsSegmentAlgorithm == SAHomomorphicCanny)
            bitImg = itsSegmentation.runHomomorphicCanny(itsbwAvgCache.clampedDiffMean(bwImg),segmentAlgorithmType(SAHomomorphicCanny));
	  else if (dp.itsSegmentAlgorithm == SAAdaptiveThreshold)
            bitImg = itsSegmentation.runAdaptiveThreshold(itsbwAvgCache.clampedDiffMean(bwImg),segmentAlgorithmType(SAAdaptiveThreshold)); 
	  else if (dp.itsSegmentAlgorithm == SAExtractForegroundBW)        
            bitImg = itsSegmentation.runGraphCut(itsbwAvgCache.clampedDiffMean(bwImg), bwImg, segmentAlgorithmType(SAExtractForegroundBW)); 
          else if (dp.itsSegmentAlgorithm == SABinaryAdaptive)       
	    bitImg = itsSegmentation.runBinaryAdaptive(itsbwAvgCache.clampedDiffMean(bwImg), bwImg, dp.itsTrackingMode);
          else 
	    bitImg = itsSegmentation.runBinaryAdaptive(itsbwAvgCache.clampedDiffMean(bwImg), bwImg, dp.itsTrackingMode);
        
	//TODO: insert average cache size > 1 logis here 
        //send byte image to UpdateEvents stage to start work
        sendByteImage(bitImg, framenum, Stages::UE_STAGE, MSG_DATAREADY, Stage::mastercomm());
          
        delete img2segment;	
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


// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */

