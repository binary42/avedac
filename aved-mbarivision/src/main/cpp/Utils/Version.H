#ifndef VERSION_H_
#define VERSION_H_

#define PACKAGE "mbarivision"

#define VERSION "1.0.0"
#include <stdio.h>
#include <iostream>

namespace Version
{
  void printVersion();  
  std::string versionString();	
}
#endif /*VERSION_H_*/

