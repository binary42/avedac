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
#include "Media/MediaSimEvents.H"
#include "Media/SimFrameSeries.H"
#include "Media/MediaOpts.H"
#include "Neuro/StdBrain.H"
#include "Neuro/NeuroOpts.H"
#include "Neuro/NeuroSimEvents.H"
#include "Neuro/SimulationViewer.H"
#include "Neuro/VisualCortex.H"
#include "Raster/Raster.H"
#include "Simulation/SimEventQueue.H"
#include "Simulation/SimEvents.H"
#include "Util/Timer.H"
#include "Util/Pause.H" 
#include "rutz/shared_ptr.h"
#include "Image/BitObject.H"
#include "Image/DrawOps.H"
#include "Image/Kernels.H"      // for twofiftyfives()
#include "DetectionAndTracking/DetectionParameters.H"
 
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
    Image<byte> labelImg(graphBitImg.getDims(), ZEROS);
    Image<byte> bitImg(graphBitImg.getDims(), ZEROS); 
    PixRGB<byte> color = graphBitImg.getVal(seed);
    const PixRGB<byte> black = PixRGB<byte>(0,0,0);

    // create a binary representation with the 1 equal to the 
    // color at the center of the seed everything else 0
    Image< PixRGB<byte> >::const_iterator sptr = graphBitImg.begin();
    Image<byte>::iterator rptr = bitImg.beginw();
    if (color != black) {
        while (sptr != graphBitImg.end())
            *rptr++ = (*sptr++ == color) ? 1 : 0;
    }
 
    // get the bit object(s) in this region
    for (int ry = region.top(); ry <= region.bottomO(); ++ry)
        for (int rx = region.left(); rx <= region.rightO(); ++rx) {
            // this location doesn't have anything -> never mind
            if (bitImg.getVal(rx, ry) == 0) continue;

            // got this guy already -> never mind
            if (labelImg.getVal(rx, ry) > 0) continue;

            BitObject obj;

            Image<byte> dest = obj.reset(bitImg, Point2D<int>(rx, ry));
            labelImg = takeMax(labelImg, dest);

            // if the object is in range, keep it
            if (obj.getArea() > minSize && obj.getArea() < maxSize) {
                LDEBUG("found object size: %d", obj.getArea());
                bos.push_back(obj);
            }
            else
                LDEBUG("found object but out of range in size %d minsize: %d maxsize: %d ",\
		 obj.getArea(), minSize, maxSize);
        }
    return bos;
}

// ######################################################################

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

            if (obj.getArea() > minSize && obj.getArea() < maxSize) bos.push_back(obj);
        }
    //LINFO("tobj = %i; tmask = %i",tobj,tmask);
    return bos;
}  

// ######################################################################

std::list<BitObject> getSalientObjects(const Image< byte >& bitImg, const list<WTAwinner> &winners) {
    // this should be 2^(smlev - 1)
    const int rectRad = 20;
    DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    std::list<WTAwinner>::const_iterator iter = winners.begin();
    std::list<BitObject> bos;
    Dims d = bitImg.getDims();
    
    //go through each winner and extract salient regions
    while (iter != winners.end()) {
        Point2D<int> winner = (*iter).p;

        // extract all the bitObjects at the salient location
        Rectangle region = Rectangle::tlbrI(winner.j - rectRad, winner.i - rectRad,
                winner.j + rectRad, winner.i + rectRad);

        region = region.getOverlap(Rectangle(Point2D<int>(0, 0), d - 1));

        LINFO("Extracting bit objects from winning point: %d %d/region %s minSize %d maxSize %d",             \
        winner.i, winner.j, convertToString(region).c_str(), p.itsMinEventArea, p.itsMaxEventArea);

        std::list<BitObject> sobjs = extractBitObjects(bitImg, region, \
        p.itsMinEventArea, p.itsMaxEventArea);

	LDEBUG("Found bitobject(s) in bitImg: %d", sobjs.size());
        
        // if no objects found so need to look for them so skip to the next winner
        if (sobjs.size() > 0) {

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
                    (*largest).setSMV((*iter).sv);
                    bos.push_back(*largest);
                }
            } // end while keepGoing
        } // end if found objects
        iter++;
        sobjs.clear();
    }// end while iter != winners.end()
    return bos;
}

// ######################################################################

