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
 * This code requires the The iLab Neuromorphic Vision C++ Toolkit developedF
 * by the University of Southern California (USC) and the iLab at USC. 
 * See http://iLab.usc.edu for information about this project. 
 *  
 * This work would not be possible without the generous support of the 
 * David and Lucile Packard Foundation
 */ 
#include "Data/MbariOpts.H"

#include "Component/ModelOptionDef.H"
#include "Image/ArrayData.H"
#include "Component/OptionManager.H" 

#include "DetectionAndTracking/TrackingModes.H"
#include "DetectionAndTracking/SaliencyTypes.H"
#include "DetectionAndTracking/SegmentTypes.H"
#include "DetectionAndTracking/ColorSpaceTypes.H"
#include "DetectionAndTracking/Segmentation.H"
#include "Image/BitObjectDrawModes.H"

// Format here is:
//
// { MODOPT_TYPE, "name", &MOC_CATEG, OPTEXP_CORE,
//   "description of what option does",
//   "long option name", 'short option name', "valid values", "default value" }
//

// alternatively, for MODOPT_ALIAS option types, format is:
//
// { MODOPT_ALIAS, "", &MOC_ALIAS, OPTEXP_CORE,
//   "description of what alias does",
//   "long option name", 'short option name', "", "list of options" }
//

// NOTE: do not change the default value of any existing option unless
// you really know what you are doing!  Many components will determine
// their default behavior from that default value, so you may break
// lots of executables if you change it.

const ModelOptionCateg MOC_MBARI = {
  MOC_SORTPRI_4, "MBARI Related Options" };

const ModelOptionType MODOPT_ARG_INT = {
  MOK_ARG, &(typeid(int))
};
const ModelOptionType MODOPT_ARG_FLOAT = {
  MOK_ARG, &(typeid(float))
};
void REQUEST_OPTIONALIAS_MBARI(OptionManager& m)
{
  m.requestOptionAlias(&OPT_MDPMosaicBenthicStills);
  m.requestOptionAlias(&OPT_MDPBenthicVideo);
  m.requestOptionAlias(&OPT_MDPEyeInTheSeaVideo);
  m.requestOptionAlias(&OPT_MDPMidwaterVideo);
  m.requestOptionAlias(&OPT_MDPMosaicStills);
  m.requestOptionAlias(&OPT_MDPTimeLapseStills);
  m.requestOptionAlias(&OPT_MDPTimeLapseRover);
}

// #################### Mbari Alias options:
const ModelOptionDef OPT_MDPMosaicBenthicStills =
  { MODOPT_ALIAS, "ALIASMosaicBenthicStills", &MOC_MBARI, OPTEXP_MRV,
    "Implements good choice of options to experiment with "
    "processing still images from a still or moving camera traversing the sea bottom",
    "mbari-mosaic-benthic-stills", '\0',"",
    "--mbari-saliency-dist=1 --mbari-tracking-mode=None "
    "--mbari-keep-boring-WTA-points=yes "
    "--mbari-save-non-interesting-events=yes --mbari-segment-graph-parameters=0.75,250,50 "
    "--mbari-segment-algorithm-input-image=Luminance --mbari-color-space=RGB "
    "--mbari-saliency-input-image=Raw --levelspec=0-2,1-4,3 "
    "--mbari-max-WTA-points=15 --mbari-max-evolve-msec=15000 --mbari-color-space=RGB "
    "--vc-type=C:200I --use-random=true  --mbari-segment-algorithm=GraphCutOnly " 
    "--shape-estim-mode=SaliencyMap --foa-radius=60 --rescale-input=1920x1080 "
    "--mbari-cache-size=1 --use-older-version=false "
    "--ior-type=Disc  --maxnorm-type=Maxnorm"  };
