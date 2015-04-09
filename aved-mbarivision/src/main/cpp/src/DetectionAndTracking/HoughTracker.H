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

#define DEFAULT_FORGET_CONSTANT 0.90f
#define DEFAULT_SCALE_INCREASE 1.25F
#define DEFAULT_MAP_SIZE 100
#define DEFAULT_MAP_STEP 2

template <class T> class Image;
template <class T> class PixRGB;
template <class T> class MbariImage;

class MbariResultViewer;
class BitObject;

namespace HoughTracker {

// ######################################################################
//! runs the HoughTracker algorithm
class HoughTracker
{
public:
  //! constructor
  /* !@img the image to segment and track
  @region the bounding box to initialize the tracker */
  HoughTracker(const Image< PixRGB<byte> >& img, BitObject& bo, float maxScale);

  //! constructor
  HoughTracker();

  //! destructor
  ~HoughTracker();

  //! free up memory associated with this tracker
  void free();

  //! update with a new frame from the video
  /* @frameNum the frame number (for display purposes)
  @img the image to segment and track
  @occlusionImg a mask representing the objects that are occluding this
  @prediction the predicted maximum point
  @binaryImg the tracked object; object pixels are white; all other pixels are black
  @evtNum the event number this tracker is assigned to
  @forgetConstant the tao forgetting constant
  @return true if object tracked*/
  bool update(nub::soft_ref<MbariResultViewer>&rv,
              const uint frameNum,
              Image< PixRGB<byte> >& img,
              const Image<byte> &occlusionImg,
              Point2D<int> &prediction,
              Rectangle &boundingBox,
              Image< byte >& binaryImg,
              const int evtNum,
              const float forgetConstant);

  /* !reset the tracker
  @img the image to segment and track
  @region the bounding box to initialize the tracker */
  void reset(const Image< PixRGB<byte> >& img, BitObject& bo, const float maxScale, const float forgetConstant);

private:

  bool run(const cv::Rect& ROI, const cv::Point& center, const cv::Mat& mask, const float forgetConstant);

  //! calculates the center of mass of the foreground segment
  // to be used to track. This center is of the currently visible part of the tracked
  // object only
  /*!@return the center of mass point in image coordinates*/
  cv::Point centerOfMass(const cv::Mat& mask);

  //! calculates the bounding box of the back projection
  /*!@return the bounding box region*/
  cv::Rect getBoundingBox(const cv::Mat& backproject);

  //! converts Hough-based back projection  to binary foreground labeled as 1 and background as 0
  Image< byte > makeBinarySegmentation(const cv::Mat& backproject, const uint frameNum, const int evtNum);

  //! show the segmentation request
  void showSegmentation(nub::soft_ref<MbariResultViewer>& rv, \
                        const cv::Mat& backproject, \
                        const std::string title, \
                        const uint frameNum, \
                        const int evtNum);

  //! mask known occlusions in back project image; sets pixels that are occluded to background
  cv::Mat maskOcclusion(const Image<byte> &occlusionImg,  const cv::Mat& backProject);

  inline cv::Rect squarify(const cv::Rect object, const double searchFactor)
  {
	int len = std::max(object.width * searchFactor, object.height * searchFactor);
	return cv::Rect( object.x + object.width/2 - len/2, object.y + object.height/2 - len/2, len, len );
  }

  Ferns itsFerns;
  cv::Rect itsMaxObject, itsImgRect, itsObject, itsSearchWindow;
  Features itsFeatures;
  cv::Point itsMaxLoc;
  int itsMapSize, itsMapStep;
};
}
#endif
