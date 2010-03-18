/*!@file MbariMetaData.C  */
//
// //////////////////////////////////////////////////////////////////// //
// The iLab Neuromorphic Vision C++ Toolkit - Copyright (C) 2001 by the //
// University of Southern California (USC) and the iLab at USC.         //
// See http://iLab.usc.edu for information about this project.          //
// //////////////////////////////////////////////////////////////////// //
// Major portions of the iLab Neuromorphic Vision Toolkit are protected //
// under the U.S. patent ``Computation of Intrinsic Perceptual Saliency //
// in Visual Environments, and Applications'' by Christof Koch and      //
// Laurent Itti, California Institute of Technology, 2001 (patent       //
// pending; application number 09/912,225 filed July 23, 2001; see      //
// http://pair.uspto.gov/cgi-bin/final/home.pl for current status).     //
// //////////////////////////////////////////////////////////////////// //
// This file is part of the iLab Neuromorphic Vision C++ Toolkit.       //
//                                                                      //
// The iLab Neuromorphic Vision C++ Toolkit is free software; you can   //
// redistribute it and/or modify it under the terms of the GNU General  //
// version 2 of the License, or (at your option) any later version.     //
//                                                                      //
// The iLab Neuromorphic Vision C++ Toolkit is distributed in the hope  //
// that it will be useful, but WITHOUT ANY WARRANTY; without even the   //
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR      //
// PURPOSE.  See the GNU General Public License for more details.       //
//                                                                      //
// You should have received a copy of the GNU General Public License    //
// along with the iLab Neuromorphic Vision C++ Toolkit; if not, write   //
// to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,   //
// Boston, MA 02111-1307 USA.                                           //
// //////////////////////////////////////////////////////////////////// //
//
// Primary maintainer for this file: DCline <dcline@mbari.org>
// $Id: MbariMetaData.C,v 1.6 2009/10/12 23:01:42 dcline Exp $
//

#include "Data/MbariMetaData.H"
#include "Util/StringConversions.H"
#include "Util/log.H"
#
#include <cstring>
#include <fstream>
#include <iostream>
#include <unistd.h>

using namespace std;


// ######################################################################
MbariMetaData::MbariMetaData()
{}
// ###################################################################### 
MbariMetaData::MbariMetaData( const MbariMetaData& m ) :
  tc(m.tc)
{}
// ######################################################################  
MbariMetaData::MbariMetaData( string s )
{
  parseMetaData( s );
}
// ######################################################################
MbariMetaData & MbariMetaData::operator=(const MbariMetaData& m)
{
  tc = m.getTC();
  return *this;
}
// ######################################################################
void MbariMetaData::writeToStream(std::ostream& os)
{
  os << tc;
  os << '\n';
}

// ######################################################################
void MbariMetaData::readFromStream(std::istream& is)
{  
  is >> tc;
}

// ######################################################################  
void MbariMetaData::parseMetaData( string s ) {
  string temp = "";
  int space;    
  
  while((space = s.find(" ", 0)) && space != (int)(string::npos)) {            
    temp = s.substr( 0, space );
    s = s.substr( space, s.length() - space );
    if( temp.find("TIMECODE:", 0 ) != string::npos ){
      tc = s.substr(1, s.length()); 
    }      
  }
}


/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */

