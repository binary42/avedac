#include "mpi.h"
#include "mpe.h"
#include "mpe_log.h"

#include "Image/FilterOps.H"
#include "Image/BitObject.H"
#include "Raster/Raster.H"
#include "Image/ColorOps.H"
#include "Util/StringConversions.H"
#include "Const.H"
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string>
#include <iostream>
#include <fstream>
#include <sstream>

#define NTSC_SD_MAX_HEIGHT	576
#define NTSC_SD_MAX_WIDTH	768

using namespace std;

unsigned char sndPackBuf[sizeof(int)*4 + sizeof(unsigned char)*NTSC_SD_MAX_HEIGHT*NTSC_SD_MAX_WIDTH*3];
unsigned char rcvPackBuf[sizeof(int)*4 + sizeof(unsigned char)*NTSC_SD_MAX_HEIGHT*NTSC_SD_MAX_WIDTH*3];  
char bitObjectBuf[sizeof(int)*4 + sizeof(unsigned char)*NTSC_SD_MAX_HEIGHT*NTSC_SD_MAX_WIDTH];  

enum dataTypes{RGBBYTEIMAGE, BYTEIMAGE, BITOBJECT};

// !returns -1 on error, otherwise send single BitObject and return MPI error code
int sendBitObject(const BitObject& bitobject, int framenum, int dest, int tag, MPI_Comm comm);

// !returns -1 on error, otherwise returns frame number this data comes from
int receiveData(void **data, dataTypes type, int source, int tag, MPI_Comm comm, MPI_Status *status, MPI_Request *r);


int main( int argc, char **argv )
{	
    int id;
	int size = 0;
	BitObject obj;
	BitObject *pobj;
	MPI_Status status;	
	Image < byte > grayim;
	MPI_Request request;

	MPI_Init( &argc, &argv );
    
	MPI_Comm_rank( MPI_COMM_WORLD, &id );
    MPI_Comm_size( MPI_COMM_WORLD, &size );
	 
	if(id == 0) 	{		
		grayim = luminance(Raster::ReadGray(Auto, "bitobject.ppm"));				
		obj.reset(grayim);
		//send data to UpdateEvents
		fprintf(stdout,"BoundingBox = %s\n",toStr(obj.getBoundingBox()).data());fflush(stdout);
		fprintf(stdout,"Centroid = %s\n",toStr(obj.getCentroid()).data());fflush(stdout);
		fprintf(stdout,"Area = %d\n",obj.getArea());fflush(stdout);
		float uxx, uyy, uxy;
		obj.getSecondMoments(uxx,uyy,uxy);fflush(stdout);
		fprintf(stdout,"uxx = %g; uyy = %g; uxy = %g\n",uxx,uyy,uxy);fflush(stdout);
		fprintf(stdout,"major Axis = %g\n", obj.getMajorAxis());fflush(stdout);
		fprintf(stdout,"minor Axis = %g\n", obj.getMinorAxis());fflush(stdout);
		fprintf(stdout,"elongation = %g\n", obj.getElongation());fflush(stdout);
		fprintf(stdout,"orientation angle = %g\n", obj.getOriAngle());fflush(stdout);
		sendBitObject(obj, 0, 1, 0, MPI_COMM_WORLD);	
	}
	else {		
		if(receiveData((void**)&pobj, BITOBJECT, 0, 0, MPI_COMM_WORLD, &status, &request) != -1) {
			fprintf(stdout,"BoundingBox = %s\n",toStr(pobj->getBoundingBox()).data());fflush(stdout);
			fprintf(stdout,"Centroid = %s\n",toStr(pobj->getCentroid()).data());fflush(stdout);
			fprintf(stdout,"Area = %d",pobj->getArea());fflush(stdout);
			float uxx, uyy, uxy;
			pobj->getSecondMoments(uxx,uyy,uxy);fflush(stdout);
			fprintf(stdout,"uxx = %g; uyy = %g; uxy = %g\n",uxx,uyy,uxy);fflush(stdout);
			fprintf(stdout,"major Axis = %g\n", pobj->getMajorAxis());fflush(stdout);
			fprintf(stdout,"minor Axis = %g\n", pobj->getMinorAxis());fflush(stdout);
			fprintf(stdout,"elongation = %g\n", pobj->getElongation());fflush(stdout);
			fprintf(stdout,"orientation angle = %g\n", pobj->getOriAngle());fflush(stdout);
			delete pobj;

		}
	}

	MPI_Barrier(MPI_COMM_WORLD);	     
	MPI_Finalize( );
    return 0;
}

int sendBitObject(const BitObject& bitobject, int framenum, int dest, int tag, MPI_Comm comm)
{  
  int packsize = 0;
  int maxsize;
  int metaData[4];  
  int mpierr;
	
  //create ostream to extract BitObject to
  std::stringbuf sb;
  std::ostream  os(&sb);
  bitobject.writeToStream(os);  
  os.flush();  

  cout << sb.str();
  cout << sb.in_avail();
  
  //initialize metadata
  maxsize = sizeof(sndPackBuf);  
  metaData[0] = BITOBJECT; 
  metaData[1] = framenum;
  metaData[2] = sb.in_avail();
  metaData[3] = 0;


  MPI_Pack(metaData, 4, MPI_INT, sndPackBuf, maxsize, &packsize, comm);      
  MPI_Pack((void *)sb.str().c_str(), sb.in_avail(), MPI_UNSIGNED_CHAR, sndPackBuf, maxsize, &packsize, comm);  
  mpierr = MPI_Send( sndPackBuf, packsize, MPI_PACKED, dest, tag, comm);  
  
  fprintf(stdout, "Sending BITOBJECT maxsize: %d packsize: %d to %d mpierr: %d\n", \
	  maxsize, packsize, dest, mpierr);
  fflush(stdout);
  return mpierr;
}
int receiveData(void **data, dataTypes type, int source, int tag, MPI_Comm comm, MPI_Status *status, MPI_Request *request)
{
  int packsize;
  int metaData[4];  
  int position = 0; 
  int mpierr;
  int exit = 0;

  //create string to extract BitObject to/from  
  std::stringbuf sb;
  std::istream  is(&sb);

  packsize = sizeof(rcvPackBuf);

  if(MPI_Recv( &rcvPackBuf, packsize, MPI_PACKED, source, tag, comm, status ) != MPI_SUCCESS) return -1;  

  position = 0;
   
  MPI_Unpack(rcvPackBuf, packsize, &position, metaData, 4, MPI_INT, comm);

  //create new bitobject
  BitObject *object = new BitObject();	  
		  
  //unpack BitObject into bitObjectBuf
  mpierr = MPI_Unpack(rcvPackBuf, packsize, &position, bitObjectBuf, metaData[2], MPI_UNSIGNED_CHAR, comm);	  		  		  
  
  sb.pubsetbuf((bitObjectBuf), metaData[2]);  

  cout << sb.str();
  cout << sb.in_avail();

  //create bitobject from stream
  object->readFromStream(is);
  *data = (void *)object;			  
		  
  fprintf(stdout, "Received BITOBJECT packsize: %d from destination %d mpierr: %d\n", \
	  packsize, source, mpierr);
  fflush(stdout);
	  
  return metaData[1]; //return frame number  
}



