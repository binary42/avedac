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

#include "Image/OpenCVUtil.H"
#include "Component/ModelManager.H"
#include "Component/ModelComponent.H"
#include "Component/ModelOptionDef.H"
#include "Image/Dims.H"

#include "DetectionAndTracking/TrackingModes.H"
#include "DetectionAndTracking/SaliencyTypes.H"
#include "DetectionAndTracking/SegmentTypes.H"
#include "DetectionAndTracking/ColorSpaceTypes.H"

#include <cstdio>
#include <iostream>
// Every 5 frame saliency is run as default
// The larger this number, the longer processing time
#define DEFAULT_SALIENCY_FRAME_DIST 5
// Default size of cache used to compute running image average
#define DEFAULT_SIZE_AVG_CACHE 10
// Default multiplier for determining the maximum event area
#define MAX_SIZE_FACTOR 700
// Default multiplier for determining the maximum tracking distance
#define MAX_DIST_RATIO 60
// Default maximum number of frames an event can be before closing
// -1 = unlimited
// 10 minutes @ 30 fps = 18000
#define DEFAULT_MAX_EVENT_FRAMES  -1
// Default minimum number of frames an event can be before considered interesting
#define DEFAULT_MIN_EVENT_FRAMES 3
// Default values for the mask. This is the mask that
// masks out video overlays, equipment, etc.
// By default there is no mask
#define DEFAULT_MASK_X_POSITION 1
#define DEFAULT_MASK_Y_POSITION 1
#define DEFAULT_MASK_HEIGHT 1
#define DEFAULT_MASK_WIDTH 1
// Default segmentation algorithm input type
#define DEFAULT_SEGMENT_ALGORITHM_INPUT_TYPE SAIDiffMean
// Default segmentation algorithm type
#define DEFAULT_SEGMENT_ALGORITHM_TYPE SAMeanAdaptiveThreshold
// Default segmentation algorithm offset to subtract from the mean or median
#define DEFAULT_SEGMENT_ALGORITHM_OFFSET 0
// Default graph based segment parameters
#define DEFAULT_SEGMENT_GRAPH_PARAMETERS "0.75,500,50"
// Default saliency input image type which is the difference
// in the mean. This is only meaningful if there are cached
// images to calculate a mean. 
#define DEFAULT_SALIENCY_INPUT_TYPE SIDiffMean
// Default structure element type to use for cleaning
// up segmented images
#define DEFAULT_SE_SIZE 2
#define MAX_SE_SIZE 5
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
// Default color space for computing saliency
#define DEFAULT_COLOR_SPACE SAColorRGB
// Default minimum frame standard deviation for processing. Anything below
// this will be skipped. Can be useful for removing frames with visual noise
// or filtering out monochrome frames
#define DEFAULT_MIN_STD_DEV 0.f
// Default event expiration frames. How long an event can exist
// without being tracked before expiring.
#define DEFAULT_EVENT_EXPIRATION_FRAMES 0

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
    bool itsSaveOriginalFrameSpec;
     //! @param itsEventExpirationFrames= How long to keep an event in memory before removing it if no bit objects
    // found to combine with the event. Useful for noisy video or reduced frame rate video where tracking problems occur
    int itsEventExpirationFrames;
    //! @param itsTrackingMode = tracking mode used to find event
    TrackingMode itsTrackingMode;
    //! @param itsColorSpaceType = color space used to determine saliency computation
    ColorSpaceType itsColorSpaceType;
    //! @param itsMinStdDev = minimum required standard deviation in frames
    float itsMinStdDev;
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
    // @param itsSegmentAlgorithmType = segment algorithm type
    SegmentAlgorithmType itsSegmentAlgorithmType;
    // @param itsSegmentAlgorithmInputType = segment algorithm input image type
    SegmentAlgorithmInputImageType itsSegmentAlgorithmInputType;
    // @param itsSegmentGraphParameters =  graph based segment algorithm parameters
    std::string itsSegmentGraphParameters;
    // @param size of the offset to subtract from the mean or median in the segment algorithm
    int itsSegmentAdaptiveOffset;
    //! @param itsCleanupStructureElementSIze = size of square structure element to erode/dilate clean-up
    int itsCleanupStructureElementSize;
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
    // @param foaRadius = the foa radius used for determining the default
    // min/max event area
    static void initialize(DetectionParameters &d, const Dims &dims, const int foaRadius);

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
    OModelParam<int> itsMaxEvolveTime;
    OModelParam<int> itsMaxWTAPoints;
    OModelParam<bool> itsSaveNonInteresting;
    OModelParam<bool> itsSaveOriginalFrameSpec;
    OModelParam<int> itsEventExpirationFrames;
    OModelParam<TrackingMode> itsTrackingMode;
    OModelParam<ColorSpaceType> itsColorSpaceType;
    OModelParam<float> itsMinStdDev;
    OModelParam<int> itsMaxEventFrames;
    OModelParam<int> itsMinEventFrames;
    OModelParam<int> itsMaxEventArea;
    OModelParam<int> itsMinEventArea;
    OModelParam<int> itsSaliencyFrameDist;
    OModelParam<std::string> itsMaskPath;
    OModelParam<int> itsSizeAvgCache;
    OModelParam<int> itsMaskXPosition;
    OModelParam<int> itsMaskYPosition;
    OModelParam<int> itsMaskWidth;
    OModelParam<int> itsMaskHeight;
    OModelParam<SegmentAlgorithmType> itsSegmentAlgorithmType;
    OModelParam<SegmentAlgorithmInputImageType> itsSegmentAlgorithmInputType;
    OModelParam<std::string> itsSegmentGraphParameters;
    OModelParam<int> itsSegmentAdaptiveOffset;
    OModelParam<int> itsCleanupStructureElementSize;
    OModelParam<SaliencyInputImageType> itsSaliencyInputType;
    OModelParam<bool> itsKeepWTABoring;
};

#endif

