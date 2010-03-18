// segment.h: interface for the Segment class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(SEGMENT_H)
#define SEGMENT_H

#include "Stages/Stage.H"
#include "Image/Image.H"
#include "Image/ImageCache.H"
#include "DetectionAndTracking/Segmentation.H"
#include "DetectionAndTracking/DetectionParameters.H"

// ######################################################################
//! Handles segmenting objects from the background for use in the UpdateEventsStage
class SegmentStage: public Stage
{
public:		

  // !Constructor
  SegmentStage(MPI_Comm mastercomm, const char *name, const DetectionParameters &detectionParms);

  // !Destructor
  virtual ~SegmentStage();
	
  // !custom SegmentStage run method.
  virtual void runStage();

  // !custom shutdown. Sends MSG_EXIT to stages pending on this data
  virtual void shutdown();
private:
  ImageCacheAvg< byte > itsbwAvgCache;
  Segmentation itsSegmentation;
};

#endif // !defined(SEGMENT_H)