std::list<BitObject> getSalientObjects(const Image< PixRGB<byte> >& graphBitImg, 
    const list<WTAwinner> &winners) {
    // this should be 2^(smlev - 1)
    const int rectRad = 20;
    DetectionParameters p = DetectionParametersSingleton::instance()->itsParameters;
    std::list<WTAwinner>::const_iterator iter = winners.begin();
    std::list<BitObject> bos;
    Dims d = graphBitImg.getDims();

    //go through each winner and extract salient regions
    while (iter != winners.end()) {
        Point2D<int> winner = (*iter).p;

        // extract all the bitObjects at the salient location
        Rectangle region = Rectangle::tlbrI(winner.j - rectRad, winner.i - rectRad,
                winner.j + rectRad, winner.i + rectRad);

        region = region.getOverlap(Rectangle(Point2D<int>(0, 0), d - 1));

        LINFO("Extracting bit objects from winning point: %d %d/region %s minSize %d maxSize %d",             \
        winner.i, winner.j, convertToString(region).c_str(), p.itsMinEventArea, p.itsMaxEventArea);

        std::list<BitObject> sobjsgraph = extractBitObjects(graphBitImg, winner, region, \
        p.itsMinEventArea,  p.itsMaxEventArea);
 
	LDEBUG("Found bitobject(s) in graphBitImg: %d", sobjsgraph.size());

        // if no objects found so need to look for them so skip to the next winner
        if (sobjsgraph.size() > 0) {

            bool keepGoing = true;
            // loop until we find a new object that doesn't overlap with anything
            // that we have found so far, or until we run out of objects
            while (keepGoing) {
                // no object left -> go to the next salient point
                if (sobjsgraph.empty()) break;

                std::list<BitObject>::iterator biter, siter, largest;

                // find the largest object
                largest = sobjsgraph.begin();
                int maxSize = 0;
                for (siter = sobjsgraph.begin(); siter != sobjsgraph.end(); ++siter)
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
                        sobjsgraph.erase(largest);
                        keepGoing = true;
                        break;
                    }

                // so, did we end up finding a BitObject that we can store?
                if (!keepGoing) {
                    (*largest).setSMV((*iter).sv);
                    bos.push_back(*largest);
                }
            } // end while keepGoing
        } // end if found objects
        iter++;
        sobjsgraph.clear();
    }// end while iter != winners.end()
    return bos;
}

// ######################################################################

std::list<WTAwinner> getGraphWinners(const Image< PixRGB<byte> >& graphBitImg,
        int framenum) {

    std::list<PixRGB<byte> > colors; 
    std::list<WTAwinner> winners;
    PixRGB<byte> seedColor;
    const int w = graphBitImg.getWidth();
    const int h = graphBitImg.getHeight();
    int numSpots = 0;
    bool found;

    for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) { 
            seedColor = graphBitImg.getVal(i, j);
            found = false;
            // add new colors to the list
            std::list<PixRGB<byte> >::const_iterator iter = colors.begin();
            while (iter != colors.end()) {
                PixRGB<byte> color = (*iter);
                if (color == seedColor) { 
                    found = true;
                    break;
                }
            iter++;
            }
            if (found == false) {
                colors.push_back(seedColor);
                WTAwinner win = WTAwinner::NONE();
                win.sv = 0.f;
                numSpots++;
                LINFO("##### winner #%d found at [%d; %d]  frame: %d#####",
                        numSpots, win.p.i, win.p.j, framenum);
                winners.push_back(win);
            }
        }
    }

    return winners;
}

