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

#include <string>
#include <Component/ModelParam.H>

#include "DetectionAndTracking/DetectionParameters.H"
#include "Util/MathFunctions.H"
#include "Data/MbariOpts.H"

#include <algorithm>
#include <cmath>

/*!@file DetectionParameters.C classes useful for containing event detection
parameters*/


// ######################################################################
/// DetectionParameters class 
// ######################################################################

DetectionParameters::DetectionParameters()
: itsMaxEvolveTime(DEFAULT_MAX_EVOLVE_TIME),
itsMaxWTAPoints(DEFAULT_MAX_WTA_POINTS),
itsSaveNonInteresting(DEFAULT_SAVE_NON_INTERESTING),
itsTrackingMode(DEFAULT_TRACKING_MODE),
itsMaxDist(40),
itsMaxFramesEvent(DEFAULT_MAX_EVENT_FRAMES),
itsMinFramesEvent(DEFAULT_MIN_EVENT_FRAMES),
itsMaxEventArea(DEFAULT_MAX_EVENT_AREA),
itsMinEventArea(DEFAULT_MIN_EVENT_AREA),
itsSaliencyFrameDist(DEFAULT_SALIENCY_FRAME_DIST),
itsMaskPath(""),
itsSizeAvgCache(DEFAULT_SIZE_AVG_CACHE),
itsMaskXPosition(DEFAULT_MASK_X_POSITION),
itsMaskYPosition(DEFAULT_MASK_Y_POSITION),
itsMaskWidth(DEFAULT_MASK_HEIGHT),
itsMaskHeight(DEFAULT_MASK_WIDTH),
itsSegmentAlgorithm(DEFAULT_SEGMENTATION_ALGORITHM),
itsSegmentAlgorithmInputType(DEFAULT_SEGMENTATION_ALGORITHM_TYPE),
itsSegmentSEType(DEFAULT_SE_TYPE),
itsSaliencyInputType(DEFAULT_SALIENCY_INPUT_TYPE),
itsKeepWTABoring(DEFAULT_KEEP_WTA_BORING){
    //initialize with some defaults
    float maxDist = itsMaxDist;
    float maxAreaDiff = pow((double) maxDist, 2) / (double) 4.0;
    itsMaxCost = pow((double) maxDist, 2) + pow((double) maxAreaDiff, 2);
}

void DetectionParameters::writeToStream(std::ostream& os) {
    // Only write the parameters that are set by model options
    // these are the options that a user can set
    os << "\tcachesize:" << itsSizeAvgCache;
    os << "\tminarea:" << itsMinEventArea << "\tmaxarea:" << itsMaxEventArea;
    os << "\ttrackingmode:" << trackingModeName(itsTrackingMode);
    os << "\tsegmentalgorithm:" << segmentAlgorithmType(itsSegmentAlgorithm);
    os << "\tsegmentalgorithminputimagetype:" << segmentAlgorithmInputImageType(itsSegmentAlgorithmInputType);
    os << "\tsaliencyinputimagetype:" << saliencyInputImageType(itsSaliencyInputType);
    os << "\tstructureelementtype:" << itsSegmentSEType;
    os << "\tminframes:" << itsMinFramesEvent;
    os << "\tmaxframes:" << itsMaxFramesEvent;
    os << "\tmaxdist:" << itsMaxDist;
    os << "\tsaliencyframedist:" << itsSaliencyFrameDist;
    os << "\tmaxcost:" << itsMaxCost;
    os << "\tmaxevolvetime(msecs):" << itsMaxEvolveTime;
    os << "\tmaxwtapoints:" << itsMaxWTAPoints;

    if (itsMaskPath.length() > 0) {
        os << "\tmaskpath:" << itsMaskPath;
        os << "\tmaskxposition:" << itsMaskXPosition;
        os << "\tmaskyposition:" << itsMaskYPosition;
        os << "\tmaskwidth:" << itsMaskWidth;
        os << "\tmaskheight:" << itsMaskHeight;
    }
    os << "\n";
}
// ######################################################################

DetectionParameters &DetectionParameters::operator=(const DetectionParameters& p) {
    this->itsMaxEvolveTime = p.itsMaxEvolveTime;
    this->itsMaxWTAPoints = p.itsMaxWTAPoints;
    this->itsMaxDist = p.itsMaxDist;
    this->itsMaxFramesEvent = p.itsMaxFramesEvent;
    this->itsMinFramesEvent = p.itsMinFramesEvent;
    this->itsMinEventArea = p.itsMinEventArea;
    this->itsMaxEventArea = p.itsMaxEventArea;
    this->itsTrackingMode = p.itsTrackingMode;
    this->itsSegmentAlgorithm = p.itsSegmentAlgorithm;
    this->itsSegmentAlgorithmInputType = p.itsSegmentAlgorithmInputType;
    this->itsSegmentSEType = p.itsSegmentSEType;
    this->itsSaliencyInputType = p.itsSaliencyInputType;
    this->itsSaliencyFrameDist = p.itsSaliencyFrameDist;
    this->itsKeepWTABoring = p.itsKeepWTABoring;
    this->itsMaskPath = p.itsMaskPath;
    this->itsMaskXPosition = p.itsMaskXPosition;
    this->itsMaskYPosition = p.itsMaskYPosition;
    this->itsMaskWidth = p.itsMaskWidth;
    this->itsMaskHeight = p.itsMaskHeight;
    this->itsSizeAvgCache = p.itsSizeAvgCache; 
    this->itsMaxCost = p.itsMaxCost;
    return *this;
}
// ######################################################################
// DetectionParameters Singleton class static member initialization
// ######################################################################
DetectionParametersSingleton* DetectionParametersSingleton::itsInstance = 0;

