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

/*!@file MbariVisualEvent.H classes useful for event tracking */
  
#ifndef VISUALEVENT_H_DEFINED
#define VISUALEVENT_H_DEFINED

#include "Image/KalmanFilter.H"
#include "Image/Point2D.H"
#include "Image/Rectangle.H"
#include "Util/Assert.H"
#include "Util/Types.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "DetectionAndTracking/HoughTracker.H"
#include "Data/MbariMetaData.H"
#include "Image/BitObject.H"

#include <list>
#include <string>
#include <vector>

template <class T> class Image;
template <class T> class PixRGB;

class Vector2D;
class StraighLine2D;

namespace MbariVisualEvent {
  // ######################################################################
  //! public class that contains information for a visual token for tracking
  class Token
  {
  public:
	
    //!default constructor
    Token();
	
    //!constructor with the location being the centroid of the BitObject
    Token (BitObject bo, uint frame);
	
    //!constructor with the location being the centroid of the BitObject
    Token (BitObject bo, uint frame, const MbariMetaData& m);
	
    //!read the Token from the input stream is
    Token (std::istream& is);
	
    //!the BitObject that holds info about the shape of this Token
    BitObject bitObject;
	
    //!the x and y coordinates of the token in the frame
    Vector2D location;
	
    //!the expected location for this token
    Vector2D prediction;
	
    //!the straight line on which this token is moving
    StraightLine2D line;
	
    //!the angle between line and the connection to the FOE
    float angle;
	
    //!the focus of expansion (FOE)  for this token
    Vector2D foe;
	
    //!the frame number at which the token occurred
    uint frame_nr;
	
    //!the Mbari meta data mfor this frame token
    MbariMetaData mbarimetadata;
	
    //! write flag; defaults to false in constructor; set to false
    // in writeToStream
    bool written;
	
    //! write the entire Token to the output stream os
    void writeToStream(std::ostream& os);
	
    //! read the Token from the input stream is
    void readFromStream(std::istream& is);
	
    //! write the Token's position to the output stream os
    void writePosition(std::ostream& os) const;

     //! copy operator
    Token & operator=(const Token& tk);
  };
	
  // ######################################################################
  //! class that contains a set of property vectors for events
  /*! each property vector contains the following fields:<br>
    <table><tr><td><b>index</b><td><b>content</b></th>
    <tr><td>0<td>event number
    <tr><td>1<td>interestingness index
    <tr><td>2<td>area
    <tr><td>3<td>Uxx
    <tr><td>4<td>Uyy
    <tr><td>5<td>Uxy
    <tr><td>6<td>major Axis
    <tr><td>7<td>minor Axis
    <tr><td>8<td>elongation
    <tr><td>9<td>orientation angle
    <tr><td>10<td>maximum intensity
    <tr><td>11<td>minimum intensity
    <tr><td>12<td>average intensity</table>*/
  class PropertyVectorSet
  {
  public:
	
    //! default contructor
    PropertyVectorSet();
	
    //! default destructor
    ~PropertyVectorSet();
	
    //! read from stream constructor
    PropertyVectorSet(std::istream& is);
	
    //! write the entire PropertyVectorSet to the output stream os
    void writeToStream(std::ostream& os);
	
    //! write the PropertyVectorSet header to the output stream os
    void writeHeaderToStream(std::ostream& os);
	
    //! read the PropertyVectorSet from the input stream is
    void readFromStream(std::istream& is);
	
    //! return the PropertyVector for an event by its number
    std::vector<float> getPropertyVectorForEvent(const int num);
	
    //! the vector of property vectors
    std::vector< std::vector<float> > itsVectors;
  };
	
  // ######################################################################
  //!stores a visual event as a sequence of tokens with additional information
  class VisualEvent
  {
  public:
    //! constructor
    /*!@param tk the first token for this event
    @param parms the detection parameters
    @param img the image the token was extracted from*/
    VisualEvent(Token tk, const DetectionParameters &parms, Image< PixRGB<byte> >& img);
	  
    //! destructor
    ~VisualEvent();
	
