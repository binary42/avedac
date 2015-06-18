#ifndef OBJECTDET_C_DEFINED
#define OBJECTDET_C_DEFINED

#include "Image/OpenCVUtil.H"
#include "Component/ModelManager.H"
#include "Component/ModelParam.H"
#include "Component/OptionManager.H"
#include "Data/Winner.H"
#include "Image/CutPaste.H"
#include "Image/FilterOps.H"
#include "Image/Kernels.H"
#include "Image/ImageCache.H"
#include "Image/Pixels.H"
#include "Image/PyramidOps.H"
#include "Media/FrameSeries.H"
#include "Media/MbariResultViewer.H"

#include <map>
#include <vector>
#include <list>

using namespace std;

// ######################################################################
//! An object detection class
class ObjectDetection : public ModelComponent
{

public:
  //! Construct
  ObjectDetection(OptionManager& mgr, const std::string& descrName = "mbarivision object detector",
                const std::string& tagName = "object detector");

  //! destructor
  virtual ~ObjectDetection();

  //! init
  void init(nub::soft_ref<InputFrameSeries> ifs);

  //! Overload so that we can reconfigure when our params get changed
  virtual void paramChanged(ModelParamBase* const param,
                            const bool valueChanged,
                            ParamClient::ChangeStatus* status);

  //! ObjectDetection
  std::list<BitObject> run(nub::soft_ref<MbariResultViewer> rv,
                const std::list<Winner> &winlist,
                const Image< PixRGB<byte> >& graphBitImg);

protected:

  //! overload start1()
  virtual void start1();

private:


  std::map<int, double> itspdf;
  uint itsFrameNum;
  float itsPrevEntropy;
  Image< PixRGB<byte> > itsGraphBitImg;
  Image< byte > itsBitImg;
  ImageCacheAvg< PixRGB<byte> > itsAvgCache;
};

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */

#endif // OBJECTDET_C_DEFINED
