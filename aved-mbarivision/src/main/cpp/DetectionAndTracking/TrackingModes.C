#include "DetectionAndTracking/TrackingModes.H"

#include "Util/StringConversions.H"
#include "Util/log.H"


std::string convertToString(const TrackingMode val)
{ return trackingModeName(val); }

void convertFromString(const std::string& str, TrackingMode& val)
{
  // CAUTION: assumes types are numbered and ordered!
  for (int i = 0; i < NTRACKINGMODES; i ++)
    if (str.compare(trackingModeName(TrackingMode(i))) == 0)
      { val = TrackingMode(i); return; }

  conversion_error::raise<TrackingMode>(str);
}

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