    //! read the VisualEvent from the input stream is
    VisualEvent(std::istream& is);
	
    //! write the entire VisualEvent to the output stream os
    void writeToStream(std::ostream& os);
	
    //! read the VisualEvent from the input stream is
    void readFromStream(std::istream& is);
	
    //! write all the positions for this event to the output stream os
    void writePositions(std::ostream& os) const;
	
    //! get the prediction for the location of the next token
    Point2D<int> predictedLocation() ;

    //! get the average acceleration speed the token is moving
    float getAcceleration() const;

    //! is this token allowed as the next one?
    bool isTokenOk(const Token& tk) const;
	
    //! returns the cost of associating tk with this event
    /*!@return returns -1.0F if the token is not valid for this event*/
    float getCost(const Token& tk);
	
    //! assign tk to this event, use foe as the focus of expansion
    void assign(const Token& tk, const Vector2D& foe,  uint validendframe);

    //! assign tk to this event, use foe as the focus of expansion, don't update the prediction
    void assign_noprediction(const Token& tk, const Vector2D& foe,  uint validendframe,  uint expireFrames);
	
    //! if the BitObject intersects with the one for this event at frameNum
    bool doesIntersect(const BitObject& obj, int frameNum) const;

	//! updates the Hough-based tracker
	// !@returns false if tracker fails
	bool updateHoughTracker(nub::soft_ref<MbariResultViewer>&rv,  uint frameNum, Image< PixRGB<byte> >& img,
	                        const Image< byte >& occlusionImg, Image< byte >& binaryImg, Rectangle &boundingBox);

    //! reset the Hough-based tracker
    void resetHoughTracker(Image< PixRGB<byte> >& img, BitObject &bo);

    //! free up memory associated with the Hough-based tracker
    void freeHoughTracker();

    // ! VisualEvent states
    enum State {
      OPEN,
      CLOSED,
      WRITE_FINI,
      DELETE
    };

    //! set the state of the VisualEvent
    inline void setState(VisualEvent::State state);
	
    //! get the state of the VisualEvent
    inline VisualEvent::State getState();
	
    //! mark this event as "closed"
    inline void close();
	
    //! whether this event is closed
    inline bool isClosed();
	  
    //! flag this event as written
    inline void flagWriteComplete ();
	
    //! flag this event for deletion
    inline void flagForDelete();
	  
    //! whether event is ready to be deleted
    inline bool isReadyToDelete();  
		
    //! whether this event is still open
    inline bool isOpen(); 
	 
    //! return the event identification number of this event
    inline uint getEventNum() const;
	
    //! return the frame number of the first token
    inline uint getStartFrame() const;
	
    //! return the frame number of the last token
    inline uint getEndFrame() const;

    //! return the frame number of the last valid( non duplicated token)
    inline uint getValidEndFrame() const;
	
    //! return starting timecode string
    inline std::string getStartTimecode() const;
      
    //! return starting timecode string
    inline std::string getEndTimecode() const;      
	  
    //! return the number of frames that this event spans
    inline uint getNumberOfFrames() const;
	
    //! return the number of tokens stored for this event
    // note that this may not be the same as getNumberOfFrames()
    // as frames may be deleted during runtime to save space
    inline uint getNumberOfTokens() const;
	
    //! return the maximum object size of any of the tokens 
    inline int getMaxSize() const;

    //! return the minimum object size of any of the tokens
    inline int getMinSize() const;

    //! return the token that has the maximum object size
    inline Token getMaxSizeToken() const;
	
    //!return a token based on a frame number
    inline Token getToken(const uint frame_num) const;
	  
    //!return reset bit object contained in frame number
    inline void resetBitObject(const uint frame_num, BitObject &obj);
	  
    //!return whether frame_num is between start frame and end frame
    inline bool frameInRange(const uint frame_num) const;	

    //! return true if the  initialize the Hough tracker
    inline bool getHoughReset() const;

    //! return the forget constant that is used with the Hough tracker
    inline void setForgetConstant(float constant);

    //! return the forget constant that is used with the Hough tracker
    inline float getForgetConstant();