const ModelOptionDef OPT_MDPEyeInTheSeaVideo = { MODOPT_ALIAS, "ALIASEyeInTheSeaVideo", &MOC_MBARI, OPTEXP_MRV,
    "Options used for processing Eye-in-the-Sea Video from the  "
    "Ocean Research and Conservation Association (ORCA)",
    "mbari-eits-video", '\0',"", 
    "--mbari-tracking-mode=KalmanFilter "
    "--mbari-keep-boring-WTA-points=yes "
    "--mbari-save-non-interesting-events=yes --mbari-save-original-frame-spec "
    "--mbari-segment-algorithm-input-image=Luminance --mbari-color-space=RGB "
    "--mbari-saliency-input-image=Raw --mbari-cache-size=2 "
    "--mbari-max-WTA-points=15 --mbari-max-evolve-msec=1000 "
    "--vc-type=OIC --use-random=true  --maxnorm-type=Maxnorm "
    "--oricomp-type=Steerable --levelspec=1-3,2-5,3 "
    "--mbari-cache-size=2 --use-older-version=false "
    "--shape-estim-mode=ConspicuityMap --ior-type=ShapeEst "
    "--mbari-max-event-area=30000 --mbari-min-std-dev=10.0 "
    "--mbari-segment-algorithm=GraphCutOnly "
    "--mbari-event-expiration-frames=3 --rescale-input=320x240 "
    "--mbari-segment-graph-parameters=0.5,1500,500"  };
const ModelOptionDef OPT_MDPBenthicVideo =
  { MODOPT_ALIAS, "ALIASBenthicVideo", &MOC_MBARI, OPTEXP_MRV,
    "Implements good choice of options to experiment with "
    "processing video from a moving camera traversing the sea bottom",
    "mbari-benthic-video", '\0',"",
    "--mbari-max-WTA-points=10 --mbari-segment-adaptive-offset=10 "
    "--mbari-tracking-mode=NearestNeighbor --mbari-max-event-area=30000 "
    "--mbari-saliency-input-image=DiffMean --mbari-segment-algorithm-input-image=Luminance "
    "--vc-type=OI --mbari-color-space=Gray --use-random=true  --mbari-se-size=4 "
    "--ori-interaction=None --oricomp-type=Steerable --boring-sm-mv=1.0 "
    "--mbari-cache-size=120 --use-older-version=false "
    "--shape-estim-mode=ConspicuityMap --ior-type=ShapeEst --maxnorm-type=Maxnorm"  };

const ModelOptionDef OPT_MDPMidwaterVideo =
  { MODOPT_ALIAS, "ALIASMidwaterVideo", &MOC_MBARI, OPTEXP_MRV,
   "Implements good choice of options to experiment with "
    "processing video from a moving camera traversing the midwater sea column",
    "mbari-midwater-video", '\0',"",
    "--mbari-saliency-dist=3 --mbari-max-WTA-points=10 --mbari-segment-graph-parameters=0.75,100,50"
    "--mbari-tracking-mode=KalmanFilter  --mbari-segment-algorithm=MeanAdaptive --mbari-segment-adaptive-offset=7 "
    "--mbari-saliency-input-image=DiffMean --mbari-segment-algorithm-input-image=Luminance "
    "--vc-type=I:5OC --mbari-color-space=RGB --use-random=true "
    "--ori-interaction=None --oricomp-type=Steerable --boring-sm-mv=1.0 "
    "--mbari-cache-size=60 --use-older-version=false --levelspec=1-3,2-5,3 "
    "--shape-estim-mode=ConspicuityMap --ior-type=ShapeEst --maxnorm-type=Maxnorm "};

const ModelOptionDef OPT_MDPMosaicStills =
  { MODOPT_ALIAS, "ALIASMosaicStills", &MOC_MBARI, OPTEXP_MRV,
    "Implements good choice of options to experiment with "
    "still frames collected from a moving camera in mosaic form",
    "mbari-mosaic-stills", '\0',"",
    "--mbari-saliency-dist=1 --mbari-tracking-mode=None "
    "--mbari-keep-boring-WTA-points=yes "
    "--boring-sm-mv=0.25 "
    "--mbari-save-non-interesting-events=yes "
    "--mbari-segment-algorithm-input-image=DiffMean --mbari-color-space=RGB"
    "--vc-type=Variance --use-random=true "
    "--mbari-saliency-input-image=Raw --mbari-cache-size=2 "
    "--mbari-max-WTA-points=25 --mbari-max-evolve-msec=15000" };

