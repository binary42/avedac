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

/*!@file MbariModelOptionDef.H Types for defining command-line options. */

#ifndef MBARIOPTIONDEF_H_
#define MBARIOPTIONDEF_H_

#include "Component/ModelComponent.H"

extern void REQUEST_OPTIONALIAS_MBARI(OptionManager& m);
struct ModelOptionCateg;
extern const ModelOptionCateg MOC_MBARI;

//! Command-line options for MbariResultManager
//@{
extern const ModelOptionDef OPT_MRVsaveEvents;
extern const ModelOptionDef OPT_MRVloadEvents;
extern const ModelOptionDef OPT_MRVsaveProperties;
extern const ModelOptionDef OPT_MRVloadProperties;
extern const ModelOptionDef OPT_MRVsavePositions;
extern const ModelOptionDef OPT_MRVmarkInteresting;
extern const ModelOptionDef OPT_MRVopacity;
extern const ModelOptionDef OPT_MRVmarkCandidate;
extern const ModelOptionDef OPT_MRVmarkPrediction;
extern const ModelOptionDef OPT_MRVmarkFOE;
extern const ModelOptionDef OPT_MRVsaveResults;
extern const ModelOptionDef OPT_MRVdisplayResults;
extern const ModelOptionDef OPT_MRVsaveOutput;
extern const ModelOptionDef OPT_MRVdisplayOutput;
extern const ModelOptionDef OPT_MRVshowEventLabels;
extern const ModelOptionDef OPT_MRVrescaleDisplay;
extern const ModelOptionDef OPT_MRVsaveEventNums;
extern const ModelOptionDef OPT_MRVsaveSummaryEventsName;
extern const ModelOptionDef OPT_MRVsaveNonInterestingEvents;
extern const ModelOptionDef OPT_MRVsaveXMLEventSet;
extern const ModelOptionDef OPT_MRVmetadataSource;
extern const ModelOptionDef OPT_MDPBenthicVideo;
extern const ModelOptionDef OPT_MDPMidwaterVideo;
//@}

//! Command-line options for DetectionParametersModelComponent
//@{
extern const ModelOptionDef OPT_MDPsaliencyInputImage;
extern const ModelOptionDef OPT_MDPsaliencyFrameDist;
extern const ModelOptionDef OPT_MDPsegmentAlgorithmInputImage;
extern const ModelOptionDef OPT_MDPsegmentAlgorithm;
extern const ModelOptionDef OPT_MDPmaxEvolveTime;
extern const ModelOptionDef OPT_MDPmaxWTAPoints;
extern const ModelOptionDef OPT_MDPkeepBoringWTAPoints;
extern const ModelOptionDef OPT_MDPtrackingMode;
extern const ModelOptionDef OPT_MDPsegmentSEType;
extern const ModelOptionDef OPT_MDPmaskPath;
extern const ModelOptionDef OPT_MDPmaskXPosition;
extern const ModelOptionDef OPT_MDPmaskYPosition;
extern const ModelOptionDef OPT_MDPmaskWidth;
extern const ModelOptionDef OPT_MDPmaskHeight;
extern const ModelOptionDef OPT_MDPminEventArea;
extern const ModelOptionDef OPT_MDPmaxEventArea;
extern const ModelOptionDef OPT_MDPsizeAvgCache;
extern const ModelOptionDef OPT_MDPMosaicStills;
extern const ModelOptionDef OPT_MDPMosaicBenthicStills;
extern const ModelOptionDef OPT_MDPTimeLapseStills;
extern const ModelOptionDef OPT_MDPTimeLapseRover;
extern const ModelOptionDef OPT_MDPMidwaterVideo;
extern const ModelOptionDef OPT_MDPBenthicVideo;
//@}

//! Command-line options for Version
//@{
extern const ModelOptionDef OPT_MbariVersion;
//@}

//! Command-line options for OutputMbariFrameSeries
//@{
extern const ModelOptionDef OPT_OutputMbariShowFrames;
//@}

#endif /*MBARIOPTIONDEF_H_*/