    //! return a vector that contains a bunch of numbers that describe the event
    /*! see source code for details */
    std::vector<float>  getPropertyVector();
	
    //! returns the maximum dimensions of the tracked object in any of the frames
    Dims getMaxObjectDims() const;

    enum Category {
      BORING,
      INTERESTING
    };

    //! get category for this event returns INTERESTING OR BORING
    VisualEvent::Category getCategory() const;

	enum TrackerType {
	  NN,
	  KALMAN,
	  HOUGH
	};

    //! set the type of tracker for the visualEvent
    inline void setTrackerType(VisualEvent::TrackerType type);

    //! returns TrackerType
    inline TrackerType getTrackerType();

    //! return true if the tracker changed
    inline bool trackerChanged();

  private:
    static uint counter;  
    uint myNum;
    std::vector<Token> tokens;
    uint startframe;
    uint endframe;
    uint validendframe;
    int max_size,min_size;
    uint maxsize_framenr;
    // ! VisualEvent state
    VisualEvent::State itsState;  
    KalmanFilter xTracker, yTracker;
    HoughTracker::HoughTracker hTracker;
    TrackerType itsTrackerType;
    bool itsTrackerChanged;
    bool itsHoughReset;
    float houghConstant;
    static const std::string trackerName[];
    //! True if interesting, otherwise false (for boring) defaults to boring
    Category itsCategory;
    DetectionParameters itsDetectionParms;

  };
	
  // ######################################################################
  // ######## VisualEventSet
  // ######################################################################
  //! contains a set of visual events to which a set of tokens can be matched
  class VisualEventSet
  {
  public:  
    //! constructor
    /*!@param maxDist the maximum distance that a token can have from an 
      event and still be considered for becoming part of the event*/
    VisualEventSet(const DetectionParameters &parameters, const std::string& filename);
	
    //! read the VisualEventSet from the input stream is
    VisualEventSet(std::istream& is);
	
    //! write the VisualEventSet header to the output stream os
    void writeHeaderToStream(std::ostream& os);
	
    //! read the VisualEventSet header from the input stream is
    void readHeaderFromStream(std::istream& is);
	
    //! write the entire VisualEventSet to the output stream os
    void writeToStream(std::ostream& os);
	
    //! read the VisualEventSet from the input stream is
    void readFromStream(std::istream& is);
	
    //! write the positions of all events to the output stream os
    void writePositions(std::ostream& os) const;
	
    //!update events with new binary map
    /*!@param img image to update events from
      @param prevImg previous frame image used in Hough tracker initialization
      @param segmentIn  image used to run segmentation to extract BitObjects from
      @param curFOE the current focus of expansion for detecting unusual motion
      @param metadata associated with current frame number*/
    void updateEvents(nub::soft_ref<MbariResultViewer>&rv, const Image< byte >& mask,
                      const uint frameNum,
                      Image< PixRGB<byte> >& img,
                      Image< PixRGB<byte> >& prevImg,
                      const Image< PixRGB<byte> >& segmentIn,
                      const Vector2D& curFOE,
                      const MbariMetaData &m);

	//! return the average speed events are moving
	float getAverageSpeed();

    //! get the min and max areas across all events
    void getAreaRange(int &minArea, int &maxArea);

    //! get average acceleration for all events except the skipEventNum
    float getAcceleration(uint skipEventNum);

    //! insert event into current list of event
    void insert(VisualEvent *event);

    //! initiate new events for all BitObjects in bos if they aren't tracked yet
    void initiateEvents(std::list<BitObject>& bos,
                                      const int frameNum,
                                      const MbariMetaData &metadata,
                                      Image< PixRGB<byte> >& img,
                                      const Vector2D& curFOE);

    //! if obj intersects with any of the event at frameNum, reset SMV and Hough bounds
    bool resetIntersect(Image< PixRGB<byte> >& img, BitObject& obj, const Vector2D& curFOE, int frameNum);

    //! if obj intersects with any of the event at frameNum, reset SMV
    bool doesIntersect(BitObject& obj, int frameNum);