const ModelOptionDef OPT_MDPTimeLapseStills =
  { MODOPT_ALIAS, "ALIASTimeLapseStills", &MOC_MBARI, OPTEXP_MRV,
    "Implements good choice of options to experiment with "
    "still frames collected from a stationary time-lapse camera",
    "mbari-timelapse-stills", '\0',"",
    "--mbari-saliency-dist=1 --mbari-tracking-mode=NearestNeighbor "
    "--mbari-keep-boring-WTA-points=yes "
    "--mbari-save-non-interesting-events=yes "
    "--mbari-segment-algorithm-input-image=DiffMean --mbari-color-space=RGB"
    "--mbari-saliency-input-image=Raw --mbari-cache-size=10 "
    "--qtime-decay=1.0 "
    "--vc-type=Variance  --use-random=true "
    "--mbari-max-WTA-points=30 --mbari-max-evolve-msec=15000 "
    "--use-older-version=false "  };

const ModelOptionDef OPT_MDPTimeLapseRover =
  { MODOPT_ALIAS, "ALIASTimeLapseRover", &MOC_MBARI, OPTEXP_MRV,
    "Implements good choice of options to experiment with "
    "time-lapse still frames collected from a benthic moving camera ",
    "mbari-timelapse-rover-stills", '\0', "",
    "--mbari-saliency-dist=1  --mbari-tracking-mode=None "
    "--mbari-keep-boring-WTA-points=yes "
    "--qtime-decay=1.0 "
    "--mbari-save-non-interesting-events=yes "
    "--mbari-segment-algorithm-input-image=DiffMean --mbari-color-space=RGB"
    "--mbari-saliency-input-image=Raw "
    "--vc-type=O:5IC --use-random=true  "
    "--mbari-max-WTA-points=15 --mbari-max-evolve-msec=15000" };

