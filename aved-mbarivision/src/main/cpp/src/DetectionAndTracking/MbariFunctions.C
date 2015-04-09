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

/*!@file mbariFunctions.C   functions used find and extract interesting 
 * objects from underwater images. 
 */ 
#include <list>

#include "Image/OpenCVUtil.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/Segmentation.H"
#include "Channels/ChannelOpts.H"
#include "Component/GlobalOpts.H"
#include "Component/JobServerConfigurator.H"
#include "Image/ColorOps.H"
#include "Image/FilterOps.H"
#include "Image/Image.H"
#include "Image/MathOps.H"
#include "Image/DrawOps.H"
#include "Image/CutPaste.H"  
#include "Image/ShapeOps.H"
#include "Image/Transforms.H"
#include "Image/Geometry2D.H"
#include "Image/MorphOps.H"
#include "Data/Winner.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "Media/MediaSimEvents.H"
#include "Media/SimFrameSeries.H"
#include "Media/MediaOpts.H"
#include "Motion/OpticalFlow.H"
#include "Motion/MotionOps.H"
#include "Neuro/StdBrain.H"
#include "Neuro/NeuroOpts.H"
#include "Neuro/NeuroSimEvents.H"
#include "Neuro/SimulationViewer.H"
#include "Neuro/VisualCortex.H"
#include "Raster/Raster.H"
#include "SIFT/Histogram.H"
#include "Simulation/SimEventQueue.H"
#include "Simulation/SimEvents.H"
#include "Util/Timer.H"
#include "Util/Pause.H" 
#include "rutz/shared_ptr.h"
#include "Image/BitObject.H"
#include "Image/DrawOps.H"
#include "Image/Kernels.H"      // for twofiftyfives()
#include "Media/MbariResultViewer.H"

// ######################################################################
template <class T> class MbariImage;

bool isGrayscale(const Image<PixRGB<byte> >& src)
{
  ASSERT(src.initialized());

  Image< PixRGB<byte> >::const_iterator aptr = src.begin();
  Image< PixRGB<byte> >::const_iterator stop = src.end();

  while ( (aptr != stop) ) {
   int color = aptr->red();
   if ( aptr->green() != color || aptr->blue() != color ) {
	break;
     }
   ++aptr;
  }

  // reached the end, and all rgb channels were equal
  if(aptr == stop) return true;

  return false;
}

// ######################################################################
BitObject extractBitObject( const Image<PixRGB <byte> >& image,
                            const Point2D<int> seed,
                            Rectangle region,
                            const int minSize,
                            const int maxSize) {

    cv::Mat input = img2ipl(image);
    cv::Rect rectangle(region.left(), region.top(), region.width(), region.height());
    cv::Mat result; //segmentation result (4 possible values)
    cv::Mat fgmdl, bgmdl; // the models (internally used)
    grabCut(input, result, rectangle, fgmdl, bgmdl, 5, cv::GC_INIT_WITH_RECT);

    // convert to image and create bit object from it
    cv::Mat mask(input.size(), CV_8UC1,cv::Scalar(0));
    mask = result ==  (cv::GC_PR_FGD | cv::GC_FGD) ; //compare and set the results to 255
	IplImage boimage = (IplImage)mask;
    Image< byte > output = ipl2gray(&boimage);
    BitObject bo(output, seed, byte(255));

    if (bo.isValid()) {
        LINFO("Extracted BitObject size %d", bo.getArea());
        return bo;
    }
    else
        bo.freeMem(); //invalidate the object

    return bo;
}

// ######################################################################
std::list<BitObject> extractBitObjects(const Image<PixRGB <byte> >& image,
        const Point2D<int> seed,
        const Rectangle searchRegion,
        const Rectangle segmentRegion,
        const int minSize,
        const int maxSize,
        const float minIntensity)
{
    Rectangle regionSearch = searchRegion.getOverlap(Rectangle(Point2D<int>(0, 0), image.getDims() - 1));
    Rectangle regionSegment = segmentRegion.getOverlap(Rectangle(Point2D<int>(0, 0), image.getDims() - 1));
    std::list<BitObject> bos;
    BitObject largestBo;
    if (regionSegment.width() < 2 || regionSegment.height() < 2) {
        LINFO("Segment region too small region to run graph cut algorithm ");
        return bos;
    }

    Segmentation segment;
    float scale = 1.0f;

    // iterate on the graph scale to try to find bit objects
    for (int i = 0; i < 3; i++) {

        std::list<BitObject> gbos;

        if (bos.size() > 1 && searchRegion == segmentRegion)
            break;

        Image< PixRGB<byte> > graphBitImg = segment.runGraph(image, regionSegment, scale);
        scale = scale * 0.80;

        std::list<PixRGB<byte>> seedColors;
        Image<byte> labelImg(graphBitImg.getDims(), ZEROS);
        Image<byte> bitImg(graphBitImg.getDims(), ZEROS);
        bool found;

        // get the bit object(s) in the search region
        for (int ry = regionSearch.top(); ry <= regionSearch.bottomO(); ++ry)
            for (int rx = regionSearch.left(); rx <= regionSearch.rightO(); ++rx) {
                PixRGB<byte> newColor = graphBitImg.getVal(Point2D<int>(rx,ry));
                found = true;

                // check if not a new seed color
                std::list<PixRGB<byte>>::const_iterator iter = seedColors.begin();
                while (iter != seedColors.end()) {
                    PixRGB<byte> colorSeed = (*iter);
                    // found existing seed color
                    if (colorSeed == newColor) {
                        found = false;
                        break;
                        }
                    iter++;
                }

                // found a new seed color
                if (found) {
                    seedColors.push_back(newColor);
                    // create a binary representation with the 1 equal to the
                    // color at the center of the seed everything else 0
                    Image< PixRGB<byte> >::const_iterator sptr = graphBitImg.begin();
                    Image<byte>::iterator rptr = bitImg.beginw();
                    while (sptr != graphBitImg.end())
                        *rptr++ = (*sptr++ == newColor) ? 1 : 0;

                    BitObject obj;
                    Image<byte> dest = obj.reset(bitImg, Point2D<int>(rx, ry));
                    obj.setMaxMinAvgIntensity(luminance(image));

                    float maxI, minI, avgI;

                     // if the object is in range, keep it
                     obj.getMaxMinAvgIntensity(maxI, minI, avgI);
                     if (obj.getArea() >= minSize && obj.getArea() <= maxSize && avgI > minIntensity) {
                         LINFO("found object size: %d avg intensity: %f", obj.getArea(), avgI);
                         bos.push_back(obj);
                     }
                     else
                        LINFO("found object but out of range in size %d minsize: %d maxsize: %d or intensity %f min intensity %f ",\
                        obj.getArea(), minSize, maxSize, avgI, minIntensity);

                }
        }
    }

    return bos;
}

