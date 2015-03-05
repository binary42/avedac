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

/*!@file MbariVisualEvent.C classes useful for event tracking */

#include "Image/OpenCVUtil.H"
#include "Image/DrawOps.H"
#include "Image/Image.H"
#include "Image/Pixels.H"
#include "Image/Rectangle.H"
#include "Image/ShapeOps.H"
#include "Image/Transforms.H"
#include "Image/colorDefs.H"
#include "Util/Assert.H"
#include "Util/StringConversions.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "Media/MbariResultViewer.H"
#include "Image/Geometry2D.H"
#include <algorithm>
#include <istream>
#include <ostream>

using namespace std;

namespace MbariVisualEvent {
	
  // ######################################################################
  // ###### Token
  // ######################################################################
  Token::Token()
    : bitObject(),
      location(),
      prediction(),
      line(),
      angle(0.0F),
      foe(0.0F,0.0F),
      frame_nr(0),
      written(false),
      scaleW(1.0f),
      scaleH(1.0f)
  {}
  // ######################################################################
  Token::Token (BitObject bo, uint frame)
    : bitObject(bo),
      location(bo.getCentroidXY()),
      prediction(),
      line(),
      angle(0.0F),
      foe(0.0F,0.0F),
      frame_nr(frame),
      written(false),
      scaleW(1.0f),
      scaleH(1.0f) {
    }
    // ######################################################################
    Token &Token::operator=(const Token& tk) {
        this->angle = tk.angle;
        this->foe = tk.foe;
        this->line = tk.line;
        this->bitObject = tk.bitObject;
        this->location = tk.location;
        this->prediction = tk.prediction;
        this->written = tk.written;
        this->scaleW = tk.scaleW;
        this->scaleH = tk.scaleH;
	return *this;
    }    
    // ######################################################################
    Token::Token(BitObject bo, uint frame, const MbariMetaData& m, float w, float h)
    : bitObject(bo),
      location(bo.getCentroidXY()),
      prediction(),
      line(),
      angle(0.0F),
      foe(0.0F,0.0F),
      frame_nr(frame),
      mbarimetadata(m),
      written(false),
      scaleW(w),
      scaleH(h) 
  { }
	
  // ######################################################################
  Token::Token (std::istream& is)
  {
    readFromStream(is);
  }
	
  // ######################################################################
  void Token::writeToStream(std::ostream& os)
  {
    if(written == false) {
      os << frame_nr << ' ';
      mbarimetadata.writeToStream(os); 
      location.writeToStream(os);
      prediction.writeToStream(os);
      line.writeToStream(os);
      os << angle << '\n';
      bitObject.writeToStream(os);
      os << "\n";
      foe.writeToStream(os);  
    }
    written = true;
  }
	
  // ######################################################################
  void Token::readFromStream(std::istream& is)
  {
    is >> frame_nr;  
    mbarimetadata.readFromStream(is);
    location.readFromStream(is);
    prediction.readFromStream(is);
    line.readFromStream(is);
    is >> angle;
    bitObject = BitObject(is);
    foe.readFromStream(is);  
  }
	
  // ######################################################################
  void Token::writePosition(std::ostream& os) const
  {
    location.writeToStream(os);
  }
	
	
  // ######################################################################
  // ####### PropertyVectorSet
  // ######################################################################
  PropertyVectorSet::PropertyVectorSet()
  {}
	
  // ######################################################################
  PropertyVectorSet::~PropertyVectorSet()
  {
    std::vector< std::vector<float> >::iterator i;
    for (i= itsVectors.begin(); i != itsVectors.end(); ++i)
      i->clear();
	
    itsVectors.clear();
  }
  // ######################################################################
  PropertyVectorSet::PropertyVectorSet(std::istream& is)
  {
    readFromStream(is);
  }
  // ######################################################################
  void PropertyVectorSet::writeHeaderToStream(std::ostream& os)
  {
    uint s2;
    if (itsVectors.empty()) s2 = 0;
    else s2 = itsVectors.front().size();
	
    os << itsVectors.size() << " " << s2 << "\n";
  }
  // ######################################################################
  void PropertyVectorSet::writeToStream(std::ostream& os)
  {
    uint s2;
    if (itsVectors.empty()) s2 = 0;
    else s2 = itsVectors.front().size();
	
    for (uint i = 0; i < itsVectors.size(); ++i)
      {
        LDEBUG("Writing property vector set %d to stream", i);
        for (uint j = 0; j < s2; ++j)
          os << itsVectors[i][j] << " ";
	
        os << "\n";
      }
  }
	
  // ######################################################################
  void PropertyVectorSet::readFromStream(std::istream& is)
  {
    uint s1, s2;
    is >> s1; is >> s2;
    itsVectors = std::vector< std::vector<float> > (s1, std::vector<float>(s2));
	
    for (uint i = 0; i < s1; ++i)
      for (uint j = 0; j < s2; ++j)
        is >> itsVectors[i][j];
  }
	
  // ######################################################################
  std::vector<float> PropertyVectorSet::getPropertyVectorForEvent(const int num)
  {
    for (uint i = 0; i < itsVectors.size(); ++i)
      if ((int)(itsVectors[i][0]) == num) return itsVectors[i];
	
    LFATAL("property vector for event number %d not found!", num);
    return std::vector<float>();
  }
	
  // ######################################################################
  // ####### VisualEvent
  // ######################################################################
  VisualEvent::VisualEvent(Token tk, const DetectionParameters &parms, MbariImage< PixRGB<byte> >& img)
    : startframe(tk.frame_nr),
      endframe(tk.frame_nr),
      max_size(tk.bitObject.getArea()),
      min_size(tk.bitObject.getArea()),
      maxsize_framenr(tk.frame_nr),
      itsState(VisualEvent::OPEN),
      itsTrackerChanged(true),
      houghArea(0),
      houghConstant(DEFAULT_FORGET_CONSTANT),
      itsDetectionParms(parms)
  {
    LDEBUG("tk.location = (%g, %g); area: %i",tk.location.x(),tk.location.y(),
           tk.bitObject.getArea());
    tokens.push_back(tk);
    ++counter;
    myNum = counter;
    validendframe = endframe;
    vector< float > p;
    float mnoise = 0.1F;
    float pnoise = 0.0F;
     //TODO: need check on values
    p = getFloatParameters(parms.itsXKalmanFilterParameters);
    pnoise = p.at(0); mnoise = p.at(1);
    xTracker.init(tk.location.x(),pnoise,mnoise);
    LINFO("Kalman X tracker parameters process noise: %g measurement noise: %g", pnoise, mnoise);

    p = getFloatParameters(parms.itsYKalmanFilterParameters);
    pnoise = p.at(0); mnoise = p.at(1);
    LINFO("Kalman Y tracker parameters process noise: %g measurement noise: %g", pnoise, mnoise);
    yTracker.init(tk.location.y(),pnoise,mnoise);
    switch (parms.itsTrackingMode) {
      case(TMKalmanFilter):
        itsTrackerType = KALMAN;
      break;
      case(TMNearestNeighbor):
        itsTrackerType = NN;
      break;
      case(TMHough):
        resetHoughTracker(img, tk.bitObject, DEFAULT_SCALE_INCREASE);
        itsTrackerType = HOUGH;
      break;
      case(TMNearestNeighborHough):
        itsTrackerType = NN;
      break;
      case(TMKalmanHough):
        itsTrackerType = KALMAN;
      break;
      case(TMNone):
        // just placeholder
        itsTrackerType = NN;
      break;
    }
  }
  // initialize static variables
  uint VisualEvent::counter = 0;
  const std::string VisualEvent::trackerName[3] = {"NearestNeighbor", "Kalman", "Hough"};

  // ######################################################################
  VisualEvent::~VisualEvent()
  {
    tokens.clear();
  }
  // ######################################################################
  VisualEvent::VisualEvent(std::istream& is)
  {
    readFromStream(is);
  }
	
  // ######################################################################
  void VisualEvent::writeToStream(std::ostream& os)
  {  
	  
    os << myNum << " " << (int) itsState << " " << startframe << " " << endframe << "\n";
    os << max_size << " " << maxsize_framenr << "\n";
	
    xTracker.writeToStream(os);
    yTracker.writeToStream(os);
	
    int ntokens = 0;
    for (uint i = 0; i < tokens.size(); ++i)
      if(tokens[i].written == false) ntokens++;
	
    os << ntokens << "\n";
	  
    for (uint i = 0; i < tokens.size(); ++i)
      if(tokens[i].written == false) {
        LDEBUG("Writing VisualEvent  %d Token %d", myNum, i);
        tokens[i].writeToStream(os);
      }
	
    os << "\n";	    
  }
	
