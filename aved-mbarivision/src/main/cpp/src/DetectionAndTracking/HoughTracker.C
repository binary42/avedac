/*
 * Copyright 2014 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1 
 * (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleitsFeatures/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, so its Features are
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

/*!@file HoughTracker.C a class for using Hough transform tracking algorithm */

#include "Image/OpenCVUtil.H"
#include "Image/ColorOps.H"
#include "Media/MbariResultViewer.H"
#include "DetectionAndTracking/HoughTracker.H"
#include "Image/ImageSet.H"
#include <csignal>

#define STEP_WIDTH 1
#define SHIFT_TO_CENTER
#define SEARCH_WINDOW 20
#define GRABCUT_ROUNDS 3

#include <vector>
template <class T> class MbariImage;

namespace HoughTracker {

// ######################################################################
HoughTracker::HoughTracker()
{}

// ######################################################################
HoughTracker::HoughTracker(const MbariImage< PixRGB<byte> >& img, const Rectangle &region)
{
    LINFO("Creating HoughTracker object size %dx%d region top %d left %d width %d height %d", \
    img.getDims().w(), img.getDims().h(), region.left(), region.top(), region.width(), region.height() );
    itsObject = cv::Rect(region.left(), region.top(), region.width(), region.height());
    int baseSize = 12;
    itsImgRect = cv::Rect(baseSize/2, baseSize/2, img.getDims().w()-baseSize, img.getDims().h()-baseSize);
    cv::Mat frame = img2ipl(img);
    itsFeatures.setImage(frame);

    cv::Mat backproject(img.getDims().h(), img.getDims().w(), CV_8UC1, cv::Scalar(cv::GC_BGD));
    cv::rectangle(backproject, cv::Point(itsObject.x-10, itsObject.y-10), cv::Point(itsObject.x+itsObject.width+10, itsObject.y+itsObject.height+10), cv::Scalar(cv::GC_PR_BGD), -1);
    cv::rectangle(backproject, cv::Point(itsObject.x, itsObject.y), cv::Point(itsObject.x+itsObject.width, itsObject.y+itsObject.height), cv::Scalar(cv::GC_FGD), -1);
    float maxScale = 2.0f;
    itsFerns.initialize(20, cv::Size(baseSize, baseSize), 8, itsFeatures.getNumChannels());
    itsMaxObject = intersect( itsImgRect, squarify(itsObject, maxScale));
    cv::Point center = getCenter(itsObject);

    LINFO("INITIAL POSITION: %d,%d %dx%d", itsObject.x,itsObject.y,itsObject.width,itsObject.height);
    itsUpdateRegion = intersect(itsMaxObject + cv::Size(40,40) - cv::Point(20,20), itsImgRect);
    run(itsUpdateRegion, center, backproject);

    itsSearchWindow = itsMaxObject + cv::Size(SEARCH_WINDOW,SEARCH_WINDOW) - cv::Point(SEARCH_WINDOW/2,SEARCH_WINDOW/2);
    LINFO(" START TRACKING");
}

// ######################################################################
bool HoughTracker::update(nub::soft_ref<MbariResultViewer>&rv, \
                            MbariImage< PixRGB<byte> >& img, \
                            Point2D<int> &prediction, \
                            Image<byte>& binaryImg, \
                            int evtNum)
{
  float backProjectRadius = 0.5;
  float backProjectminProb = 0.5;
  double minVal, maxVal = 6.0f;
  cv::Point minLoc;
  cv::Mat frame = img2ipl(img);
  itsFeatures.setImage(frame);
  cv::Size imgSize = cv::Size(frame.cols, frame.rows);
  cv::Mat result(imgSize.height, imgSize.width, CV_32FC1, cv::Scalar(0.0));
  cv::Mat backproject(img.getDims().h(), img.getDims().w(), CV_8UC1, cv::Scalar(cv::GC_BGD));
  cv::Point center;

  LINFO("EVALUATE");
  itsFerns.evaluate(itsFeatures, intersect(itsSearchWindow, itsImgRect), result, STEP_WIDTH, 0.5f);
  cv::Mat out = result;

  normalize(out, out, 255, 0, cv::NORM_MINMAX);
  minMaxLoc(result, &minVal, &maxVal, &minLoc, &itsMaxLoc);
  LINFO("LOCATE: maximum is at (%d/%d: %f)", itsMaxLoc.x, itsMaxLoc.y, maxVal);

  Point2D<int> ptMax(itsMaxLoc.x, itsMaxLoc.y);
  prediction = ptMax;

  if(maxVal < 3.0f)
    return false;

  center = cv::Point(itsMaxLoc.x, itsMaxLoc.y);

  setCenter(itsMaxObject, center);
  setCenter(itsObject, center);
  setCenter(itsSearchWindow, center);

  LINFO("BACKPROJECT");
  backproject = cv::Scalar(cv::GC_BGD);
  cv::rectangle(backproject, cv::Point(itsMaxObject.x, itsMaxObject.y), cv::Point(itsMaxObject.x+itsMaxObject.width, itsMaxObject.y+itsMaxObject.height), cv::Scalar(cv::GC_PR_BGD), -1);

  int cnt = itsFerns.backProject(itsFeatures, backproject, intersect( itsMaxObject, itsImgRect), itsMaxLoc, backProjectRadius, STEP_WIDTH, backProjectminProb);
  showSegmentation(rv, backproject, "BackProject",  img.getFrameNum(), evtNum);

  if(cnt > 0)
  {
    LINFO("SEGMENT");
    cv::Mat subframe(frame, intersect( itsSearchWindow, itsImgRect));
    cv::Mat subbackProject(backproject, intersect( itsSearchWindow, itsImgRect));

    cv::Mat fgmdl, bgmdl;
    grabCut(subframe, subbackProject, itsObject, fgmdl, bgmdl, GRABCUT_ROUNDS, cv::GC_INIT_WITH_MASK);
    showSegmentation(rv, backproject, "Segmentation", img.getFrameNum(), evtNum);

    LINFO("UPDATE");

  #ifdef SHIFT_TO_CENTER
    center = centerOfMass(backproject);
    setCenter(itsObject, center );
    setCenter(itsMaxObject, center);
    setCenter(itsSearchWindow, center);
  #endif
  }

  showResult(rv, frame, backproject, itsMaxObject, getBoundingBox(backproject), img.getFrameNum(), evtNum);

  if(cnt > 0)
  {
    itsUpdateRegion = intersect(itsMaxObject + cv::Size(40,40) - cv::Point(20,20), itsImgRect);
    run(itsUpdateRegion, center, backproject);
  }

  Point2D<int> ptCenter(center.x, center.y);
  prediction = ptCenter;
  makeBinarySegmentation(rv, backproject, binaryImg, img.getFrameNum(), evtNum);
  return true;
}

void HoughTracker::run(const cv::Rect& ROI, const cv::Point& center, cv::Mat& mask)
{
    int numPos = 0;
    int numNeg = 0;

    for(int x = ROI.x; x < ROI.x+ROI.width; x+=STEP_WIDTH)
        for(int y = ROI.y; y < ROI.y+ROI.height; y+=STEP_WIDTH)
        {
            if( (mask.at<unsigned char>( y, x ) == cv::GC_FGD) || (mask.at<unsigned char>( y, x ) == cv::GC_PR_FGD) )
            {
              itsFerns.update(itsFeatures, cv::Point(x, y), 1, center);
              numPos++;
           }
            else if(mask.at<unsigned char>( y, x ) == cv::GC_BGD)
           {
              itsFerns.update(itsFeatures, cv::Point(x, y), 0, center);
              numNeg++;
           }
        }
    itsFerns.forget(0.90);
    LINFO("UPDATED %d points (%d+, %d-)", numPos+numNeg,numPos,numNeg);
}

cv::Rect  HoughTracker::getBoundingBox(cv::Mat& backProject)
{
	cv::Point min(backProject.cols,backProject.rows);
	cv::Point max(0,0);

	for(int x = 0; x < backProject.cols; x++)
		for(int y = 0; y < backProject.rows; y++)
		{
			if( (backProject.at<unsigned char>( y, x ) == cv::GC_FGD) || (backProject.at<unsigned char>( y, x ) == cv::GC_PR_FGD) )
			{
				if(x < min.x)	min.x = x;
				if(y < min.y)	min.y = y;
				if(x > max.x)	max.x = x;
				if(y > max.y)	max.y = y;
			}
		}

	return cv::Rect(min.x, min.y, max.x - min.x, max.y - min.y);
}

cv::Point HoughTracker::centerOfMass(cv::Mat& mask)
{
	float c_x = 0.0f;
	float c_y = 0.0f;
	float c_n = 0.0f;

	for(int x = 0; x < mask.cols; x++)
		for(int y = 0; y < mask.rows; y++)
			if( (mask.at<unsigned char>( y, x ) == cv::GC_FGD) || (mask.at<unsigned char>( y, x ) == cv::GC_PR_FGD) )
			{
				c_x += x;
				c_y += y;
				c_n += 1.0f;
			}

	return cv::Point( static_cast<int>(round(c_x/c_n)), static_cast<int>(round(c_y/c_n)));
}

void HoughTracker::makeBinarySegmentation(nub::soft_ref<MbariResultViewer>&rv, \
											cv::Mat& backproject, \
											Image< byte >& output, \
											uint frameNum, \
											int evtNum)
{
    IplImage* display = cvCreateImage(cvGetSize(img2ipl(output)), 8, 3 );

	for(int x = 0; x < backproject.cols; x++)
		for(int y = 0; y < backproject.rows; y++)
		{
			switch( backproject.at<unsigned char>(y,x) )
			{
				case cv::GC_BGD:
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+0  ) = 0;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+1  ) = 0;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+2  ) = 0;
					break;
				case cv::GC_FGD:
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+0 ) = 255;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+1 ) = 255;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+2 ) = 255;
					break;
				case cv::GC_PR_BGD:
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+0  ) = 0;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+1  ) = 0;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+2  ) = 0;
					break;
				case cv::GC_ma:
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+0 ) = 255;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+1 ) = 255;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+2 ) = 255;
					break;
				default:
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+0  ) = 255;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+1  ) = 255;
					CV_IMAGE_ELEM( (display), unsigned char, y, x*3+2  ) = 255;
			}
		}

    LINFO("BINARY");
    output = luminance(ipl2rgb(display));
    rv->output(output, -1, "Binary", -1);
}