// ######################################################################
/*std::list<BitObject> extractBitObjects(const Image<PixRGB <byte> >& image,
        const Rectangle foregroundRegion,
        const Rectangle region,
        const int minSize,
        const int maxSize)
{

    std::list<BitObject> bos;
    if (region.width() < 2 || region.height() < 2) {
        LINFO("Too small region to run graph cut algorithm ");
        return bos;
    }

    cv::Mat input = img2ipl(image);
    cv::Rect rectangle(foregroundRegion.left(), foregroundRegion.top(), foregroundRegion.width(), foregroundRegion.height());
    cv::Rect rectangle(region.left(), region.top(), region.width(), region.height());
    cv::Mat mask; //segmentation result (4 possible values)
    cv::Mat fgmdl, bgmdl; // the models (internally used)
    grabCut(input, mask, rectangle, fgmdl, bgmdl, 5, cv::GC_INIT_WITH_RECT);
	//grabCut(subframe, subbackProject, itsObject, fgmdl, bgmdl, GRABCUT_ROUNDS, cv::GC_INIT_WITH_MASK);

    cv::Mat imgrey(input.size(), CV_8U,cv::Scalar(0));
    cv::Mat imcanny(input.size(), CV_8U,cv::Scalar(0));
    //cv::Mat imbin(input.size(), CV_8UC1,cv::Scalar(0));
    imgrey = mask == (cv::GC_PR_FGD | cv::GC_FGD) ; // set all foreground results to 255
    Canny(imgrey, imcanny, 255, 1);
    //imbin = threshold(mask, 255, 1, cv::THRESH_BINARY); // convert to binary as needed for BitObjects

	IplImage grey = (IplImage)imgrey;
    Image< byte > output = ipl2gray(&grey);

    // get blobs and create BitObjects out of each
    std::vector< std::vector<cv::Point> > contours;
    std::vector< cv::Point > contour;
    std::vector< cv::Vec4i > hierarchy;
    cv::Point2f center;
    cv::Rect boundRect;
    float radius;
    findContours(imgrey, contours, hierarchy, cv::RETR_TREE, cv::CHAIN_APPROX_SIMPLE);
    cv::Moments m;

    LINFO("======================================Found %d contours", contours.size());

    for (int i = 0; i< contours.size(); i++) {
        boundRect = boundingRect(cv::Mat(contours[i]));
        m = moments(contours[i]);
        cv::Point pt1 = boundRect.tl();
        cv::Point pt2 = boundRect.br();
        int width = pt2.x - pt1.x;
        int height = pt2.y - pt1.y;
        Point2D<int> ctr((int)(m.m10/m.m00),(int)(m.m01/m.m00));

        if (ctr.i < 0 || ctr.j < 0)  {
            std::vector<cv::Point> pts2 = contours[i];
            cv::Point ptr2 = pts2.back();
            ctr = Point2D<int>(ptr2.x, ptr2.y);
        }
        LINFO("====================center %d,%d top left %d,%d width %d height %d ", ctr.i, ctr.j,\
                                                                                    pt1.x, pt1.y, width, height);
        const Point2D<int> topleft(pt1.x, pt1.y);
		const Dims dims(width, height);
        Rectangle bbox(topleft, dims);
        BitObject bo(output, ctr, byte(255));

        // if can't find a valid bit object because butterfly-shaped contour, e.g. try choosing a point along
        // the edge of the contour
        if (!bo.isValid()) {
            LINFO("Invalid bit object maybe because of butterfly-shaped contour...trying point along the contour");
            std::vector<cv::Point> pts = contours[i];
            cv::Point pt = pts.back();
            bo.reset(output, Point2D<int>(pt.x, pt.y), byte(255));
        }

        LINFO("==============area %d< %d < %d", minSize, bo.getArea(), maxSize);

        if (bo.getArea() >= minSize && bo.getArea() <= maxSize)
            bos.push_back(bo);
    }

    return bos;

}*/

std::list<BitObject> extractBitObjects(const Image<byte>& bImg,
        Rectangle region,
        const int minSize,
        const int maxSize) {

    Timer timer;
    Image<byte> bitImg = replaceVals(bImg, byte(0), byte(0), byte(1));
    int tmask = 0, tobj = 0;
    std::list<BitObject> bos;
    Dims d = bitImg.getDims();
    region = region.getOverlap(Rectangle(Point2D<int>(0, 0), d - 1));
    Image<byte> labelImg(bitImg.getDims(), ZEROS);

    for (int ry = region.top(); ry <= region.bottomO(); ++ry)
        for (int rx = region.left(); rx <= region.rightO(); ++rx) {
            // this location doesn't have anything -> never mind
            if (bitImg.getVal(rx, ry) == 0) continue;

            // got this guy already -> never mind
            if (labelImg.getVal(rx, ry) > 0) continue;

            timer.reset();
            BitObject obj;

            Image<byte> dest = obj.reset(bitImg, Point2D<int>(rx, ry));
            tobj += timer.get();

            timer.reset();
            labelImg = takeMax(labelImg, dest);
            tmask += timer.get();

            if (obj.getArea() >= minSize && obj.getArea() <= maxSize) bos.push_back(obj);
        }
    //LINFO("tobj = %i; tmask = %i",tobj,tmask);
    return bos;
}  

// ######################################################################

std::list<BitObject> getSalientObjects(const Image< PixRGB<byte> >& graphBitImg, const list<Winner> &winners) {
    const int rectRad = 5;
    DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    std::list<Winner>::const_iterator iter = winners.begin();
    std::list<BitObject> bos;
    Dims d = graphBitImg.getDims();

    //go through each winner and extract salient regions
    while (iter != winners.end()) {
        Point2D<int> winner = (*iter).getWTAwinner().p;

        // extract all the bitObjects at the salient location
        Rectangle region = Rectangle::tlbrI(winner.j - rectRad, winner.i - rectRad,
                winner.j + rectRad, winner.i + rectRad);

        region = region.getOverlap(Rectangle(Point2D<int>(0, 0), d - 1));

        LDEBUG("Extracting bit objects from winning point: %d %d/region %s minSize %d maxSize %d", \
        winner.i, winner.j, convertToString(region).c_str(), p.itsMinEventArea, p.itsMaxEventArea);

        std::list<BitObject> sobjs = extractBitObjects(graphBitImg, winner, region, region, p.itsMinEventArea, p.itsMaxEventArea);

	    LDEBUG("Found bitobject(s) in graphBitImg: %ld", sobjs.size());

        std::list<BitObject>::iterator biter, siter, largest;
        if (sobjs.size() > 0) {
            // if only one object, just use it
            if (sobjs.size() == 1) {
                largest = sobjs.begin();
                bos.push_back((*largest));
            }
            else {
                bool keepGoing = true;
                // loop until we find a new object that doesn't overlap with anything
                // that we have found so far, or until we run out of objects
                while (keepGoing) {
                    // no object left -> go to the next salient point
                    if (sobjs.empty()) break;

                    std::list<BitObject>::iterator biter, siter, largest;

                    // find the largest object
                    largest = sobjs.begin();
                    int maxSize = 0;
                    for (siter = sobjs.begin(); siter != sobjs.end(); ++siter)
                        if (siter->getArea() > maxSize) {
                            maxSize = siter->getArea();
                            largest = siter;
                        }

                    // does the largest objects intersect with any of the already stored guys?
                    keepGoing = false;
                    for (biter = bos.begin(); biter != bos.end(); ++biter)
                        if (largest->isValid() && biter->isValid() && biter->doesIntersect(*largest)) {
                            // no need to store intersecting objects -> get rid of largest
                            // and look for the next largest
                            sobjs.erase(largest);
                            keepGoing = true;
                            break;
                        }

                    // so, did we end up finding a BitObject that we can store?
                    if (!keepGoing) {
                        (*largest).setSMV((*iter).getWTAwinner().sv);
                        bos.push_back(*largest);
                    }
                sobjs.clear();
                } // end while keepGoing
            } // end if found objects
        }
        iter++;
    }// end while iter != winners.end()
    return bos;
}

