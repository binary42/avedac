/*!@file MbariMetaData.H  */
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
// Public License as published by the Free Software Foundation; either  //
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
// $Id: MbariMetaData.H,v 1.2 2009/09/30 21:58:57 dcline Exp $
//

#ifndef MBARI_METADATA_H
#define MBARI_METADATA_H

#include <cstring>
#include <fstream>
#include <iostream>
#include <unistd.h>

// ######################################################################
//! Super class that manages MBARI specific metadata like timecode
class MbariMetaData
{
public:

  //! Constructor
  MbariMetaData() ;

  //! Constructor. Copies from existing MbariMetaData Class
  MbariMetaData( const MbariMetaData& m );

  //! Constructor. Create from string - used to parse string from
  // image file headers
  MbariMetaData( std::string s );

  // ! copy operator
  MbariMetaData & operator=(const MbariMetaData& m);

  //! read the MbariMetadata from the input stream is
  void readFromStream(std::istream& is);

  //! write the MbariMetadata to the output stream os
  void writeToStream(std::ostream& os);

  // ######################################################################
  inline std::string getTC() const { return tc; }

  // ######################################################################
  inline void setMetaData( std::string s ) { parseMetaData( s ); }
  
private:
  void parseMetaData( std::string s );  
  std::string tc;   //Timecode string
};
#endif

/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
