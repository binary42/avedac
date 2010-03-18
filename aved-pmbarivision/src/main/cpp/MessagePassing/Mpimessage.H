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
