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

#ifndef SEGMENTATION_H_
#define SEGMENTATION_H_

#include "Image/ColorOps.H"
#include "Image/Image.H"
#include "Image/PixelsTypes.H"
#include "DetectionAndTracking/TrackingModes.H"

#include "Util/StringConversions.H"

// ######################################################################
//! Container class for running different segmentation algorithms
class Segmentation
{
public:

  Segmentation();
  virtual ~Segmentation();
  Image<byte> runBinaryAdaptive(const Image<byte>& mean, const Image<byte>& src);
  Image<byte> runBinaryAdaptive(const Image<byte> &bwImg);
  Image< PixRGB<byte> > runGraph(const Image < PixRGB<byte> > &image);
  Image<byte> mean_thresh(const Image<byte>& src,  const int size, const int con);
  Image<byte> median_thresh(const Image<byte>& src, const int size, const int con);
  Image<byte> meanMaxMin_thresh(const Image<byte>& src, const int size, const int con);
  
private:

  /* private functions related to the BinaryAdaptive algorithm */
  byte getThreshold(const Image<byte> &bwImg);
  Image<byte> runBinaryAdaptive(const Image<byte>& src, const byte& threshold, const float& pct);
};

#endif /*SEGMENTATION_H_*/
