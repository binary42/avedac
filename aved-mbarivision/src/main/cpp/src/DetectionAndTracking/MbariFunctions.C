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

#include "DetectionAndTracking/MbariFunctions.H"
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
#include "Media/MediaSimEvents.H"
#include "Media/SimFrameSeries.H"
#include "Media/MediaOpts.H"
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
#include "DetectionAndTracking/DetectionParameters.H"
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
std::list<BitObject> extractBitObjects(const Image<PixRGB <byte> >& graphBitImg,
        const Point2D<int> seed,
        Rectangle region,
        const int minSize,
        const int maxSize) {

    std::list<BitObject> bos;
    Dims d = graphBitImg.getDims();
    region = region.getOverlap(Rectangle(Point2D<int>(0, 0), d - 1));
    std::list<PixRGB<byte>> seedColors;
    Image<byte> labelImg(graphBitImg.getDims(), ZEROS);
    Image<byte> bitImg(graphBitImg.getDims(), ZEROS);
    PixRGB<byte> black(0,0,0);
    bool found = false;

    // get the bit object(s) in this region
    for (int ry = region.top(); ry <= region.bottomO(); ++ry)
        for (int rx = region.left(); rx <= region.rightO(); ++rx) {
            PixRGB<byte> newColor = graphBitImg.getVal(Point2D<int>(rx,ry));
            found = false;

            std::list<PixRGB<byte>>::const_iterator iter = seedColors.begin();
            while (iter != seedColors.end()) {
                PixRGB<byte> color = (*iter);
                // found  seed color
                if (color == newColor) {
                    found = true;
                    break;
                    }
                iter++;
            }
            if (!found  && newColor != black) {
                seedColors.push_back(newColor);
                // create a binary representation with the 1 equal to the
                // color at the center of the seed everything else 0
                Image< PixRGB<byte> >::const_iterator sptr = graphBitImg.begin();
                Image<byte>::iterator rptr = bitImg.beginw();
                while (sptr != graphBitImg.end())
                    *rptr++ = (*sptr++ == newColor) ? 1 : 0;

                BitObject obj;
                Image<byte> dest = obj.reset(bitImg, Point2D<int>(rx, ry));

                 // if the object is in range, keep it
                 if (obj.getArea() >= minSize && obj.getArea() <= maxSize) {
                     LDEBUG("found object size: %d", obj.getArea());
                     bos.push_back(obj);
                 }
                 else
                     LDEBUG("found object but out of range in size %d minsize: %d maxsize: %d ",\
                    obj.getArea(), minSize, maxSize);

            }
    }
    return bos;
}

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

        std::list<BitObject> sobjs = extractBitObjects(graphBitImg, winner, region, p.itsMinEventArea, p.itsMaxEventArea);

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
            // loop until we find a closest object
            while (keepGoing) {
                // no object left -> go to the next salient point
                if (sobjs.empty()) break;
                // find the largest object within acceptable distance to the winner
                largest = sobjs.begin();
                int maxSize = 0;

                float distul, distbr;
                for (siter = sobjs.begin(); siter != sobjs.end(); ++siter) {
                    r2 = siter->getBoundingBox();
                     // calculate distance between bounding box corners
                      distul = sqrt(pow((double)(r1.top() - r2.top()),2.0) +  pow((double)(r1.left() - r2.left()),2.0));
                      distbr = sqrt(pow((double)(r1.bottomI() - r2.bottomI()),2.0) + pow((double)(r1.rightI() - r2.rightI()),2.0));

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

        int maxArea = boFOA.getArea();
        int minArea = p.itsMinEventArea;

        // if the graph or binary segmented images have no deviation, no sense in extracting objects from them
        LINFO("Extracting bit objects from winning point %i: %d %d/region %s minSize %d maxSize %d", \
                i, winner.i, winner.j, convertToString(regionGraph).c_str(), minArea, maxArea);
        sobjsGraph = extractBitObjects(graphBitImgMasked, winner, regionGraph, minArea, maxArea);
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
                if (biter->doesIntersect(bo))
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
                    if (biter->doesIntersect(boFOA))
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
    brain->reset(MC_RECURSE);
    seq->resetTime();
    float scale = 1.0F;
    Dims size = img.getDims();
    Dims newSize = size;

    // Scale down if either dimension is greater than 480
    if (size.w() > 480 || size.h() > 480){
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

                    if (bo.isValid() && bo.getArea() > 0)
                        winners.push_back(Winner(win, bo));
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
        const list<BitObject> &bitObjectFrameList) {
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
                    } else { // if the pixel is really a background pixel
                        val = savePreviousPicture.getVal(i, j);
                    }
                    cacheImg.setVal(i, j, val);
                }
            }
            return cacheImg;
        } else {
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

// ######################################################################
void adjustGamma(Image<byte>&lum, std::map<int, double> &cdfw, Image<PixHSV<float> >&hsvRes)
 {
  Image<byte>::iterator aptr = lum.beginw();
  Image<PixHSV<float> >::iterator itr = hsvRes.beginw();
  while(itr != hsvRes.endw())
    {
      int valint = (int) (*aptr);
      float gamma = 1 - cdfw[valint];
      // apply gamma in the value only, preserving hue and saturation
      itr->p[2] = pow(itr->p[2], gamma);
      ++aptr;
      ++itr;
    }
}

// ######################################################################
 Image<PixRGB<byte> > enhanceImage(const Image<PixRGB<byte> >& img, std::map<int, double> &cdfw)
{
    Image<byte> lumImg = luminance(img);
    Image<PixRGB<float> > input = img;
    Image<PixHSV<float> > hsvRes = static_cast< Image<PixHSV<float> > > (input/255);
    adjustGamma(lumImg, cdfw, hsvRes);
    Image<PixRGB<float> > foutput = static_cast< Image<PixRGB<float> > > (hsvRes);
    foutput = foutput * 255.0F;
    Image<PixRGB<byte> > rgbImg = static_cast< Image<PixRGB<int> >>(foutput);

    return rgbImg;
}

// ######################################################################
std::map<int, double> updateGammaCurve(const Image<PixRGB<byte> >& img, std::map<int, double> &pdf, bool init)
{
    LINFO("==========================================================================================Updating gamma curve");
    std::map<int, double> pdfw,cdfw;
    float pdfmin = 1.f;
    float pdfmax = 0.f;

    if (init){
        Dims d = img.getDims();
        float ttl = d.w()*d.h();
        Histogram h(luminance(img));
        for(int i=0 ; i< 256; i++) {
            // fast pdf approximation
            pdf[i] = h.getValue(i)/ttl;
            if (pdf[i] < pdfmin)
                pdfmin = pdf[i];
            if (pdf[i] > pdfmax)
                pdfmax = pdf[i];
        }
    }
    else {
        for(int i=0 ; i< 256; i++) {
            if (pdf[i] < pdfmin)
                pdfmin = pdf[i];
            if (pdf[i] > pdfmax)
                pdfmax = pdf[i];
        }
    }

    // fast weighting distribution
    float alpha = 1.0f;
    float sumpdfw = 0.f;
    for(int i=0 ; i<  256; i++) {
        pdfw[i] = pdfmax * pow( (pdf[i] - pdfmin) / (pdfmax - pdfmin) , alpha);
        sumpdfw += pdfw[i];
    }

    // modified cumulative distribution function
    for(int i=0; i< 256; i++)
      for(int k=0; k< i; k++)
        cdfw[i] += pdfw[k]/sumpdfw;

    return cdfw;
}

// ######################################################################
float updateEntropyModel(const Image<PixRGB<byte> >& img, std::map<int, double> &pdf){
    Dims d = img.getDims();
    Histogram h(luminance(img));
    float ttl = d.w()*d.h();

    // fast pdf approximation
    for(int i=0 ; i< 256; i++)
        pdf[i] = h.getValue(i)/ttl;

    // calculate entropy
    float H = 0.f;
    for(int i=0 ; i< 256; i++) {
        if (pdf[i] > 0.F)
            H += pdf[i]*log(pdf[i]);
    }

    return -1*H;
}