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
#if !defined(SEGMENT_H)
#define SEGMENT_H

#include "Stages/Stage.H"
#include "Image/Image.H"
#include "Image/ImageCache.H"
#include "Media/FrameSeries.H"
#include "Media/MbariResultViewer.H"
#include "DetectionAndTracking/Segmentation.H"
#include "DetectionAndTracking/DetectionParameters.H"

// ######################################################################
//! Handles segmenting objects from the background for use in the UpdateEventsStage
class SegmentStage: public Stage
{
public:		

  // !Constructor
  SegmentStage(MPI_Comm mastercomm, const char *name,
		       nub::soft_ref<InputFrameSeries> &ifs,
		       nub::soft_ref<MbariResultViewer> &rv,
                       const FrameRange framerange,
                       const std::string& inputFileStem);
  // !Destructor
  virtual ~SegmentStage();
	
  // !custom SegmentStage run method.
  virtual void runStage();

  // !custom shutdown. Sends MSG_EXIT to stages pending on this data
  virtual void shutdown();

  // preloads images to get valid average
  void preload();

private:
  ImageCacheAvg< byte > itsbwAvgCache;
  ImageCacheAvg< PixRGB<byte> > itsAvgCache;
  Segmentation itsSegmentation;
  nub::soft_ref<InputFrameSeries> itsifs;
  nub::soft_ref<MbariResultViewer> itsRv;
  FrameRange itsFrameRange;
  std::string itsInputFileStem;

};

#endif // !defined(SEGMENT_H)