// ######################################################################
BitObject findBestBitObject(Rectangle r1, int maxDist, std::list<BitObject>& sobjs, std::list<BitObject>& bos )
{
    BitObject bo;
    Rectangle r2;
    std::list<BitObject>::iterator biter, siter, largest;
    if (sobjs.size() > 0) {
        // if only one object, just use it if it's within acceptable distance to the winner
        if (sobjs.size() == 1) {
            largest = sobjs.begin();
            r2 = largest->getBoundingBox();
            // calculate distance between bounding box corners
            float distul = sqrt(pow((double)(r1.top() - r2.top()),2.0) +  pow((double)(r1.left() - r2.left()),2.0));
            float distbr = sqrt(pow((double)(r1.bottomI() - r2.bottomI()),2.0) + pow((double)(r1.rightI() - r2.rightI()),2.0));

            if (distul < maxDist && distbr < maxDist)
                bo = (*largest);
        }
        else {
            bool keepGoing = true;

            // loop until we find a closest largest object within an acceptable distance to the winner
            while (keepGoing) {
                // no object left -> go to the next salient point
                if (sobjs.empty()) break;
                largest = sobjs.begin();
                int maxSize = 0;

                float distul, distbr;
                for (siter = sobjs.begin(); siter != sobjs.end(); ++siter) {
                    r2 = siter->getBoundingBox();
                     // calculate distance between bounding box corners
                      distul = sqrt(pow((double)(r1.top() - r2.top()),2.0) +  pow((double)(r1.left() - r2.left()),2.0));
                      distbr = sqrt(pow((double)(r1.bottomI() - r2.bottomI()),2.0) + pow((double)(r1.rightI() - r2.rightI()),2.0));

                    // if within the maximum allowed distance keep
                    if (siter->getArea() > maxSize && distul < maxDist && distbr < maxDist) {
                        maxSize = siter->getArea();
                        largest = siter;
                    }
                }
                // does the largest objects intersect with any of the already stored guys?
                keepGoing = false;
                for (biter = bos.begin(); biter != bos.end(); ++biter)
                    if (largest->isValid() && biter->isValid() && biter->doesIntersect(*largest)) {
                        // no need to store intersecting objects -> get rid of largest
                        // and look for the next largest
                        sobjs.erase(largest);
                        keepGoing = true;
                        break;
                    }
                // so, did we end up finding a BitObject that we can store?
                if (!keepGoing) {
                    bo = (*largest);
                }
            sobjs.clear();
            } // end while keepGoing
        }
    }

    return bo;
}

// ######################################################################

std::list<BitObject> getFOAObjects(const list<Winner> &winners, const Image< byte >& mask) {
    DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    std::list<Winner>::const_iterator iter = winners.begin();
    std::list<BitObject> bos;

    // go through each winner and extract salient regions
    while (iter != winners.end()) {
        Image< byte > img = (*iter).getBitObject().getObjectMask();

        // mask FOA with user supplied mask
        img = maskArea(img, mask);
        BitObject boFOA(img);

        int area = boFOA.getArea();

        if (area >= p.itsMinEventArea && area <= p.itsMaxEventArea) {
            boFOA.setSMV((*iter).getWTAwinner().sv);
            bos.push_back(boFOA);
        }

        iter++;
    }// end while iter != winners.end()
    return bos;
}

// ######################################################################

std::list<BitObject> getSalientObjects(const Image< PixRGB<byte> >& graphBitImg, const Image< byte >& bitImg,
 const list<Winner> &winners, const Image< byte >& mask) {
    // this should be 2^(smlev - 1)
    const int rectRadBin = 2;
    const int rectRadGraph = 2;
    DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    std::list<Winner>::const_iterator iter = winners.begin();
    std::list<BitObject> bos;
    Dims d = graphBitImg.getDims();
    int i = 0;

    Image< PixRGB<byte> > graphBitImgMasked = maskArea(graphBitImg, mask);
    Image<byte> bitImgMasked = maskArea(bitImg, mask);

    // go through each winner and extract salient regions
    while (iter != winners.end()) {
        Point2D<int> winner = (*iter).getWTAwinner().p;

        Image< byte > img = (*iter).getBitObject().getObjectMask();
        // mask FOA with user supplied mask for equipment/shadows
        img = maskArea(img, mask);
        BitObject boFOA(img);

        // extract all the bitObjects near the salient location
        Rectangle regionBin = Rectangle::tlbrI(winner.j - rectRadBin, winner.i - rectRadBin,
                winner.j + rectRadBin, winner.i + rectRadBin);
        Rectangle regionGraph = Rectangle::tlbrI(winner.j - rectRadGraph, winner.i - rectRadGraph,
                winner.j + rectRadGraph, winner.i + rectRadGraph);

        regionBin = regionBin.getOverlap(Rectangle(Point2D<int>(0, 0), d - 1));
        regionGraph = regionGraph.getOverlap(Rectangle(Point2D<int>(0, 0), d - 1));

        std::list<BitObject> sobjsBin, sobjsGraph;

        int maxArea = std::min(boFOA.getArea(), p.itsMaxEventArea);
        int minArea = p.itsMinEventArea;

        // get region from the graphcut using the foa mask as a guiding rectangle
        LINFO("Extracting bit objects from winning point %i: %d %d/region %s minSize %d maxSize %d", \
                i, winner.i, winner.j, convertToString(boFOA.getBoundingBox()).c_str(), minArea, maxArea);
        sobjsGraph = extractBitObjects(graphBitImgMasked, winner, boFOA.getBoundingBox(), boFOA.getBoundingBox(), minArea, maxArea);
        LINFO("Found bitobject(s) in graphcut img: %ld", sobjsGraph.size());

        LINFO("Extracting bit objects from winning point %i: %d %d/region %s minSize %d maxSize %d", \
            i, winner.i, winner.j, convertToString(regionBin).c_str(), minArea, maxArea);
        sobjsBin = extractBitObjects(bitImgMasked, regionBin, minArea,  maxArea);
        LINFO("Found bitobject(s) in graphBitImg: %ld", sobjsBin.size());

        BitObject bo;
        sobjsBin.splice(sobjsBin.begin(), sobjsGraph);

        // get largest bit object similar to the FOA mask
        bo = findBestBitObject(boFOA.getBoundingBox(), p.itsMaxDist, sobjsBin, bos);

        std::list<BitObject>::iterator biter;

        if (bo.isValid()) {

            // check for intersections
            bool found = true;
            for (biter = bos.begin(); biter != bos.end(); ++biter)
                if (biter->isValid() && biter->doesIntersect(bo))
                    found = false;

            int area = bo.getArea();
            if(found && area >= p.itsMinEventArea && area <= p.itsMaxEventArea) {
                bo.setSMV((*iter).getWTAwinner().sv);
                bos.push_back(bo);
            }
        }
        else {
            // if still can't find object, try to use the foamask
            int area = boFOA.getArea();
            if (area >= p.itsMinEventArea && area <= p.itsMaxEventArea) {
                // check for intersections
                bool found = true;
                for (biter = bos.begin(); biter != bos.end(); ++biter)
                    if (biter->isValid() && biter->doesIntersect(boFOA))
                        found = false;

                if (found) {
                    boFOA.setSMV((*iter).getWTAwinner().sv);
                    bos.push_back(boFOA);
                }
            }
        }

        i++;
        iter++;
    }// end while iter != winners.end()
    return bos;
}

 // ######################################################################

