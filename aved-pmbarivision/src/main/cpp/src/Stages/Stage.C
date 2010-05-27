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

#include "Stages/Stage.H"
#include "PipelineControl/PipelineController.H"
#include "Raster/Raster.H"
#include "Component/ModelManager.H"
#include "Utils/Const.H"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

template <class T> class Image;
template <class T> class PixRGB;

namespace Stages {
	
  // ! Returns number of stages
  const int getNumStages() {
    return 4;
  }
  // ! Names for stages and controllers. The controller should always be zero
  //enum stageID{CP_STAGE, SG_STAGE, GSR_STAGE, UE_STAGE};
  const char *stageNames[] = {"Controller", "CachePreprocess","Segment", "GetSalientRegions", "UpdateEvents"};

  // ! Returns Stage name that corresponds to id
  const char * stageName(stageID id){
    if(id <= getNumStages() && id >= 0)
      return (stageNames[id]);    
  	
    return NULL;
    //TODO: return error string 
  }
}
//////////////////////////////////////////////////////////////////////
// Construction/Destruction
///////////////////////////////////////////////////////////////////
Stage::Stage(MPI_Comm mastercomm, const char *name)
  :itsmastercomm(mastercomm)
{
  int id;
  MPI_Comm_rank( mastercomm, &id );    
  LINFO("Creating Stage: %s comm:%d id: %d", name, mastercomm, id);

  itsname = strdup(name);
  itsid = static_cast<Stages::stageID>(id);
}
Stage::~Stage()
{
  LINFO("Deleting stage %s id: %d", itsname, itsid);
  free((void *)itsname);
}
const char *Stage::name() {
  return itsname;
}
Stages::stageID Stage::id()
{
  return itsid;
}
void Stage::run()
{
  int flag;
  int fshutdown = 0;
  MPI_Status status;
  MPI_Request request;
    	
  do {
  
    status.MPI_TAG = -1;
    status.MPI_SOURCE = -1;

    if(MPI_Irecv( &flag, 1, MPI_INT, 0, MPI_ANY_TAG, itsmastercomm, &request ) == MPI_SUCCESS) {	
      MPI_Wait(&request, &status);	
      Stages::stageID id = static_cast<Stages::stageID>(status.MPI_SOURCE);
      switch (status.MPI_TAG)			{	
      case(Controller::MSG_START):				
        LINFO("%s received MSG_START from %s", itsname, Stages::stageName(id));
        runStage();
        LINFO("%s returned from runStage", stageName(itsid));
        break;
      case(Controller::MSG_INIT):				
        LDEBUG("%s received MSG_INIT from %s", itsname, Stages::stageName(id));
        initStage();
        LDEBUG("%s returned from initStage", Stages::stageName(itsid));
        MPI_Isend( &flag, 1, MPI_INT, Stages::CONTROLLER, Controller::MSG_INIT_FINISHED, Stage::mastercomm(),&request);
        break;
      case(Controller::MSG_EXIT):
        LDEBUG("%s received MSG_EXIT from %s", itsname, Stages::stageName(id));
        break;
      case(Controller::MSG_SHUTDOWN):
        LDEBUG("%s received MSG_SHUTDOWN from %s", itsname, Stages::stageName(id));
        shutdown();
        fshutdown = 1;
        LDEBUG("%s sending message MSG_SHUTDOWN_FINISHED to Controller", Stage::name());
        MPI_Send( &flag, 1, MPI_INT, Stages::CONTROLLER, Controller::MSG_SHUTDOWN_FINISHED, Stage::mastercomm());
        break;
      default:
        LDEBUG("%s received unknown message %d from %s", itsname, status.MPI_TAG, Stages::stageName(id));
        break;			
      }
    }
  }
  while (!fshutdown);	
  
  LINFO("%s exit", itsname);
}
void Stage::runStage()
{
}
void Stage::initStage()
{
}

int Stage::mastercomm()
{
  return itsmastercomm;

}

void Stage::shutdown()
{
  //do nothing. override for custom stage shutdown method
}

void Stage::calculatepi(double n)
{
  double PI25DT = 3.141592653589793238462643;
  double i, itspi, pi, h, sum, x;	
  
  h   = 1.0 / (double) n;
  sum = 0.0;
  for (i = 1; i <= n; i++) {
    x = h * ((double)i - 0.5);
    sum += 4.0 / (1.0 + x*x);
  }
  itspi = h * sum;
  
  LINFO("%s pi is approximately %.16f, Error is %.16f\n",  Stage::name(), pi, fabs(pi - PI25DT));
}
void Stage::writeRGB(const Image< PixRGB<byte> >& image,
                     const RasterFileFormat ft, const int framenum)
{
  // format frame number as a 6-digit string:
  char filename[32]; sprintf(filename, "Res%06d", framenum);
	
  // write the image:
  Raster::WriteRGB(image, filename, ft);	
}

bool Stage::probeMasterForExit()
{
  MPI_Status status;
  int flag;

  status.MPI_TAG = -1;
  status.MPI_SOURCE = -1;
  
  MPI_Iprobe(Stages::CONTROLLER, Controller::MSG_EXIT, mastercomm() , &flag, &status);		
  if(status.MPI_SOURCE == Stages::CONTROLLER) return true;
  return false;

}
