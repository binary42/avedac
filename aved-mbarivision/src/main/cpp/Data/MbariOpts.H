/*!@file MbariModelOptionDef.H Types for defining command-line options. */

// //////////////////////////////////////////////////////////////////// //
// The iLab Neuromorphic Vision C++ Toolkit - Copyright (C) 2000-2003   //
// by the University of Southern California (USC) and the iLab at USC.  //
// See http://iLab.usc.edu for information about this project.          //
// //////////////////////////////////////////////////////////////////// //
// Major portions of the iLab Neuromorphic Vision Toolkit are protected //
// under the U.S. patent ``Computation of Intrinsic Perceptual Saliency //
// in Visual Environments, and Applications'' by Christof Koch and      //
// Laurent Itti, California Institute of Technology, 2001 (patent       //
// pending; application number 09/912,225 filed July 23, 2001; see      //
// http://pair.uspto.gov/cgi-bin/final/home.pl for current status).     //
// //////////////////////////////////////////////////////////////////// //
// This file is part of the iLab Neuromorphic Vision C++ Toolkit.       //
//                                                                      //
// The iLab Neuromorphic Vision C++ Toolkit is free software; you can   //
// redistribute it and/or modify it under the terms of the GNU General  //
// Public License as published by the Free Software Foundation; either  //
// version 2 of the License, or (at your option) any later version.     //
//                                                                      //
// The iLab Neuromorphic Vision C++ Toolkit is distributed in the hope  //
// that it will be useful, but WITHOUT ANY WARRANTY; without even the   //
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR      //
// PURPOSE.  See the GNU General Public License for more details.       //
//                                                                      //
// You should have received a copy of the GNU General Public License    //
// along with the iLab Neuromorphic Vision C++ Toolkit; if not, write   //
// to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,   //
// Boston, MA 02111-1307 USA.                                           //
// //////////////////////////////////////////////////////////////////// //
//
// Primary maintainer for this file: Laurent Itti <itti@usc.edu>
// $HeadURL: svn://iLab.usc.edu/trunk/saliency/src/Component/ModelOptionDef.H $
// $Id: MbariOpts.H,v 1.6 2009/09/15 16:13:17 dcline Exp $
//
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
