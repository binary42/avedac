// Controller.C: implementation of the Controller class.
//
//////////////////////////////////////////////////////////////////////

#include "PipelineController.H"
#include "MessagePassing/Mpimessage.H"
#include "Stages/Stage.H"
#include <stdio.h>
#include <stdlib.h>


//////////////////////////////////////////////////////////////////////
// Initialize static objects
//////////////////////////////////////////////////////////////////////

Controller::Controller(MPI_Comm mastercomm)
  :mastercomm(mastercomm)
{
  int id;
  MPI_Comm_rank( mastercomm, &id );    
  LINFO("Creating Controller comm:%d id: %d", mastercomm, id);	
}

Controller::~Controller()
{
}

void Controller::run()
{
  int value;
  int exitflags = 0;
  int i;
  MPI_Status s;
  MPI_Request r;
  Stages::stageID id = Stages::CP_STAGE;

  MPE_Log_event(20,0,"");

  //Send MSG_INIT to start initialization of all stages 
  //skip master process 0  and receipt of GSR_STAGE INIT_FINISHED
  for (i=1; i<=Stages::getNumStages(); i++) {
    MPI_Send(&exitflags, 1, MPI_INT, i, Controller::MSG_INIT, mastercomm);     
    if(MPI_Recv(&exitflags, 1, MPI_INT, i, Controller::MSG_INIT_FINISHED, mastercomm, &s) == MPI_SUCCESS)
      id = static_cast<Stages::stageID>(s.MPI_SOURCE);
    LDEBUG("Received MSG_INIT_FINISHED from  %s stage", Stages::stageName(id));
  } 

  //Send MSG_START to start all stages 
  //skip master process 0, 
  for (i=1; i<=Stages::getNumStages(); i++) {
    id = static_cast<Stages::stageID>(i);
    LDEBUG("Sending MSG_START to  %s stage", Stages::stageName(id));
    MPI_Send(&exitflags, 1, MPI_INT, i, Controller::MSG_START, mastercomm);             
  }      
  
  //After receive message from UpdateEvents stage that last frame is received,  
  //Send/receive MSG_EXIT for ordered shutdown  - skipping master stages 0
  do  {  
    s.MPI_TAG = -1;
    s.MPI_SOURCE = -1;      
    if(MPI_Iprobe( Stages::UE_STAGE, MASTER_SHUTDOWN, mastercomm, &exitflags, &s) == MPI_SUCCESS) {
      if(s.MPI_SOURCE == Stages::UE_STAGE && s.MPI_TAG == MASTER_SHUTDOWN &&
         MPI_Recv(&exitflags, 1, MPI_INT, Stages::UE_STAGE, MASTER_SHUTDOWN, mastercomm, &s) == MPI_SUCCESS){    
        LDEBUG("Received MSG_EXIT from  %s stage", stageName(Stages::UE_STAGE));  
        break;
      }
    }
    usleep(100000);
  }while(1);


  for (i=1; i<=Stages::getNumStages(); i++) {    
    s.MPI_SOURCE=-1; s.MPI_TAG=-1; r = -1;      
    Stages::stageID id = static_cast<Stages::stageID>(i);
    LDEBUG("Sending MSG_EXIT to  %s stage", stageName(id));
    MPI_Isend(&exitflags, 1, MPI_INT, i, Controller::MSG_EXIT, mastercomm, &r);
    MPI_Wait(&r, &s);
  }  
  
  //Send MSG_SHUTDOWN to shutdown all stages 
  //skip master stage 0, 
  for (i=1; i<=Stages::getNumStages(); i++) {    
    s.MPI_SOURCE=-1; s.MPI_TAG=-1; r = -1;
    Stages::stageID id = static_cast<Stages::stageID>(i);      
    LDEBUG("Sending MSG_SHUTDOWN to  %s stage", stageName(id));
    MPI_Isend(&exitflags, 1, MPI_INT, i, Controller::MSG_SHUTDOWN, mastercomm, &r);
    MPI_Wait(&r, &s);
    id = static_cast<Stages::stageID>(s.MPI_SOURCE);  
    if(MPI_Recv(&exitflags, 1, MPI_INT, i, Controller::MSG_SHUTDOWN_FINISHED, mastercomm, &s) == MPI_SUCCESS)
      LDEBUG("Received MSG_SHUTDOWN_FINISHED from  %s stage", Stages::stageName(id));
  }      
  
  MPE_Log_event(21,0,"");
}

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
