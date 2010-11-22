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

/*!@file DetectionParameters.H classes useful for containing event detection
parameters*/

#ifndef DETECTIONPARAMETERS_H_DEFINED
#define DETECTIONPARAMETERS_H_DEFINED

#include "Component/ModelManager.H"
#include "Component/ModelComponent.H"
#include "Component/ModelOptionDef.H"

#include "DetectionAndTracking/TrackingModes.H"
#include "DetectionAndTracking/SaliencyTypes.H"
#include "DetectionAndTracking/SegmentTypes.H"

#include <cstdio>
#include <iostream>
// Every 5 frame saliency is run as default
// The larger this number, the longer processing time
#define DEFAULT_SALIENCY_FRAME_DIST 5
// Default size of cache used to compute running image average
#define DEFAULT_SIZE_AVG_CACHE 10
// Default minimum and max size in square pixels an event can be
// to be a valid event. These work well for NTSC size video and should
// be scaled for larger/smaller video accordingly
#define DEFAULT_MIN_EVENT_AREA 34
#define DEFAULT_MAX_EVENT_AREA 1000
// Default maximum number of frames an event can be before closing
// -1 = unlimited
// 10 minutes @ 30 fps = 18000
#define DEFAULT_MAX_EVENT_FRAMES  -1
// Default minimum number of frames an event can be before considered interesting
#define DEFAULT_MIN_EVENT_FRAMES 5
// Default values for the mask. This is the mask that
// masks out video overlays, equipment, etc.
// By default there is no mask
#define DEFAULT_MASK_X_POSITION 1
#define DEFAULT_MASK_Y_POSITION 1
#define DEFAULT_MASK_HEIGHT 1
#define DEFAULT_MASK_WIDTH 1
// Default segmentation algorithm to remove background from foreground event objects
// Should be one of the following per the MbariOpts.C OPT_MDPsegmentationAlgorithm options
// BinaryAdaptive|AdaptiveThreshold|BackgroundCanny|HomomorphicCanny|GraphCut
#define DEFAULT_SEGMENTATION_ALGORITHM SABinaryAdaptive
// Default segmentation algorithm input type
#define DEFAULT_SEGMENTATION_ALGORITHM_TYPE SAIMaxRGB
// Default saliency input image type which is the difference
// in the mean. This is only meaningful if there are cached
// images to find a mean. TODO: put check for this and
// alert user if wrong
#define DEFAULT_SALIENCY_INPUT_TYPE SIDiffMean
// Default structure element type to use for cleaning
// up segmented images
#define DEFAULT_SE_TYPE "benthic"
// Default tracking mode to track event objects
#define DEFAULT_TRACKING_MODE TMKalmanFilter
// Default maximum evolve time of the brain model in msecs
#define DEFAULT_MAX_EVOLVE_TIME  500
// Default maximum number of winner-take-tall points to
// find in the brain model
#define DEFAULT_MAX_WTA_POINTS 20
// Default way to handle boring WTA points from the bring
// false throws away any boring points. Once the first
// boring point is found, the brain stops for a given frame
// if this is set to false. If set to true, the brain continues to
// evolve until max points or evolve time is up.
#define DEFAULT_KEEP_WTA_BORING false
// Default way to handle saving "non-interesting" events
// or events that don't last longer than the
// minimum event frames (see DEFAULT_MIN_EVENT_FRAMES)
#define DEFAULT_SAVE_NON_INTERESTING false
// Default way to handle saving events in the original
// frame size specification
#define DEFAULT_SAVE_ORG_FRAME_SPEC false

// ######################################################################
//! Class that contains event detection parameters used to filter and track events 
class DetectionParameters {
public:

    //! Consructor
    DetectionParameters();