  // ######################################################################
  void VisualEvent::readFromStream(std::istream& is) 
  {
    int state;
    is >> myNum;
    is >> state;
    is >> startframe;
    is >> endframe;
    is >> max_size;
    is >> maxsize_framenr;
	  
    itsState = (VisualEvent::State)state;
	
    xTracker.readFromStream(is);
    yTracker.readFromStream(is);
	
    int t = 0;
    is >> t;
	
    for (int i = 0; i < t; ++i) {
      tokens.push_back(Token(is));
      LINFO("Reading VisualEvent %d Token %ld", myNum, tokens.size());
    }
  }
  // ######################################################################
  void VisualEvent::writePositions(std::ostream& os) const
  {
    for (uint i = 0; i < tokens.size(); ++i)
      tokens[i].writePosition(os);
	
    os << "\n";
  }

  // ######################################################################
  void VisualEvent::setTrackerType(VisualEvent::TrackerType type)
  {
    if (itsTrackerType != type) {
     LINFO("Event %i switching to %s ", myNum, trackerName[type].c_str());
     itsTrackerChanged = true;
    }
    else {
     itsTrackerChanged = false;
    }
    itsTrackerType = type;
  }

// ######################################################################
  float VisualEvent::getAcceleration() const
  {
    if (getNumberOfFrames() > 2) {
      //int numFrames = getNumberOfFrames();
      int numSamples = 3;//std::min(4,numFrames); if need to do larger average
      uint endFrame = getEndFrame();
      uint frameNum = endFrame - numSamples + 1;
      bool init = false;
      float lv = 0.F;
      float asum = 0.F;

      while(frameNum < endFrame) {
        Token t1 = getToken(frameNum);
        Token t2 = getToken(frameNum+1);
        if (t1.bitObject.isValid() && t2.bitObject.isValid()) {
        Point2D<int> p2 = t2.bitObject.getCentroid();
        Point2D<int> p1 = t1.bitObject.getCentroid();
        // distance between centroids
        float v = sqrt(pow((double)(p1.i - p2.i),2.0) + pow((double)(p1.j - p2.j),2.0));

        if (init)
          asum += (v - lv);
        else
         init = true;
        lv = v;
        }
        frameNum++;
      }
      return asum/numSamples;
    }
    return 0.F;
  }

  // ######################################################################
  Point2D<int> VisualEvent::predictedLocation()
  {
    int x = int(xTracker.getEstimate() + 0.5F);
    int y = int(yTracker.getEstimate() + 0.5F);
    return Point2D<int>(x,y);
  }
	
  // ######################################################################
  bool VisualEvent::isTokenOk(const Token& tk) const
  {
    LINFO("tk.frame_nr %d startframe %d endframe %d validendframe: %d itsState: %i", \
            tk.frame_nr, startframe, endframe, validendframe, (int) itsState);
    return ((tk.frame_nr - endframe) >= 1) && (itsState != CLOSED);
  }
	
  // ######################################################################
  float VisualEvent::getCost(const Token& tk)
  {
    if (!isTokenOk(tk)) return -1.0F;
	 
    float cost = (xTracker.getCost(tk.location.x()) +
                  yTracker.getCost(tk.location.y()));
	
    LINFO("Event no. %i; obj location: %g, %g; predicted location: %g, %g; cost: %g maxCost: %g",
           myNum, tk.location.x(), tk.location.y(), xTracker.getEstimate(), 
           yTracker.getEstimate(), cost, itsDetectionParms.itsMaxCost);
    return cost;
  }

   // ######################################################################
  void VisualEvent::assign_noprediction(const Token& tk, const Vector2D& foe, uint validendframe, uint expireFrames)
  {
    ASSERT(isTokenOk(tk));

    double smv = tokens.back().bitObject.getSMV();

    tokens.push_back(tk);

    uint frameNum;
    if (validendframe - getStartFrame() >= expireFrames)
         frameNum = validendframe - expireFrames;
    else
        frameNum = validendframe;

    tokens.back().prediction = Vector2D(xTracker.getEstimate(),
                                        yTracker.getEstimate());
    tokens.back().location = Vector2D(xTracker.update(tk.location.x()),
                                      yTracker.update(tk.location.y()));

    LINFO("Getting token for frame: %d actual location: %g %g", frameNum,
            tokens.back().prediction.x(), tokens.back().prediction.y());

    // initialize token SMV to last token SMV
    // this is sort of a strange way to propagate values
    // need a bitObject copy operator?
    tokens.back().bitObject.setSMV(smv);

    tokens.back().foe = foe;

    if (tk.bitObject.getArea() > (int) max_size)
      {
        max_size = tk.bitObject.getArea();
        maxsize_framenr = tk.frame_nr;
      }
    if (tk.bitObject.getArea() < (int) min_size)
      {
        min_size = tk.bitObject.getArea();
      }
    endframe = tk.frame_nr;
    this->validendframe = validendframe;
  }

  // ######################################################################
  void VisualEvent::resetHoughTracker(Image< PixRGB<byte> >& img, BitObject &bo, float maxScale )
  {
    // save the area used to reset since the Hough tracker is bounded by a max area
    houghArea = bo.getArea();
    hTracker.reset(img, bo, maxScale, houghConstant);
  }

  // ######################################################################
  void VisualEvent::freeHoughTracker()
  {
    hTracker.free();
  }

  // ######################################################################
  bool VisualEvent::updateHoughTracker(nub::soft_ref<MbariResultViewer>& rv, uint frameNum,
                                        Image< PixRGB< byte > >& img,
                                        const Image< byte >& occlusionImg,
                                        Image< byte >& binaryImg,
                                        Point2D<int>& prediction,
                                        Rectangle &boundingBox)
  {
    return hTracker.update(rv, frameNum, img, occlusionImg, prediction, boundingBox, binaryImg, myNum, houghConstant);
  }

  // ######################################################################
  void VisualEvent::assign(const Token& tk, const Vector2D& foe, uint validendframe)
  {
    ASSERT(isTokenOk(tk));
	
    double smv = tokens.back().bitObject.getSMV();
	  
    tokens.push_back(tk);
	  
    // initialize token SMV to last token SMV  
    // this is sort of a strange way to propagate values
    // need a bitObject copy operator?
    tokens.back().bitObject.setSMV(smv);
	
    tokens.back().prediction = Vector2D(xTracker.getEstimate(),
                                        yTracker.getEstimate());
    tokens.back().location = Vector2D(xTracker.update(tk.location.x()),
                                      yTracker.update(tk.location.y()));
    tokens.back().foe = foe;
 
    // update the straight line
    //Vector2D dir(xTracker.getSpeed(), yTracker.getSpeed());
    Vector2D dir = tokens.front().location - tokens.back().location;
    tokens.back().line.reset(tokens.back().location, dir);
	
    if (foe.isValid())
      tokens.back().angle = dir.angle(tokens.back().location - foe);
    else
      tokens.back().angle = 0.0F;
	
    if (tk.bitObject.getArea() > (int) max_size)
      {
        max_size = tk.bitObject.getArea();
        maxsize_framenr = tk.frame_nr;
      }
    if (tk.bitObject.getArea() < (int) min_size)
      {
        min_size = tk.bitObject.getArea();
      }
    endframe = tk.frame_nr;
    this->validendframe = validendframe;
  } 
  // ######################################################################
  bool VisualEvent::doesIntersect(const BitObject& obj, int frameNum) const
  {
    if (frameNum > 0 && !frameInRange(frameNum)) return false;
    else return getToken(frameNum).bitObject.doesIntersect(obj);
  }
  // ######################################################################
  VisualEvent::Category VisualEvent::getCategory() const
  { 
    // If is at least itsDetectionParms.itsMinFrameNum is INTERESTING
    // otherwise BORING
    return ((int)  getNumberOfFrames() >=  itsDetectionParms.itsMinEventFrames ? \
            (VisualEvent::Category)INTERESTING:(VisualEvent::Category)BORING);
  }	
  // ######################################################################
  std::vector<float>  VisualEvent::getPropertyVector()
  {
    std::vector<float> vec;
    Token tk = getMaxSizeToken();
    BitObject bo = tk.bitObject;
	
    // 0 - event number
    vec.push_back(getEventNum());
	
    // 1 - interesting value
    vec.push_back(getCategory());
	
    // not valid?
    if (!bo.isValid())
      {
        // 2  - set area to -1
        vec.push_back(-1);
	
        // 3-12 set everyone to 0
        for (uint i = 3; i <= 12; ++i)
          vec.push_back(0);
	
        // done
        return vec; 
      }
	
    // we're valid
	
    // 2 - area
    vec.push_back(bo.getArea());
	
    // 3, 4, 5 - uxx, uyy, uxy
    float uxx, uyy, uxy;
    bo.getSecondMoments(uxx, uyy, uxy);
    vec.push_back(uxx);
    vec.push_back(uyy);
    vec.push_back(uxy);
	
    // 6 - major axis
    vec.push_back(bo.getMajorAxis());
	
    // 7 - minor axis
    vec.push_back(bo.getMinorAxis());
	
    // 8 - elongation
    vec.push_back(bo.getElongation());
	
    // 9 - orientation angle
    vec.push_back(bo.getOriAngle());
	
    // 10, 11, 12 - max, min, avg intensity
    float maxIntens,minIntens,avgIntens;
    bo.getMaxMinAvgIntensity(maxIntens, minIntens, avgIntens);
    vec.push_back(maxIntens);
    vec.push_back(minIntens);
    vec.push_back(avgIntens);

    // 13 - angle with respect to expansion
    vec.push_back(tk.angle);
	
    // done -> return the vector
    return vec;
  }
	