// ######################################################################
// ###### DetectionParametersSingleton class
// ######################################################################

DetectionParametersSingleton::~DetectionParametersSingleton() {
    if (itsInstance) {
        delete itsInstance;
    }
}
// ######################################################################

DetectionParametersSingleton::DetectionParametersSingleton(const DetectionParameters &d) {
    this->itsParameters = d;
}
// ######################################################################

void DetectionParametersSingleton::initialize(const DetectionParameters &p) {
    DetectionParametersSingleton *d = instance();
    // initialize paramters
    d->itsParameters = p;
}
// ######################################################################

DetectionParametersSingleton* DetectionParametersSingleton::instance() {
    //initialize with some defaults if not created already
    if (itsInstance == 0) {
        DetectionParameters p;
        p.itsMaxDist = 40; 
        float maxDist = p.itsMaxDist;
        float maxAreaDiff = pow((double) maxDist, 2) / (double) 4.0;
        p.itsMaxCost = pow((double) maxDist, 2) + pow((double) maxAreaDiff, 2);
        p.itsMinEventArea = DEFAULT_MIN_EVENT_AREA; //TODO: change these - should be a factor of frame size
        p.itsMaxEventArea = DEFAULT_MAX_EVENT_AREA;
        itsInstance = new DetectionParametersSingleton(p);
    }
    return itsInstance;
}
// ######################################################################
/// DetectionParametersModelComponent class 
// ######################################################################

DetectionParametersModelComponent::DetectionParametersModelComponent(ModelManager &mgr)
: ModelComponent(mgr, std::string("DetectionParameters"), std::string("DetectionParameters")),
itsMaxWTAPoints(&OPT_MDPmaxWTAPoints, this),
itsMaxEvolveTime(&OPT_MDPmaxEvolveTime, this),
itsTrackingMode(&OPT_MDPtrackingMode, this),
itsSegmentAlgorithm(&OPT_MDPsegmentAlgorithm, this),
itsSegmentAlgorithmInputType(&OPT_MDPsegmentAlgorithmInputImage, this),
itsSegmentSEType(&OPT_MDPsegmentSEType, this),
itsSaliencyInputType(&OPT_MDPsaliencyInputImage, this),
itsMaskPath(&OPT_MDPmaskPath, this),
itsMaskXPosition(&OPT_MDPmaskXPosition, this),
itsMaskYPosition(&OPT_MDPmaskYPosition, this),
itsMaskWidth(&OPT_MDPmaskWidth, this),
itsMaskHeight(&OPT_MDPmaskHeight, this),
itsSizeAvgCache(&OPT_MDPsizeAvgCache, this),
itsMinEventArea(&OPT_MDPminEventArea, this),
itsMaxEventArea(&OPT_MDPmaxEventArea, this),
itsSaliencyFrameDist(&OPT_MDPsaliencyFrameDist, this),
itsKeepWTABoring(&OPT_MDPkeepBoringWTAPoints, this) {
};
// ######################################################################

void DetectionParametersModelComponent::reset(DetectionParameters *p) {
    if (itsMaxEvolveTime.getVal() > 0)
        p->itsMaxEvolveTime = itsMaxEvolveTime.getVal();
    if (itsMaxWTAPoints.getVal() > 0)
        p->itsMaxWTAPoints = itsMaxWTAPoints.getVal();
    if (itsTrackingMode.getVal() >= TMKalmanFilter)
        p->itsTrackingMode = itsTrackingMode.getVal();
    if (itsSegmentAlgorithmInputType.getVal() > 0)
        p->itsSegmentAlgorithmInputType = itsSegmentAlgorithmInputType.getVal();
    if (itsSaliencyInputType.getVal() > 0)
        p->itsSaliencyInputType = itsSaliencyInputType.getVal();
    if (itsSegmentAlgorithm.getVal() > 0)
        p->itsSegmentAlgorithm = itsSegmentAlgorithm.getVal();
    if (itsSegmentSEType.getVal().length() > 0)
        p->itsSegmentSEType = itsSegmentSEType.getVal().data();
    if (itsMaskPath.getVal().length() > 0)
        p->itsMaskPath = itsMaskPath.getVal().data();
    if (itsMaskXPosition.getVal() >= 0)
        p->itsMaskXPosition = itsMaskXPosition.getVal();
    if (itsMaskYPosition.getVal() >= 0)
        p->itsMaskYPosition = itsMaskYPosition.getVal();
    if (itsMaskWidth.getVal() > 0)
        p->itsMaskWidth = itsMaskWidth.getVal();
    if (itsMaskHeight.getVal() > 0)
        p->itsMaskHeight = itsMaskHeight.getVal();
    if (itsSizeAvgCache.getVal() > 0)
        p->itsSizeAvgCache = itsSizeAvgCache.getVal();
    if (itsMinEventArea.getVal() > 0)
        p->itsMinEventArea = itsMinEventArea.getVal();
    else
        p->itsMinEventArea = DEFAULT_MIN_EVENT_AREA; 
    if (itsMaxEventArea.getVal() > 0)
        p->itsMaxEventArea = itsMaxEventArea.getVal();
    else
        p->itsMaxEventArea = DEFAULT_MAX_EVENT_AREA;
    if (itsSaliencyFrameDist.getVal() > 0)
        p->itsSaliencyFrameDist = itsSaliencyFrameDist.getVal();

    p->itsKeepWTABoring = itsKeepWTABoring.getVal();   
}
