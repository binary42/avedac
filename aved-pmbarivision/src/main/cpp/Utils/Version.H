#ifndef VERSION_H
#define VERSION_H

#define PACKAGE "pmbarivision"
#define VERSION "0.3.1"
#include <stdio.h> 
#include <iostream>

// ######################################################################
//! Container version info for this software. This should not conflict with the version info in the mbarivision module
namespace PVersion
{
  void printversion(); 
  std::string versionString();
}
#endif
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