  // ######################################################################
  Dims VisualEvent::getMaxObjectDims() const
  {
    int w = -1, h = -1;
    std::vector<Token>::const_iterator t;
    for (t = tokens.begin(); t != tokens.end(); ++t)
      {
        Dims d = t->bitObject.getObjectDims();
        w = std::max(w, d.w());
        h = std::max(h, d.h());
      }
    return Dims(w,h);
  }

  // ######################################################################
  // ###### VisualEventSet
  // ######################################################################
  VisualEventSet::VisualEventSet(const DetectionParameters &parameters, 
                                 const std::string& fileName)
    : startframe(-1),
      endframe(-1),
      itsFileName(fileName),
      itsDetectionParms(parameters)
  {
	    
  }
	
  // ######################################################################
  VisualEventSet::VisualEventSet(std::istream& is)
  {
    readFromStream(is);
  }
	
  void VisualEventSet::readHeaderFromStream(std::istream& is)
  {
    is >> itsFileName;
    is >> itsDetectionParms.itsMaxDist;
    is >> itsDetectionParms.itsMaxCost;
    is >> itsDetectionParms.itsMinEventFrames;
    is >> itsDetectionParms.itsMinEventArea;
    is >> startframe;
    is >> endframe;
  }
	
  void VisualEventSet::writeHeaderToStream(std::ostream& os)
  {
    os << itsFileName << "\n";
    os << itsDetectionParms.itsMaxDist << " "
       << itsDetectionParms.itsMaxCost << " "
       << itsDetectionParms.itsMinEventFrames << " "
       << itsDetectionParms.itsMinEventArea << "\n";
    os << startframe << ' ' << endframe << '\n';
	
    os << "\n";
  }
	
