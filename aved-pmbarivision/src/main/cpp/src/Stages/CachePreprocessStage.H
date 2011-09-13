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

#if !defined(CACHEPREPROCESS_H)
#define CACHEPREPROCESS_H

#include "MessagePassing/Mpidef.H"
#include "Stages/Stage.H"
#include "Media/FrameSeries.H"
#include "Image/ImageCache.H"
#include "DetectionAndTracking/DetectionParameters.H"
#include "Media/FrameRange.H"
#include "Media/MbariResultViewer.H"

// ######################################################################
//! Handles caching images and minor preprocessing before sending them to the SegmentStage
class CachePreprocessStage : public Stage  
{
public:
	
  // !constructor
  CachePreprocessStage(MPI_Comm mastercomm, 
		       const char *name,
		       nub::soft_ref<InputFrameSeries> &ifs,
		       nub::soft_ref<MbariResultViewer> &rv,
		       const FrameRange framerange,
		       const std::string& inputFileStem,
                       const Dims& dims);

  //!desctructor
  virtual ~CachePreprocessStage();

  // !custom Segment run method.
  virtual void runStage();
	
  virtual void shutdown();

  // preloads images to get valid average
  void preload();

private:	
  ImageCacheAvg< PixRGB<byte> > itsAvgCache;
  nub::soft_ref<InputFrameSeries> itsifs;
  nub::soft_ref<MbariResultViewer> itsRv;
  FrameRange itsFrameRange; 
  std::string itsInputFileStem;
  Dims itsDims;
};

#endif // !defined(CACHEPREPROCESS_H)
