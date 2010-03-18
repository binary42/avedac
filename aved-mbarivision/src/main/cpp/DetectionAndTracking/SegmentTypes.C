#include "DetectionAndTracking/SegmentTypes.H"

#include "Util/StringConversions.H"
#include "Util/log.H"

void convertFromString(const std::string& str, SegmentAlgorithmType& val)
{
  // CAUTION: assumes types are numbered and ordered!
  for (int i = 0; i < NSEGMENT_ALGORITHMS; i ++)
    if (str.compare(segmentAlgorithmType(SegmentAlgorithmType(i))) == 0)
      { val = SegmentAlgorithmType(i); return; }

  conversion_error::raise<SegmentAlgorithmType>(str);
}

std::string convertToString(const SegmentAlgorithmType val)
{ return segmentAlgorithmType(val); }

void convertFromString(const std::string& str, SegmentAlgorithmInputImageType& val) {
  // CAUTION: assumes types are numbered and ordered!
  for (int i = 0; i < NSEGMENT_ALGORITHM_INPUT_IMAGE_TYPES; i ++)
    if (str.compare(segmentAlgorithmInputImageType(SegmentAlgorithmInputImageType(i))) == 0)
      { val = SegmentAlgorithmInputImageType(i); return; }

  conversion_error::raise<SegmentAlgorithmInputImageType>(str);
}

std::string convertToString(const SegmentAlgorithmInputImageType val)
{ return segmentAlgorithmInputImageType(val); }

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */

