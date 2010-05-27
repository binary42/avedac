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
  
#include "Parallel/pvisionTCP-defs.H"
#include "Image/fancynorm.H"
#include "Image/Pixels.H"
#include "Image/Image.H"
#include "Image/Transforms.H"
#include "Image/ColorOps.H"
#include "Image/ShapeOps.H"
#include "Image/ImageSet.H"
#include "Image/MathOps.H"
#include "Image/PyramidOps.H"
#include "Util/Timer.H"
#include "Component/ModelManager.H"
#include <signal.h>
#include "Beowulf/Beowulf.H"
static bool goforever = true;  //!< Will turn false on interrupt signal

//! Signal handler (e.g., for control-C)
void terminate(int s) { LERROR("*** INTERRUPT ***"); goforever = false; }

//! Compute a conspicuity map from an image received in a message
void computeCMAP(TCPmessage& msg, const PyramidType ptyp,
                 const float ori, const float coeff,
                 const int slave, bool useAbsVal, nub::soft_ref<Beowulf>& b);

//! Compute a conspicuity map from two images received in a message
void computeCMAP2(TCPmessage& msg, const PyramidType ptyp,
                  const float ori, const float coeff,
                  const int slave,bool useAbsVal, nub::soft_ref<Beowulf>& b);

//! Compute a conspicuity map from an image
void computeCMAP(const Image<float>& fima, const PyramidType ptyp,
                 const float ori, const float coeff,
                 const int slave, nub::soft_ref<Beowulf>& b, 
                 const int32 id, bool useAbsVal);

// ######################################################################
// ##### Global options:
// ######################################################################
#define sml        2
#define delta_min  3
#define delta_max  4
#define level_min  0
#define level_max  2
#define maxdepth   level_max + delta_max + 1
#define normtyp    VCXNORM_FANCY

// ######################################################################
int main(const int argc, const char **argv)
{
  MYLOGVERB = LOG_INFO;

  // instantiate a model manager:
  ModelManager manager("MBARI Parallel Vision TCP - Slave");

  // Instantiate our various ModelComponents:
  nub::soft_ref<Beowulf>
    beo(new Beowulf(manager, "Beowulf Slave", "BeowulfSlave", false));
  manager.addSubComponent(beo);

  // Parse command-line:
  if (manager.parseCommandLine(argc, argv, "", 0, 0) == false) return(1);

  // setup signal handling:
  signal(SIGHUP, terminate); //signal(SIGINT, terminate);
  signal(SIGQUIT, terminate); //signal(SIGTERM, terminate);
  srand48(time(0) + getpid());          // init random numbers

  // various processing inits:
  TCPmessage rmsg;            // message being received and to process
  TCPmessage smsg;            // message being sentx

  // let's get all our ModelComponent instances started:
  manager.start();

  // wait for data and process it:
  while(goforever)
    {
      int32 rframe, raction, rnode = -1;  // receive from any node
      if (beo->receive(rnode, rmsg, rframe, raction, 5))  // 5 msec wait
        {
          //LINFO("Frame %d, action %d from node %d", rframe, raction, rnode);
          // select processing branch based on frame number:
          switch(raction)
            {
            case BEO_INIT:       // ##############################
  	      break;
            case BEO_LUMINANCE:  // ##############################
              computeCMAP(rmsg, Gaussian5, 0.0, 1, 0, true, beo);
              break;
            case BEO_REDGREEN:   // ##############################
              computeCMAP2(rmsg, Gaussian5, 0.0, 1, 1, true, beo);
              break;
            case BEO_BLUEYELLOW: // ##############################
              computeCMAP2(rmsg, Gaussian5, 0.0, 1, 2, true, beo);
              break;
            case BEO_ORI0:       // ##############################
              computeCMAP(rmsg, Oriented9, 0.0, 1, 3, false, beo);
              break;
            case BEO_ORI45:      // ##############################
              computeCMAP(rmsg, Oriented9, 45.0, 1, 4, false, beo);
              break;
            case BEO_ORI90:      // ##############################
              computeCMAP(rmsg, Oriented9, 90.0, 1, 5, false, beo);
              break;
            case BEO_ORI135:     // ##############################
              computeCMAP(rmsg, Oriented9, 135.0, 1, 6, false, beo);
              break;
            default: // ##############################
              LERROR("Bogus action %d -- IGNORING.", raction);
              break;
            }
        }
    }
  
  // we got broken:
  manager.stop();
  return 0;
}