std::list<Winner> filterGraphWinners(const Image< byte >& clipMask,
        const list<Winner> &winlist) {

    std::list<PixRGB<byte> > colors;
    std::list<Winner> winners;
    std::list<Winner>::const_iterator currWinner;
    const byte mask = 0;

    for (currWinner = winlist.begin(); currWinner != winlist.end(); ++currWinner) {
        Point2D<int> ctr = (*currWinner).getWTAwinner().p;
        if (clipMask.getVal(ctr.i, ctr.j) != mask)
            winners.push_back((*currWinner));
    }

    return winners;
}

 // ######################################################################
list<Winner> getSalientWinners(
        nub::soft_ref<MbariResultViewer>& rv,
        const Image< byte >& clipMask,
        const Image< PixRGB<byte> > &img,
        nub::soft_ref<StdBrain> brain,
        nub::soft_ref<SimEventQueue> seq,
        float maxEvolveTime,
        int maxNumSalSpots,
        int framenum
        ) {
    std::list<Winner> winners;
    int numSpots = 0;
    SimStatus status = SIM_CONTINUE;
    DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters; 
 
    if (p.itsMinStdDev > 0.f) {
        float stddevlum = stdev(luminance(img));
        // get the standard deviation in the input image
        // if there is no deviation, this image is uniform and
        // will have no saliency so return empty winners
        if (stddevlum <= p.itsMinStdDev) {
            LINFO("##### frame: %d standard deviation in luminance: %f less than or equal to minimum: %f. No winners will be computed !!#####", framenum, stddevlum, p.itsMinStdDev);
            return winners;
        } else {
            LINFO("##### frame: %d standard deviation in luminance: %f ##### ", framenum, stddevlum);
        }
    }

    LINFO("Start at %.2fms", seq->now().msecs());
    //brain->reset(MC_RECURSE);
    seq->resetTime();
    float scale = 1.0F;
    Dims size = img.getDims();
    Dims newSize = size;

    // Scale down if width greater than 640
    if (size.w() > 640){
        scale = 2.0f;
        newSize = Dims(size/scale);
    }
    Image< byte > resizedClipMask = rescale(clipMask, newSize);
    Image< PixRGB<byte> > resizedImg = rescale(img, newSize);

    // initialize the max time to simulate
    const SimTime simMaxEvolveTime = SimTime::MSECS(seq->now().msecs()) + SimTime::MSECS(p.itsMaxEvolveTime);

    InputFrame iframe = InputFrame::fromRgb(&resizedImg, seq->now(), &resizedClipMask, InputFrame::emptyCache);
    //InputFrame iframe = InputFrame::fromRgb(&img, seq->now(), &clipMask, InputFrame::emptyCache);

    // Post the image to the queue:
    seq->post(rutz::make_shared(new SimEventRetinaImage(brain.get(), InputFrame(iframe),
                                                      Rectangle(Point2D<int>(0,0), newSize),
                                                      Point2D<int>(0,0))));
    /*seq->post(rutz::make_shared(new SimEventRetinaImage(brain.get(), InputFrame(iframe),
                                                      Rectangle(Point2D<int>(0,0), size),
                                                      Point2D<int>(0,0))));*/

    try { 

	LINFO("Checking for winner...");

        // main loop:
        while (status == SIM_CONTINUE) {

            // switch to next time step:
            status = seq->evolve();

            if (SeC<SimEventWTAwinner> e = seq->check<SimEventWTAwinner>(brain.get())) {

	    	    WTAwinner win = e->winner();
                win.p.i = (int) ( (float) win.p.i*scale );
                win.p.j = (int) ( (float) win.p.j*scale );
                LINFO("##### winner #%d found at [%d; %d] with %f voltage frame: %d#####",
                        numSpots, win.p.i, win.p.j, win.sv, framenum);

                numSpots++;

                if (SeC<SimEventShapeEstimatorOutput> e = seq->check<SimEventShapeEstimatorOutput>(brain.get())) {
                    Image<byte> foamask = Image<byte>(e->smoothMask()*255);
                    foamask = zoomXY(foamask, scale, scale);

                    BitObject bo;
                    bo.reset(makeBinary(foamask,byte(0),byte(0),byte(1)));
                    bo.setSMV(win.sv);

                    if (bo.isValid() && bo.getArea() > p.itsMinEventArea && (!win.boring || p.itsKeepWTABoring) )
                        winners.push_back(Winner(win, bo, framenum));
                }

                // if a boring event detected, and not keeping boring WTA points then break simulation
                if (win.boring && p.itsKeepWTABoring == false) {
                    rutz::shared_ptr<SimEventBreak>
                            e(new SimEventBreak(0, "##### boring event detected #####"));
                    seq->post(e);
                }

                if (numSpots >= maxNumSalSpots) {
                    rutz::shared_ptr<SimEventBreak>
                            e(new SimEventBreak(brain.get(), "##### found maximum number of salient spots #####"));
                    seq->post(e);
                }

                LINFO("##### time now:%f msecs max evolve time:%f msecs frame: %d #####", seq->now().msecs(), simMaxEvolveTime.msecs(), framenum);

                if (seq->now().msecs() >= simMaxEvolveTime.msecs()) {
                    LINFO("##### time limit reached time now:%f msecs max evolve time:%f msecs frame: %d #####", seq->now().msecs(), simMaxEvolveTime.msecs(), framenum);
                    rutz::shared_ptr<SimEventBreak>
                            e(new SimEventBreak(brain.get(), "##### time limit reached #####"));
                    seq->post(e);
                }
            }
            
            if (seq->now().msecs() >= simMaxEvolveTime.msecs()) {
                LINFO("##### time limit reached time now:%f msecs max evolve time:%f msecs frame: %d #####", seq->now().msecs(), simMaxEvolveTime.msecs(), framenum);
                break;
            }

        }
    } catch (const exception& e) { 
    }
    
    LINFO("Simulation terminated. Found %d numspots in frame: %d", numSpots, framenum);
    return winners;
}

// ######################################################################

Image< PixRGB<byte > > showAllWinners(const list<Winner> winlist, const Image< PixRGB<byte > > & img, int maxDist) {
    Image< PixRGB<byte > > result = img;
    std::list<Winner>::const_iterator currWinner;
    const PixRGB<byte> color = COL_CANDIDATE;
    int i=0;

    for (currWinner = winlist.begin(); currWinner != winlist.end(); ++currWinner) {
        Point2D<int> ctr = (*currWinner).getWTAwinner().p;
        BitObject bo = (*currWinner).getBitObject();
        Point2D<int> offset = Point2D<int>(2, 2);
        //drawCircle(result, ctr, maxDist, red);
         // write the number of each winner
        //std::string numText = toStr(i);
        // write the text and create the overlay image
        std::ostringstream ss;
        ss.precision(3);
        ss << toStr(i) << "," << 1000.F*bo.getSMV() << " mV";

        writeText(result, ctr+offset, ss.str().data());
        bo.drawOutline(result, color);
        i++;
    }
    return result;
}

// ######################################################################

Image<byte> showAllObjects(const std::list<BitObject>& objs) {
    Image<byte> result(0, 0, ZEROS);
    std::list<BitObject>::const_iterator currObj;
    for (currObj = objs.begin(); currObj != objs.end(); ++currObj) {
        Image<byte> mask = currObj->getObjectMask(byte(255));
        if (result.initialized())
            result = takeMax(result, mask);
        else
            result = mask;
    }
    return result;
}

