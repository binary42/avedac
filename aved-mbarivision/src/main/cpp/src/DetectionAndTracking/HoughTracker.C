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
#include "DetectionAndTracking/MbariFunctions.H"
#include "Image/ImageSet.H"
#include <csignal>

#define STEP_WIDTH 1
#define SHIFT_TO_CENTER
#define SEARCH_WINDOW 20
#define GRABCUT_ROUNDS 5

#include <vector>
template <class T> class MbariImage;

namespace HoughTracker {

// ######################################################################
HoughTracker::HoughTracker()
{}

// ######################################################################
HoughTracker::HoughTracker(const MbariImage< PixRGB<byte> >& img, BitObject &bo, const float maxScale)
{
   	reset(img, bo, DEFAULT_FORGET_CONSTANT, maxScale);
}

// ######################################################################
HoughTracker::~HoughTracker()
{
	free();
}

// ######################################################################
void HoughTracker::free()
{
	itsFeatures.clear();
	itsFerns.clear();
}

// ######################################################################
void HoughTracker::reset(const MbariImage< PixRGB<byte> >& img, BitObject& bo,
						const float maxScale, const float forgetConstant)
{
    Rectangle region = bo.getBoundingBox();
    Point2D<int> center = bo.getCentroid();
	LINFO("Resetting HoughTracker region top %d left %d width %d height %d", \
    region.left(), region.top(), region.width(), region.height() );
	free();
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

	try
    {
    	int baseSize = 12;
    	itsObject = cv::Rect(region.left(), region.top(), region.width(), region.height());
		itsImgRect = cv::Rect(baseSize/2, baseSize/2, img.getDims().w()-baseSize, img.getDims().h()-baseSize);
		cv::Mat frame = img2ipl(img);
		itsFeatures.setImage(frame);

    	float opacity = 1.0F;
    	byte foreground(cv::GC_FGD);
	  	Image< byte > mask(img.getDims(), ZEROS); // initialize as background
	  	bo.drawShape(mask, foreground, opacity); // initialize shape as foreground
  		cv::Mat backProject = img2ipl(mask);

		//cv::Mat backProject(img.getDims().h(), img.getDims().w(), CV_8UC1, cv::Scalar(cv::GC_BGD));
		//cv::rectangle(backProject, cv::Point(itsObject.x-10, itsObject.y-10), cv::Point(itsObject.x+itsObject.width+10, itsObject.y+itsObject.height+10), cv::Scalar(cv::GC_PR_BGD), -1);
		///cv::rectangle(backProject, cv::Point(itsObject.x, itsObject.y), cv::Point(itsObject.x+itsObject.width, itsObject.y+itsObject.height), cv::Scalar(cv::GC_FGD), -1);
 		itsFerns.initialize(20, cv::Size(baseSize, baseSize), 8, itsFeatures.getNumChannels());
		itsMaxObject = intersect( itsImgRect, squarify(itsObject, maxScale));
		cv::Point objCenter(center.i,center.j);

		LINFO("Initial position: %d,%d %dx%d", itsObject.x,itsObject.y,itsObject.width,itsObject.height);
	 	cv::Rect updateRegion = intersect(itsMaxObject + cv::Size(HOUGH_EDGE_OFFSET,HOUGH_EDGE_OFFSET) - cv::Point(10,10), itsImgRect);
		run(updateRegion, objCenter, backProject, forgetConstant);

		itsSearchWindow = itsMaxObject + cv::Size(SEARCH_WINDOW,SEARCH_WINDOW) - cv::Point(SEARCH_WINDOW/2,SEARCH_WINDOW/2);
		LINFO(" Start tracking");
    }
    catch (...) {
    	LINFO("Exception occurred");
    }
}

// ######################################################################
bool HoughTracker::update(nub::soft_ref<MbariResultViewer>&rv, \
                            MbariImage< PixRGB<byte> >& img, \
                            const Image<byte> &occlusionImg, \
                            Point2D<int> &prediction, \
                            Rectangle &boundingBox, \
                            Image<byte>& binaryImg, \
                            const int evtNum, \
                            const float forgetConstant)
{
  float backProjectRadius = 0.5;
  float backProjectminProb = 0.5;
  double minVal, maxVal = 6.0f;
  cv::Point minLoc;
  cv::Mat frame = img2ipl(img);
  itsFeatures.setImage(frame);
  cv::Size imgSize = cv::Size(frame.cols, frame.rows);
  cv::Mat result(imgSize.height, imgSize.width, CV_32FC1, cv::Scalar(0.0));
  cv::Mat backProject(img.getDims().h(), img.getDims().w(), CV_8UC1, cv::Scalar(cv::GC_BGD));
  cv::Point center;
  DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;

  try
  {
	  LINFO("Evaluate");
	  itsFerns.evaluate(itsFeatures, intersect(itsSearchWindow, itsImgRect), result, STEP_WIDTH, 0.5f);
	  cv::Mat out = result;

	  normalize(out, out, 255, 0, cv::NORM_MINMAX);
	  minMaxLoc(result, &minVal, &maxVal, &minLoc, &itsMaxLoc);
	  LINFO("Locate: maximum is at (%d/%d: %f)", itsMaxLoc.x, itsMaxLoc.y, maxVal);

	  Point2D<int> ptMax(itsMaxLoc.x, itsMaxLoc.y);
	  prediction = ptMax;

	  if(maxVal < 3.0f) {
	  	LINFO("Max val too small: %f", maxVal);
		return false;
	  }

	  center = cv::Point(itsMaxLoc.x, itsMaxLoc.y);

	  setCenter(itsMaxObject, center);
	  setCenter(itsObject, center);
	  setCenter(itsSearchWindow, center);

	  LINFO("Backproject");
	  backProject = cv::Scalar(cv::GC_BGD);
	  cv::rectangle(backProject, cv::Point(itsMaxObject.x, itsMaxObject.y), cv::Point(itsMaxObject.x+itsMaxObject.width, itsMaxObject.y+itsMaxObject.height), cv::Scalar(cv::GC_PR_BGD), -1);

	  int cnt = itsFerns.backProject(itsFeatures, backProject, intersect( itsMaxObject, itsImgRect), itsMaxLoc, backProjectRadius, STEP_WIDTH, backProjectminProb);
	  showSegmentation(rv, backProject, "BackProject",  img.getFrameNum(), evtNum);

	  if(cnt > 0)
	  {
		LINFO("Segment");
		cv::Mat subframe(frame, intersect( itsSearchWindow, itsImgRect));
		cv::Mat subbackProject(backProject, intersect( itsSearchWindow, itsImgRect));

		cv::Mat fgmdl, bgmdl;
		grabCut(subframe, subbackProject, itsObject, fgmdl, bgmdl, GRABCUT_ROUNDS, cv::GC_INIT_WITH_MASK);
		backProject = maskOcclusion(occlusionImg,  backProject);
		showSegmentation(rv, backProject, "Segmentation", img.getFrameNum(), evtNum);

	  #ifdef SHIFT_TO_CENTER
		center = centerOfMass(backProject);
		setCenter(itsObject, center );
		setCenter(itsMaxObject, center);
		setCenter(itsSearchWindow, center);
	  #endif
	  }

	  //showResult(rv, frame, backProject, itsMaxObject, getBoundingBox(backProject), "Hough", img.getFrameNum(), evtNum);

	  if(cnt > 0)
	  {
		cv::Rect updateRegion = intersect(itsMaxObject + cv::Size(HOUGH_EDGE_OFFSET,HOUGH_EDGE_OFFSET) - cv::Point(10,10), itsImgRect);
		run(updateRegion, center, backProject, forgetConstant);
	  }

	  Point2D<int> ptCenter(center.x, center.y);
	  prediction = ptCenter;

	  cv::Rect bbox = getBoundingBox(backProject);
	  if (bbox.width >=0 && bbox.height >=0) {
		  const Point2D<int> topleft(bbox.x, bbox.y);
		  const Dims dims(bbox.width, bbox.height);
		  Rectangle r(topleft, dims);
		  boundingBox = r;
		  binaryImg = makeBinarySegmentation(rv, backProject, img.getFrameNum(), evtNum);
		  return true;
	  }
	  return
	  	false;
  }
  catch (...) {
  	LINFO("Exception occurred");
  	return false;
  }
}

void HoughTracker::run(const cv::Rect& ROI, const cv::Point& center, const cv::Mat& mask, const float forgetConstant)
{
    int numPos = 0;
    int numNeg = 0;

    //try {
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
    itsFerns.forget(forgetConstant);
    LINFO("Updated %d points (%d+, %d-)", numPos+numNeg,numPos,numNeg);
  //} catch(VoteTooLargeException){
  //  	LINFO("Voting exception");
  //}
}

cv::Rect  HoughTracker::getBoundingBox(const cv::Mat& backProject)
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

cv::Point HoughTracker::centerOfMass(const cv::Mat& mask)
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

Image< byte > HoughTracker::makeBinarySegmentation(nub::soft_ref<MbariResultViewer>&rv, \
											const cv::Mat& backProject, \
											const uint frameNum, \
											const int evtNum)
{
    DetectionParameters dp = DetectionParametersSingleton::instance()->itsParameters;
    IplImage* display = cvCreateImage(cvSize(backProject.cols,backProject.rows), 8, 3 );
 	Image< byte > output;

	for(int x = 0; x < backProject.cols; x++)
		for(int y = 0; y < backProject.rows; y++)
		{
			switch( backProject.at<unsigned char>(y,x) )
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
				case cv::GC_PR_FGD:
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

    LINFO("Binary");
    output = luminance(ipl2rgb(display));
    output = maskArea(output, &dp);
    //#ifdef DEBUG
    rv->output(output, frameNum, "Binary", -1);
    //#endif
    cvReleaseImage(&display);
    return output;
}


cv::Mat HoughTracker::maskOcclusion(const Image<byte> &occlusionImg,  const cv::Mat& backProject)
{
	cv::Mat backProjectO(backProject.rows, backProject.cols, CV_8UC1, cv::Scalar(cv::GC_BGD));
	if (occlusionImg.getWidth() == backProject.cols && occlusionImg.getHeight() == backProject.rows) {
		for (int x = 0; x < backProjectO.cols; x++)
			for (int y = 0; y < backProjectO.rows; y++)
				if (occlusionImg.getVal(x, y) == 0)
				    backProjectO.at<unsigned char>(y,x) = cv::GC_PR_BGD; //set masked occlusion as possible background pixel
			 	else
			 		backProjectO.at<unsigned char>(y,x) = backProject.at<unsigned char>(y,x);
	} else {
		LFATAL("invalid sized occlusion mask; size is %dx%d but should be same size as input frame %dx%d",
				occlusionImg.getWidth(), occlusionImg.getHeight(), backProject.cols, backProject.rows);
	}
	return backProjectO;
}

void HoughTracker::showSegmentation(nub::soft_ref<MbariResultViewer>&rv,\
 									const cv::Mat& backProject, \
 									const std::string title, \
 									const uint frameNum, \
 									const int evtNum)
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
    //#ifdef DEBUG
    rv->output(output, frameNum, title, evtNum);
    //#endif
}

void HoughTracker::showResult(nub::soft_ref<MbariResultViewer>&rv, \
                                const cv::Mat& frame, \
                                const cv::Mat& backProject, \
                                const cv::Rect& ROI, \
                                const cv::Rect& object, \
                                const std::string title, \
                                const uint frameNum, \
                                const int evtNum)
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
	cv::erode(contour, contour, cv::Mat(), cv::Point(-1, -1), 1);
	cv::dilate(contour, contour, cv::Mat(), cv::Point(-1, -1), 5);
	cv::erode(contour, contour, cv::Mat(), cv::Point(-1, -1), 2);
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
    rv->output(output, frameNum, title, evtNum);
	display.release();
	overlay.release();
}

}
