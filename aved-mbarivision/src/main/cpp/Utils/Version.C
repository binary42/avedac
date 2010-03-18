
#include "Utils/Version.H"
#include <sstream>

namespace Version
{
  void printVersion() {
    fprintf(stderr, "%s v%s (C) 2003-2009 MBARI built %s at %s \n", PACKAGE, VERSION, __DATE__, __TIME__);
  }	
  std::string versionString() {
    std::stringbuf sb;
    std::ostream  os(&sb);  
    os << PACKAGE  << " v" << VERSION  << " (C) 2003-2009 MBARI built " << __DATE__ << " at " << __TIME__  << "\n";
    os.flush();  
    return sb.str();
  }
}
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