void HoughTracker::showSegmentation(nub::soft_ref<MbariResultViewer>&rv,\
 									cv::Mat& backProject, \
 									std::string title, \
 									uint frameNum, \
 									int evtNum)
{
    cv::Mat display(backProject.rows, backProject.cols, CV_8UC3, cv::Scalar(0,0,0));

	for(int x = 0; x < backProject.cols; x++)
		for(int y = 0; y < backProject.rows; y++)
		{
			switch( backProject.at<unsigned char>(y,x) )
			{
				case cv::GC_BGD:
				    display.at<unsigned char>(y,x*3+0) = 255;
				    display.at<unsigned char>(y,x*3+1) = 0;
				    display.at<unsigned char>(y,x*3+2) = 0;
					break;
				case cv::GC_FGD:
				    display.at<unsigned char>(y,x*3+0) = 0;
				    display.at<unsigned char>(y,x*3+1) = 0;
				    display.at<unsigned char>(y,x*3+2) = 255;
					break;
				case cv::GC_PR_BGD:
				    display.at<unsigned char>(y,x*3+0) = 255;
				    display.at<unsigned char>(y,x*3+1) = 128;
				    display.at<unsigned char>(y,x*3+2) = 128;
					break;
				case cv::GC_PR_FGD:
				    display.at<unsigned char>(y,x*3+0) = 128;
				    display.at<unsigned char>(y,x*3+1) = 128;
				    display.at<unsigned char>(y,x*3+2) = 255;
					break;
				default:
				    display.at<unsigned char>(y,x*3+0) = 0;
				    display.at<unsigned char>(y,x*3+1) = 128;
				    display.at<unsigned char>(y,x*3+2) = 0;
			}
		}

	IplImage image = (IplImage)display;
    Image< PixRGB<byte> > output = ipl2rgb(&image);
    rv->output(output, frameNum, title, evtNum);
}

void HoughTracker::showResult(nub::soft_ref<MbariResultViewer>& rv, \
							cv::Mat& frame, \
							cv::Mat& backProject, \
							const cv::Rect& ROI, \
							const cv::Rect& object, \
							uint frameNum, \
							int evtNum)
{
	cv::Mat display = frame.clone();
	cv::Mat overlay = backProject.clone();

	for(int x = 0; x < overlay.cols; x++)
		for(int y = 0; y < overlay.rows; y++)
		{
			if( (overlay.at<unsigned char>( y, x ) == cv::GC_FGD) || (overlay.at<unsigned char>( y, x ) == cv::GC_PR_FGD) )
				overlay.at<unsigned char>( y, x ) = 1;
			else
				overlay.at<unsigned char>( y, x ) = 0;
		}

	cv::Mat contour = overlay.clone();
	erode(contour, contour,cv:: Mat(), cv::Point(-1, -1), 1);
	dilate(contour, contour, cv::Mat(), cv::Point(-1, -1), 5);
	erode(contour, contour, cv::Mat(), cv::Point(-1, -1), 2);
	contour -= overlay;

	for(int x = ROI.x-10; x < ROI.x+ROI.width+10; x++)
		for(int y = ROI.y-10; y < ROI.y+ROI.height+10; y++)
		{
			if(overlay.at<unsigned char>( y, x ) > 0)
			{
				float val = (float)display.at<unsigned char>(y,x*3+2);
				display.at<unsigned char>(y,x*3+2) = static_cast<unsigned char>(val * 0.7f + 255.0f * 0.3f);
			}
			else if(contour.at<unsigned char>( y, x ) > 0)
			{
				display.at<unsigned char>(y,x*3+2) = 255;
				display.at<unsigned char>(y,x*3+1) = 0;
				display.at<unsigned char>(y,x*3+0) = 0;
			}
		}

	IplImage image = (IplImage)display;
    Image< PixRGB<byte> > output = ipl2rgb(&image);
    rv->output(output, frameNum, "Tracking", evtNum);
}

}