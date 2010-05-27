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
#if !defined(MBARIMPIDATA_H)
#define MBARIMPIDATA_H

#include "mpi.h"
#include "Image/PixelsTypes.H"
#include "Image/MathOps.H"
#include "Image/BitObject.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "DetectionAndTracking/MbariFunctions.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "Stages/SalientWinner.H"
#include <list>

using namespace std;

enum dataType{RGBBYTEIMAGE, BYTEIMAGE, SALIENTWINNERLIST, VISUALEVENTSET};
enum sendrecvType{NON_BLOCKING, BLOCKING};

#define HD_MAX_HEIGHT	1080
#define HD_MAX_WIDTH	1920

// !returns -1 on error, otherwise send RGB byte image and return MPI error code
int sendRGBByteImage(const Image< PixRGB<byte> >& image, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType=BLOCKING);

// !returns -1 on error, otherwise send byte image and return MPI error code
int sendByteImage(const Image<byte>& image, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType=BLOCKING);

// !returns -1 on error, otherwise returns frame number this data comes from
int receiveData(void **data, dataType type, int source, int tag, MPI_Comm comm, MPI_Status *status, MPI_Request *r, sendrecvType srType=BLOCKING );

// !returns -1 on error, otherwise send MbariVisualEventSet and return MPI error code
int sendVisualEventSet(MbariVisualEvent::VisualEventSet &set, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType=BLOCKING);

// !returns -1 on error, otherwise sends BitObjects and return MPI error code
int sendBitObjectList(list<BitObject> &sobjs, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType=BLOCKING);

// !returns -1 on error, otherwise sends list of SalientWinners and return MPI error code
int sendSalientWinnerList(list<SalientWinner> &list, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType=BLOCKING);

// !sends list of objects, can be anytime of object with writeToStream function overload
template <class T> 
int sendList(std::list<T> &list, dataType type, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType);

#endif