// ######################################################################
Image< PixRGB<byte > > maskArea(const Image< PixRGB<byte > > & img, const Image< byte > & mask, const byte maskval ) {

    Image< PixRGB<byte >  > resultfinal(img.getDims(), ZEROS);
    resultfinal = img;
    if (mask.getWidth() == img.getWidth() && img.getHeight() == mask.getHeight()) {
        for (int i = 0; i < mask.getWidth(); i++)
            for (int j = 0; j < mask.getHeight(); j++)
                if (mask.getVal(i, j) == maskval)
                    resultfinal.setVal(i, j, 0); // flag as background the considered area
    } else {
        LFATAL("invalid sized image mask; size is %dx%d but should be same size as input frame %dx%d",
                mask.getWidth(), mask.getHeight(), img.getWidth(), img.getHeight());
    }
    return resultfinal;
}

// ######################################################################
Image< PixRGB<byte > > maskArea(const Image< PixRGB<byte > > & img, DetectionParameters *parms, const byte maskval) {

    Image< PixRGB<byte >  > resultfinal(img.getDims(), ZEROS);
    resultfinal = img;

    // The mask is defined by a picture
    if (parms->itsMaskPath.length() > 0) {

        Image<byte> mask(img.getDims(), ZEROS);
        mask = Raster::ReadGray(parms->itsMaskPath.c_str());

        if (mask.getWidth() == img.getWidth() && img.getHeight() == mask.getHeight()) {
            for (int i = 0; i < mask.getWidth(); i++)
                for (int j = 0; j < mask.getHeight(); j++)
                    if (mask.getVal(i, j) == maskval)
                        resultfinal.setVal(i, j, 0); // flag as background the considered area
        } else {
            LFATAL("invalid sized image mask: %s ; size is %dx%d but should be same size as input frame %dx%d",
                    parms->itsMaskPath.c_str(), mask.getWidth(), mask.getHeight(), img.getWidth(), img.getHeight());
        }
    }

    // The mask is defined by a rectangle
    if (parms->itsMaskXPosition != 1 || parms->itsMaskYPosition != 1 || parms->itsMaskWidth != 1 || parms->itsMaskHeight != 1) {
        int mask_is_valid = 0; // is the mask valid ?

        // if the 2 original points are in the picture
        if (parms->itsMaskXPosition > 1 || parms->itsMaskXPosition < img.getWidth() || parms->itsMaskYPosition > 1 || parms->itsMaskYPosition < img.getHeight()) {
            // if the mask size is ok with the picture
            if ((parms->itsMaskXPosition + parms->itsMaskWidth) < img.getWidth() || (parms->itsMaskYPosition + parms->itsMaskHeight) < img.getHeight()) {

                mask_is_valid = 1;

            }
        }

        if (mask_is_valid == 1) // if the mask is valid
        {
            for (int i = parms->itsMaskXPosition; i < (parms->itsMaskXPosition + parms->itsMaskWidth); i++)
                for (int j = parms->itsMaskYPosition; j < (parms->itsMaskYPosition + parms->itsMaskHeight); j++)
                    resultfinal.setVal(i, j, 0); // flag as background the considered area

        } else { // else let the result as the input picture
            resultfinal = img;
        }
    }
    return resultfinal;
}
// ######################################################################
Image< byte > maskArea(const Image< byte >& img, const Image< byte >& mask, const byte maskval ) {

    Image< byte > resultfinal(img.getDims(), ZEROS);
    resultfinal = img;

    if (mask.getWidth() == img.getWidth() && img.getHeight() == mask.getHeight()) {
        for (int i = 0; i < mask.getWidth(); i++)
            for (int j = 0; j < mask.getHeight(); j++)
                if (mask.getVal(i, j) == maskval)
                    resultfinal.setVal(i, j, 0); // flag as background the considered area
    } else {
        LFATAL("invalid sized image mask; size is %dx%d but should be same size as input frame %dx%d",
                mask.getWidth(), mask.getHeight(), img.getWidth(), img.getHeight());
    }
    return resultfinal;
}

// ######################################################################
Image< byte > maskArea(const Image< byte >& img, DetectionParameters *parms, const byte maskval) {

    Image< byte > resultfinal(img.getDims(), ZEROS);
    resultfinal = img;

    // The mask is defined by a picture
    if (parms->itsMaskPath.length() > 0) {

        Image<byte> mask(img.getDims(), ZEROS);
        mask = Raster::ReadGray(parms->itsMaskPath.c_str());

        if (mask.getWidth() == img.getWidth() && img.getHeight() == mask.getHeight()) {
            for (int i = 0; i < mask.getWidth(); i++)
                for (int j = 0; j < mask.getHeight(); j++)
                    if (mask.getVal(i, j) == maskval)
                        resultfinal.setVal(i, j, 0); // flag as background the considered area
        } else {
            LFATAL("invalid sized image mask: %s ; size is %dx%d but should be same size as input frame %dx%d",
                    parms->itsMaskPath.c_str(), mask.getWidth(), mask.getHeight(), img.getWidth(), img.getHeight());
        }
    }

    // The mask is defined by a rectangle
    if (parms->itsMaskXPosition != 1 || parms->itsMaskYPosition != 1 || parms->itsMaskWidth != 1 || parms->itsMaskHeight != 1) {
        int mask_is_valid = 0; // is the mask valid ?

        // if the 2 original points are in the picture
        if (parms->itsMaskXPosition > 1 || parms->itsMaskXPosition < img.getWidth() || parms->itsMaskYPosition > 1 || parms->itsMaskYPosition < img.getHeight()) {
            // if the mask size is ok with the picture
            if ((parms->itsMaskXPosition + parms->itsMaskWidth) < img.getWidth() || (parms->itsMaskYPosition + parms->itsMaskHeight) < img.getHeight()) {

                mask_is_valid = 1;

            }
        }

        if (mask_is_valid == 1) // if the mask is valid
        {
            for (int i = parms->itsMaskXPosition; i < (parms->itsMaskXPosition + parms->itsMaskWidth); i++)
                for (int j = parms->itsMaskYPosition; j < (parms->itsMaskYPosition + parms->itsMaskHeight); j++)
                    resultfinal.setVal(i, j, 0); // flag as background the considered area

        } else { // else let the result as the input picture
            resultfinal = img;
        }
    }
    return resultfinal;
}

// ##################################################################

float getMax(const Image<float> matrix) {

    float maxVal = 0.00;

    for (int i = 0; i < matrix.getWidth(); i++)
        for (int j = 0; j < matrix.getHeight(); j++)
            if ((float) (matrix.getVal(i, j)) > maxVal)
                maxVal = (float) (matrix.getVal(i, j));

    return maxVal;

}



// ##################################################################

Image< byte > getMaskImage(const Image< byte > &img, const list<BitObject> &bitObjectFrameList) {
    if (!bitObjectFrameList.empty()) {
        Image<byte> bgMask = showAllObjects(bitObjectFrameList);
        if (bgMask.getWidth() > 0) {
            Image<byte> cacheImg(img.getDims(), ZEROS);
            byte val;
            for (int i = 0; i < img.getWidth(); i++) {
                for (int j = 0; j < img.getHeight(); j++) {
                    if (bgMask.getVal(i, j) == 255) {
                        // if the pixel is included in an event, clear it
                        val = 0;
                    } else { //otherwise use the image value
                        val = img.getVal(i, j);
                    }
                    cacheImg.setVal(i, j, val);
                }
            }
            return cacheImg;
        } else {
            return img;
        } // if no event found in the frame just return the image
    }
    return img;
}

// ##################################################################