    //! if obj intersects with any of the events in frameNum, return true and first found intersecting eventNum
    bool doesIntersect(BitObject& obj, uint *eventNum, int frameNum);

    //! return the number of stored events
    uint numEvents() const;
	
    //! delete all stored events
    void reset();
	
    // ! returns minimum size for an "interesting event"
    const int minSize();
	
    //! clean up the event list - erase all unsuccessful candidates
    /*!@param currFrame - the current frame number in processing
      @param lastframe in this sequence*/
    void cleanUp(uint currFrame, uint lastframe=1);
	
    //! close all events (for clean-up at the end)
    void closeAll();
	
    //! print out debugging info on all events
    void printAll();
	
    //! returns a set of all tokens stored at a particular frame number
    std::vector<Token> getTokens(uint frameNum);
	                  
    void drawTokens(Image< PixRGB<byte> >& img,
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
                    float scaleW = 1.0F, float scaleH = 1.0F);
    
    //! returns a PropertyVectorSet for this VisualEventSet
    PropertyVectorSet getPropertyVectorSet();
	  
    //! returns a PropertyVectorSet that can be saved for this VisualEventSet
    PropertyVectorSet getPropertyVectorSetToSave();
	
    //! return the latest frame number before currFrame for which events have been closed already
    int getAllClosedFrameNum(uint currFrame);
	
    //! test whether the event with eventNum exists in this set
    bool doesEventExist(uint eventNum) const;
	
    //! Returns the event with number eventNum
    VisualEvent* getEventByNumber(uint eventNum) const;
	
    //! Replace the event for eventnum
    void replaceEvent(uint eventum, VisualEvent *event);
	
    //! Returns iterators pointing to all events at framenum
    std::list<VisualEvent *> getEventsForFrame(uint framenum);
	
    //! Returns a list of all BitObject at framnum
    std::list<BitObject> getBitObjectsForFrame(uint framenum);

    //! Returns an iterator pointing to all (interesting or boring)
    // ready to be written for given framenum
    std::list<VisualEvent *> getEventsReadyToSave(uint framenum);
	
  private:
    // compute the right position for a text label
    Point2D<int> getLabelPosition(Dims imgDims,Rectangle bbox,
                             Dims textDims) const;
	
    // runs the nearest neighbor tracking on @param event
    // returns true if able to track
    bool runNearestNeighborTracker(VisualEvent *event, const uint frameNum, const MbariMetaData &m,
                               const Image< PixRGB<byte> >& segmentIn,
                               const Vector2D& curFOE,
                               bool skip  = false);
	
    // runs the Kalman filter tracking on @param event
    // returns true if able to track
    bool runKalmanTracker(VisualEvent *event, const uint frameNum, const MbariMetaData &m,
                          const Image< PixRGB<byte> >& segmentIn,
                          const Vector2D& curFOE,
                          bool skip = false);

	// run the Hough-based tracker on @param event
	// returns true if able to track
    bool runHoughTracker(nub::soft_ref<MbariResultViewer>&rv, VisualEvent *currEvent, const uint frameNum, const MbariMetaData &m,
                                Image< PixRGB<byte> >& img,
                                const Image< byte >& mask,
                                const Vector2D& curFOE,
                                bool skip = false);

	// run the combination Hough and NearestNeighbor tracker on @param event
    void runNearestNeighborHoughTracker(VisualEvent *event, const uint frameNum, const MbariMetaData &m,
                               const Image< PixRGB<byte> >& segmentIn,
                               const Image< byte >& mask,
                               const Vector2D& curFOE,
                               Image< PixRGB< byte> >& img,
                               Image< PixRGB< byte> >& prevImg);

	// run the combination Hough and Kalman tracker on @param event
    void runKalmanHoughTracker(nub::soft_ref<MbariResultViewer>&rv, VisualEvent *event, const uint frameNum, const MbariMetaData &m,
                               const Image< PixRGB<byte> >& segmentIn,
                               const Image< byte >& mask,
                               const Vector2D& curFOE,
                               Image< PixRGB< byte> >& img,
                               Image< PixRGB< byte> >& prevImg);

