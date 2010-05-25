/*
 * Copyright 2010 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1 
 * (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This is a program to automate detection and tracking of events in underwater 
 * video. This is based on modified version from Dirk Walther's 
 * work that originated at the 2002 Workshop  Neuromorphic Engineering 
 * in Telluride, CO, USA. 
 * 
 * This code requires the The iLab Neuromorphic Vision C++ Toolkit developed
 * by the University of Southern California (USC) and the iLab at USC. 
 * See http://iLab.usc.edu for information about this project. 
 *  
 * This work would not be possible without the generous support of the 
 * David and Lucile Packard Foundation
 */ 

/*!@file MbariFunctions.H miscellaneous functions used by mbarivision
 */ 
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
