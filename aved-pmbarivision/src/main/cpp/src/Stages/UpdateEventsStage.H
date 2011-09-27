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

#if !defined(UPDATEEVENTS_H)
#define UPDATEEVENTS_H

#include "MessagePassing/Mpidef.H"
#include "Stages/Stage.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "DetectionAndTracking/Segmentation.H"
#include "Image/MbariImageCache.H"
#include "Data/MbariMetaData.H"
#include "Media/FrameSeries.H"
#include "Media/MbariResultViewer.H"
#include "DetectionAndTracking/FOEestimator.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "Media/FrameRange.H"
#include "Stages/SalientWinner.H"
#include <map>

// ######################################################################
//! Runs tracking and updates cached events. Handles writing results to the output
class UpdateEventsStage : public Stage  
{
public:
  // !Constructor
  UpdateEventsStage(MPI_Comm mastercomm, const char *name, 
		    nub::soft_ref<InputFrameSeries> &ifs,
		    nub::soft_ref<MbariResultViewer> &rv,
		    const std::string& eventsFileStem,
		    const FrameRange &framerange);
  
  // !Destructor
  virtual ~UpdateEventsStage();
  
  // !custom UpdateEventsStage run method.
  virtual void runStage();  

  // !custom MbariBeoResultViewer init method.
  virtual void initStage();

  // !custom shutdown. Sends MSG_EXIT to stages pending on this stage
  virtual void shutdown();
  
private:
  
  // !updates events and focus of expansion calculation
  void updateEvents();  
  
  // !initiate events for frameNum. Uses itsSalientFrameCache and itsWinner cache
  // to seed events
  void initiateEvents(int frameNum, Image< byte > bitImg);

  MbariVisualEvent::VisualEventSet itsEventSet;	
  MbariImageCache< byte > itsOutCache;	
  MbariImageCache< byte > itsSalientFrameCache;
  MbariImageCache< PixRGB<byte> > itsRGBOutCache;
  std::map<int, std::list<SalientWinner>* > itsWinners;    
  nub::soft_ref<InputFrameSeries> itsifs;
  nub::soft_ref<MbariResultViewer> itsRv;
  std::string itsInputFileStem;
  FrameRange itsFrameRange; 
  FOEestimator itsFOEEst;
  int itsLastEventSeedFrameNum;  
};

#endif // !defined(UPDATEEVENTS_H)