    // run the check for failure conditions on the @param event
    void checkFailureConditions(VisualEvent *currEvent, Dims d);

    std::list<VisualEvent *> itsEvents;
    int startframe; 
    int endframe;
    std::string itsFileName;
    DetectionParameters itsDetectionParms;
  };
	  
  // ######################################################################
  // ########### INLINED METHODS
  // ######################################################################
  inline VisualEvent::TrackerType VisualEvent::getTrackerType()
  { return itsTrackerType; }

  // ######################################################################
  inline bool VisualEvent::trackerChanged()
  { return itsTrackerChanged; }

  // ######################################################################
  inline void VisualEvent::setState(VisualEvent::State state)
  { itsState = state; }
	
  // ######################################################################
  inline VisualEvent::State VisualEvent::getState()
  { return itsState; }
	
  // ######################################################################
  inline void VisualEvent::close()
  { itsState = CLOSED; }
	
  // ######################################################################
  inline bool VisualEvent::isClosed()
  { return (itsState == CLOSED ? true: false); }
	
  // ######################################################################
  inline bool VisualEvent::isOpen()
  { return (itsState == OPEN ? true: false); }
	
  // ######################################################################
  inline void VisualEvent::flagForDelete() 
  { itsState = DELETE; }	
	
  // ######################################################################
  inline void VisualEvent::flagWriteComplete()
  { itsState = WRITE_FINI; }	
	
  // ######################################################################
  inline bool VisualEvent::isReadyToDelete() 
  { return (itsState == DELETE ? true: false); }
		
  // ######################################################################
  inline uint VisualEvent::getEventNum() const
  { return myNum; }
	
  // ######################################################################
  inline uint VisualEvent::getStartFrame() const
  { return startframe; }	
	
  // ######################################################################
  inline uint VisualEvent::getValidEndFrame() const
  { return validendframe; }
  
    // ######################################################################
  inline uint VisualEvent::getEndFrame() const
  { return endframe; }
  // ######################################################################
  inline std::string VisualEvent::getStartTimecode() const
  {
    Token tk  = getToken(startframe);
    return tk.mbarimetadata.getTC();
  }
  // ######################################################################
  std::string VisualEvent::getEndTimecode() const
  {
    Token tk  = getToken(endframe);
    return tk.mbarimetadata.getTC();
  }
  // ######################################################################
  inline uint VisualEvent::getNumberOfFrames() const
  { return (endframe - startframe + 1); }
	
  // ######################################################################
  inline uint VisualEvent::getNumberOfTokens() const
  { return tokens.size(); }
	
  // ######################################################################
  inline int VisualEvent::getMaxSize() const
  { return max_size; }

  // ######################################################################
  inline int VisualEvent::getMinSize() const
  { return min_size; }
	
  // ######################################################################
  inline Token VisualEvent::getMaxSizeToken() const
  { return getToken(maxsize_framenr); }
	
  // ######################################################################
  inline Token VisualEvent::getToken(uint frame_num) const
  {
    Token empty; 
    ASSERT (frameInRange(frame_num));
    for(uint i=0; i<tokens.size(); i++) {
      Token tk = tokens[i];
      if(tk.frame_nr == frame_num) 
        return tk;
    }
    return empty;
  }
  // ######################################################################
  inline void VisualEvent::resetBitObject(const uint frame_num,
                                          BitObject &obj)
  {
    ASSERT (frameInRange(frame_num));
    tokens[frame_num - startframe].bitObject = obj;
  }
	
  // ######################################################################
  inline bool VisualEvent::frameInRange(const uint frame_num) const
  {
    return ((frame_num >= startframe) && (frame_num <= endframe));
  }

  // ######################################################################
  inline bool VisualEvent::getHoughReset() const
  {
    return itsHoughReset;
  }

  // ######################################################################
  inline float VisualEvent::getForgetConstant()
  {
    return houghConstant;
  }

  // ######################################################################
  inline void VisualEvent::setForgetConstant(float constant)
  {
    houghConstant = constant;
  }
}
#endif
 
