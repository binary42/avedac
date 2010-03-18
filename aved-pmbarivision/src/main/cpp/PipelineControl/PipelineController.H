// Controller.h: interface for the Controller class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(CONTROLLER_H)
#define CONTROLLER_H

#include "MessagePassing/Mpidef.H"
// ######################################################################
//! Controls messaging to start/stop/shutdown pipeline stages
class Controller  
{
public:	
//!constructor
Controller(MPI_Comm mastercomm);

//!destructor
virtual ~Controller();

void run();

// !controller messages
enum Messages {MSG_START=10, MSG_EXIT=11, MSG_EXIT_DONE=12, MSG_INIT=13, MSG_INIT_FINISHED=14, MSG_SHUTDOWN=15, MSG_SHUTDOWN_FINISHED=16};

#define MASTER_SHUTDOWN 300		
	
private:

MPI_Comm mastercomm;
MPI_Comm comm;
};

#endif // !defined(CONTROLLER_H)
