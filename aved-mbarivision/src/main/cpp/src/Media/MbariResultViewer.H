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

#ifndef MBARIRESULTVIEWER_H_
#define MBARIRESULTVIEWER_H_

#include "Component/ModelComponent.H"
#include "Component/ModelParam.H"
#include "Image/ArrayData.H"
#include "Image/Pixels.H"
#include "Media/FrameRange.H"
#include "Media/FrameSeries.H"
#include "Util/Types.H"
#include "rutz/shared_ptr.h"

#include "Image/BitObjectDrawModes.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "Image/MbariImage.H"
#include "Utils/MbariXMLParser.H"

#include <string>
#include <vector>

class ModelManager;
class XWinManaged;
template <class T> class MbariImage;

using namespace MbariVisualEvent;
using namespace std;

// ######################################################################
//! Handles everything regarding displaying and saving results
class MbariResultViewer : public ModelComponent
{
public:
  //! Constructor - give a ModelManager and an OutputFrameSeries
  /* for both the events and and full-frame output */
  MbariResultViewer(ModelManager& mgr, 
			nub::soft_ref<OutputFrameSeries> evtofs, 
			nub::soft_ref<OutputFrameSeries> ofs, 
			string binDir);

 // ! Destructor
  virtual ~MbariResultViewer();
	
  //! implements ModelComponent::paramChanged
  /*! intercepts setting of MRVmarkXXX variables and sets drawing
    colors accordingly */
  virtual void paramChanged(ModelParamBase* const param,
			    const bool valueChanged,
			    ParamClient::ChangeStatus* status);	
	
  //! destroy all the windows
  virtual void reset1();
	
  //! updates the MbariFramesSeries output one frame
  void updateNext();
	
  //! save and/or display intermediate result
  /*! whether the result image is saved and/or displayed is
    determined by the ModelParams
    @param img the image containing the intermediate result
    @param frameNum the frame number of the image
    @param resultName a string that defines the kind of interemediate
    result - this is appended to the filestem for the file name
    and/or window title
    @param resNum if there are several results of this type for each frame,
    the number of the result*/
  template <class T>
  void output(const Image<T>& img, const uint frameNum,
	      const string& resultName, const int resNum = -1);	
	
  //! display intermediate result
  /*!@param img the image containing the intermediate result
    @param frameNum the frame number of the image
    @param resultName a string that defines the kind of interemediate
    result - this is appended to the filestem for the file name
    and/or window title
    @param resNum if there are several results of this type for each frame,
    the number of the resul*/
  template <class T>
  void display(const Image<T>& img, const uint frameNum,
	       const string& resultName, const int resNum = -1);
				 
  //! save intermediate result
  /*!@param img the image containing the intermediate result
    @param frameNum the frame number of the image
    @param resultName a string that defines the kind of interemediate
    result - this is appended to the filestem for the file name
    and/or window title
    @param resNum if there are several results of this type for each frame,
    the number of the resul*/
  void save(const Image< PixRGB<byte> >& img, const uint frameNum,
	    const string& resultName, const int resNum = -1);

  //! save intermediate result
  /*!@param img the image containing the intermediate result
    @param frameNum the frame number of the image
    @param resultName a string that defines the kind of interemediate
    result - this is appended to the filestem for the file name
    and/or window title
    @param resNum if there are several results of this type for each frame,
    the number of the resul*/
  void save(const Image<byte>& img, const uint frameNum,
	    const string& resultName, const int resNum = -1);

  //! save intermediate result
  /*!@param img the image containing the intermediate result
    @param frameNum the frame number of the image
    @param resultName a string that defines the kind of interemediate
    result - this is appended to the filestem for the file name
    and/or window title
    @param resNum if there are several results of this type for each frame,
    the number of the resul*/
  void save(const Image<float>& img, const uint frameNum,
	    const string& resultName, const int resNum = -1);

  //! save and possibly display an output frame
  /*!Depending on the ModelParams, interesting events, candidates
    for interesting events and skipped event locations are marked
    in the frame, the frame is displayed (if desired) and saved.
    @param resultImg the output image
    @param evts the event set to be used for drawing the events
    @param circleRadius the radius of the circles used for marking*/

  void outputResultFrame(MbariImage< PixRGB<byte> >& resultImg, 
			 MbariVisualEvent::VisualEventSet& evts, 
			 const int circleRadius);

  //! check if the LoadEventsName is set
  bool isLoadEventsNameSet() const;
  
  //! check if the Save Ouput is set
  bool isSaveOutputSet() const;

  //! check if the Display Ouput is set
  bool isDisplayOutputSet() const;


  //! load the VisualEventSet from the file LoadEventsName
  void loadVisualEventSet(MbariVisualEvent::VisualEventSet& ves) const;

  //! check if the LoadPropertiesName is set
  bool isLoadPropertiesNameSet() const;

  //! load the PropertyVector from the file LoadPropertiesName
  void loadProperties(MbariVisualEvent::PropertyVectorSet& pvs) const;

  //! check if the SaveEventsName is set
  bool isSaveEventsNameSet() const;

  //! save the VisualEventSet to the file SaveEventsName
  void saveVisualEventSet(MbariVisualEvent::VisualEventSet& ves) const;

  //! check if the SavePropertiesName is set
  bool isSavePropertiesNameSet() const;

  //! save the PropertyVector to the file SavePropertiesName
  void saveProperties(MbariVisualEvent::PropertyVectorSet& pvs);

  //! check if the SavePositionsName is set
  bool isSavePositionsNameSet() const;

