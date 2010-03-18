/* 
 * File:   SegmentTypes.H
 * Author: aved
 *
 * Created on April 6, 2009, 4:00 PM
 */

#ifndef SEGMENTTYPES_H_DEFINED
#define	SEGMENTTYPES_H_DEFINED

#include <string>

  // ! Segment algorithm used for extracting the foreground objects
enum SegmentAlgorithmType {
  SABinaryAdaptive = 0,
  SAAdaptiveThreshold = 1,
  SABackgroundCanny = 2,
  SAHomomorphicCanny = 3,
  SAExtractForegroundBW  = 4 //! graphcut
  // if you add a new type here, also update the names in the function below!
};
//! number of algorithm type
#define NSEGMENT_ALGORITHMS 5

//! Returns name of segment algorithm
inline const char* segmentAlgorithmType(const SegmentAlgorithmType p)
{
  static const char n[NSEGMENT_ALGORITHMS][25] = {
    "BinaryAdaptive", "AdaptiveThreshold", "BackgroundCanny",
   "HomomorphicCanny", "ExtractForegroundBW"};
  return n[int(p)];
}

//! segmentAlgorithmType overload */
void convertToString(const SegmentAlgorithmType val,
                     std::string& str);

//! segmentAlgorithmInputImageType overload */
void convertFromString(const std::string& str, SegmentAlgorithmType& val);

enum SegmentAlgorithmInputImageType {
  SAIMaxRGB = 0,
  SAILuminance = 1
  // if you add a new type here, also update the names in the function below!
};

//! number of algorithm input images types
#define NSEGMENT_ALGORITHM_INPUT_IMAGE_TYPES 2

//! Returns name of segment algorithm
inline const char* segmentAlgorithmInputImageType(const SegmentAlgorithmInputImageType p)
{
  static const char n[NSEGMENT_ALGORITHM_INPUT_IMAGE_TYPES][20] = {
    "MaxRGB", "Luminance"};
  return n[int(p)];
};

//! segmentAlgorithmInputImageType overload */
void convertToString(
                    const SegmentAlgorithmInputImageType val,
                     std::string& str);

//! segmentAlgorithmInputImageType overload */
void convertFromString(const std::string& str, SegmentAlgorithmInputImageType& val);

#endif	/* SEGMENTTYPES_H_DEFINED */

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
