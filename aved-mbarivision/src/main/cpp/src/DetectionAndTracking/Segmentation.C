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

#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/segment/image.h"
#include "DetectionAndTracking/segment/misc.h"
#include "DetectionAndTracking/segment/pnmfile.h"
#include "DetectionAndTracking/segment/segment-image.h"
#include "DetectionAndTracking/Segmentation.H"
#include "Image/MathOps.H"
#include "Raster/Raster.H"
#include "Raster/PngWriter.H"
#include <cstdio>
#include <cstdlib>

Segmentation::Segmentation() {
}

Segmentation::~Segmentation() {
}

Image<byte> Segmentation::runBinaryAdaptive(const Image<byte> &meanBgnd, const Image<byte>& src) {
    Image<byte> resultfinal(src.getDims(), ZEROS);
    Image<float> meanimg(src.getDims(), ZEROS);
    Image<byte>::const_iterator bptr = meanBgnd.begin();
    const int w = src.getWidth(), h = src.getHeight();
    float mymean, s, sum;
    int N, NC, col, inc;
    float pct = 0.75f;
    byte threshold ;

    N = w*h;
    NC = w;
    s = (float) (NC * 2);
    col = 0;
    inc = 1;
    sum = 127.F * s;

    Image<byte>::const_iterator sptr = src.begin();
    Image<byte>::iterator rfinalptr = resultfinal.beginw();
    Image<float>::iterator meanptr = meanimg.beginw();

    for (int i = 0; i < N; i++) {
        if (col >= NC) {
            col = NC - 1;
            inc = -1;
	    bptr += NC - 1;
            sptr += NC - 1;
            rfinalptr += NC - 1;
            meanptr += NC - 1;
        } else if (col < 0) {
            col = 0;
            inc = 1;
	    bptr += NC;
            sptr += NC;
            rfinalptr += NC;
            meanptr += NC;
        }
        sum = sum - sum / s + (float) * sptr;

        if (i > NC) {
            Image<float>::iterator ptr = meanptr;
            ptr -= NC;
            mymean = (sum + *ptr) / s;
        } else mymean = sum / s;

        *meanptr = mymean;
        int p= (int) (mymean * (100.0F - pct) / 100.0F);
	threshold = *bptr;

        if (i > NC && *sptr > p && abs(*sptr - threshold) > 3)
            *rfinalptr = 255; //white
        else
            *rfinalptr = 0; //black

        rfinalptr += inc;
        sptr += inc;
        col += inc;
	bptr += inc;
        meanptr += inc;
    }
    return resultfinal;
}

// ######################################################################

Image<byte> Segmentation::runBinaryAdaptive(const Image<byte> &bwImg) {
    byte bgthreshold = getThreshold(bwImg);
    return (runBinaryAdaptive(bwImg, bgthreshold, (const float) 0.75f));
}

// ######################################################################

Image< PixRGB<byte> > Segmentation::runGraph(const Image < PixRGB<byte> >&input) {
    //printf("processing\n");
  int num_ccs;
  float sigma = 0.75;
  int k = 500;
  int min_size = 50;

    image<rgb> *im = new image<rgb > (input.getWidth(), input.getHeight());

    for (int x = 0; x < input.getWidth(); x++)
        for (int y = 0; y < input.getHeight(); y++) {
            rgb val;
            PixRGB<byte> val2 = input.getVal(x, y);
            val.r = val2.red();
            val.g = val2.green();
            val.b = val2.blue();
            imRef(im, x, y) = val;
        }

    // run segmentation
    image <rgb> *seg = segment_image(im, sigma, k, min_size, &num_ccs);

    // initialize the output image with the segmented results
    Image < PixRGB<byte> > output = input;
    for (int x = 0; x < input.getWidth(); x++)
        for (int y = 0; y < input.getHeight(); y++) {
            rgb val = imRef(seg, x, y);
            PixRGB<byte> val2((byte) val.r, (byte) val.g, (byte) val.b);
            output.setVal(x, y, val2);
        }

    delete im;
    delete seg;
    //printf("got %d components\n", num_ccs);
    //printf("done! uff...thats hard work.\n");
    return output;
}
// ######################################################################
// ###### Private Functions related to the BinaryAdaptive algorithm #####
// ######################################################################

// ######################################################################

byte Segmentation::getThreshold(const Image<byte> &bwImg) {
    float bwmean = mean(bwImg);
    float bwstddev = stdev(bwImg);
    byte bgthreshold = (byte) (bwmean +  bwstddev);
    return bgthreshold;
}

// ######################################################################

Image<byte> Segmentation::runBinaryAdaptive(const Image<byte>& src, const byte& threshold, const float& pct) {
    Image<byte> resultfinal(src.getDims(), ZEROS);
    Image<float> meanimg(src.getDims(), ZEROS);
    const int w = src.getWidth(), h = src.getHeight();
    float mymean, s, sum;
    int N, NC, col, inc;

    N = w*h;
    NC = w;
    s = (float) (NC * 2);
    col = 0;
    inc = 1;
    sum = 127.F * s;

    Image<byte>::const_iterator sptr = src.begin();
    Image<byte>::iterator rfinalptr = resultfinal.beginw();
    Image<float>::iterator meanptr = meanimg.beginw();

    for (int i = 0; i < N; i++) {
        if (col >= NC) {
            col = NC - 1;
            inc = -1;
            sptr += NC - 1;
            rfinalptr += NC - 1;
            meanptr += NC - 1;
        } else if (col < 0) {
            col = 0;
            inc = 1;
            sptr += NC;
            rfinalptr += NC;
            meanptr += NC;
        }
        sum = sum - sum / s + (float) * sptr;

        if (i > NC) {
            Image<float>::iterator ptr = meanptr;
            ptr -= NC;
            mymean = (sum + *ptr) / s;
        } else mymean = sum / s;

        *meanptr = mymean;
        int p= (int) (mymean * (100.0F - pct) / 100.0F);

        if (i > NC && *sptr > p )//&& *sptr > threshold)
            *rfinalptr = 255; //white
        else
            *rfinalptr = 0; //black

        rfinalptr += inc;
        sptr += inc;
        col += inc;
        meanptr += inc;
    }
    return resultfinal;
}
 