  //! save the positions to the file SavePositionsName
  void savePositions(const MbariVisualEvent::VisualEventSet& ves) const;

  //! determine whether the frames actually need to be loaded
  bool needFrames() const;

  //! returns the size of the cache for running averages (from the command line)
  uint getAvgCacheSize() const;

  //! determine whether we have to save any event clips
  bool isSaveEventClip() const;

  //! return the number of event clips we have to save
  uint numSaveEventClips() const;

  //! returns the event number with idx to be saved as a clip
  uint getSaveEventClipNum(uint idx) const;

  //! save a cut-out piece of frame containing a single event
  void saveSingleEventFrame(MbariImage< PixRGB<byte> >& img,
			    int frameNum, MbariVisualEvent::VisualEvent *event);

  //! save the VisualEventList to the file SaveEventsName
  void saveVisualEvent(MbariVisualEvent::VisualEventSet& ves,
		       std::list<MbariVisualEvent::VisualEvent *> &ves);
						 
  //! check if the SaveEventsSummaryName is set
  bool isSaveEventSummaryNameSet() const;
	
  //! save the VisualEventList to the file SaveEventsSummaryName
  void saveVisualEventSummary(string versionString,
			      std::list<MbariVisualEvent::VisualEvent *> &ves);
	
  //! save the positions to the file SavePositionsName
  void savePositions(const std::list<MbariVisualEvent::VisualEvent *> &ves) const;  
	
  //! check if the saveXMLEventsNameSet is set
  bool isSaveXMLEventsNameSet() const;  

  //! check if the metadataSource is set
  bool hasMetadataSource() const;  
	
  //! check if save all event clips is set
  bool isSaveAllEventClips() const;
	
  //! save the VisualEventList to the file saveXMLEventSetName
  /*! @param ves Visual Event Set
    @param currframe current frame we are writing 
    @param timecode current frame timecode
    @param fr frame range for this event set
  */	
  void saveVisualEventSetToXML(std::list<MbariVisualEvent::VisualEvent *> &ves,
			       int currframe,
			       string timecode,
			       FrameRange fr);
							     
  //! Creates AVED XML document with header information: 
  /*! @param versionString the version string that represents this binary 
    @param fr frame range of this event sequence
    @param timecodefirst starting timecode of this event sequence, e.g. in 00:01:12
    @param timecodelast ending timecode of this event sequence, e.g. in 00:01:12
    @param params detection parameters 
    * */
  void createXMLDocument(string versionString,
			 FrameRange fr,
			 string timecodefirst,
			 string timecodelast,
			 DetectionParameters params); 
	
protected:

  //! destroy windows and other internal variables
  void freeMem();

  //! get an internal reference number from a resultName
  /*! If this resultName is new, a new reference is created*/
  uint getNumFromString(const string& resultName);

  //! make a label text from the reference number and the frame number
  string getLabel(const uint num, const uint frameNum, const int resNum = -1);

  //! make the file stem for saving files
  string getFileStem(const string& resultName, const int resNum = -1);

  //! draw an image to a window, deal with creating and rescaling the window
  template <class T>
  XWinManaged* displayImage(const Image<T>& img,
			    XWinManaged* win,
			    const char* label);

  //! parse the SaveEventNumString and store the numbers in itsSaveEventNums
  void parseSaveEventNums(const string& value);

  OModelParam<bool> itsSaveResults;     //!<whether intermediate results are saved
  OModelParam<bool> itsDisplayResults;  //!<whether intermediate results are displayed
  OModelParam<BitObjectDrawMode> itsMarkInteresting; //!<the way interesting events are marked
  OModelParam<float> itsOpacity;        //!<opacity of shape or outline markings of events
  OModelParam<bool> itsMarkCandidate;   //!<whether candidates for int. events are marked
  OModelParam<bool> itsMarkPrediction;  //!<whether predictions of event locations are marked
  OModelParam<bool> itsMarkFOE;         //!<whether the focus of expansion is marked
  OModelParam<bool> itsSaveOutput;      //!<whether the output frames are saved
  OModelParam<bool> itsDisplayOutput;   //!<whether the output frames are displayed
  OModelParam<bool> itsShowEventLabels; //!<whether the event labels are written into output frames

  OModelParam<Dims> itsRescaleDisplay;  //!<rescale intermediate results to this scale for display

  OModelParam<int> itsSizeAvgCache;     //!<the number of images used for running average

  OModelParam<string> itsSaveEventsName;
  OModelParam<string> itsLoadEventsName;
  OModelParam<string> itsSavePropertiesName;
  OModelParam<string> itsLoadPropertiesName;
  OModelParam<string> itsSavePositionsName;

  OModelParam<string> itsSaveEventNumString;
  OModelParam<string> itsSaveSummaryEventsName;
  OModelParam<string> itsSaveXMLEventSetName;
  OModelParam<string> itsMetadataSource;
  
private:
  bool itsSaveNonInterestingEvents; //!<whether the non-interesting events are saved or not
	
  MbariXMLParser* itsXMLParser;
  std::vector<string> itsResultNames;
  std::vector<XWinManaged*> itsResultWindows;
  XWinManaged* resFrameWindow;
  PixRGB<byte> colInteresting, colCandidate, colPrediction, colFOE;
  nub::soft_ref<OutputFrameSeries> itsOfs, itsEvtOfs;
  std::vector<uint> itsSaveEventNums;
  bool XMLfileCreated;
  bool appendEvents, appendEventSummary, appendEventsXML;
  bool appendProperties;
  bool itsSaveEventNumsAll;
	
};

#endif /*MBARIRESULTVIEWER_H_*/
