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

#if !defined(STAGE_H)
#define STAGE_H

#include <stdio.h>
#include "MessagePassing/Mpidef.H"
#include "Raster/RasterFileFormat.H"
#include "Util/log.H"
#include "Image/PixelsTypes.H"
#include "Image/Image.H"

#define PI_ITERATIONS (double) 9999999
#include <stdio.h>

// ######################################################################
//! Enumerates all 5 stages and contains utilities to gett information about each stage
namespace Stages {
  
  // ! Message Passsing IDs
  enum stageID{CP_STAGE = 1, SG_STAGE = 2, GSR_STAGE  = 3, UE_STAGE  = 4};

  // !Global variable - the first created object that controllers synchronizing
  // the stages. This must be 0
  const int CONTROLLER = 0;
  
  // !returns Stage name that corresponds to id
  const char * stageName(stageID id);
  
  //! returns number of stages 
  const int getNumStages();  
}

// ######################################################################
//! Base class for MPI messaging between stages
class Stage  
{
public:			

  // !Constructor
  Stage(MPI_Comm mastercomm, const char *name);

  // !Destructor
  virtual ~Stage();
	
  // !custom Stage run method. should be overriden
  virtual void runStage();

  // !custom Stage init method. called before runstage.
  virtual void initStage();

  // !custom Stage shutdown. overriden if any clean-up must 
  // happen after receiving exit from the controller
  virtual void shutdown();

  // !entry point for stage run
  void run();

  // !stagename
  const  char * name();

  // !stageid
  Stages::stageID id();

  // !stage-to-stage messages
  enum Messages{MSG_DATAREADY=20, MSG_DATARECVD=21, MSG_EXIT=22};
	
  // !returns master comm channel
  int mastercomm();

  // !function to calculate pi to simulate some work
  void calculatepi(double n);

  // !probes master comm channel for exit signal
  // returns true if received exit signal
  bool probeMasterForExit();

	
  //! Write a frame to disk
  void writeRGB(const Image< PixRGB<byte> >& image, const RasterFileFormat ft, const int framenum);
	
private:		
  Stages::stageID itsid;
  MPI_Comm itsmastercomm;
  char *itsname;	
};

#endif // !defined(STAGE_H)