/**
 *AdapThresh is an algorithm to apply adaptive thresholding to an image.
 *@author Timothy Sharman
 */

  /**
   *Applies the adaptive thresholding operator to the specified image array
   *using the mean function to find the threshold value
   *
   *@param src pixel array representing image to be thresholded
   *@param size the size of the neigbourhood used in finding the threshold
   *@param con the constant value subtracted from the mean
   *@return a thresholded pixel array of the input image array
   */

  Image<byte> Segmentation::mean_thresh(const Image<byte>& src,  const int size, const int con){
    Image<byte> resultfinal(src.getDims(), ZEROS);
    const int i_w = src.getWidth(), i_h = src.getHeight();
    int mean = 0;
    int count = 0;
    int a, b;

    //Now find the sum of values in the size X size neigbourhood
    for(int j = 0; j < i_h; j++){
      for(int i = 0; i < i_w; i++){
	mean = 0;
	count = 0;
        
	//Check the local neighbourhood
	for(int k = 0; k < size; k++){
	  for(int l = 0; l < size; l++){
	      a = i - ((int)(size/2)+k);
	      b = j - ((int)(size/2)+l);
	      if (a >=0 && b >=0) {
	         mean = mean + src.getVal(a,b);
	         count++;
		}
	  }
	}
	//Find the mean value
	if (count > 0) {
	  mean = (int)(mean /count) - con;
 	}
 
	//Threshold below the mean
	if(src.getVal(i,j) > mean){
	  resultfinal.setVal(i,j,0); 
	}
	else {
	  resultfinal.setVal(i,j,255);
	}
      }
    }
    return resultfinal;
  }

  /**
   *Applies the adaptive thresholding operator to the specified image array
   *using the median function to find the threshold value
   *
   *@param src pixel array representing image to be thresholded
   *@param width width of the image in pixels
   *@param height height of the image in pixels
   *@param size the size of the neigbourhood used in finding the threshold
   *@param con the constant value subtracted from the median
   *@return a thresholded pixel array of the input image array
   */ 
  Image<byte> Segmentation::median_thresh(const Image<byte>& src, const int size, const int con){
    Image<byte> resultfinal(src.getDims(), ZEROS);
    const int i_w = src.getWidth(), i_h = src.getHeight();

    int median = 0;  
    std::vector<int> values(size*size);
    int count = 0;
    int a,b;

    //Now find the values in the size X size neigbourhood
    for(int j = 0; j < i_h; j++){
      for(int i = 0; i < i_w; i++){
	median = 0;
	count = 0;
        
	//Check the local neighbourhood
	for(int k = 0; k < size; k++){
	  for(int l = 0; l < size; l++){
	      a = i - ((int)(size/2)+k);
	      b = j - ((int)(size/2)+l);
	      if (a >=0 && b >=0) {
	        values[count] = src.getVal(a,b);
	        count++;
	      }
	  }
	}
	//Find the median value

	//First Sort the array
        std::sort(values.begin(), values.end());

	//Then select the median
	count = count / 2;
	median = values[count] - con;
   
	//Threshold below the mean
	if(src.getVal(i,j) >= median){
	  resultfinal.setVal(i,j,0);
	}
	else {
	  resultfinal.setVal(i,j,255); 
	}
      }
    }
    return resultfinal;
  }

  /**
   *Applies the adaptive thresholding operator to the specified image array
   *using the mean of max & min function to find the threshold value
   *
   *@param src pixel array representing image to be thresholded
   *@param size the size of the neigbourhood used in finding the threshold
   *@param con the constant value subtracted from the mean
   *@return a thresholded pixel array of the input image array
   */

  Image<byte> Segmentation::meanMaxMin_thresh(const Image<byte>& src, const int size, const int con){
    Image<byte> resultfinal(src.getDims(), ZEROS);
    const int i_w = src.getWidth(), i_h = src.getHeight();

    int mean = 0;
    int max = 0, min = 0;
    
    int a,b;
    int tmp;

    //Now find the max and min of values in the size X size neigbourhood
    for(int j = 0; j < i_h; j++){
      for(int i = 0; i < i_w; i++){
	mean = 0;
	max = src.getVal(i,j);
	min = src.getVal(i,j);
	//Check the local neighbourhood
	for(int k = 0; k < size; k++){
	  for(int l = 0; l < size; l++){
	      a = i - ((int)(size/2)+k);
	      b = j - ((int)(size/2)+l);
	      if (a >=0 && b >=0) {
	          tmp = src.getVal(a,b);
	          if(tmp > max){
		    max = tmp;
	          }
	         if(tmp < min){
		    min = tmp;
	          }
		}
	  }
	}
	//Find the mean value

	tmp = max + min;
	tmp = tmp / 2;
	mean = tmp - con;

	//Threshold below the mean
	if(src.getVal(i,j) >= mean){
	  resultfinal.setVal(i,j,0);
	}
	else {
	  resultfinal.setVal(i,j,255);
	}
      }
    }
    return resultfinal;
  }