// #################### MbariResultViewer options:
// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVsaveEvents =
  { MODOPT_ARG_STRING, "MRVsaveEvents", &MOC_MBARI, OPTEXP_MRV,
    "Save the event structure to a text file",
    "mbari-save-events", '\0', "fileName", "" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVloadEvents =
  { MODOPT_ARG_STRING, "MRVloadEvents", &MOC_MBARI, OPTEXP_MRV,
    "Load the event structure from a text file "
    "instead of computing it from the frames",
    "mbari-load-events", '\0', "fileName", "" };
    
// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVsaveProperties =
  { MODOPT_ARG_STRING, "MRVsaveProperties", &MOC_MBARI, OPTEXP_MRV,
    "Save the event property vector to a text file",
    "mbari-save-properties", '\0', "fileName", "" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVloadProperties =
  { MODOPT_ARG_STRING, "MRVloadProperties", &MOC_MBARI, OPTEXP_MRV,
    "Load the event property vector from a text file",
    "mbari-load-properties", '\0', "fileName", "" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVsavePositions =
  { MODOPT_ARG_STRING, "MRVsavePositions", &MOC_MBARI, OPTEXP_MRV,
    "Save the positions of events to a text file",
    "mbari-save-positions", '\0', "fileName", "" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVmarkInteresting =
  { MODOPT_ARG(BitObjectDrawMode), "MRVmarkInteresting", &MOC_MBARI, OPTEXP_MRV,
    "Way to mark interesting events in output frames of MBARI programs",
    "mbari-mark-interesting", '\0', "<None|Shape|Outline|BoundingBox>",
    "BoundingBox" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVopacity =
  { MODOPT_ARG(float), "MRVopacity", &MOC_MBARI, OPTEXP_MRV,
    "Opacity of shape or outline markings of events",
    "mbari-opacity", '\0', "<0.0 ... 1.0>", "1.0" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVmarkCandidate =
  { MODOPT_FLAG, "MRVmarkCandidate", &MOC_MBARI, OPTEXP_MRV,
    "Mark candidates for interesting events in output frames of MBARI programs",
    "mbari-mark-candidate", '\0', "", "true" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVmarkPrediction =
  { MODOPT_FLAG, "MRVmarkPrediction", &MOC_MBARI, OPTEXP_MRV,
    "Mark the Kalman Filter's prediction for the location of an object "
    "in output frames of MBARI programs",
    "mbari-mark-prediction", '\0', "", "false" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVmarkFOE =
  { MODOPT_FLAG, "MRVmarkFOE", &MOC_MBARI, OPTEXP_MRV,
    "Mark the focus of expansion in the output frames of MBARI programs",
    "mbari-mark-foe", '\0', "", "false" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVsaveResults =
  { MODOPT_FLAG, "MRVsaveResults", &MOC_MBARI, OPTEXP_MRV,
    "Save intermediate results in MBARI programs to disc",
    "mbari-save-results", '\0', "", "false" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVdisplayResults =
  { MODOPT_FLAG, "MRVdisplayResults", &MOC_MBARI, OPTEXP_MRV,
    "Display intermediate results in MBARI programs",
    "mbari-display-results", '\0', "", "false" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVsaveOutput =
  { MODOPT_FLAG, "MRVsaveOutput", &MOC_MBARI, OPTEXP_MRV,
    "Save output frames in MBARI programs",
    "mbari-save-output", '\0', "", "false" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVdisplayOutput =
  { MODOPT_FLAG, "MRVdisplayOutput", &MOC_MBARI, OPTEXP_MRV,
    "Display output frames in MBARI programs",
    "mbari-display-output", '\0', "", "false" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVshowEventLabels =
  { MODOPT_FLAG, "MRVshowEventLabels", &MOC_MBARI, OPTEXP_MRV,
    "Write event labels into the output frames",
    "mbari-label-events", '\0', "", "true" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVrescaleDisplay =
  { MODOPT_ARG(Dims), "MRVrescaleDisplay", &MOC_MBARI, OPTEXP_MRV,
    "Rescale displays to <width>x<height>, or 0x0 for no rescaling",
    "mbari-rescale-display", '\0', "<width>x<height>", "0x0" };

// Used by: MbariResultViewer
const ModelOptionDef OPT_MRVsaveEventNums =
  { MODOPT_ARG_STRING, "MRVsaveEventNums", &MOC_MBARI, OPTEXP_MRV,
    "Save cropped, event-centered images of specific events, or all events. Will save according to bounding box.",
    "mbari-save-event-num", '\0', "ev1,ev1,...,evN; or: all", "" };

const ModelOptionDef OPT_MRVsaveSummaryEventsName =
  { MODOPT_ARG_STRING, "MRVsummaryEvents", &MOC_MBARI, OPTEXP_MRV,
    "Save a human readable summary of all the events to a text file",
    "mbari-save-event-summary", '\0', "fileName", "" };
    
const ModelOptionDef OPT_MRVsaveXMLEventSet =
  { MODOPT_ARG_STRING, "MRVsaveXMLEventSet", &MOC_MBARI, OPTEXP_MRV,
    "Save a XML output per all events",
    "mbari-save-events-xml", '\0', "fileName", "" }; 

const ModelOptionDef OPT_MRVmetadataSource =
  { MODOPT_ARG_STRING, "MRVmetadataSource", &MOC_MBARI, OPTEXP_MRV,
    "Add video input source information to XML output",
    "mbari-source-metadata", '\0', "fileName", "" };

// #################### DetectionParametersModelComponent options:
const ModelOptionDef OPT_MDPtrackingMode =
  { MODOPT_ARG(TrackingMode), "MDPtrackingMode", &MOC_MBARI, OPTEXP_MRV,
    "Way to mark interesting events in output of MBARI programs",
    "mbari-tracking-mode", '\0', "<KalmanFilter|NearestNeighbor|None>",
    "KalmanFilter" };
const ModelOptionDef OPT_MDPsegmentAlgorithmType =
  { MODOPT_ARG(SegmentAlgorithmType), "MDPsegmentAlgorithm", &MOC_MBARI, OPTEXP_MRV,
    "Segment algorithm to find foreground objects",
    "mbari-segment-algorithm", '\0', "<MeanAdaptive|MedianAdaptive|MeanMinMaxAdapative>", 
    "MedianAdaptive" };
const ModelOptionDef OPT_MDPsegmentAdaptiveOffset =
  { MODOPT_ARG_INT, "MDPsegmentAdaptiveOffset", &MOC_MBARI, OPTEXP_MRV,
    "Size of the offset to subtract from the mean or median in the segment algorithm",
    "mbari-segment-adaptive-offset", '\0', "0-50",
    "5" };
const ModelOptionDef OPT_MDPsegmentGraphParameters =
  { MODOPT_ARG_STRING, "MDPsegmentGraphParameters", &MOC_MBARI, OPTEXP_MRV,
    "Graph segment parameters, in the order sigma, k, minsize. Generally,the defaults work.\
     Dont mess with this unless you need to.  See algorithm details in Segmentation.C.",
    "mbari-segment-graph-parameters", '\0', "sigma, k, minsize", "0.75,500,50" };
const ModelOptionDef OPT_MDPcolorSpace = {
   MODOPT_ARG(ColorSpaceType), "MDPcolorSpace", &MOC_MBARI, OPTEXP_MRV,
   "Input image color space. Used to determine whether to compute saliency on color channels or not",
    "mbari-color-space", '\0', "<RGB|YCBCR|Gray>",
    "RGB" };
const ModelOptionDef OPT_MDPsegmentAlgorithmInputImage = {
   MODOPT_ARG(SegmentAlgorithmInputImageType), "MDPsegmentInputImage", &MOC_MBARI, OPTEXP_MRV,
   "Segment algorithm input images type",
    "mbari-segment-algorithm-input-image", '\0', "<DiffMean|Luminance>",
    "DiffMean" };
const ModelOptionDef OPT_MDPsaliencyInputImage = {
      MODOPT_ARG(SaliencyInputImageType), "MDPsaliencyInputImage", &MOC_MBARI, OPTEXP_MRV,
    "Saliency input image type",
    "mbari-saliency-input-image", '\0', "<Raw|DiffMean|None>",
    "DiffMean" };
const ModelOptionDef OPT_MDPcleanupSESize =
  { MODOPT_ARG_INT, "MDPcleanupSESize", &MOC_MBARI, OPTEXP_MRV,
    "Size of structure element to do morhphological erode/dilate to clean-up segmented image",
    "mbari-se-size", '\0', "2-10",
    "2" };
const ModelOptionDef OPT_MDPmaskPath =
  { MODOPT_ARG_STRING, "MDPmaskPath", &MOC_MBARI, OPTEXP_MRV,
    "MaskPath: path to the mask image",
    "mbari-mask-path", '\0', "<file>",
    "" };
const ModelOptionDef OPT_MDPmaskXPosition =
  { MODOPT_ARG_INT, "MDPmaskXPosition", &MOC_MBARI, OPTEXP_MRV,
    "MaskXPosition: x position of the mask point of reference; ",
    "mbari-mask-xposition", '\0', "<int>", "1" };
const ModelOptionDef OPT_MDPmaskYPosition =
  { MODOPT_ARG_INT, "MDPmaskYPosition", &MOC_MBARI, OPTEXP_MRV,
    "MaskYPosition: y position of the mask point of reference; ",
    "mbari-mask-yposition", '\0', "<int>", "1" };
const ModelOptionDef OPT_MDPmaskWidth =
  { MODOPT_ARG_INT, "MDPmaskWidth", &MOC_MBARI, OPTEXP_MRV,
    "MaskWidth: mask width; ",
    "mbari-mask-width", '\0', "<int>", "1" };
const ModelOptionDef OPT_MDPmaskHeight =
  { MODOPT_ARG_INT, "MDPmaskHeight", &MOC_MBARI, OPTEXP_MRV,
    "MaskHeight: mask height; ",
    "mbari-mask-height", '\0', "<int>", "1" };
const ModelOptionDef OPT_MDPminEventArea =
  { MODOPT_ARG_INT, "MDPBminEventArea", &MOC_MBARI, OPTEXP_MRV,
    "The minimum area an event must be to be candidate. When set to 0, defaults to foa size, which is derived from the image size.",
    "mbari-min-event-area", '\0', "<int>", "0" };
const ModelOptionDef OPT_MDPmaxEventArea =
  { MODOPT_ARG_INT, "MDPBmaxEventArea", &MOC_MBARI, OPTEXP_MRV,
    "The maximum area an event can be, to be candidate. When set to 0, defaults to a multiplied factor of the foa size, which is derived from the image size.",
    "mbari-max-event-area", '\0', "<int>", "0" };
const ModelOptionDef OPT_MDPminEventFrames =
  { MODOPT_ARG_INT, "MDPBminEventFrames", &MOC_MBARI, OPTEXP_MRV,
    "The minimum number of frames an event must be to be candidate",
    "mbari-min-event-frames", '\0', "<int>", "1" };
const ModelOptionDef OPT_MDPmaxEventFrames =
  { MODOPT_ARG_INT, "MDPBmaxEventFrames", &MOC_MBARI, OPTEXP_MRV,
    "The maximum number of frames an event can be; defaults to indefinite",
    "mbari-max-event-frames", '\0', "<int>", "-1" };
const ModelOptionDef OPT_MDPsizeAvgCache =
  { MODOPT_ARG_INT, "MDPBsizeAvgCache", &MOC_MBARI, OPTEXP_MRV,
    "The number of frames used to compute the running average",
    "mbari-cache-size", '\0', "<int>", "30" };
const ModelOptionDef OPT_MDPsaliencyFrameDist =
  { MODOPT_ARG_INT, "MDPBsaliencyFrameDist", &MOC_MBARI, OPTEXP_MRV,
    "The number of frames to delay between saliency map computations ",
    "mbari-saliency-dist", '\0', "<int>", "5" };
const ModelOptionDef OPT_MDPmaxEvolveTime =
  { MODOPT_ARG_INT, "MDPBmaxEvolveTime", &MOC_MBARI, OPTEXP_MRV,
    "Maximum amount of time in milliseconds to evolve the brain until stopping",
    "mbari-max-evolve-msec", '\0', "<int>", "500" };
const ModelOptionDef OPT_MDPmaxWTAPoints =
  { MODOPT_ARG_INT, "MDPBmaxWTAPoints", &MOC_MBARI, OPTEXP_MRV,
    "Maximum number of winner-take-all points to find in each frame",
    "mbari-max-WTA-points", '\0', "<int>", "20" };
const ModelOptionDef OPT_MDPkeepBoringWTAPoints =
  { MODOPT_FLAG, "MDPkeepBoringWTAPoints", &MOC_MBARI, OPTEXP_MRV,
    "Keep boring WTA points from saliency computation. Turning this on "
    "will increase the number of candidates but can also increase the"
    "number of false detections",
    "mbari-keep-boring-WTA-points", '\0', "", "false" };
const ModelOptionDef OPT_MDPsaveNonInterestingEvents =
  { MODOPT_FLAG, "OPT_MDPsaveNonInterestingEvents", &MOC_MBARI, OPTEXP_MRV,
    "Save non-interesting events. Default is to remove non-interesting events, set to true to save",
    "mbari-save-non-interesting-events", '\1', "", "false" };
const ModelOptionDef OPT_MDPsaveOriginalFrameSpec =
  { MODOPT_FLAG, "OPT_MDPsaveOriginalFrameSpec", &MOC_MBARI, OPTEXP_MRV,
    "Save events in original frame size specs, but run saliency computation on reduced frame size. This does nothing if the frames are not resized with the --rescale-input option. Default is set to false",
    "mbari-save-original-frame-spec", '\0', "", "false" };
const ModelOptionDef OPT_MDPminStdDev =
  { MODOPT_ARG_FLOAT, "OPT_MDPminStdDev", &MOC_MBARI, OPTEXP_MRV,
    "Minimum std deviation of input image required for processing. This is useful to remove black frames, or frames with high visual noise",
    "mbari-min-std-dev", '\0.', "<float>", "0."};
const ModelOptionDef OPT_MDPeventExpirationFrames = {
   MODOPT_ARG_INT, "MDPeventExpirationFrames", &MOC_MBARI, OPTEXP_MRV,
   "How long to keep an event in memory before removing it if no bit objects found to combine with the event. Useful for noisy video or reduced frame rate video where tracking problems occur.",
    "mbari-event-expiration-frames", '\0', "<int>",
    "0" }; 

// ####################

// #################### Version options:
const ModelOptionDef OPT_MbariVersion =
  { MODOPT_FLAG, "MBARIVersion", &MOC_MBARI, OPTEXP_MRV,
    "Print version",
    "version", 'v', "", "false" };
// ####################
