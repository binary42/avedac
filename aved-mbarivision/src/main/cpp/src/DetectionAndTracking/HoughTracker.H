/*
 * Copyright 2014 MBARI
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

/*!@file HoughTracker.H a class for Hough-based tracking of Non-Rigid Objects
  in video. Based on work done by
 */

#ifndef HOUGHTRACKER_H_DEFINED
#define HOUGHTRACKER_H_DEFINED

#include "Image/OpenCVUtil.H"
#include "Image/Dims.H"
#include "Image/Rectangle.H"
#include "Image/Geometry2D.H"
#include "DetectionAndTracking/houghtrack/fern.h"
#include "DetectionAndTracking/houghtrack/utilities.h"
#include "Image/MbariImage.H"
#include "nub/ref.h"

template <class T> class Image;
template <class T> class PixRGB;
template <class T> class MbariImage;

class MbariResultViewer;

namespace HoughTracker {

// ######################################################################
//! runs the HoughTracker algorithm
class HoughTracker
{
public:
  //! constructor
  /* !@img the image to segment and track
  @region the bounding box to initialize the tracker */
  HoughTracker(const MbariImage< PixRGB<byte> >& img, const Rectangle &region);

 //! constructor
  HoughTracker();

  //! update with a new frame from the video
  /* !@rv the results viewer to display intermediate results
  @img the image to segment and track
  @prediction the predicted maximum point
  @binaryImg the tracked object; object pixels are 1; all other pixels are 0
  @evtNum the event number this tracker is assigned to
  @return true if object tracked*/
  bool update(nub::soft_ref<MbariResultViewer>&rv, \
              MbariImage< PixRGB<byte> >& img, \
              Point2D<int> &prediction, \
              Image< byte >& binaryImg, \
              int evtNum);

private:

  void run(const cv::Rect& ROI, const cv::Point& center, cv::Mat& mask);

  //! calculates the center of mass of the foreground segment
  // to be used to track. This center is of the currently visible part of the tracked
  // object only
  /*!@return the center of mass point in image coordinates*/
  cv::Point centerOfMass(cv::Mat& mask);

  //! calculates the bounding box of the back projection
  /*!@return the bounding box region*/
  cv::Rect getBoundingBox(cv::Mat& backproject);

  //! converts Hough-based back projection  to binary
  void makeBinarySegmentation(nub::soft_ref<MbariResultViewer>&rv, \
											cv::Mat& backproject, \
											Image< byte >& output, \
											uint frameNum, \
											int evtNum);

  //! colorisze and show the end results
  void showResult(nub::soft_ref<MbariResultViewer>&rv, \
                                cv::Mat& frame, \
                                cv::Mat& backproject, \
                                const cv::Rect& ROI, \
                                const cv::Rect& object, \
                                uint frameNum, \
                                int evtNum);

  //! show the segmentation request
  void showSegmentation(nub::soft_ref<MbariResultViewer>& rv, \
                        cv::Mat& backproject, \
                        std::string title, \
                        uint frameNum, \
                        int evtNum);

  inline cv::Rect squarify(cv::Rect object, double searchFactor)
  {
	int len = std::max(object.width * searchFactor, object.height * searchFactor);
	return cv::Rect( object.x + object.width/2 - len/2, object.y + object.height/2 - len/2, len, len );
  }

  Ferns itsFerns;
  cv::Rect itsMaxObject, itsImgRect, itsObject, itsSearchWindow, itsUpdateRegion ;
  Features itsFeatures;
  cv::Point itsMaxLoc;
};
}
#endif
