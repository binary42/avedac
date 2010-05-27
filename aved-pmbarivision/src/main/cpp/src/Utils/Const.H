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

#ifndef INCONSTH
#define INCONSTH       1

#ifndef FALSE
#define FALSE   0
#endif

#ifndef TRUE
#define TRUE    ~FALSE
#endif

#define ON      TRUE
#define OFF     FALSE

#ifndef OK
#define OK       1
#define SUCCESS  ~OK
#endif

#ifndef ERROR
#define ERROR    -1
#endif

#ifndef FOREVER
#define FOREVER for (;;)
#endif

#ifndef min
#define min(a,b)        (a) < (b) ? (a) : (b)
#endif
#ifndef max
#define max(a,b)        (a) > (b) ? (a) : (b)
#endif

#endif /* INCONSTH */