    //! @param itsMaxEvolveTime = maximum time to evolve the brain model for every frame
    int itsMaxEvolveTime;
    //! @param itsMaxWTAPoints = maximum winner-take-all points to collect ifrom the brain model every frame
    int itsMaxWTAPoints;
    //! @param  itsSaveNonInteresting = true if want to keep non-interesting events 
    bool itsSaveNonInteresting;
    //! @param  itsSaveOriginalFrameSpec = true if want to save events in original frame size
    bool itsSaveOriginalFrameSpec;;
    //! @param mode = tracking mode used to find event
    TrackingMode itsTrackingMode;
    //! @param itsMaxDist = maximum distance in pixels predicted centroid must be within
    int itsMaxDist;
    //! @param itsMaxEventFrames = maximum number of frames any event can be
    int itsMaxEventFrames;
    //! @param itsMinEventFrames = minimum number of frames an event must have before considered event, or deleted
    int itsMinEventFrames;
    //! @param itsMaxEventArea = maximum area an event must be
    int itsMaxEventArea;
    //! @param itsMinEventArea = minimum area an event must be
    int itsMinEventArea;
    // ! every @param frames that saliency is run
    int itsSaliencyFrameDist;
   //! @param itsMaskPath = path of the image which represent the mask (this one should be binair)
    std::string itsMaskPath;
     //! @param itsSizeAvgCache = size of running average cache
    uint itsSizeAvgCache;
  //! @param itsMaskXPosition = the x position of the reference point
    int itsMaskXPosition;
      //! @param itsMaskYPosition = the y position of the reference point
    int itsMaskYPosition;
    //! @param itsMaskWidth = mask width
    int itsMaskWidth;
   //! @param itsMaskHeight = mask height
    int itsMaskHeight;
   // @param itsSegmentALgorithm = segmentat algorithm to find foreground objects
    SegmentAlgorithmType itsSegmentAlgorithm;
   // @param itsSegmentALgorithmInputType = segment algorithm input image type
    SegmentAlgorithmInputImageType itsSegmentAlgorithmInputType;
    // @param itsSegmentSEType = structure element used for cleaning up segmented image
    std::string itsSegmentSEType;
     // @param itsSaliencyInputImageType = type of image to input into saliency computation
    SaliencyInputImageType itsSaliencyInputType;    
    //! @param itsKeepWTABoring = true if want to keep WTA boring points as candidates for detections
    bool itsKeepWTABoring;
    //! @param itsMaxCost = used for maximum cost function in tracking algorithm
    float itsMaxCost;
     //! write the DetectionParameters to the output stream os
    DetectionParameters & operator=(const DetectionParameters& p);
    //! write the DetectionParameters to the output stream os
    void writeToStream(std::ostream& os);
};

// ######################################################################
//! Singleton class that contains event detection parameters used throughout program
// there should be only once instance of this used

class DetectionParametersSingleton {
public:
    //! client access exclusively through this
    static DetectionParametersSingleton *instance();

    //! client can initialize parameters through this
    static void initialize(const DetectionParameters &d);

    ~DetectionParametersSingleton();

    DetectionParameters itsParameters;

protected:
    // default constructor
    DetectionParametersSingleton(const DetectionParameters &d);

private:
    static DetectionParametersSingleton *itsInstance;
};


// ######################################################################
//! ModelComponent to tie parameters into command line options with the saliency toolkit

class DetectionParametersModelComponent : public ModelComponent {
public:
    //! constructor
    DetectionParametersModelComponent(ModelManager &mgr);

    //! Overwrites DetectionParamaters with command line options
    void reset(DetectionParameters *p);

private:
    OModelParam<int> itsMaxWTAPoints;
    OModelParam<int> itsMaxEvolveTime;
    OModelParam<TrackingMode> itsTrackingMode;
    OModelParam<SegmentAlgorithmType> itsSegmentAlgorithm;
    OModelParam<SegmentAlgorithmInputImageType> itsSegmentAlgorithmInputType;
    OModelParam<std::string> itsSegmentSEType;
    OModelParam<SaliencyInputImageType> itsSaliencyInputType;
    OModelParam<std::string> itsMaskPath;
    OModelParam<int> itsMaskXPosition;
    OModelParam<int> itsMaskYPosition;
    OModelParam<int> itsMaskWidth;
    OModelParam<int> itsMaskHeight;
    OModelParam<int> itsSizeAvgCache;
    OModelParam<int> itsMinEventArea;
    OModelParam<int> itsMaxEventArea;
    OModelParam<int> itsMinEventFrames;
    OModelParam<int> itsMaxEventFrames;
    OModelParam<int> itsSaliencyFrameDist; 
    OModelParam<bool> itsKeepWTABoring;
    OModelParam<bool> itsSaveNonInteresting;
    OModelParam<bool> itsSaveOriginalFrameSpec;;
};

#endif

