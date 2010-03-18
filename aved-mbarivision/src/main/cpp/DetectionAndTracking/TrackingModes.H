/*!@file TrackingModes.H used for event tracking */

// //////////////////////////////////////////////////////////////////// //
// The iLab Neuromorphic Vision C++ Toolkit - Copyright (C) 2000-2003   //
// by the University of Southern California (USC) and the iLab at USC.  //
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
// Primary maintainer for this file: Danelle Cline <dcline@mbari.org>
// $Id: TrackingModes.H,v 1.3 2009/04/16 23:58:06 dcline Exp $
//

#ifndef TRACKINGMODES_H_DEFINED
#define TRACKINGMODES_H_DEFINED

#include <string>

// ! Tracking mode used for visual event tracking 
enum TrackingMode {
  TMKalmanFilter = 0, //! Kalman filter - assumes constant motion
  TMNearestNeighbor = 1,  //! Tracking nearest neighbor - good for non-motion
  TMNone = 2, //! No tracking - use for still frame images
  // if you add a new mode here, also update the names in the function below!
};
//! number of event tracking modes:
#define NTRACKINGMODES 3

//! Returns name of trakcing mode
inline const char* trackingModeName(const TrackingMode p)
{
  static const char n[NTRACKINGMODES][25] = {
    "KalmanFilter", "NearestNeighbor", "None"};
  return n[int(p)];
}

//! TrackingMode overload
/*! Format is "name" as defined in TrackingModes.H */
void convertToString(const TrackingMode val,
                     std::string& str);

//! TrackingMode overload
/*! Format is "name" as defined in TrackingModes.H */
void convertFromString(const std::string& str, TrackingMode& val);


#endif

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
