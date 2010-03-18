// cachepreprocess.h: interface for the CachePreprocess class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(CACHEPREPROCESS_H)
#define CACHEPREPROCESS_H

#include "MessagePassing/Mpidef.H"
#include "Stages/Stage.H"
#include "Media/FrameSeries.H"
#include "Image/ImageCache.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "Media/FrameRange.H"

// ######################################################################
//! Handles caching images and minor preprocessing before sending them to the SegmentStage
class CachePreprocessStage : public Stage  
{
public:
	
  // !constructor
  CachePreprocessStage(MPI_Comm mastercomm, 
		       const char *name,
		       nub::soft_ref<InputFrameSeries> &ifs, 
		       const DetectionParameters &detectionParms,
		       const FrameRange framerange,
		       const std::string& inputFileStem);

  //!desctructor
  virtual ~CachePreprocessStage();

  // !custom Segment run method.
  virtual void runStage();
	
  virtual void shutdown();

  // preloads images to get valid average
  void preload();

private:	
  ImageCacheAvg< PixRGB<byte> > itsAvgCache;
  nub::soft_ref<InputFrameSeries> itsifs;
  FrameRange itsFrameRange; 
  std::string itsInputFileStem;
};

#endif // !defined(CACHEPREPROCESS_H)