Image< PixRGB<byte> > getBackgroundImage(const Image< PixRGB<byte> > &img,
        const Image< PixRGB<byte> > &currentBackgroundMean, Image< PixRGB<byte> > savePreviousPicture,
        const list<BitObject> &bitObjectFrameList, PixRGB<byte> &avgVal) {
    int numPixels = 0;PixRGB<float> avgValFlt;
    if (!bitObjectFrameList.empty()) {
        Image<byte> bgMask = showAllObjects(bitObjectFrameList);
        if (bgMask.getWidth() > 0) {
            Image< PixRGB<byte> > cacheImg(img.getDims(), ZEROS);
            PixRGB<byte> val;
            for (int i = 0; i < currentBackgroundMean.getWidth(); i++) {
                for (int j = 0; j < currentBackgroundMean.getHeight(); j++) {
                    if (bgMask.getVal(i, j) > 125) {
                        // if the pixel is included in an event -> take the current backgroundValue
                        val = currentBackgroundMean.getVal(i, j);
                        avgValFlt += PixRGB<float> (val);
                        numPixels++;
                     } else { // if the pixel is really a background pixel
                        val = savePreviousPicture.getVal(i, j);
                    }
                    cacheImg.setVal(i, j, val);
                }
            }
            if (numPixels > 0)
                avgVal = PixRGB<byte>(avgValFlt / (float) numPixels);
             return cacheImg;
        } else {
            if (numPixels > 0)
                avgVal = PixRGB<byte>(avgValFlt / (float) numPixels);
             return savePreviousPicture;
        } // if no event found in the frame just add the complete frame
    }
    return img;
}


// ######################################################################
//! compute input filename for current frame
std::string getInputFileName(const std::string& stem,
        const int framenumber) {
    // if there is a '#' in the stem, replace it with the frame number
    std::string::size_type hashpos = stem.find_first_of('#');

    if (hashpos != stem.npos) {
        std::string fname = stem;
        fname.replace(hashpos, 1, sformat("%06d", framenumber));
        return fname;
    }
    // else... no '#', so just return the filename as-is
    return stem;
}


// ######################################################################
// Return the parameters from a comma-delimited string
vector< float > getFloatParameters(const string  &str) {
    vector< float > aFloats;
    istringstream floats(str);
    string floatStr;
    float aFloat;
    while (getline(floats, floatStr, ',')
            && istringstream(floatStr) >> aFloat) {
        aFloats.push_back(aFloat);
    }
    return aFloats;
}

Image<float> convolveFeatures(const ImageSet<float>& imgFeatures,
                                   const ImageSet<float>& filterFeatures)
{
  if (imgFeatures.size() == 0)
    return Image<float>();

  ASSERT(imgFeatures.size() == filterFeatures.size());

  //Compute size of output
  int w = imgFeatures[0].getWidth() - filterFeatures[0].getWidth() + 1;
  int h = imgFeatures[0].getHeight() - filterFeatures[0].getHeight() + 1;

  int filtWidth = filterFeatures[0].getWidth();
  int filtHeight = filterFeatures[0].getHeight();
  int srcWidth = imgFeatures[0].getWidth();

  Image<float> score(w,h, ZEROS);

  for(uint i=0; i<imgFeatures.size(); i++)
  {
    Image<float>::const_iterator srcPtr = imgFeatures[i].begin();
    Image<float>::const_iterator filtPtr = filterFeatures[i].begin();
    Image<float>::iterator dstPtr = score.beginw();

    for(int y=0; y<h; y++)
      for(int x=0; x<w; x++)
      {
        //Convolve the filter
        float val = 0;
        for(int yp = 0; yp < filtHeight; yp++)
          for(int xp = 0; xp < filtWidth; xp++)
          {
            val += srcPtr[(y+yp)*srcWidth + (x+xp)] * filtPtr[yp*filtWidth + xp];
          }

        *(dstPtr++) += val;
      }
  }

  return score;
}


ImageSet<float> getOriHistogram(const Image<float>& mag, const Image<float>& ori, int numOrientations, int numBins)
{
  Dims blocksDims = Dims(
      (int)round((double)mag.getWidth()/double(numBins)),
      (int)round((double)mag.getHeight()/double(numBins)));

  ImageSet<float> hist(numOrientations, blocksDims, ZEROS);

  Image<float>::const_iterator magPtr = mag.begin(), oriPtr = ori.begin();
  //Set the with an height to a whole bin numbers. 
  //If needed replicate the data when summing the bins
  int w = blocksDims.w()*numBins; 
  int h = blocksDims.h()*numBins;
  int magW = mag.getWidth(); 
  int magH = mag.getHeight();
  int histWidth = blocksDims.w(); 
  int histHeight = blocksDims.h();

  for (int y = 1; y < h-1; y ++)
    for (int x = 1; x < w-1; x ++)
    {
      // add to 4 histograms around pixel using linear interpolation
      double xp = ((double)x+0.5)/(double)numBins - 0.5;
      double yp = ((double)y+0.5)/(double)numBins - 0.5;
      int ixp = (int)floor(xp);
      int iyp = (int)floor(yp);
      double vx0 = xp-ixp;
      double vy0 = yp-iyp;
      double vx1 = 1.0-vx0;
      double vy1 = 1.0-vy0;
      

      //If we are outside out mag/ori data, then use the last values in it
      int magX = std::min(x, magW-2);
      int magY = std::min(y, magH-2);
      double mag = magPtr[magY*magW  + magX];
      int ori = int(oriPtr[magY*magW + magX]);

      Image<float>::iterator histPtr = hist[ori].beginw();

      if (ixp >= 0 && iyp >= 0)
        histPtr[iyp*histWidth + ixp] += vx1*vy1*mag;

      if (ixp+1 < histWidth && iyp >= 0)
        histPtr[iyp*histWidth + ixp+1] += vx0*vy1*mag;

      if (ixp >= 0 && iyp+1 < histHeight) 
        histPtr[(iyp+1)*histWidth + ixp] += vx1*vy0*mag;

      if (ixp+1 < histWidth && iyp+1 < histHeight) 
        histPtr[(iyp+1)*histWidth + ixp+1] += vx0*vy0*mag;
    }

  return hist;
}


