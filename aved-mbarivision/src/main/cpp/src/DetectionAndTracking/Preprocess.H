#ifndef PREPROCESS_C_DEFINED
#define PREPROCESS_C_DEFINED

#include "Image/OpenCVUtil.H"
#include "Component/ModelManager.H"
#include "Component/ModelParam.H"
#include "Component/OptionManager.H"
#include "Data/Winner.H"
#include "Image/CutPaste.H"
#include "Image/FilterOps.H"
#include "Image/Kernels.H"
#include "Image/ImageCache.H"
#include "Image/MbariImage.H"
#include "Image/Pixels.H"
#include "Image/PyramidOps.H"
#include "Media/FrameSeries.H"

#include <map>
#include <vector>
#include <list>

using namespace std;

// ######################################################################
//! Preprocessing class that create cache of incoming frames and contrast enhances
class Preprocess : public ModelComponent
{

public:
  //! Construct
  Preprocess(OptionManager& mgr, const std::string& descrName = "mbarivision image preprocess", const std::string& tagName = "preprocessor");

  //! destructor
  virtual ~Preprocess();

  //! initialize cache using the @param ifs Input frame series
  void init(nub::soft_ref<InputFrameSeries> ifs, const Dims rescaledDims);

  //! Overload so that we can reconfigure when our params get changed
  virtual void paramChanged(ModelParamBase* const param,
                            const bool valueChanged,
                            ParamClient::ChangeStatus* status);


  //! Update the cache using previous bit objects found then
  // return the latest image encapsulated in the MbariImage which may metadata like timecode
  MbariImage< PixRGB<byte> > update(const Image< PixRGB<byte> >& img, const Image< PixRGB<byte> >& prevImg,
                                            const uint frameNum, const list<BitObject> bitObjectFrameList);

  //! Return background image
  Image< PixRGB<byte> > background(const Image< PixRGB<byte> >& img, const Image< PixRGB<byte> >& prevImg,
                                            const uint frameNum, const list<BitObject> bitObjectFrameList);

  //! Returns the absolute difference between the image and the cache mean
  Image< PixRGB<byte> > absDiffMean(MbariImage< PixRGB<byte> >& image);

  //! Returns the absolute difference between the image and the cache mean
  Image< PixRGB<byte> > clampedDiffMean(MbariImage< PixRGB<byte> >& image);

  //! Returns the cache mean
  Image< PixRGB<byte> > mean();

protected:

  //! overload start1()
  virtual void start1();

private:

  //! Update the cache
  void update(const Image< PixRGB<byte> >& img, const uint framenum);

  //! Contrast enhance using adaptive gamma
  Image< PixRGB<byte> > contrastEnhance(const Image< PixRGB<byte> >& img);

  //! Checks the entropy of the image to flag when gamma needs adjusting
  void checkEntropy(Image< PixRGB<byte> >& img);

  // ! Adjust gamma  in place in @hsvRes image
  void adjustGamma(Image<byte>&lum, std::map<int, double> &cdfw, Image<PixHSV<float> >&hsvRes);

  // ! Contrast enhance image
  Image<PixRGB<byte> > enhanceImage(const Image<PixRGB<byte> >& img, std::map<int, double> &cdfw);

  // ! Update the mapping curve for contrast enhancement; returns the cumulative distribution function
  std::map<int, double> updateGammaCurve(const Image<PixRGB<byte> >& img, std::map<int, double> &pdf,  bool init = true);

  // ! Update the entropy model for contrast enhancement; returns the entropy approximation
  float updateEntropyModel(const Image<PixRGB<byte> >& img, std::map<int, double> &pdf);

  //! Input frame source
  OModelParam<std::string> itsFrameSource;
  OModelParam<int> itsSizeAvgCache;
  OModelParam<float> itsMinStdDev; //! minimum std dev for image to be included in averaging cache

  ImageCacheAvg< PixRGB<byte> > itsAvgCache;
  std::map<int, double> itspdf;
  float itsPrevEntropy;
  uint itsMinFrame;

};

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */

#endif // PREPROCESS_C_DEFINED
