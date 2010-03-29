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