ImageSet<double> computeFeatures(const ImageSet<float>& hist)
{

  // compute energy in each block by summing over orientations
  Image<double> norm = getHistogramEnergy(hist);

  const int w = norm.getWidth();
  const int h = norm.getHeight();
  
  const int numFeatures = hist.size() +   //Contrast-sensitive features
                    hist.size()/2 + //contrast-insensitive features
                    4 +             //texture features
                    1;              //trancation feature (this is zero map???)

  const int featuresW = std::max(w-2, 0);
  const int featuresH = std::max(h-2, 0);

  ImageSet<double> features(numFeatures, Dims(featuresW, featuresH), ZEROS);

  Image<double>::const_iterator normPtr = norm.begin();
  Image<double>::const_iterator ptr;

  // small value, used to avoid division by zero
  const double eps = 0.0001;

  for(int y=0; y<featuresH; y++)
    for(int x=0; x<featuresW; x++)
    {

      //Combine the norm values of neighboring bins
      ptr = normPtr + (y+1)*w + x+1;
      const double n1 = 1.0 / sqrt(*ptr + *(ptr+1) +
                                   *(ptr+w) + *(ptr+w+1) +
                                   eps);
      ptr = normPtr + y*w + x+1;
      const double n2 = 1.0 / sqrt(*ptr + *(ptr+1) +
                                   *(ptr+w) + *(ptr+w+1) +
                                   eps);
      ptr = normPtr + (y+1)*w + x;
      const double n3 = 1.0 / sqrt(*ptr + *(ptr+1) +
                                   *(ptr+w) + *(ptr+w+1) +
                                   eps);
      ptr = normPtr + y*w + x;      
      const double n4 = 1.0 / sqrt(*ptr + *(ptr+1) +
                                   *(ptr+w) + *(ptr+w+1) +
                                   eps);

      //For texture features
      double t1 = 0, t2 = 0, t3 = 0, t4 = 0;

      // contrast-sensitive features
      uint featureId = 0;
      for(uint ori=0; ori < hist.size(); ori++)
      {
        Image<float>::const_iterator histPtr = hist[ori].begin();
        const float histVal = histPtr[(y+1)*w + x+1];
        double h1 = std::min(histVal * n1, 0.2);
        double h2 = std::min(histVal * n2, 0.2);
        double h3 = std::min(histVal * n3, 0.2);
        double h4 = std::min(histVal * n4, 0.2);

        t1 += h1; t2 += h2; t3 += h3; t4 += h4;

        Image<double>::iterator featuresPtr = features[featureId++].beginw();
        featuresPtr[y*featuresW + x] = 0.5 * (h1 + h2 + h3 + h4);
      }

      // contrast-insensitive features
      int halfOriSize = hist.size()/2;
      for(int ori=0; ori < halfOriSize; ori++)
      {
        Image<float>::const_iterator histPtr1 = hist[ori].begin();
        Image<float>::const_iterator histPtr2 = hist[ori+halfOriSize].begin();
        const double sum = histPtr1[(y+1)*w + x+1] + histPtr2[(y+1)*w + x+1];
        double h1 = std::min(sum * n1, 0.2);
        double h2 = std::min(sum * n2, 0.2);
        double h3 = std::min(sum * n3, 0.2);
        double h4 = std::min(sum * n4, 0.2);

        Image<double>::iterator featuresPtr = features[featureId++].beginw();
        featuresPtr[y*featuresW + x] = 0.5 * (h1 + h2 + h3 + h4);
      }

      // texture features
      Image<double>::iterator featuresPtr = features[featureId++].beginw();
      featuresPtr[y*featuresW + x] = 0.2357 * t1;

      featuresPtr = features[featureId++].beginw();
      featuresPtr[y*featuresW + x] = 0.2357 * t2;
      
      featuresPtr = features[featureId++].beginw();
      featuresPtr[y*featuresW + x] = 0.2357 * t3;

      featuresPtr = features[featureId++].beginw();
      featuresPtr[y*featuresW + x] = 0.2357 * t4;

      // truncation feature
      // This seems to be just 0, do we need it?
      featuresPtr = features[featureId++].beginw();
      featuresPtr[y*featuresW + x] = 0;

    }



  return features;

}


Image<PixRGB<byte> > getHistogramImage(const ImageSet<float>& hist, const int lineSize)
{
  if (hist.size() == 0)
    return Image<PixRGB<byte> >();


  Image<float> img(hist[0].getDims()*lineSize, ZEROS);
  //Create a one histogram with the maximum features for the 9 orientations
  //TODO: features need to be separated
  for(uint feature=0; feature<9; feature++)
  {
    float ori = (float)feature/M_PI + (M_PI/2);
    for(int y=0; y<hist[feature].getHeight(); y++)
    {
      for(int x=0; x<hist[feature].getWidth(); x++)
      {
        float histVal = hist[feature].getVal(x,y);

        //TODO: is this redundant since the first 9 features are
        //contained in the signed 18 features?
        if (hist[feature+9].getVal(x,y) > histVal)
          histVal = 100.f*hist[feature+9].getVal(x,y);
        if (hist[feature+18].getVal(x,y) > histVal)
          histVal = 100.f*hist[feature+18].getVal(x,y);
        if (histVal < 0) histVal = 0; //TODO: do we want this?

        drawLine(img, Point2D<int>((lineSize/2) + x*lineSize,
                                   (lineSize/2) + y*lineSize),
                                   -ori, lineSize,
                                   histVal);
      }
    }
  }

  inplaceNormalize(img, 0.0F, 255.0F);

  return toRGB(img);
}


ImageSet<float> getFeatures(const Image<PixRGB<byte> >& img, int numBins)
{
  int itsNumOrientations = 18;

  Image<float> mag, ori;
  getMaxGradient(img, mag, ori, itsNumOrientations);

  ImageSet<float> histogram = getOriHistogram(mag, ori, itsNumOrientations, numBins);

  ImageSet<double> features = computeFeatures(histogram);

  return features;
}

void getMaxGradient(const Image<PixRGB<byte> >& img,
    Image<float>& mag, Image<float>& ori,
    int numOrientations)
{
  if (numOrientations != 0 &&
      numOrientations > 18)
    LFATAL("Can only support up to 18 orientations for now.");
  
  mag.resize(img.getDims()); ori.resize(img.getDims());

  Image<PixRGB<byte> >::const_iterator src = img.begin();
  Image<float>::iterator mPtr = mag.beginw(), oPtr = ori.beginw();
  const int w = mag.getWidth(), h = mag.getHeight();

  float zero = 0;

  // first row is all zeros:
  for (int i = 0; i < w; i ++) { *mPtr ++ = zero; *oPtr ++ = zero; }
  src += w;

  // loop over inner rows:
  for (int j = 1; j < h-1; j ++)
    {
      // leftmost pixel is zero:
      *mPtr ++ = zero; *oPtr ++ = zero; ++ src;

      // loop over inner columns:
      for (int i = 1; i < w-1; i ++)
        {
          PixRGB<int> valx = src[1] - src[-1];
          PixRGB<int> valy = src[w] - src[-w];

          //Mag
          double mag1 = (valx.red()*valx.red()) + (valy.red()*valy.red());
          double mag2 = (valx.green()*valx.green()) + (valy.green()*valy.green());
          double mag3 = (valx.blue()*valx.blue()) + (valy.blue()*valy.blue());

          double mag = mag1;
          double dx = valx.red();
          double dy = valy.red();

          //Get the channel with the strongest gradient
          if (mag2 > mag)
          {
            dx = valx.green();
            dy = valy.green();
            mag = mag2;
          }
          if (mag3 > mag)
          {
            dx = valx.blue();
            dy = valy.blue();
            mag = mag3;
          }

          *mPtr++ = sqrt(mag);
          if (numOrientations > 0)
          {
            //Snap to num orientations
            double bestDot = 0;
            int bestOri = 0;
            for (int ori = 0; ori < numOrientations/2; ori++) {
              double dot = itsUU[ori]*dx + itsVV[ori]*dy;
              if (dot > bestDot) {
                bestDot = dot;
                bestOri = ori;
              } else if (-dot > bestDot) {
                bestDot = -dot;
                bestOri = ori+(numOrientations/2);
              }
            }
            *oPtr++ = bestOri;

          } else {
            *oPtr++ = atan2(dy, dx);
          }
          ++ src;
        }

      // rightmost pixel is zero:
      *mPtr ++ = zero; *oPtr ++ = zero; ++ src;
    }

  // last row is all zeros:
  for (int i = 0; i < w; i ++) { *mPtr ++ = zero; *oPtr ++ = zero; }
}