  // ######################################################################
  void VisualEventSet::writeToStream(std::ostream& os)
  {
    std::list<VisualEvent *>::iterator currEvent;
	
    os << itsFileName << "\n";
    os << itsDetectionParms.itsMaxDist << " "
       << itsDetectionParms.itsMaxCost << " "
       << itsDetectionParms.itsMinEventFrames << " "
       << itsDetectionParms.itsMinEventArea << "\n";
    os << startframe << ' ' << endframe << '\n';
	
    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); ++currEvent)
      (*currEvent)->writeToStream(os);
	    
    os << "\n";
  }
	
	
  // ######################################################################
  void VisualEventSet::readFromStream(std::istream& is)
  {
    is >> itsFileName; LINFO("filename: %s",itsFileName.data());
    is >> itsDetectionParms.itsMaxDist; 
    is >> itsDetectionParms.itsMaxCost;
    is >> itsDetectionParms.itsMinEventFrames;
    is >> itsDetectionParms.itsMinEventArea;
    is >> startframe;
    is >> endframe;
	
    itsEvents.clear();
	
    while (is.eof() != true)
      itsEvents.push_back(new VisualEvent(is));
  }
	
  // ######################################################################
  void VisualEventSet::writePositions(std::ostream& os) const
  {
    std::list<VisualEvent *>::const_iterator currEvent;
    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); ++currEvent)
      (*currEvent)->writePositions(os);
  }
  // ######################################################################
  void VisualEventSet::insert(VisualEvent *event)
  {
    itsEvents.push_back(event);
  }
  // ######################################################################
  void VisualEventSet::runKalmanHoughTracker(VisualEvent *currEvent,
                                               const uint frameNum,
                                               const MbariMetaData &metadata,
                                               const Image< byte >& binMap,
                                               const Image<PixRGB < byte > >& graphMap,
                                               const Image< byte >& mask,
                                               const Vector2D& curFOE,
                                               nub::soft_ref<MbariResultViewer>& rv,
                                               Image< PixRGB< byte > >& img,
                                               Image< PixRGB< byte > >& prevImg)
  {
   bool found = false;
   Token evtToken;

    // prefer the Kalman tracker, and fall back to the Hough tracker
    if (!runKalmanTracker(currEvent, frameNum, metadata, img, binMap, graphMap, curFOE, true)){
      evtToken = currEvent->getToken(currEvent->getEndFrame());

      // only use the Hough tracker if object found to be interesting or has high enough voltage
      if (!currEvent->isClosed() && (evtToken.bitObject.getSMV() > .005F ||
                                    currEvent->getCategory() == VisualEvent::INTERESTING)){
        currEvent->setTrackerType(VisualEvent::HOUGH);

        // reset Hough tracker if only now switching to this tracker to save computation
        if (currEvent->trackerChanged()) {
          evtToken = currEvent->getToken(currEvent->getEndFrame());
          LINFO("Resetting Hough Tracker frame: %d event: %d with bounding box %s",
                 frameNum,currEvent->getEventNum(),toStr(evtToken.bitObject.getBoundingBox()).data());
          currEvent->resetHoughTracker(prevImg, evtToken.bitObject, DEFAULT_SCALE_INCREASE);
          currEvent->setForgetConstant(DEFAULT_FORGET_CONSTANT);
        }

        // try to run the Hough tracker; if fails, switch to Kalman tracker
        if (runHoughTracker(rv, currEvent, frameNum, metadata, img, mask, curFOE, true))
          found = true;
        else {
          currEvent->setTrackerType(VisualEvent::KALMAN);
          LINFO("Event %i - Hough Tracker failed",currEvent->getEventNum());
        }
      }
    }
    else
    {
      found = true;
      currEvent->setTrackerType(VisualEvent::KALMAN);
    }

    int frameInc = int(frameNum - currEvent->getValidEndFrame());
    if ( frameInc > itsDetectionParms.itsEventExpirationFrames ) {
      LINFO("Event %i - KalmanHough Tracker failed, closing event",currEvent->getEventNum());
      currEvent->close();
    }

    checkFailureConditions(currEvent, img.getDims());

    if (!currEvent->isClosed() && !found) {
      // assign an empty token
      evtToken = currEvent->getToken(currEvent->getEndFrame());
      evtToken.frame_nr = frameNum;
      currEvent->assign_noprediction(evtToken, curFOE,  currEvent->getValidEndFrame(),\
        itsDetectionParms.itsEventExpirationFrames);
    }
  }

  // ######################################################################
  void VisualEventSet::checkFailureConditions(VisualEvent *currEvent, Dims d)
  {
    float acc = 0.F;
    Token evtToken = currEvent->getToken(currEvent->getEndFrame());
    ///Rectangle r1 = evtToken.bitObject.getBoundingBox();

    if (currEvent->getNumberOfFrames() > 1) {
      Token evtToken2 = currEvent->getToken(currEvent->getEndFrame()-1);
      // area difference
      int areaCurrent = evtToken.bitObject.getArea();
      int areaLast = evtToken2.bitObject.getArea();

      // acceleration
      acc = currEvent->getAcceleration();
      float accAll = getAcceleration(currEvent->getEventNum());
      float pacc = 0.F;
      if (accAll != 0.F) pacc = 100.F*(accAll-acc)/accAll;
      LINFO("Event %i avg acceleration %f acceleration %f percent change %6.2f", currEvent->getEventNum(),
      accAll, acc, pacc);

      // large accelerations can indicate a tracking failure
      if (abs(acc) > 7.0F )   {
         LINFO("Event %i tracker acceleration error - closed",currEvent->getEventNum());
         currEvent->close();
      }
      /*
      if( areaCurrent < areaLast && currEvent->getTrackerType() == VisualEvent::HOUGH) {
            float c = 0.80F*currEvent->getForgetConstant();
            if (c < 0.10F) c = 0.F; //clamp to 0 when too small to avoid drifting
            LINFO("Event %i growing small in Hough tracking mode. Changing forget ratio from %3.2f to %3.2f to avoid drift", \
            currEvent->getEventNum(), currEvent->getForgetConstant(), c);
            currEvent->setForgetConstant(c);
      }*/
    }

    //float percentdiff = 0.F;

    // check distance to edge and size; if close to edge, turn down forget ratio
    /*if ( (r1.bottomI() >= d.h()-20 || r1.rightI() >= d.w()-20 || r1.top() <=20 || r1.left() <=20) \
      && currEvent->getTrackerType() == VisualEvent::HOUGH )
    {
      float c = 0.80F*currEvent->getForgetConstant();
      if (c < 0.10F) c = 0.F; //clamp to 0 when too small to avoid drifting
      LINFO("Event %i near edge. Changing forget ratio from %3.2f to %3.2f  to avoid drift", \
      currEvent->getEventNum(), currEvent->getForgetConstant(), c);
      currEvent->setForgetConstant(c);
    }*/

    /*int maxSize = currEvent->getMaxSize() ;
    int size = evtToken.bitObject.getArea();

    if( size < maxSize/4 ) {
      float c = 0.25F*currEvent->getForgetConstant();
      if (c < 0.10F) c = 0.F; //clamp to 0 when too small to avoid drifting
      LINFO("=========>Event %i growing small in Hough tracking mode. Changing forget ratio from %3.2f to %3.2f to avoid drift", \
      currEvent->getEventNum(), currEvent->getForgetConstant(), c);
      currEvent->setForgetConstant(c);
    }*/
  }
  // ######################################################################
  void VisualEventSet::runNearestNeighborHoughTracker(VisualEvent *currEvent,
                                               const uint frameNum,
                                               const MbariMetaData &metadata,
                                               const Image<byte>& binMap,
                                               const Image<PixRGB<byte>>& graphMap,
                                               const Image<byte> &mask,
                                               const Vector2D& curFOE,
                                               nub::soft_ref<MbariResultViewer>& rv,
                                               Image< PixRGB<byte> >& img,
                                               Image< PixRGB<byte> >& prevImg)
  {

   bool found = false;
   Token evtToken;

    // prefer the NN tracker, and fall back to the Hough tracker
    if (!runNearestNeighborTracker(currEvent, frameNum, metadata, binMap, graphMap, curFOE, true)){
      evtToken = currEvent->getToken(currEvent->getEndFrame());

      // only use the Hough tracker if object found to be interesting or has high enough voltage
      if (!currEvent->isClosed() && (evtToken.bitObject.getSMV() > .005F ||
                                    currEvent->getCategory() == VisualEvent::INTERESTING)){
        currEvent->setTrackerType(VisualEvent::HOUGH);

        // reset Hough tracker if only now switching to this tracker to save computation
        if (currEvent->trackerChanged()) {
          evtToken = currEvent->getToken(currEvent->getEndFrame());
          LINFO("Resetting Hough Tracker frame: %d event: %d with bounding box %s",
                 frameNum,currEvent->getEventNum(),toStr(evtToken.bitObject.getBoundingBox()).data());
          currEvent->resetHoughTracker(prevImg, evtToken.bitObject, DEFAULT_SCALE_INCREASE);
          currEvent->setForgetConstant(DEFAULT_FORGET_CONSTANT);
        }

        // try to run the Hough tracker; if fails, switch to NN tracker
        if (runHoughTracker(rv, currEvent, frameNum, metadata, img, mask, curFOE, true))
          found = true;
        else {
          currEvent->setTrackerType(VisualEvent::NN);
          LINFO("Event %i - Hough Tracker failed",currEvent->getEventNum());
        }
      }
    }
    else
    {
      found = true;
      currEvent->setTrackerType(VisualEvent::NN);
    }

    int frameInc = int(frameNum - currEvent->getValidEndFrame());
    if ( frameInc > itsDetectionParms.itsEventExpirationFrames ) {
      LINFO("Event %i - NearestNeighborHough Tracker failed, closing event",currEvent->getEventNum());
      currEvent->close();
    }

    checkFailureConditions(currEvent, img.getDims());

    if (!currEvent->isClosed() && !found) {
      // assign an empty token
      evtToken = currEvent->getToken(currEvent->getEndFrame());
      evtToken.frame_nr = frameNum;
      currEvent->assign_noprediction(evtToken, curFOE,  currEvent->getValidEndFrame(),\
        itsDetectionParms.itsEventExpirationFrames);
    }

  }
  // ######################################################################
  bool VisualEventSet::runHoughTracker(nub::soft_ref<MbariResultViewer>& rv,
                                       VisualEvent *currEvent, const uint frameNum, const MbariMetaData &metadata,
                                       Image< PixRGB<byte> >& img,
                                       const Image< byte >& mask,
                                       const Vector2D& curFOE,
                                       bool skip)
  {

    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
    Image< byte > binaryImg(img.getDims(), ZEROS);
    Image< byte > occlusionImg(img.getDims(), ZEROS);
    occlusionImg = highThresh(occlusionImg, byte(0), byte(255)); //invert image
    Point2D<int> prediction;
    Rectangle region;
    // get a copy of the last token in this event
    Token evtToken = currEvent->getToken(currEvent->getEndFrame());
    uint intersectEventNum;
    bool found = false;
    bool occlusion = false;
    BitObject obj;
    const byte black = byte(0);
    float opacity = 1.0F;
    const byte threshold = byte(1);

    // if an object intersects, create a mask for it
    if (doesIntersect(evtToken.bitObject, &intersectEventNum, frameNum)) {
        LINFO("Event %i - Hough Tracker intersection with event %i",currEvent->getEventNum(),\
                                                                    intersectEventNum);
        VisualEvent* vevt = getEventByNumber(intersectEventNum);
        BitObject intersectObj = vevt->getToken(frameNum).bitObject;
        intersectObj.drawShape(occlusionImg, black, opacity);
        occlusion = true;
        rv->output(occlusionImg, frameNum, "Occlusion");
    }

    // then apply the mask
    occlusionImg = maskArea(occlusionImg, mask);

    LINFO("Running Hough Tracker for event %d", currEvent->getEventNum());
	if (!currEvent->updateHoughTracker(rv, frameNum, img, occlusionImg, binaryImg, prediction, region)) {
	    if (!skip) {
          LINFO("Event %i - Hough Tracker failed, closing event",currEvent->getEventNum());
          currEvent->close();
        }
        return false;
    }

    // create new token from returned binary image
    obj.reset(binaryImg, prediction, threshold);

    LINFO("Found Hough object size: width %d height %d; top %d left %d area %d",region.width(), region.height(),
    region.top(), region.left(), obj.getArea() );

    // allow to grow up to 4.0x and shrink up to 0.5
    int minArea = std::max(itsDetectionParms.itsMinEventArea,(int)(0.5*evtToken.bitObject.getArea()));
    int maxArea = std::min(itsDetectionParms.itsMaxEventArea,(int)(4.0*evtToken.bitObject.getArea()));

    if (obj.getArea() >= minArea && obj.getArea() <= maxArea ) {
      // apply same cost function as Kalman to make sure Hough is not drifting
      float cost = currEvent->getCost(Token(obj,frameNum));

      // skip cost function with occlusion since this shifts the centroid
      if (occlusion || currEvent->trackerChanged())
        found = true;
      else if(cost < itsDetectionParms.itsMaxCost) {
        found = true;
      }
    }
    else
      LINFO("Event %i - no token found within area bounds min area: %d max area: %d ", \
          currEvent->getEventNum(), minArea, maxArea);

    if (!found && !skip) {
        if ( int(frameNum - currEvent->getValidEndFrame()) >= itsDetectionParms.itsEventExpirationFrames ) {
          currEvent->close();
          LINFO("Event %i - no token found, closing event",currEvent->getEventNum());
        }
        else {
          // skip over putting in a placeholder for prediction when running multiple trackers and
          // let the multiple tracker algorithm decide
           LINFO("##########Event %i - no token found, keeping event open for expiration frames: %d ##########",
                            currEvent->getEventNum(), itsDetectionParms.itsEventExpirationFrames);
          // get a copy of the last token in this event as placeholder
          Token evtToken = currEvent->getToken(currEvent->getEndFrame());
          evtToken.frame_nr = frameNum;
          currEvent->assign_noprediction(evtToken, curFOE,  currEvent->getValidEndFrame(), \
                                      itsDetectionParms.itsEventExpirationFrames);
         }
   }

   if (found) {
     // associate the best fitting guy
     Token tl = currEvent->getToken(currEvent->getEndFrame());
     Token tk(obj, frameNum, metadata, tl.scaleW, tl.scaleH);
     tk.bitObject.computeSecondMoments();
     region = tk.bitObject.getBoundingBox();
     currEvent->assign(tk, curFOE, frameNum);
     LINFO("Event %i - token found at %g, %g area: %d",currEvent->getEventNum(),
           tl.location.x(),
           tl.location.y(),
           tk.bitObject.getArea());
   }

   return found;
  }

  // ######################################################################
  bool VisualEventSet::runKalmanTracker(VisualEvent *currEvent, const uint frameNum, const MbariMetaData &metadata,
                                        Image< PixRGB<byte> >& img,
                                        const Image<byte> &binMap,
                                        const Image<PixRGB<byte>>& graphMap,
                                        const Vector2D& curFOE,
                                        bool skip)
  {

    bool found = false;

    // get the predicted location
    const Point2D<int> pred = currEvent->predictedLocation();
	
    // get a copy of the last token in this event for prediction
    Token evtToken = currEvent->getToken(currEvent->getEndFrame());

    // allow to grow up to 2.0x and shrink up to 0.5
    int minArea = std::max(itsDetectionParms.itsMinEventArea,(int)(0.5*evtToken.bitObject.getArea()));
    int maxArea = std::min(itsDetectionParms.itsMaxEventArea,(int)(2.0*evtToken.bitObject.getArea()));

    LINFO("Event %i prediction: %d,%d", currEvent->getEventNum(), pred.i, pred.j);

    // is the prediction too far outside the image?
    int gone = itsDetectionParms.itsMaxDist;
    if ((pred.i < -gone) || (pred.i >= (binMap.getWidth() + gone)) ||
        (pred.j < -gone) || (pred.j >= (binMap.getHeight() + gone)))
      {
        currEvent->close();
        LINFO("Event %i out of bounds - closed",currEvent->getEventNum());
        return false;
      }

    // adjust prediction if negative
    const Point2D<int> center =  Point2D<int>(std::max(pred.i,0), std::max(pred.j,0));

    // get the region used for searching for a match
    Dims searchDims = Dims(itsDetectionParms.itsMaxDist,itsDetectionParms.itsMaxDist);
    Rectangle region = Rectangle::centerDims(center, searchDims);
    region = region.getOverlap(Rectangle(Point2D<int>(0, 0), img.getDims() - 1));
    LINFO("Region %i %s ", currEvent->getEventNum(),toStr(region).data());

    if (!region.isValid()) {
      LINFO("Invalid region. Closing event %i", currEvent->getEventNum());
      currEvent->close();
      return false;
    }

    std::list<BitObject> objs, objs2;
    objs = extractBitObjects(graphMap, center, region, minArea, maxArea);

    if (itsDetectionParms.itsSegmentAlgorithmType != SAGraphCut) {
      objs2 = extractBitObjects(binMap, region, minArea, maxArea);
      objs.splice(objs.end(), objs2);
    }

    LINFO("pred. location: %s; region: %s; Number of extracted objects: %ld",
           toStr(pred).data(),toStr(region).data(),objs.size());

    // now look which one fits best
    float lCost = -1.0F;
    int size = objs.size();

    std::list<BitObject>::iterator cObj, lObj = objs.begin();
    for (cObj = objs.begin(); cObj != objs.end(); ++cObj)
      {
        std::list<BitObject>::iterator next = cObj;
        ++next;

        if (size > 1 && doesIntersect(*cObj, frameNum)) {
          objs.erase(cObj);
          cObj = next;
          continue;      
        }

	    float areaCost = ((float)(evtToken.bitObject.getArea())/(float)(cObj->getArea()));
        float cost = currEvent->getCost(Token(*cObj,frameNum)) + areaCost;

        if (cost < 0.0F) {
          objs.erase(cObj);
          cObj = next;
          continue;
        }
	
        if ((lCost == -1.0F) || (cost < lCost))
          {
            lCost = cost;
            lObj = cObj;
            found = true;
          }
      }

    float distul = 0.F;
    float distbr = 0.F;
    if (found) {
      Rectangle r1 = evtToken.bitObject.getBoundingBox();
      Rectangle r2 = lObj->getBoundingBox();
      distul = sqrt(pow((double)(r1.top() - r2.top()),2.0) +  pow((double)(r1.left() - r2.left()),2.0));
      distbr = sqrt(pow((double)(r1.bottomI() - r2.bottomI()),2.0) + pow((double)(r1.rightI() - r2.rightI()),2.0));
    }

    // cost too high
    if ( found && (lCost > itsDetectionParms.itsMaxCost || lCost == -1.0) ) {
      LINFO("Event %i - no token found, event cost: %f maxCost: %f ",
     		    currEvent->getEventNum(), lCost, itsDetectionParms.itsMaxCost);
      found = false;
    }

    if ( distul > itsDetectionParms.itsMaxCost && distbr > itsDetectionParms.itsMaxCost ) {
      LINFO("Event %i - token found but bounding box too skewed: upper left delta: %g bottom right delta: %g max: %g",
     		    currEvent->getEventNum(), distul, distbr, itsDetectionParms.itsMaxCost);
      found = false;
    }

    // skip over this when running multiple trackers; let the multiple tracker algorithm decide
    if (!skip && !found) {
        if ( int(frameNum - currEvent->getValidEndFrame()) > itsDetectionParms.itsEventExpirationFrames )
            currEvent->close();
        else {
	        LINFO("########## Event %i - no token found, keeping event open for expiration frames: %d ##########",
              currEvent->getEventNum(), itsDetectionParms.itsEventExpirationFrames);
              evtToken.frame_nr = frameNum;
              currEvent->assign_noprediction(evtToken, curFOE,  currEvent->getValidEndFrame(), itsDetectionParms.itsEventExpirationFrames);
        }
    }

    if (found) {
      // associate the best fitting guy
      Token tl = currEvent->getToken(currEvent->getEndFrame());
      Token tk(*lObj, frameNum, metadata, tl.scaleW, tl.scaleH);
      tk.bitObject.computeSecondMoments();
      region = tk.bitObject.getBoundingBox();
      currEvent->assign(tk, curFOE, frameNum);
      LINFO("Event %i - token found at %g, %g area: %d",currEvent->getEventNum(),
            tl.location.x(),
            tl.location.y(),
            tk.bitObject.getArea());
    }
	    
    objs.clear();
    return found;

  }
  // ######################################################################
  bool VisualEventSet::runNearestNeighborTracker(VisualEvent *currEvent, const uint frameNum, const MbariMetaData &metadata,
                                             const Image<byte> &binMap,
                                             const Image<PixRGB<byte>>& graphMap,
                                             const Vector2D& curFOE,
                                             bool skip)
  {
    Dims d;
    Point2D<int> center;

    // get a copy of the last token in this event for prediction
    Token evtToken = currEvent->getToken(currEvent->getEndFrame());

    int minArea = std::max(itsDetectionParms.itsMinEventArea,(int)(0.5*evtToken.bitObject.getArea()));
    int maxArea = std::min(itsDetectionParms.itsMaxEventArea,(int)(2.0*evtToken.bitObject.getArea()));

    // get the object dimensions and centroid for token
    d = evtToken.bitObject.getObjectDims();
    center = evtToken.bitObject.getCentroid();

    // get the region used for searching for a match
    Dims searchDims = Dims(itsDetectionParms.itsMaxDist,itsDetectionParms.itsMaxDist);
    Rectangle region = Rectangle::centerDims(center, searchDims);
    region = region.getOverlap(Rectangle(Point2D<int>(0, 0), binMap.getDims() - 1));
    LINFO("Region %i %s ", currEvent->getEventNum(),toStr(region).data());

    if (!region.isValid()) {
      LINFO("Invalid region. Closing event %i", currEvent->getEventNum());
      currEvent->close();
      return false;
    }

    std::list<BitObject> objs;
    if (itsDetectionParms.itsSegmentAlgorithmType == SAGraphCut)
      objs = extractBitObjects(graphMap, center, region, minArea, maxArea);
    else
      objs = extractBitObjects(binMap, region, minArea, maxArea);

    LINFO("region: %s; Number of extracted objects: %ld", toStr(region).data(),objs.size());

    // now find which one fits best
    float maxCost = itsDetectionParms.itsMaxCost;
    float lCost = -1.0F;
    bool found = false;
    int size = objs.size();
    Point2D<int> p1 = evtToken.bitObject.getCentroid();
    Rectangle r1 = evtToken.bitObject.getBoundingBox();

    std::list<BitObject>::iterator cObj, lObj = objs.begin();
    for (cObj = objs.begin(); cObj != objs.end(); ++cObj)
      {
        std::list<BitObject>::iterator next = cObj;
        ++next;

        if (size > 1 && doesIntersect(*cObj, frameNum)) {
          objs.erase(cObj);
          cObj = next;
          continue;
        }

        Point2D<int> p2 = cObj->getCentroid();
        Rectangle r2 = cObj->getBoundingBox();

        // calculate cost function as distance between centroids
        float cost1 = sqrt(pow((double)(p1.i - p2.i),2.0) + pow((double)(p1.j - p2.j),2.0));
        // calculate other cost function as distance between bounding box corners
        float cost2 = sqrt(pow((double)(r1.top() - r2.top()),2.0) +  pow((double)(r1.left() - r2.left()),2.0));
        float cost3 = sqrt(pow((double)(r1.bottomI() - r2.bottomI()),2.0) +
        pow((double)(r1.rightI() - r2.rightI()),2.0));
        float cost = cost1 + cost2 + cost3;

        if (cost < 0.0F ) {
          objs.erase(cObj);
          cObj = next;
          continue;
        }
        else if ((lCost == -1.0F) || (cost < lCost))
          {
            lCost = cost;
            lObj = cObj;
            found = true;
          }
      }// end for all objects

    // cost too high no fitting object found? -> close event
    if ( found  && (lCost > maxCost || lCost == -1.0) )
      {
        found = false;
        // skip over this when running multiple trackers; let the multiple tracker algorithm decide
        if (!skip) {
          if ( int(frameNum - currEvent->getValidEndFrame()) > itsDetectionParms.itsEventExpirationFrames ) {
              currEvent->close();
              LINFO("Event %i - no token found, closing event",currEvent->getEventNum());
          }
          else {
              LINFO("##########Event %i - no token found, keeping event open for expiration frames: %d ##########",
                    currEvent->getEventNum(), itsDetectionParms.itsEventExpirationFrames);
              evtToken.frame_nr = frameNum;
              currEvent->assign_noprediction(evtToken, curFOE,  currEvent->getValidEndFrame(),
               itsDetectionParms.itsEventExpirationFrames);
        }
        }
      }
   if (found) {
      // associate this token to the best fitting guy
      Vector2D emtpy(0.F,0.F);
      Token tl = currEvent->getToken(currEvent->getEndFrame());
      Token tk(*lObj, frameNum, metadata, tl.scaleW, tl.scaleH);
      tk.bitObject.computeSecondMoments();
      region = tk.bitObject.getBoundingBox();
      currEvent->assign(tk, emtpy, frameNum);
      LINFO("Event %i - token found at %g, %g ",currEvent->getEventNum(),
            tl.location.x(),
            tl.location.y());
    }

    objs.clear();
    return found;
  }

  // ######################################################################
  void VisualEventSet::getAreaRange(int &minArea, int &maxArea)
  {
    std::list<VisualEvent *>::iterator currEvent;
    int currMinArea = itsDetectionParms.itsMinEventArea;
    int currMaxArea = itsDetectionParms.itsMaxEventArea;

    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); ++currEvent) {
      if((*currEvent)->getMaxSize() > currMaxArea)
       currMaxArea = (*currEvent)->getMaxSize();

      if((*currEvent)->getMinSize() < currMinArea)
       currMinArea = (*currEvent)->getMinSize();
    }
  }

  // ######################################################################
  float VisualEventSet::getAcceleration(uint skipEventNum)
  {
    std::list<VisualEvent *>::iterator currEvent;
    float sumAccel = 0.F;
    int i = 0;

    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); ++currEvent) {
      if((*currEvent)->getEventNum() != skipEventNum) {
       sumAccel += (*currEvent)->getAcceleration();
       i++;
       }
    }
    if (i > 0)
      return sumAccel/(float)i;
    return 0.F;
  }


  // ######################################################################
  void VisualEventSet::updateEvents(nub::soft_ref<MbariResultViewer>& rv,
                                    const Image< byte >& mask,
                                    const uint frameNum,
                                    Image< PixRGB<byte> >& img,
                                    Image< PixRGB<byte> >& prevImg,
                                    const Image<byte>& binMap,
                                    const Image< PixRGB<byte> >& graphMap,
                                    const Vector2D& curFOE,
                                    const MbariMetaData &metadata)
  {
    if (startframe == -1) {startframe = (int) frameNum; endframe = (int) frameNum;}
    if ((int) frameNum > endframe) endframe = (int) frameNum;

    Image<byte> binMapMasked = maskArea(binMap, mask);
    Image<PixRGB<byte>> graphMapMasked = maskArea(graphMap, mask);

    std::list<VisualEvent *>::iterator currEvent;
	
    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); ++currEvent)
      if ((*currEvent)->isOpen()) {
        switch(itsDetectionParms.itsTrackingMode) {
        case(TMKalmanFilter):
          (*currEvent)->setTrackerType(VisualEvent::KALMAN);
          runKalmanTracker(*currEvent, frameNum, metadata, img, binMapMasked, graphMapMasked, curFOE);
          break;
        case(TMNearestNeighbor):
          (*currEvent)->setTrackerType(VisualEvent::NN);
          runNearestNeighborTracker(*currEvent, frameNum, metadata, binMapMasked, graphMapMasked, curFOE);
          break;
        case(TMHough):
          runHoughTracker(rv, *currEvent, frameNum, metadata, img, mask, curFOE);
          checkFailureConditions(*currEvent, img.getDims());
          break;
        case(TMNearestNeighborHough):
          runNearestNeighborHoughTracker(*currEvent, frameNum, metadata, binMapMasked, graphMapMasked, mask, curFOE, rv, img, prevImg);
          break;
        case(TMKalmanHough):
          runKalmanHoughTracker(*currEvent, frameNum, metadata, binMapMasked, graphMapMasked, mask, curFOE, rv, img, prevImg);
          break;
        case(TMNone):
          break;
        default:
          (*currEvent)->setTrackerType(VisualEvent::KALMAN);
          runKalmanTracker(*currEvent, frameNum, metadata, img, binMapMasked, graphMapMasked, curFOE);
          break;
        }
      }
  }

  // ######################################################################
  void VisualEventSet::initiateEvents(std::list<BitObject>& bos, \
                                      const MbariMetaData &metadata, \
                                      float scaleW, float scaleH, \
                                      MbariImage< PixRGB<byte> >& img)
  {
    int frameNum = img.getFrameNum();
    if (startframe == -1) {startframe = frameNum; endframe = frameNum;}
    if (frameNum > endframe) endframe = frameNum;  
	
    std::list<BitObject>::iterator currObj;
	
    // loop over the BitObjects
    currObj = bos.begin();
    while(currObj != bos.end())    {
      std::list<BitObject>::iterator next = currObj;
      ++next;
	            
      // is there an intersection with an event?
      if (resetIntersect(img, *currObj, frameNum))
        bos.erase(currObj);
	
      currObj = next;
    }

    // now go through all the remaining BitObjects and create new events for them
    // if they are not already out of bounds and using a tracker
    for (currObj = bos.begin(); currObj != bos.end(); ++currObj)
      {
        itsEvents.push_back(new VisualEvent(Token(*currObj, frameNum, metadata, scaleW, scaleH), itsDetectionParms, img));
        LINFO("assigning object of area: %i to new event %i",currObj->getArea(),
              itsEvents.back()->getEventNum());
      }
  }
	
  // ######################################################################
  bool VisualEventSet::resetIntersect(MbariImage< PixRGB<byte> >& img, BitObject& obj, int frameNum)
  {
    // ######## Initialization of variables, reading of parameters etc.
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
    std::list<VisualEvent *>::iterator cEv;
    //int area;
    //float areadiff;
    //float areapercentdiff;
    Token evtToken;
    //Point2D<int> p1,p2;
    //float dist;
Rectangle r1, r2;
float distul, distbr;

    for (cEv = itsEvents.begin(); cEv != itsEvents.end(); ++cEv) {
      if ((*cEv)->doesIntersect(obj, frameNum)) {
        //reset the SMV for this bitObject
        (*cEv)->resetBitObject(frameNum, obj);
        //reset Hough tracker depending on tracking mode
        switch (dp.itsTrackingMode) {
              case(TMHough):
                /*area = (*cEv)->getHoughArea();
                areadiff = -1.F;
                if (area > 0) {
                  areadiff =(float)(obj.getArea() - area)/(float)area;
                   LINFO("Hough Tracker frame: %d event: %d areadiff %f, default scale: %f",
                      frameNum,(*cEv)->getEventNum(), areadiff,DEFAULT_SCALE_INCREASE);
                }*/
                // if area changed at least half the scale factor or grown smaller,
                // reset the Hough tracker
                /*if (areadiff != -1.F && (areadiff > DEFAULT_SCALE_INCREASE/2.F || areadiff < 0.F) )  {
                  LINFO("Resetting Hough Tracker frame: %d event: %d with bounding box %s",
                       frameNum,(*cEv)->getEventNum(),toStr(obj.getBoundingBox()).data());
                  (*cEv)->resetHoughTracker(img, obj, DEFAULT_SCALE_INCREASE);
                //}*/
              break;

              case(TMNearestNeighborHough):
              case(TMKalmanHough):
                evtToken = (*cEv)->getToken((*cEv)->getEndFrame());
                /*area = evtToken.bitObject.getArea();
                areapercentdiff = -1.F;
                if (area > 0) {
                  areapercentdiff =(float)100.F*abs(obj.getArea() - area)/(float)area;
                   LINFO("Hough Tracker frame: %d event: %d area change %3.2f %%", frameNum,(*cEv)->getEventNum(), areapercentdiff);
                }*/
                //evtToken2 = (*cEv)->getToken((*cEv)->getEndFrame());
                //p1 = evtToken2.bitObject.getCentroid();
                //p2 = obj.getCentroid();
                //dist = sqrt(pow((double)(p1.i - p2.i),2.0) + pow((double)(p1.j - p2.j),2.0));
                ////} && dist < dp.itsMaxDist){//} && obj.getArea() > (*cEv)->getHoughArea()) {

      r2 = obj.getBoundingBox();
      r1 = evtToken.bitObject.getBoundingBox();
       // calculate distance between bounding box corners
      distul = sqrt(pow((double)(r1.top() - r2.top()),2.0) +  pow((double)(r1.left() - r2.left()),2.0));
      distbr = sqrt(pow((double)(r1.bottomI() - r2.bottomI()),2.0) + pow((double)(r1.rightI() - r2.rightI()),2.0));

                if ((*cEv)->getTrackerType() == VisualEvent::HOUGH){//}  && (distul < dp.itsMaxDist || distbr < dp.itsMaxDist)) {//&& areapercentdiff < MAX_AREA_PERCENT_INCREASE) {
                  LINFO("Resetting Hough Tracker frame: %d event: %d with bounding box %s",
                         frameNum,(*cEv)->getEventNum(),toStr(obj.getBoundingBox()).data());
                          (*cEv)->resetHoughTracker(img, obj, DEFAULT_SCALE_INCREASE);
                }
              break;
              case(TMKalmanFilter):
              case(TMNearestNeighbor):
              case(TMNone):
              break;
            }
      return true;
      }
    }
    return false;
  }

  // ######################################################################
  bool VisualEventSet::doesIntersect(BitObject& obj, int frameNum)
  {
    std::list<VisualEvent *>::iterator cEv;
    for (cEv = itsEvents.begin(); cEv != itsEvents.end(); ++cEv)
      if ((*cEv)->doesIntersect(obj,frameNum)) {
        //reset the SMV for this bitObject
        (*cEv)->resetBitObject(frameNum, obj);
        return true;
      }
    return false;
  }

  // ######################################################################
  bool VisualEventSet::doesIntersect(BitObject& obj, uint* eventNum, int frameNum)
  {
    std::list<VisualEvent *>::iterator cEv;
    for (cEv = itsEvents.begin(); cEv != itsEvents.end(); ++cEv)
      // return the first object that intersects
      if ((*cEv)->doesIntersect(obj,frameNum)) {
        *eventNum = (*cEv)->getEventNum();
        return true;
      }
    return false;
  }

  // ######################################################################
  uint VisualEventSet::numEvents() const
  {
    return itsEvents.size();
  }
	
  // ######################################################################
  void VisualEventSet::reset()
  {
    itsEvents.clear();
  }
	
  // ######################################################################
  void VisualEventSet::replaceEvent(uint eventnum, VisualEvent *event)
  {
    std::list<VisualEvent *>::iterator currEvent = itsEvents.begin();
    while (currEvent != itsEvents.end())  {
      if((*currEvent)->getEventNum() == eventnum) {
        itsEvents.insert(currEvent, event);
        delete *currEvent;
        itsEvents.erase(currEvent);    
        return;
      }
      ++currEvent;
    }
    LFATAL("Event %d does not exist in event list cannot replace", eventnum);
  }
  // ######################################################################
  void VisualEventSet::cleanUp(uint currFrame, uint lastFrame)
  {
    std::list<VisualEvent *>::iterator currEvent = itsEvents.begin();
	
    while(currEvent != itsEvents.end()) {
      std::list<VisualEvent *>::iterator next = currEvent;
      ++next;
	
      switch((*currEvent)->getState())
        {			
        case(VisualEvent::DELETE):
          LINFO("Erasing event %i", (*currEvent)->getEventNum());
          delete *currEvent;
          itsEvents.erase(currEvent);
          break;
        case(VisualEvent::WRITE_FINI):   
          LINFO("Event %i flagged as written", (*currEvent)->getEventNum());
          break;	    
        case(VisualEvent::CLOSED):   
          LINFO("Event %i flagged as closed", (*currEvent)->getEventNum());
          break;
        case(VisualEvent::OPEN):
          if (itsDetectionParms.itsMaxEventFrames > 0 && currFrame > ((*currEvent)->getStartFrame() + itsDetectionParms.itsMaxEventFrames)){
            //limit event to itsMaxFrames
            LINFO("Event %i reached max frame count:%d - flagging as closed", (*currEvent)->getEventNum(),\
                  itsDetectionParms.itsMaxEventFrames);
            (*currEvent)->close();		
          }
          break;
        default:
          //this event is still within the window of itsMaxFrames
          LDEBUG("Event %d still open", (*currEvent)->getEventNum());
          break;
        }
	    
      currEvent = next;
    } // end for loop over events 
  }
	
  // ######################################################################
  void VisualEventSet::closeAll()
  {
    std::list<VisualEvent *>::iterator cEvent;
    for (cEvent = itsEvents.begin(); cEvent != itsEvents.end(); ++cEvent)
      (*cEvent)->close();
  }
  // ######################################################################
  void VisualEventSet::printAll()
  {
    std::list<VisualEvent *>::iterator cEvent;
    for (cEvent = itsEvents.begin(); cEvent != itsEvents.end(); ++cEvent)
      {
        LINFO("EVENT %d sframe %d eframe %d numtokens %d",
              (*cEvent)->getEventNum(),
              (*cEvent)->getStartFrame(),
              (*cEvent)->getEndFrame(),
              (*cEvent)->getNumberOfTokens());
      }
  }
  // ######################################################################
  std::vector<Token> VisualEventSet::getTokens(uint frameNum)
  {
    std::vector<Token> tokens;
    std::list<VisualEvent *>::iterator currEvent;
    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); ++currEvent)
      {
        // does this guy participate in frameNum?
        if (!(*currEvent)->frameInRange(frameNum)) continue;
	
        tokens.push_back((*currEvent)->getToken(frameNum));
      } // end loop over events
	
    return tokens;
  }

  // ######################################################################
  void VisualEventSet::drawTokens(Image< PixRGB<byte> >& img,
                                  uint frameNum,
                                  int circleRadius,
                                  BitObjectDrawMode mode,
                                  float opacity,
                                  PixRGB<byte> colorInteresting,
                                  PixRGB<byte> colorCandidate,
                                  PixRGB<byte> colorPred,
                                  PixRGB<byte> colorFOE,
                                  bool showEventLabels,
                                  bool showCandidate,
                                  bool saveNonInterestingEvents,
                                  float scaleW,
                                  float scaleH)
  {
    // dimensions of the number text and location to put it at
    const int numW = 10; 
    const int numH = 21;
    Token tk;

    std::list<VisualEvent *>::iterator currEvent;
    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); ++currEvent)
    {      
      	// does this guy  participate in frameNum ? and
      	// if also saving non-interesting events and this is BORING event, be sure to save this
        // otherwise, save all INTERESTING events
        if( (*currEvent)->frameInRange(frameNum) &&
            ( (saveNonInterestingEvents && (*currEvent)->getCategory() == MbariVisualEvent::VisualEvent::BORING ) ||
            (*currEvent)->getCategory() == MbariVisualEvent::VisualEvent::INTERESTING  || 
            showCandidate ) ) 
          {
            PixRGB<byte> circleColor;            
            tk = (*currEvent)->getToken(frameNum);

	        if(!tk.location.isValid())
		      continue;

            Point2D<int> center = tk.location.getPoint2D();
	        center.i *= tk.scaleW;
	        center.j *= tk.scaleH;

            if ((*currEvent)->getCategory() == VisualEvent::INTERESTING)
              circleColor = colorInteresting;
            else
              circleColor = colorCandidate;
	
            // if requested, prepare the event labels
            Image< PixRGB<byte> > textImg;
            if (showEventLabels)
              {
                // write the text and create the overlay image
                std::string numText = toStr((*currEvent)->getEventNum());
                //std::ostringstream ss;
                //ss.precision(2);
               // ss << numText << "," << (*currEvent)->getForgetConstant();
                //ss << numText << "," << tk.bitObject.getStdDev();

                //textImg.resize(numW * ss.str().length(), numH, NO_INIT);
                textImg.resize(numW * numText.length(), numH, NO_INIT);
                textImg.clear(COL_WHITE);
                //writeText(textImg, Point2D<int>(0,0), ss.str().data());
                writeText(textImg, Point2D<int>(0,0), numText.data());
              }
	
            // draw the event object itself if requested
            if (circleColor != COL_TRANSPARENT)
              {
                // the box so that the text knows where to go
                Rectangle bbox;
	
                // draw rectangle or circle and determine the pos of the number label
                if (tk.bitObject.isValid())
                  {
                    tk.bitObject.draw(mode, img, circleColor, opacity);
                    bbox = tk.bitObject.getBoundingBox(BitObject::IMAGE);
                    Point2D<int> topleft(bbox.left()*scaleW, bbox.top()*scaleH);
                    Dims dims(bbox.width()*scaleW, bbox.height()*scaleH);
                    bbox = Rectangle(topleft, dims);
                    bbox = bbox.getOverlap(img.getBounds());
                  }
                else
                  {
                    LINFO("BitObject is invalid: area: %i;",tk.bitObject.getArea());
                    LFATAL("bounding box: %s",toStr(tk.bitObject.getBoundingBox()).data());
                    drawCircle(img, center, circleRadius, circleColor);
                    bbox = Rectangle::tlbrI(center.j - circleRadius, center.i - circleRadius,
                                            center.j + circleRadius, center.i + circleRadius);
                    bbox = bbox.getOverlap(img.getBounds());
                  }
	
                // if requested, write the event labels into the image
                if (showEventLabels)
                  {
                    Point2D<int> numLoc = getLabelPosition(img.getDims(),bbox,textImg.getDims());
                    Image<PixRGB <byte> > textImg2 = replaceVals(textImg,COL_BLACK,circleColor);
                    textImg2 = replaceVals(textImg2,COL_WHITE,COL_TRANSPARENT);
                    pasteImage(img,textImg2,COL_TRANSPARENT, numLoc, opacity);
                  } // end if (showEventLabels)
	
              } // end if we're not transparent
	
            // now do the same for the predicted value
            if ((colorPred != COL_TRANSPARENT) && tk.prediction.isValid())
              {
                Point2D<int> ctr = tk.prediction.getPoint2D();
                ctr.i *= tk.scaleW;
	            ctr.j *= tk.scaleH;
                Rectangle ebox =
                  Rectangle::tlbrI(ctr.j - circleRadius, ctr.i - circleRadius, ctr.j + circleRadius, ctr.i + circleRadius);
                    ebox = ebox.getOverlap(img.getBounds());

                    // round down the radius in case near the edges
                    if (ebox.width() > 0 && ebox.height() > 0) {
                        int radius = (int) sqrt(pow(ebox.width(), 2.0) + pow(ebox.height(), 2.0))/2;
                        drawCircle(img, ctr, radius, colorPred);
                        if (showEventLabels) { 
                            Point2D<int> numLoc = getLabelPosition(img.getDims(), ebox, textImg.getDims());
                            Image< PixRGB<byte> > textImg2 = replaceVals(textImg, COL_BLACK, colorPred);
                            textImg2 = replaceVals(textImg2, COL_WHITE, COL_TRANSPARENT);
                            pasteImage(img, textImg2, COL_TRANSPARENT, numLoc, opacity);
                        }
                    }
              }
	
          }
      } // end loop over events
	
    if ((colorFOE != COL_TRANSPARENT) && tk.foe.isValid())
      {
        Point2D<int> ctr = tk.foe.getPoint2D();
        ctr.i *= tk.scaleW;
        ctr.j *= tk.scaleH;
        drawDisk(img, ctr,2,colorFOE);
      }
  }
	
	
  // ######################################################################
  Point2D<int> VisualEventSet::getLabelPosition(Dims imgDims,
                                           Rectangle bbox, 
                                           Dims textDims) const
  {
    // distance of the text label from the bbox
    const int dist = 2;
	
    Point2D<int> loc(bbox.left(),(bbox.top() - dist - textDims.h()));
	
    // not enough space to the right? -> shift as appropriate
    if ((loc.i + textDims.w()) > imgDims.w())
      loc.i = imgDims.w() - textDims.w() - 1;
	
    // not enough space on the top? -> move to the bottom
    if (loc.j < 0)
      loc.j = bbox.bottomI() + dist;
	
    return loc;
  }
	
  // ######################################################################
  PropertyVectorSet VisualEventSet::getPropertyVectorSet()
  {
    PropertyVectorSet pvs;
	
    std::list<VisualEvent *>::iterator currEvent;
    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); 
         ++currEvent)
      pvs.itsVectors.push_back((*currEvent)->getPropertyVector());
	
    return pvs;
  }
	
  // ######################################################################
  PropertyVectorSet VisualEventSet::getPropertyVectorSetToSave()
  {
    PropertyVectorSet pvs;
    std::list<VisualEvent *>::iterator currEvent;
    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); 
         ++currEvent) {
      if((*currEvent)->isClosed()) {
        pvs.itsVectors.push_back((*currEvent)->getPropertyVector());
      }
    }
	
    return pvs;
  }
	
  // ######################################################################
  int VisualEventSet::getAllClosedFrameNum(uint currFrame)
  {
    std::list<VisualEvent *>::iterator currEvent;
    for (int frame = (int)currFrame; frame >= -1; --frame)
      {
        bool done = true;
	
        for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); 
             ++currEvent)
          {
            done &= ((frame < (int)(*currEvent)->getStartFrame()) 
                     || (*currEvent)->isClosed());
            if (!done) break;
          }
	      
        if (done) return frame;
      }
    return -1;
  }
	
  // ######################################################################
  bool VisualEventSet::doesEventExist(uint eventNum) const
  {
    std::list<VisualEvent *>::const_iterator evt;
    for (evt = itsEvents.begin(); evt != itsEvents.end(); ++evt)
      if ((*evt)->getEventNum() == eventNum) return true;
	
    return false;
  }
  // ######################################################################
  VisualEvent *VisualEventSet::getEventByNumber(uint eventNum) const
  {
    std::list<VisualEvent *>::const_iterator evt;
    for (evt = itsEvents.begin(); evt != itsEvents.end(); ++evt)
      if ((*evt)->getEventNum() == eventNum) return *evt;
	
    LFATAL("Event with number %i does not exist.",eventNum);
	
    return *evt;
  }
  // ######################################################################
  std::list<VisualEvent *>
  VisualEventSet::getEventsReadyToSave(uint framenum)
  {
    std::list<VisualEvent *> result;
    std::list<VisualEvent *>::iterator evt;
    for (evt = itsEvents.begin(); evt != itsEvents.end(); ++evt)
      if ((*evt)->isClosed()) result.push_back(*evt);
	
    return result;
  }
	
  // ######################################################################
  std::list<VisualEvent *>
  VisualEventSet::getEventsForFrame(uint framenum)
  {
    std::list<VisualEvent *> result;
    std::list<VisualEvent *>::iterator evt;
    for (evt = itsEvents.begin(); evt != itsEvents.end(); ++evt) 
      if ((*evt)->frameInRange(framenum)) result.push_back(*evt);
	
    return result;
  }
	
	
  // ######################################################################
  std::list<BitObject>
  VisualEventSet::getBitObjectsForFrame(uint framenum)
  {
    std::list<BitObject> result;
    std::list<VisualEvent *>::iterator evt;
	 
    for (evt = itsEvents.begin(); evt != itsEvents.end(); ++evt) 
      if ((*evt)->frameInRange(framenum))
        if((*evt)->getToken(framenum).bitObject.isValid())
          result.push_back((*evt)->getToken(framenum).bitObject);
	  
    return result;
  }

  // ######################################################################
  const int VisualEventSet::minSize()
  {
    return itsDetectionParms.itsMinEventArea;
  }
}