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
#ifndef SEGMENTTYPES_H_DEFINED
#define	SEGMENTTYPES_H_DEFINED

#include <string>

  // ! Segment algorithm used for extracting the foreground objects
enum SegmentAlgorithmType {
  SAMeanAdaptiveThreshold = 0,
  SAMedianAdaptiveThreshold = 1,
  SAMeanMinMaxAdaptiveThreshold = 2
  // if you add a new type here, also update the names in the function below!
};
//! number of algorithm type
#define NSEGMENT_ALGORITHMS 3

//! Returns name of segment algorithm
inline const char* segmentAlgorithmType(const SegmentAlgorithmType p)
{
  static const char n[NSEGMENT_ALGORITHMS][25] = {
    "MeanAdaptive", "MedianAdaptive", "MeanMinMaxAdaptive"};
  return n[int(p)];
}

//! segmentAlgorithmType overload */
void convertToString(const SegmentAlgorithmType val,
                     std::string& str);

//! segmentAlgorithmInputImageType overload */
void convertFromString(const std::string& str, SegmentAlgorithmType& val);

enum SegmentAlgorithmInputImageType {
  SAIMaxRGB = 0,
  SAILuminance = 1
  // if you add a new type here, also update the names in the function below!
};

//! number of algorithm input images types
#define NSEGMENT_ALGORITHM_INPUT_IMAGE_TYPES 2

//! Returns name of segment algorithm
inline const char* segmentAlgorithmInputImageType(const SegmentAlgorithmInputImageType p)
{
  static const char n[NSEGMENT_ALGORITHM_INPUT_IMAGE_TYPES][20] = {
    "MaxRGB", "Luminance"};
  return n[int(p)];
};

//! segmentAlgorithmInputImageType overload */
void convertToString(
                    const SegmentAlgorithmInputImageType val,
                     std::string& str);

//! segmentAlgorithmInputImageType overload */
void convertFromString(const std::string& str, SegmentAlgorithmInputImageType& val);

#endif	/* SEGMENTTYPES_H_DEFINED */
