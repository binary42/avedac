// getsalientregions.h: interface for the GetSalientRegions class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(GETSALIENTREGIONS_H)
#define GETSALIENTREGIONS_H

#include "MessagePassing/Mpidef.H"
#include "Stages/Stage.H"
#include "Image/Point2D.H"
#include "Beowulf/Beowulf.H"
#include "Neuro/StdBrain.H"
#include "Image/Geometry2D.H"
#include "DetectionAndTracking/MbariFunctions.H"

// ######################################################################
//! Runs the longest stage. This stage communicates with workers running on
// Beowulf nodes to run an faster version of saliency algorithm. 
class GetSalientRegionsStage : public Stage  
{
public:
  // !constructor
  GetSalientRegionsStage(MPI_Comm mastercomm, const char *name, \
			 const int argc, const char **argv,
			 nub::soft_ref<Beowulf> &beo,
			 const float &maxEvolveTime,
			 const uint &maxNumSalSpots,
			 bool &fastBeoSaliency);

  // !destructor
  virtual ~GetSalientRegionsStage();

  // !custom GetSalientRegionsStage run method.
  virtual void runStage();

  // !custom GetSalientRegionsStage initStage method.
  virtual void initStage();
      
  // !custom shutdown. Sends MSG_EXIT to stages pending on this stage
  virtual void shutdown();

private:
  
  //! A fast method to scan iteratively for winning locations
  //! returns list of SalientWinners of the most salient points
  /*! @param sml pyramid level of the saliency map
   * @param sm the float scaled down saliency map  to scan for winners
   * @param color intensity map
   @param color conspicuity map  	
   @param orientation conspicuity map  	
   @param maxEvolveTime the maximum evolution time for the saliency map
   @param maxNumSalSpots the maximum number of salient locations extracted*/ 									
  list<WTAwinner> getSalientWinnersMaxFast(int sml,
					   Image<float> &sm,
					   Image<float> &intensity,
					   Image<float> &color,
					   Image<float> &orientation,
					   float maxEvolveTime,
					   int maxNumSalSpots);
  
  
  // ! runs saliency filter and creates list of winners
  std::list<WTAwinner> getWinners(const Image< PixRGB<byte> > &img, int framenum);
  
  //! function to receive results from slaves
  int receiveCMAPS(nub::soft_ref<Beowulf>& beo, Image<float> *cmap,
		   int32 *cmapframe);
  
  //! sendImage to vision slaves
  void sendImage(const Image< PixRGB<byte> >& img, int framenum);
  
  nub::soft_ref<Beowulf> itsBeo;
  uint itsMaxNumSalSpots;
  float itsMaxEvolveTime;
  bool itsFastBeoSaliency;
  const char **itsArgv;
  const int itsArgc; 
};

#endif // !defined(GETSALIENTREGIONS_H)
