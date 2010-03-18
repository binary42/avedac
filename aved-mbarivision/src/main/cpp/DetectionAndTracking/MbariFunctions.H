/*!@file MbariFunctions.H a few functions used by MBARI programs
 */
// //////////////////////////////////////////////////////////////////// //
// The iLab Neuromorphic Vision C++ Toolkit - Copyright (C) 2000-2002   //
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
// Primary maintainer for this file: Dirk Walther <walther@caltech.edu>
// $HeadURL: svn://iLab.usc.edu/trunk/saliency/src/MBARI/mbariFunctions.H $
// $Id: MbariFunctions.H,v 1.5 2009/04/16 23:58:06 dcline Exp $
//

#ifndef MBARIUTILS_DEFINED
#define MBARIUTILS_DEFINED
#include "Media/SimFrameSeries.H"
#include "Util/Types.H" 
#include "Neuro/WinnerTakeAll.H"
#include "Neuro/StdBrain.H"
#include "Simulation/SimEventQueueConfigurator.H"
#include "Image/Point2D.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "Image/Transforms.H"
#include "Parallel/pvisionTCP-defs.H"
class Token;
class MbariResultViewer;
class Brain; 
class BitObject;
template <class T> class PixRGB;
template <class T> class Image; 
namespace rutz { template <class T> class shared_ptr; }
namespace nub { template <class T> class soft_ref; }

namespace rutz {
    template <class T> class shared_ptr;
}
namespace nub {
    template <class T> class soft_ref;
}

#include <iostream>
#include <list>

using namespace std;

//! compute input filename for current frame
std::string getInputFileName(const std::string& stem,
        const int framenumber);

//! extract a set of BitObjects from bitImg, which intersect region
/*! bitImg is flooded starting from each point within region,
  and each BitObject exceeding the minSize is stored in the
  list of BitObjects that is returned. */
std::list<BitObject> extractBitObjects(const Image<byte>& bitImg,
        Rectangle region,
        const int minSize,
        const int maxSize);

//! extract a set of BitObjects from a color labeled images, which intersect region
/*! Same as above, except assumption is image is color labeled by 
 * external segmentation algorithm. Image is then flooded starting 
 * from each point within region, and each BitObject exceeding the 
 * minSize is stored in the list of BitObjects that is returned. */
std::list<BitObject> extractBitObjects(const Image<PixRGB <byte> >& bImg,
        const Point2D<int> seed,
        Rectangle region,
        const int minSize,
        const int maxSize);

//! returns list of SalientWinners of the most salient points
/*! @param manager the model manager
 @param brain the brain to process saliency
 @param seq the event queue
 @param maxEvolveTime the maximum time to evolve the brain before stopping
 @param maxNumSalSpots the maximum number of salient locations extracted before stopping
 *
 *If either @param maxEolveTime or @param maxNumSalSpots
 *conditions are met, this function will return.s
 */
list<WTAwinner> getSalientWinners(
        nub::ref<SimOutputFrameSeries> simofs,
        const Image< PixRGB<byte> > &img,
        nub::ref<StdBrain> brain,
        nub::ref<SimEventQueue> seq,
        float maxEvolveTime,
        int maxNumSalSpots,
        int framenum);


//! returns the BitObjects at the most winning locations points
/*!*@param bitImg a binary image used for extracting BitObjects at the
 * winner locations
 @param winners list of Salient winners*/
std::list<BitObject> getSalientObjects(
        const Image<byte>& bitImg,
        const Image<PixRGB<byte> >& colorbitImg,
        const list<WTAwinner> &winners);

//! returns the BitObjects at the most winning locations points
/*!*@param bitImg a binary image used for extracting BitObjects at the 
 * winner locations
 @param winners list of Salient winners*/
std::list<BitObject> getSalientObjects(const Image<byte>& bitImg,
        const list<WTAwinner> &winners);

//! all BitObjects in objs are drawninto the return image
Image<byte> showAllObjects(const std::list<BitObject>& objs);

// ! Run mask option: flag the area specified as background after the segmentation
Image< byte > maskArea( const Image<byte>& img, DetectionParameters *parms);

// ! Run mask option on a color image: flag the area specified as background after the segmentation
Image<  PixRGB <byte > >  maskArea( const Image<  PixRGB<byte> > & img, DetectionParameters *parms);

// ! Return the max value of a matrix
float getMax(const Image<float> matrix);

// ! Return the picture to add to the background Model: take in consideration if it's a fixedCam then 
// objects detected are not included into it
Image< PixRGB<byte> > getImageToAddToTheBackground(bool needIt, const Image< PixRGB<byte> > &img,
        const Image< PixRGB<byte> > &currentBackgroundMean,
        Image< PixRGB<byte> > savePreviousPicture,
        const list<BitObject> &bitObjectFrameList,
        const DetectionParameters &params);

#endif

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