Image<double> getHistogramEnergy(const ImageSet<float>& hist)
{
  if (hist.size() == 0)
    return Image<double>();

  Image<double> norm(hist[0].getDims(), ZEROS);

  //TODO: check for overflow
  int halfOriSize = hist.size()/2;
  // compute energy in each block by summing over orientations
  for(int ori=0; ori<halfOriSize; ori++)
  {
    Image<float>::const_iterator src1Ptr = hist[ori].begin();
    Image<float>::const_iterator src2Ptr = hist[ori+halfOriSize].begin();

    Image<double>::iterator normPtr = norm.beginw();
    Image<double>::const_iterator normPtrEnd = norm.end();

    while(normPtr < normPtrEnd)
    {
      *(normPtr++) += (*src1Ptr + *src2Ptr) * (*src1Ptr + *src2Ptr);
      src1Ptr++;
      src2Ptr++;
    }
  }

  return norm;
}

std::vector<double> getFeatures2(Image< byte > &mmapInput, Image<PixRGB <byte> > &rawInput, HistogramOfGradients &hog,
                                Dims scaledDims, Rectangle bbox)
{
    // compute the correct bounding box and cut it out
    Dims dims = rawInput.getDims();
    float scaleW = (float)dims.w()/(float)scaledDims.w();
    float scaleH = (float)dims.h()/(float)scaledDims.h();
    Rectangle bboxScaled = Rectangle::tlbrI(bbox.top()*scaleH, bbox.left()*scaleW,
                                            (bbox.top() + bbox.height())*scaleH,
                                            (bbox.left() + bbox.width())*scaleW);
    bboxScaled = bboxScaled.getOverlap(Rectangle(Point2D<int>(0, 0), dims - 1));

    // scale if needed and cut out the rectangle and save it
    Image<float>  lum,rg,by;
    Image< PixRGB<byte> > rawCroppedInput = crop(rawInput, bboxScaled);
    Image< byte > mmapCroppedInput = crop(mmapInput, bboxScaled);

    // get the static features features used in training
    getLAB(rawCroppedInput, lum, rg, by);
    std::vector<float> hist = hog.createHistogram(lum,rg,by);
    std::vector<double> histDouble(hist.begin(), hist.end());

    // get the motion features used in training
    hist = hog.createHistogram(mmapCroppedInput);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());

    return histDouble;
}

std::vector<double> getFeatures(Image<PixRGB <byte> > &prevInput, Image<PixRGB <byte> > &input,
                                HistogramOfGradients &hog,  Dims scaledDims, Rectangle bbox)
{

    // compute the correct bounding box and cut it out
    Dims dims = input.getDims();
    float scaleW = (float)dims.w()/(float)scaledDims.w();
    float scaleH = (float)dims.h()/(float)scaledDims.h();
    Rectangle bboxScaled = Rectangle::tlbrI(bbox.top()*scaleH, bbox.left()*scaleW,
                                            (bbox.top() + bbox.height())*scaleH,
                                            (bbox.left() + bbox.width())*scaleW);
    bboxScaled = bboxScaled.getOverlap(Rectangle(Point2D<int>(0, 0), dims - 1));
    
    // scale if needed and cut out the rectangle and save it
    Image<float>  lum,rg1,by1;
    Image< PixRGB<byte> > evtImg = crop(prevInput, bboxScaled);
    Image< PixRGB<byte> > evtImgPrev = crop(input, bboxScaled);

    // get the features used in training
    getLAB(evtImg, lum, rg1, by1);
    std::vector<float> hist = hog.createHistogram(lum,rg1,by1);
    std::vector<double> histDouble(hist.begin(), hist.end());
    
    Image<float> rg(evtImg.getDims(), ZEROS);
    Image<float> by(evtImg.getDims(), ZEROS);
    // compute the optic flow
    rutz::shared_ptr<MbariOpticalFlow> flow =
    getOpticFlow
    (Image<byte>(luminance(evtImg)),
    Image<byte>(luminance(evtImgPrev)));
    
    Image<PixRGB<byte> > opticFlow = drawOpticFlow(evtImgPrev, flow);
    //rv->display(opticFlow, frameNum, "opticflow");
    std::vector<rutz::shared_ptr<MbariFlowVector> > vectors = flow->getFlowVectors();
    Image< float > xflow(evtImg.getDims(), ZEROS);
    Image< float > yflow(evtImg.getDims(), ZEROS);
    
    // we are going to assume we have a sparse flow field
    // and random access on the field
    for(uint v = 0; v < vectors.size(); v++)
    {
      Point2D<float> pt = vectors[v]->p1;
      uint i = pt.i;
      uint j = pt.j;
      float xmag  = 100.0f * vectors[v]->xmag;
      float ymag  = 100.0f * vectors[v]->ymag;
      xflow.setVal(i,j, xmag );
      yflow.setVal(i,j, ymag );
      //if(xmag > 0.0) xflow.setVal(i,j, xmag );
      //if(ymag > 0.0) yflow.setVal(i,j, ymag );
    }
    
    Image<float> mag, ori;
    gradientSobel(xflow, mag, ori, 3);
    //rv->display(static_cast< Image<byte> >(xflow), frameNum, "XFlow");
    //rv->display(static_cast< Image<byte> >(mag), frameNum, "XGradmag");
    hist = hog.createHistogram(static_cast< Image<byte> >(xflow),rg,by);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());
    
    gradientSobel(yflow, mag, ori, 3);
    //rv->display(static_cast< Image<byte> >(yflow), frameNum, "YFlow");
    //rv->display(static_cast< Image<byte> >(mag), frameNum, "YGradmag");
    hist = hog.createHistogram(static_cast< Image<byte> >(yflow),rg,by);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());
    
    Image<float> yFilter(1, 3, ZEROS);
    Image<float> xFilter(3, 1, ZEROS);
    yFilter.setVal(0, 0, 1.F);
    yFilter.setVal(0, 2, 1.F);
    xFilter.setVal(0, 0, 1.F);
    xFilter.setVal(2, 0, 1.F);
    
    Image<float> yyflow = sepFilter(yflow, Image<float>(), yFilter, CONV_BOUNDARY_CLEAN);
    Image<float> xxflow = sepFilter(xflow, xFilter, Image<float>(), CONV_BOUNDARY_CLEAN);
    hist = hog.createHistogram(static_cast< Image<byte> >(yyflow),rg,by);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());
    hist = hog.createHistogram(static_cast< Image<byte> >(xxflow),rg,by);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());
    
    //rv->display(static_cast< Image<byte> >(yyflow), frameNum, "YYder");
    //rv->display(static_cast< Image<byte> >(xxflow), frameNum, "XXder");
    
    Image<float> yxflow = sepFilter(yflow, xFilter, Image<float>(), CONV_BOUNDARY_CLEAN);
    Image<float> xyflow = sepFilter(xflow, Image<float>(), yFilter, CONV_BOUNDARY_CLEAN);
    hist = hog.createHistogram(static_cast< Image<byte> >(yxflow),rg,by);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());
    hist = hog.createHistogram(static_cast< Image<byte> >(xyflow),rg,by);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());
    
    //rv->display(static_cast< Image<byte> >(yxflow), frameNum, "YXder");
    //rv->display(static_cast< Image<byte> >(xyflow), frameNum, "XYder");
    hist = hog.createHistogram(static_cast< Image<byte> >(yxflow),rg,by);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());
    hist = hog.createHistogram(static_cast< Image<byte> >(xyflow),rg,by);
    histDouble.insert(histDouble.begin(), hist.begin(), hist.end());

    return histDouble;
}