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
  
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "DetectionAndTracking/MbariFunctions.H"

#include "Image/DrawOps.H"
#include "Image/Image.H"
#include "Image/Pixels.H"
#include "Image/Rectangle.H"
#include "Image/ShapeOps.H"
#include "Image/Transforms.H"
#include "Image/colorDefs.H"
#include "Util/Assert.H"
#include "Util/StringConversions.H"

#include "Image/Geometry2D.H"

#include <algorithm>
#include <istream>
#include <ostream>

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
  VisualEvent::VisualEvent(Token tk, const DetectionParameters &parms)
    : startframe(tk.frame_nr),
      endframe(tk.frame_nr),
      max_size(tk.bitObject.getArea()),
      maxsize_framenr(tk.frame_nr),
      itsState(VisualEvent::OPEN),
      xTracker(tk.location.x(),0.1F,10.0F),
      yTracker(tk.location.y(),0.1F,0.0F),
      itsDetectionParms(parms)
  {
    LDEBUG("tk.location = (%g, %g); area: %i",tk.location.x(),tk.location.y(),
           tk.bitObject.getArea());
    tokens.push_back(tk);
    ++counter;
    myNum = counter;
    validendframe = endframe;
  }
  // initialize static variable
  uint VisualEvent::counter = 0;
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
	
    LINFO("Event no. %i; obj location: %g, %g; predicted location: %g, %g; cost: %g",
           myNum, tk.location.x(), tk.location.y(), xTracker.getEstimate(), 
           yTracker.getEstimate(), cost);
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

    Token t = getToken(frameNum);

    tokens.back().prediction =  Vector2D(t.location.x(), t.location.y());
    tokens.back().location = Vector2D(t.location.x(), t.location.y());
    xTracker.update(t.location.x());
    yTracker.update(t.location.y()); 

    LINFO("Getting token for frame: %d actual location: %g %g", frameNum,
            tokens.back().prediction.x(), tokens.back().prediction.y());

    // initialize token SMV to last token SMV
    // this is sort of a strange way to propogate values
    // need a bitObject copy operator?
    tokens.back().bitObject.setSMV(smv);
    
    tokens.back().foe = foe;
 
    if (tk.bitObject.getArea() > (int) max_size)
      {
        max_size = tk.bitObject.getArea();
        maxsize_framenr = tk.frame_nr;
      }
    endframe = tk.frame_nr;
    this->validendframe = validendframe;
  }

  // ######################################################################
  void VisualEvent::assign(const Token& tk, const Vector2D& foe, uint validendframe)
  {
    ASSERT(isTokenOk(tk));
	
    double smv = tokens.back().bitObject.getSMV();
	  
    tokens.push_back(tk);
	  
    // initialize token SMV to last token SMV  
    // this is sort of a strange way to propogate values
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
  void VisualEventSet::runKalmanTracker(VisualEvent *currEvent, \
                                        uint frameNum,
                                        const MbariMetaData &metadata,
                                        const Image<byte>& binMap, 
                                        const Vector2D& curFOE)
  {
	
    // get the predicted location
    const Point2D<int> pred = currEvent->predictedLocation();
	
    // get a copy of the last token in this event for prediction
    Token evtToken = currEvent->getToken(currEvent->getEndFrame());    
	
    // is the prediction too far outside the image?
    //int gone = itsMaxDist / 2;
    int gone = 0;
    if ((pred.i < -gone) || (pred.i >= (binMap.getWidth() + gone)) ||
        (pred.j < -gone) || (pred.j >= (binMap.getHeight() + gone)))
      {
        currEvent->close();
        LINFO("Event %i out of bounds - closed",currEvent->getEventNum());
        return;
      }
	  
    // get the region used for searching for a match
    Rectangle region = Rectangle::tlbrI(pred.j - itsDetectionParms.itsMaxDist,
                                        pred.i - itsDetectionParms.itsMaxDist,
                                        pred.j + itsDetectionParms.itsMaxDist,
                                        pred.i + itsDetectionParms.itsMaxDist);
											
    Dims d =  binMap.getDims();
    region = region.getOverlap(Rectangle(Point2D<int>(0,0), d - 1));

    // extract all the BitObjects from the region
    // extract as small as 1/2 of the last occurance of this event 
    // extract as large as 2x the the last occurance of this event
    std::list<BitObject> objs = extractBitObjects(binMap, region, evtToken.bitObject.getArea()/2, evtToken.bitObject.getArea()*2);

    LINFO("pred. location: %s; region: %s; Number of extracted objects: %ld",
           toStr(pred).data(),toStr(region).data(),objs.size());
	  
    // now look which one fits best
    float lCost = -1.0F, areapercentdiff = 0.F, percentdiff = 0.F;
    int area1 = evtToken.bitObject.getArea();
	 
    std::list<BitObject>::iterator cObj, lObj = objs.end();
    for (cObj = objs.begin(); cObj != objs.end(); ++cObj)
      {
        std::list<BitObject>::iterator next = cObj;
        ++next;
	
        if (doesIntersect(*cObj, frameNum)) {
          objs.erase(cObj);
          cObj = next;
          continue;      
        }
	      
        int areadiff = cObj->getArea() - area1;
        if(abs(areadiff) > 0 && area1 > 0) percentdiff = (float)abs(areadiff)/(float)area1;
	
        float cost = currEvent->getCost(Token(*cObj,frameNum));
	      
        if (cost < 0.0F) {
          objs.erase(cObj);
          cObj = next;
          continue;
        }
	
        if ((lCost == -1.0F) || (cost < lCost))
          {
            lCost = cost;
            lObj = cObj;
            LINFO("best cost: %f maxCost: %f areadiff: %d change:%f",lCost, 
                   itsDetectionParms.itsMaxCost,areadiff, percentdiff); 
            areapercentdiff =  percentdiff;
          }
      }
	   
    // cost too high no fitting object found? -> close event
    if ((lCost > itsDetectionParms.itsMaxCost) || areapercentdiff > 3.F || lCost == -1.0 || lObj == objs.end()) {
        if ( int(frameNum - currEvent->getValidEndFrame()) >= itsDetectionParms.itsEventExpirationFrames ) {
            currEvent->close();
            LINFO("Event %i - no token found, closing event cost: %f maxCost: %f size: %d areaDiffPercent: %f ",
		currEvent->getEventNum(), lCost, itsDetectionParms.itsMaxCost, area1, areapercentdiff);
        }
        else {
             LINFO("########## Event %i - no token found, keeping event open for expiration frames: %d ##########",
		currEvent->getEventNum(), itsDetectionParms.itsEventExpirationFrames); 
            evtToken.frame_nr = frameNum;
            currEvent->assign_noprediction(evtToken, curFOE,  currEvent->getValidEndFrame(), itsDetectionParms.itsEventExpirationFrames);
        }
    }
    else    {
      // associate the best fitting guy
      Token tl = currEvent->getToken(currEvent->getEndFrame());
      Token tk(*lObj, frameNum, metadata, tl.scaleW, tl.scaleH);
      tk.bitObject.computeSecondMoments();
      currEvent->assign(tk, curFOE, frameNum);
      LINFO("Event %i - token found at %g, %g area: %d",currEvent->getEventNum(),
            tl.location.x(), 
            tl.location.y(),
            tk.bitObject.getArea());
    }
	    
    objs.clear();    
  }
  // ######################################################################
  void VisualEventSet::runNearestNeighborTracker(VisualEvent *currEvent,
                                             uint frameNum,
                                             const MbariMetaData &metadata,
                                             const Image<byte>& binMap,
                                             const Vector2D& curFOE)
  {
    Dims d;
    Point2D<int> center;
    int maxDistHeightPred, maxDistWidthPred;
	
    // get a copy of the last token in this event for prediction
    Token evtToken = currEvent->getToken(currEvent->getEndFrame());    
	
    // get the object dimensions and centroid for token
    d = evtToken.bitObject.getObjectDims();
    center = evtToken.bitObject.getCentroid();
	
    // calculate maximum bounding box distance prediction
    maxDistHeightPred = d.h()/2  + itsDetectionParms.itsMaxDist;
    maxDistWidthPred = d.w()/2  + itsDetectionParms.itsMaxDist;
	
    // calculate the region the bounding box should be in
    Rectangle region = Rectangle::tlbrI(std::max(0,center.j - maxDistHeightPred),
                                        std::max(0,center.i - maxDistWidthPred),
                                        std::min(center.j + maxDistHeightPred,binMap.getHeight()),
                                        std::min(center.i + maxDistWidthPred,binMap.getWidth()));
    region = region.getOverlap(binMap.getBounds());
	
    // make sure prediction is not outside image; allow for image size
    if ( ((center.j + itsDetectionParms.itsMaxDist) >= binMap.getHeight() + d.h()) ||
         ((center.i + itsDetectionParms.itsMaxDist) >= binMap.getWidth() + d.w()) )
      {
        currEvent->close();
        LINFO("Event %i out of bounds - closed",currEvent->getEventNum());
        return;
      }
 

    // extract all the BitObjects from the region
    // extract as small as 1/2 of the last occurance of this event
    // extract as large as 2x the the last occurance of this event
    std::list<BitObject> objs = extractBitObjects(binMap, region, evtToken.bitObject.getArea()/2, evtToken.bitObject.getArea()*2);

    LINFO("region: %s; Number of extracted objects: %ld", toStr(region).data(),objs.size());

    // now find which one fits best
    float lCost = -1.0F, cost = -1.0F, areapercentdiff = 0.F, percentdiff = 0.F;
    Rectangle d1 = evtToken.bitObject.getBoundingBox();
    int area1 = evtToken.bitObject.getArea();
	
    std::list<BitObject>::iterator cObj, lObj = objs.end();
    for (cObj = objs.begin(); cObj != objs.end(); ++cObj)
      {
        std::list<BitObject>::iterator next = cObj;
        ++next;
	
        if (doesIntersect(*cObj, frameNum)) {
          objs.erase(cObj);
          cObj = next; 
          continue;
        }
	
        Rectangle d2 = cObj->getBoundingBox();
        // calculate cost function as sum of distance between bounding box corners
        float cost1 =  sqrt(pow((double)abs(d1.top() - d2.top()),2.0)+pow((double)abs(d1.left() - d2.left()),2.0));
        float cost2 =  sqrt(pow(((double)abs(d1.bottomI() - d2.bottomI())),2.0)+pow((double)abs(d1.rightI() - d2.rightI()),2.0));
        int areadiff = cObj->getArea() - area1;
        cost = cost1 + cost2;
        if(abs(areadiff) > 0 && area1 > 0) percentdiff = (float)abs(areadiff)/(float)area1;
	      
        if (cost < 0.0F) {
          objs.erase(cObj);
          cObj = next; 
          continue;      
        }
        else if ((lCost == -1.0F) || cost < lCost )
          {
            lCost = cost;
            lObj = cObj;
            LINFO("best cost: %f maxCost: %f areadiff: %d %%change:%f",lCost, \
                  itsDetectionParms.itsMaxCost,areadiff,
                  percentdiff); 			   
            areapercentdiff = percentdiff;
          }        
      }// end for all objects
	   
    // cost too high no fitting object found? -> close event
    if ( (lCost > itsDetectionParms.itsMaxCost) || (areapercentdiff > 3.0F) || (lCost == -1.0))
      {
        if ( int(frameNum - currEvent->getValidEndFrame()) >= itsDetectionParms.itsEventExpirationFrames ) {
            currEvent->close();
            LINFO("Event %i - no token found, closing event",currEvent->getEventNum());
        }
        else {
             LINFO("##########Event %i - no token found, keeping event open for expiration frames: %d ##########",
		currEvent->getEventNum(), itsDetectionParms.itsEventExpirationFrames); 
            evtToken.frame_nr = frameNum;
            currEvent->assign_noprediction(evtToken, curFOE,  currEvent->getValidEndFrame(),  itsDetectionParms.itsEventExpirationFrames);
        }
      } 
    else {
      // associate this token to the best fitting guy
      Vector2D emtpy(0.F,0.F);
      Token tl = currEvent->getToken(currEvent->getEndFrame());
      Token tk(*lObj, frameNum, metadata, tl.scaleW, tl.scaleH);
      tk.bitObject.computeSecondMoments();
      currEvent->assign(tk, emtpy, frameNum);
      LINFO("Event %i - token found at %g, %g area: %d",currEvent->getEventNum(),
            tl.location.x(), 
            tl.location.y(),
            area1);
    }    
	   
    objs.clear();    
  }
	
  // ######################################################################
  void VisualEventSet::updateEvents(const Image<byte>& binMap,  
                                    const Vector2D& curFOE, 
                                    int frameNum,
                                    const MbariMetaData &metadata)
  {
    if (startframe == -1) {startframe = frameNum; endframe = frameNum;}
    if (frameNum > endframe) endframe = frameNum;
	
    std::list<VisualEvent *>::iterator currEvent;
	
    for (currEvent = itsEvents.begin(); currEvent != itsEvents.end(); ++currEvent)
      if ((*currEvent)->isOpen()) {
        switch(itsDetectionParms.itsTrackingMode) {
        case(TMKalmanFilter):
          runKalmanTracker(*currEvent, frameNum, metadata, binMap, curFOE);
          break;
        case(TMNearestNeighbor):
          runNearestNeighborTracker(*currEvent, frameNum, metadata, binMap, curFOE);
          break;
        case(TMNone):
          break;
        default:
          runKalmanTracker(*currEvent, frameNum, metadata, binMap, curFOE);
          break;
        }
      }
  }
	
  // ######################################################################
  void VisualEventSet::initiateEventsColor(std::list<BitObject>& bos, int frameNum, \
                                      const MbariMetaData &metadata, float scaleW, float scaleH)
  {
    if (startframe == -1) {startframe = frameNum; endframe = frameNum;}
    if (frameNum > endframe) endframe = frameNum;  
	
    std::list<BitObject>::iterator currObj;
	
    //  go through all the remaining BitObjects and create new events for them
    for (currObj = bos.begin(); currObj != bos.end(); ++currObj)
      {
        itsColorEvents.push_back(new VisualEvent(Token(*currObj, frameNum, metadata, scaleW, scaleH), itsDetectionParms));
        LINFO("assigning object of area: %i to new event %i",currObj->getArea(),
              itsColorEvents.back()->getEventNum());
      }
  }
	
  // ######################################################################
  void VisualEventSet::initiateEvents(std::list<BitObject>& bos, int frameNum, \
                                      const MbariMetaData &metadata, float scaleW, float scaleH )
  {
    if (startframe == -1) {startframe = frameNum; endframe = frameNum;}
    if (frameNum > endframe) endframe = frameNum;  
	
    std::list<BitObject>::iterator currObj;
	
    // loop over the BitObjects
    currObj = bos.begin();
    while(currObj != bos.end())    {
      std::list<BitObject>::iterator next = currObj;
      ++next;
	            
      // is there an intersection with an event?
      if (doesIntersect(*currObj, frameNum))   
        bos.erase(currObj);       
	
      currObj = next;
    }
	
    // now go through all the remaining BitObjects and create new events for them
    for (currObj = bos.begin(); currObj != bos.end(); ++currObj)
      {
        itsEvents.push_back(new VisualEvent(Token(*currObj, frameNum, metadata, scaleW, scaleH), itsDetectionParms));
        LINFO("assigning object of area: %i to new event %i",currObj->getArea(),
              itsEvents.back()->getEventNum());
      }
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
                                  bool saveNonInterestingEvents)
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
                textImg.resize(numW * numText.length(), numH, NO_INIT);
                textImg.clear(COL_WHITE);
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
	
    // not enough space to the right? -> shift as apropriate
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
  std::list<BitObject>
  VisualEventSet::getColorBitObjectsForFrame(uint framenum)
  {
    std::list<BitObject> result;
    std::list<VisualEvent *>::iterator evt;
	 
    for (evt = itsColorEvents.begin(); evt != itsColorEvents.end(); ++evt) 
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


