// stage.h: interface for the stage class.
//
//////////////////////////////////////////////////////////////////////

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
