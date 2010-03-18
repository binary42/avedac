/* 
 * File:   SaliencyTypes.H
 * Author: aved
 *
 * Created on April 6, 2009, 1:20 PM
 */

#ifndef SALIENCYTYPES_H_DEFINED
#define	SALIENCYTYPES_H_DEFINED

#include <string>

    // ! Segment algorithm used for extracting the foreground objects
enum SaliencyInputImageType {
  SIDiffMean= 0,
  SIRaw = 1
  // if you add a new type here, also update the names in the function below!
};
//! number of algorithm type
#define NSALIENCY_INPUT_IMAGE_TYPES 2

//! Returns name of segment algorithm
inline const char* saliencyInputImageType(const SaliencyInputImageType p)
{
  static const char n[NSALIENCY_INPUT_IMAGE_TYPES][15] = {
    "DiffMean", "Raw"};
  return n[int(p)];
}

//! segmentAlgorithmType overload */
void convertToString(const SaliencyInputImageType val,
                     std::string& str);

//! segmentAlgorithmInputImageType overload */
void convertFromString(const std::string& str, SaliencyInputImageType& val);

#endif	/* _SALIENCYTYPES_H */
// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */

