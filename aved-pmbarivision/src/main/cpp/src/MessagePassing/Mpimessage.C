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

#include "MessagePassing/Mpimessage.H"
#include "PipelineControl/PipelineController.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include <stdlib.h>
#include <stdio.h>
#include <string>
#include <iostream>
#include <fstream>
#include <sstream>
#include "Utils/Const.H"
#include "Stages/Stage.H"
using namespace std;

/* Statically allocate memory for frames */
unsigned char sndPackBuf[sizeof(int)*4 + sizeof(unsigned char)*MAX_IMAGE_HEIGHT*MAX_IMAGE_WIDTH*3];
unsigned char rcvPackBuf[sizeof(int)*4 + sizeof(unsigned char)*MAX_IMAGE_HEIGHT*MAX_IMAGE_WIDTH*3];  
char objectBuf[sizeof(int)*4 + sizeof(unsigned char)*MAX_IMAGE_HEIGHT*MAX_IMAGE_WIDTH];  

int sendRGBByteImage(const Image< PixRGB<byte> >& image, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType)
{
  int packsize = 0;
  int maxsize;
  int metaData[4];  
  int mpierr = MPI_SUCCESS;
  MPI_Status status;	

  maxsize = sizeof(sndPackBuf);  
  
  metaData[0] = RGBBYTEIMAGE; 
  metaData[1] = framenum;
  metaData[2] = image.getWidth();
  metaData[3] = image.getHeight(); 

  unsigned char *buf = sndPackBuf;

  MPI_Pack(metaData, 4, MPI_INT, buf, maxsize, &packsize, comm);
  MPI_Pack((void *)image.getArrayPtr(), image.getSize()*3, MPI_UNSIGNED_CHAR, buf, maxsize, &packsize, comm);    

  if(srType == BLOCKING)
    mpierr = MPI_Send( buf, packsize, MPI_PACKED, dest, tag, comm );
  else {
    MPI_Request request;
    MPI_Isend( buf, packsize, MPI_PACKED, dest, tag, comm, &request );  
  }
  Stages::stageID id = static_cast<Stages::stageID>(dest);
  LDEBUG("Sending RGBBYTEIMAGE maxsize: %d packsize: %d to %s mpierr:%d", \
         maxsize, packsize, Stages::stageName(id),mpierr);

  return mpierr;
}
int sendByteImage(const Image<byte>& image, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType)
{
  int packsize = 0;
  int maxsize;
  int metaData[4];  
  int mpierr = MPI_SUCCESS;
  MPI_Status status;

  maxsize = sizeof(sndPackBuf);  

  metaData[0] = BYTEIMAGE; 
  metaData[1] = framenum;
  metaData[2] = image.getWidth();
  metaData[3] = image.getHeight();

  unsigned char *buf = sndPackBuf;
  
  MPI_Pack(metaData, 4, MPI_INT, buf, maxsize, &packsize, comm);
  MPI_Pack((void *)image.getArrayPtr(), image.getSize(), MPI_UNSIGNED_CHAR, buf, maxsize, &packsize, comm);  

  if(srType == BLOCKING)
    mpierr = MPI_Send( buf, packsize, MPI_PACKED, dest, tag, comm );
  else {
    MPI_Request request;
    MPI_Isend( buf, packsize, MPI_PACKED, dest, tag, comm, &request );  
  }
  Stages::stageID id = static_cast<Stages::stageID>(dest);
  LDEBUG("Sending BYTEIMAGE maxsize: %d packsize: %d to %s mpierr:%d", \
         maxsize, packsize, Stages::stageName(id), mpierr);

  return mpierr;
}
int sendVisualEventSet(MbariVisualEvent::VisualEventSet &set, int framenum, int dest, int tag, MPI_Comm comm,sendrecvType srType)
{
  int packsize = 0;
  int maxsize;
  int metaData[4];  
  int mpierr;
  MPI_Status status;

  //initialize metadata
  maxsize = sizeof(sndPackBuf);  
  metaData[0] = VISUALEVENTSET; 
  metaData[1] = framenum;  

  //create ostream to extract BitObjects to
  stringbuf sb;
  ostream  os(&sb);  

  // create buffer of objects
  set.writeToStream(os);
  os.flush();  

  //for debug only
  //cout << sb.str();
  //cout << sb.str().size();
    
  metaData[2] = sb.str().size();
  metaData[3] = 0;
  
  MPI_Pack(metaData, 4, MPI_INT, sndPackBuf, maxsize, &packsize, comm);      
  MPI_Pack((void *)sb.str().c_str(), sb.str().size(), MPI_UNSIGNED_CHAR, sndPackBuf, maxsize, &packsize, comm);  

  if(srType == BLOCKING)
    mpierr = MPI_Send(sndPackBuf, packsize, MPI_PACKED, dest, tag, comm );  
  else {
    MPI_Request request;
    MPI_Isend( sndPackBuf, packsize, MPI_PACKED, dest, tag, comm, &request );  
  }
  LDEBUG("Sending visual event set");
  return mpierr; 
}
int sendSalientWinnerList(list<SalientWinner> &l, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType)
{
  sendList(l, SALIENTWINNERLIST, framenum, dest, tag, comm, srType);  
}

template <class T> 
int sendList(list<T> &l, dataType type, int framenum, int dest, int tag, MPI_Comm comm, sendrecvType srType)
{  
  int packsize = 0;
  int maxsize;
  int metaData[4];  
  int mpierr;
  MPI_Status status;
  typename list<T>::iterator i;

  //initialize metadata
  maxsize = sizeof(sndPackBuf);  
  metaData[0] = type; 
  metaData[1] = framenum;  

  //create ostream to extract  to
  stringbuf sb;
  ostream  os(&sb);  

  for (i = l.begin(); i != l.end(); ++i) {  
    //write object to ostream
    i->writeToStream(os);  	
  }
  os.flush();  

  //for debug only
  //cout << sb.str();
  //cout << sb.str().size();
    
  metaData[2] = sb.str().size();
  metaData[3] = l.size();  
  
  MPI_Pack(metaData, 4, MPI_INT, sndPackBuf, maxsize, &packsize, comm);      
  MPI_Pack((void *)sb.str().c_str(), sb.str().size(), MPI_UNSIGNED_CHAR, sndPackBuf, maxsize, &packsize, comm);  

  if(srType == BLOCKING)
    mpierr = MPI_Send(sndPackBuf, packsize, MPI_PACKED, dest, tag, comm );  
  else {
    MPI_Request request;
    mpierr = MPI_Isend( sndPackBuf, packsize, MPI_PACKED, dest, tag, comm, &request );  
  }
  return mpierr;
}

int receiveData(void **data, dataType type, int source, int tag, MPI_Comm comm, MPI_Status *status, MPI_Request *request, sendrecvType srType)
{
  int packsize;
  int metaData[4];  
  int position = 0; 
  int mpierr = 0; 
  MPI_Status masterstatus;

  packsize = sizeof(rcvPackBuf);

  if(srType == BLOCKING){  
    if(MPI_Recv( &rcvPackBuf, packsize, MPI_PACKED, source, tag, comm, status ) != MPI_SUCCESS) return -1;  
  }
  else {
    MPI_Irecv( &rcvPackBuf, packsize, MPI_PACKED, source, tag, comm, request );  
    MPI_Wait(request, status);
  }

  position = 0;
   
  MPI_Unpack(rcvPackBuf, packsize, &position, metaData, 4, MPI_INT, comm);

  //check if data type requested returned in packet
  if(metaData[0] != type) {
    LDEBUG("ERROR: received invalid data type: %d in receiveData", metaData[0]);
    return -1;
  }
  else {
    LDEBUG("Received data type: %d in receiveData", metaData[0]);
    switch(type)
      {
      case(RGBBYTEIMAGE):
        {			  
          //unpack image and copy to image pointer
          Image< PixRGB<byte> > *imbyte = new Image< PixRGB<byte> >(metaData[2],metaData[3], ZEROS);	  
          if(metaData[2] > 0 && metaData[3] > 0)
            mpierr = MPI_Unpack(rcvPackBuf, packsize, &position, imbyte->getArrayPtr(), \
                                imbyte->getSize()*3, MPI_UNSIGNED_CHAR, comm);	  
          *data = (void *)imbyte;				  
          LDEBUG("Received RGBBYTEIMAGE packsize: %d from destination %d mpierr: %d", \
                 packsize, source, mpierr);
        }
		  
        break;
      case(BYTEIMAGE):
        {
          //unpack image and copy to image pointer
          Image< byte > *im = new Image< byte >(metaData[2],metaData[3], ZEROS);	  
          if(metaData[2] > 0 && metaData[3] > 0) 
            mpierr = MPI_Unpack(rcvPackBuf, packsize, &position, im->getArrayPtr(), im->getSize(), MPI_UNSIGNED_CHAR, comm);	  
          *data = (void *)im;		  		  			  		  
          LDEBUG("Received BYTEIMAGE packsize: %d from destination %d mpierr: %d", \
                 packsize, source, mpierr);
        }
        break;
      case(SALIENTWINNERLIST):
        {
          list<SalientWinner> *bos = new list<SalientWinner>;
          SalientWinner object;

          //create string to extract to/from  
          std::stringbuf sb;
          std::istream  is(&sb);

          int size = min((unsigned int)metaData[2],sizeof(objectBuf));

          //unpack SalientWinner into objectBuf
          mpierr = MPI_Unpack(rcvPackBuf, packsize, &position, objectBuf, size, MPI_UNSIGNED_CHAR, comm);	  		  		  

          sb.pubsetbuf(objectBuf, size );  

          //for debug only
          //cout << sb.str();
          //cout << sb.str().size();  		  

          //create SalientWinners from stream
          for(int i=0; i< metaData[3]; i++){
            object.readFromStream(is);			  
            bos->push_back(object);
          }
          *data = (void *)bos;			  
		  
          LDEBUG("Received SALIENTWINNERLIST size: %d from destination %d mpierr: %d", size, source, mpierr);
        }
        break;
      case(VISUALEVENTSET):
        {
          MbariVisualEvent::VisualEventSet *set;

          //create string to extract MbariVisualEvent::VisualEventSet to/from  
          stringbuf sb;
          istream  is(&sb);

          int size = min((unsigned int)metaData[2],sizeof(objectBuf));

          //unpack MbariVisualEvent::VisualEventSet into objectBuf
          mpierr = MPI_Unpack(rcvPackBuf, packsize, &position, objectBuf, size, MPI_UNSIGNED_CHAR, comm);	  		  		  

          sb.pubsetbuf(objectBuf, size );  

          //for debug only
          //cout << sb.str();
          //cout << sb.str().size();

          //create MbariVisualEvent::VisualEventSet from stream
          set = new MbariVisualEvent::VisualEventSet(is);	  
			
          *data = (void *)set;			  		  
          LDEBUG("Received MBARIVISUALEVENTSET size: %d from destination %d mpierr: %d", size, source, mpierr);
        }
        break;			
      default:
        LDEBUG("Error: received unknown packet type %d in receiveImage()", type);
        return -1;
      }//end switch
    if(mpierr == MPI_SUCCESS)  return metaData[1]; //return frame number if successfull
  }//end else    

  return -1;
}

