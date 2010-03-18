#include "DetectionAndTracking/SaliencyTypes.H"

#include "Util/StringConversions.H"
#include "Util/log.H"

std::string convertToString(const SaliencyInputImageType val)
{ return saliencyInputImageType(val); }

void convertFromString(const std::string& str, SaliencyInputImageType& val)
{
  // CAUTION: assumes types are numbered and ordered!
  for (int i = 0; i < NSALIENCY_INPUT_IMAGE_TYPES; i ++)
    if (str.compare(saliencyInputImageType(SaliencyInputImageType(i))) == 0)
      { val = SaliencyInputImageType(i); return; }

  conversion_error::raise<SaliencyInputImageType>(str);
}

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */

