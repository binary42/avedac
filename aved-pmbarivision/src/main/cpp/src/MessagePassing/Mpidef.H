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

#ifndef MPIDEF_H_
#define MPIDEF_H_
#include "mpi.h"
enum events {event1a, event1b, event2a, event2b, event3a, event3b, event4a, \
	event4b, event5a, event5b, event6a, event6b, event7a, event7b, event8a, \
	event8b, event9a, event9b, event10a, event10b, event11a, event11b };
		
#ifdef MPELOG
extern "C" {
#include "mpe.h"
inline void MBARI_Init_MPE_States() {	
MPE_Describe_state(event1a, event1b, "CP", "orange");
MPE_Describe_state(event2a, event2b, "preprocess", "LightSkyBlue");
MPE_Describe_state(event3a, event3b, "segment", "green");
MPE_Describe_state(event4a, event4b, "updateint events", "light coral");  
MPE_Describe_state(event5a, event5b, "saveresults", "yellow");
MPE_Describe_state(event6a, event6b, "brainevolve+extract+search", "cyan");
MPE_Describe_state(event7a, event7b, "extractBitObjs", "PeachPuff1");
MPE_Describe_state(event8a, event8b, "searchBitObjs", "lavender");
MPE_Describe_state(event9a, event9b, "getSalRegions", "magenta1"); 
MPE_Describe_state(event10a, event10b, "braininput", "red");
MPE_Describe_state(event11a, event11b, "test", "yellow");
};
}
#else
extern "C" {
#define MPE_Init_log void
#define MPE_Start_log void
#define MPE_Finish_log void
inline void MPE_Describe_state(int, int, const char*, const char*){};
inline int MPE_Log_event(int, int, const char *){};
}
#endif


#endif /*MPIDEF_H_*/