// ######################################################################
list<WTAwinner> getSalientWinners(
        nub::soft_ref<SimOutputFrameSeries> simofs,
        const Image< PixRGB<byte> > &img,
        nub::soft_ref<StdBrain> brain,
        nub::soft_ref<SimEventQueue> seq,
        float maxEvolveTime,
        int maxNumSalSpots,
        int framenum
        ) {
    std::list<WTAwinner> winners;
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
    brain->getWTA()->reset(MC_RECURSE);
 
    // initialize the max time to simulate
    const SimTime simMaxEvolveTime = SimTime::MSECS(seq->now().msecs()) + SimTime::MSECS(p.itsMaxEvolveTime);

    rutz::shared_ptr<SimEventInputFrame>
            eif(new SimEventInputFrame(brain.get(),
            GenericFrame(img),
            framenum));

    seq->post(eif);
 
    try { 

        // main loop:
        while (status == SIM_CONTINUE) {

            // evolve brain:
            brain->evolve(*seq);

            // switch to next time step:
            status = seq->evolve();

            if (SeC<SimEventWTAwinner> e = seq->check<SimEventWTAwinner > (0)) {

                WTAwinner win = e->winner();

                LINFO("##### winner #%d found at [%d; %d] with %f voltage frame: %d#####",
                        numSpots, win.p.i, win.p.j, win.sv, framenum);
		
		winners.push_back(win);
                numSpots++;

                // if a boring event detected, and not keeping boring WTA points then break simulation
                if (win.boring && p.itsKeepWTABoring == false) {
                    rutz::shared_ptr<SimEventBreak>
                            e(new SimEventBreak(0, "##### boring event detected #####"));
                    seq->post(e);
                }

                if (numSpots >= maxNumSalSpots) {
                    rutz::shared_ptr<SimEventBreak>
                            e(new SimEventBreak(0, "##### found maximum number of salient spots #####"));
                    seq->post(e);
                }

                LINFO("##### time now:%f msecs max evolve time:%f msecs frame: %d #####", seq->now().msecs(), simMaxEvolveTime.msecs(), framenum);

                if (seq->now().msecs() >= simMaxEvolveTime.msecs()) {
                    LINFO("##### time limit reached time now:%f msecs max evolve time:%f msecs frame: %d #####", seq->now().msecs(), simMaxEvolveTime.msecs(), framenum);
                    rutz::shared_ptr<SimEventBreak>
                            e(new SimEventBreak(0, "##### time limit reached #####"));
                    seq->post(e);
                } 

                // Evolve output frame series. It will trigger a save() on our
                // modules as needed, before we start loading new inputs and
                // processing them for the new time step.
                simofs->evolve(*seq);
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

Image< PixRGB<byte > > showAllWinners(const list<WTAwinner> winlist, const Image< PixRGB<byte > > & img, int maxDist) {
    Image< PixRGB<byte > > result = img;
    std::list<WTAwinner>::const_iterator currWinner;
    const PixRGB<byte> red = PixRGB<byte > (255, 0, 0);

    for (currWinner = winlist.begin(); currWinner != winlist.end(); ++currWinner) {
        Point2D<int> ctr = (*currWinner).p;
        drawCircle(result, ctr, maxDist, red);
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
Image< PixRGB<byte > > maskArea(const Image< PixRGB<byte > > & img, DetectionParameters *parms) {

    Image< PixRGB<byte >  > resultfinal(img.getDims(), ZEROS);
    resultfinal = img;

    // The mask is defined by a picture
    if (parms->itsMaskPath.length() > 0) {

        Image<byte> mask(img.getDims(), ZEROS);
        mask = Raster::ReadGray(parms->itsMaskPath.c_str());

        if (mask.getWidth() == img.getWidth() && img.getHeight() == mask.getHeight()) {
            for (int i = 0; i < mask.getWidth(); i++)
                for (int j = 0; j < mask.getHeight(); j++)
                    if (mask.getVal(i, j) >= 127)
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
Image< byte > maskArea(const Image< byte >& img, DetectionParameters *parms) {

    Image< byte > resultfinal(img.getDims(), ZEROS);
    resultfinal = img;

    // The mask is defined by a picture
    if (parms->itsMaskPath.length() > 0) {

        Image<byte> mask(img.getDims(), ZEROS);
        mask = Raster::ReadGray(parms->itsMaskPath.c_str());

        if (mask.getWidth() == img.getWidth() && img.getHeight() == mask.getHeight()) {
            for (int i = 0; i < mask.getWidth(); i++)
                for (int j = 0; j < mask.getHeight(); j++)
                    if (mask.getVal(i, j) >= 127)
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

Image< PixRGB<byte> > getImageToAddToTheBackground(const Image< PixRGB<byte> > &img,
        const Image< PixRGB<byte> > &currentBackgroundMean, Image< PixRGB<byte> > savePreviousPicture,
        const list<BitObject> &bitObjectFrameList) {
    if (!bitObjectFrameList.empty()) {
        Image<byte> bgMask = showAllObjects(bitObjectFrameList);
        if (bgMask.getWidth() > 0) {
            Image< PixRGB<byte> > imgToAddToTheCache(img.getDims(), ZEROS);
            PixRGB<byte> val;
            for (int i = 0; i < currentBackgroundMean.getWidth(); i++) {
                for (int j = 0; j < currentBackgroundMean.getHeight(); j++) {
                    if (bgMask.getVal(i, j) > 125) {
                        // if the pixel is included in an event -> take the current backgroundValue
                        val = currentBackgroundMean.getVal(i, j);
                    } else { // if the pixel is really a background pixel
                        val = savePreviousPicture.getVal(i, j);
                    }
                    imgToAddToTheCache.setVal(i, j, val);
                }
            }
            return imgToAddToTheCache;
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