// ######################################################################
void computeCMAP(TCPmessage& msg, const PyramidType ptyp,
                 const float ori, const float coeff,
                 const int mapn, bool useAbsVal,
                 nub::soft_ref<Beowulf>& b)
{
  Image<byte> ima = msg.getElementByteIma();  
  Image<float> fima = ima; // convert to float

  computeCMAP(fima, ptyp, ori, coeff, mapn, b, msg.getID(), useAbsVal);
}

// ######################################################################
void computeCMAP2(TCPmessage& msg, const PyramidType ptyp,
                  const float ori, const float coeff,
                  const int mapn, bool useAbsVal,
                  nub::soft_ref<Beowulf>& b)
{
  Image<byte> ima1 = msg.getElementByteIma();
  Image<byte> ima2 = msg.getElementByteIma();
  Image<float> fima = ima1 - ima2;

  computeCMAP(fima, ptyp, ori, coeff, mapn, b, msg.getID(), useAbsVal);
}

// ######################################################################
void computeCMAP(const Image<float>& fima, const PyramidType ptyp,
                 const float ori, const float coeff,
                 const int mapn, nub::soft_ref<Beowulf>& b, const int32 id, 
                 bool useAbsVal)
{
  float mi, ma;
		
  // compute pyramid:
  ImageSet<float> pyr = buildPyrGeneric(fima, level_min, maxdepth, ptyp, ori);
    
  // alloc conspicuity map and clear it:
  Image<float> cmap(pyr[sml].getDims(), ZEROS);
    
  LDEBUG("CMAP W:%d H:%d image width:%d height:%d", cmap.getWidth(), cmap.getHeight(), fima.getWidth(), fima.getHeight());
  
  for (int delta = delta_min; delta <= delta_max; delta ++)
    for (int lev = level_min; lev <= level_max; lev ++)
      {
        Image<float> submap = centerSurround(pyr, lev, (lev + delta), useAbsVal);
        getMinMax(submap, mi, ma);
        LDEBUG("mapnumber:%d(%d,%d): raw range [%f .. %f]", mapn, lev, lev + delta, mi, ma);
        
        submap = downSize(submap, cmap.getWidth(), cmap.getHeight());
        LDEBUG("downSizing %dx%d",cmap.getWidth(), cmap.getHeight());        
        
        if (normtyp == VCXNORM_FANCY) {
          submap = maxNormalize(submap, 0.0f, 0.0f, normtyp);
          LDEBUG("%d(%d,%d): applying Fancy (0.0 .. 0.0)", mapn, lev, lev + delta);
        }
        else {
          submap = maxNormalize(submap, MAXNORMMIN, MAXNORMMAX, normtyp);
          LDEBUG("%d(%d,%d): applying (%f .. %f)", mapn, lev, lev + delta, MAXNORMMIN, MAXNORMMAX);
        }
        
        getMinMax(submap, mi, ma);
        LDEBUG("mapnumber:%d(%d,%d): final range [%f .. %f]", mapn, lev, lev + delta, mi, ma);
        
        cmap += submap;
      }
  
  // multiply by conspicuity coefficient:
  cmap *= coeff;
  
  if (normtyp == VCXNORM_FANCY) {
    LINFO("%d applying Fancy(0.0 .. 0.0)", mapn);
    cmap = maxNormalize(cmap, 0.0f, 0.0f, normtyp);
  }
  else
    {
      LINFO("%d applying (%f .. %f)", mapn, MAXNORMMIN, MAXNORMMAX);
      cmap = maxNormalize(cmap, MAXNORMMIN, MAXNORMMAX, normtyp);
    } 
  getMinMax(cmap, mi, ma);
  LDEBUG("Mapnumber:%d: final range [%f .. %f]", mapn, mi, ma);        
  
  // send off resulting conspicuity map to master:
  TCPmessage smsg(id, BEO_CMAP | (mapn << 16));
  smsg.addImage(cmap);
  
  // let's compute a crude estimate of our time to idle:
  int qlen = b->nbReceived();  // how many messages to process?
  
  // send off the result:
  b->send(-1, smsg);
}
