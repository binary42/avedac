#ifndef SEGMENTATION_H_
#define SEGMENTATION_H_

#include "Image/ColorOps.H"
#include "Image/Image.H"
#include "Image/PixelsTypes.H"
#include "DetectionAndTracking/TrackingModes.H"

#include "Util/StringConversions.H"

// ######################################################################
//! Container class for running different segmentation algorithms
class Segmentation
{
public:

  Segmentation();
  virtual ~Segmentation();
  Image<byte> runBinaryAdaptive(const Image<byte> &diff, const Image<byte> &bwImg, const TrackingMode tm);
  Image<byte> runGraphCut(const Image< PixRGB<byte> >& img, const Image<PixRGB<byte> >& meanimg,  const char *SEType);
  Image<byte> runAdaptiveThreshold(const Image< PixRGB<byte> > &bwImg, const char *SEType);
  Image<byte> runHomomorphicCanny(const Image< PixRGB<byte> > &img, const char *SEType);
  Image<byte> runBackgroundCanny(const Image< PixRGB<byte> > &img, const char *SEType);
  Image< PixRGB<byte> > test(const Image < PixRGB<byte> > &image);
private:

  /* private functions related to the BinaryAdaptive algorithm */
  byte getThreshold(const Image<byte> &diff, const Image<byte> &bwImg, const TrackingMode tm);
  Image<byte> makeBinaryAdaptive(const Image<byte>& src, const byte& threshold, const float& pct);	
};

#endif /*SEGMENTATION_H_*/
