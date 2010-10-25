/*!@file Neuro/WinnerTakeAll.C 2D winner-take-all network */

// //////////////////////////////////////////////////////////////////// //
// The iLab Neuromorphic Vision C++ Toolkit - Copyright (C) 2001 by the //
// University of Southern California (USC) and the iLab at USC.         //
// See http://iLab.usc.edu for information about this project.          //
// //////////////////////////////////////////////////////////////////// //
// Major portions of the iLab Neuromorphic Vision Toolkit are protected //
// under the U.S. patent ``Computation of Intrinsic Perceptual Saliency //
// in Visual Environments, and Applications'' by Christof Koch and      //
// Laurent Itti, California Institute of Technology, 2001 (patent       //
// pending; application number 09/912,225 filed July 23, 2001; see      //
// http://pair.uspto.gov/cgi-bin/final/home.pl for current status).     //
// //////////////////////////////////////////////////////////////////// //
// This file is part of the iLab Neuromorphic Vision C++ Toolkit.       //
//                                                                      //
// The iLab Neuromorphic Vision C++ Toolkit is free software; you can   //
// redistribute it and/or modify it under the terms of the GNU General  //
// Public License as published by the Free Software Foundation; either  //
// version 2 of the License, or (at your option) any later version.     //
//                                                                      //
// The iLab Neuromorphic Vision C++ Toolkit is distributed in the hope  //
// that it will be useful, but WITHOUT ANY WARRANTY; without even the   //
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR      //
// PURPOSE.  See the GNU General Public License for more details.       //
//                                                                      //
// You should have received a copy of the GNU General Public License    //
// along with the iLab Neuromorphic Vision C++ Toolkit; if not, write   //
// to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,   //
// Boston, MA 02111-1307 USA.                                           //
// //////////////////////////////////////////////////////////////////// //
//
// Primary maintainer for this file: Laurent Itti <itti@usc.edu>
// $HeadURL: svn://iLab.usc.edu/trunk/saliency/src/Neuro/WinnerTakeAll.C $
// $Id: WinnerTakeAll.C 10316 2008-03-11 18:34:52Z lior $
//

#include "Neuro/WinnerTakeAllStdMbari.H"

#include "Channels/ChannelOpts.H" // for OPT_LevelSpec
#include "Component/GlobalOpts.H"
#include "Component/OptionManager.H"
#include "Image/Image.H"
#include "Image/MathOps.H"
#include "Image/Transforms.H" // for chamfer34()
#include "Media/MediaSimEvents.H"
#include "Neuro/NeuroOpts.H"
#include "Neuro/WTAwinner.H"
#include "Neuro/NeuroSimEvents.H"
#include "Simulation/SimEventQueue.H"
#include "Simulation/SimEvents.H"
#include "Transport/FrameInfo.H"
#include "Transport/FrameOstream.H"
#include "Util/TextLog.H"
#include "Util/log.H"
#include "Util/sformat.H"
#include "rutz/trace.h"
 

// ######################################################################
// ######################################################################
// ########## WinnerTakeAllStdMbari implementation
// ######################################################################
// ######################################################################

WinnerTakeAllStdMbari::WinnerTakeAllStdMbari(OptionManager& mgr,
                                   const std::string& descrName,
                                   const std::string& tagName) :
  WinnerTakeAllAdapter(mgr, descrName, tagName),
  itsNeurons(),
  itsMaxVal(0.f), itsGIN(), itsT(), itsGleak(1.0e-8F), itsGinh(1.0e-2F), itsGinput(5.0e-8F)
{
GVX_TRACE(__PRETTY_FUNCTION__);
  itsGIN.setGleak(itsGleak);
}
// ######################################################################
WinnerTakeAllStdMbari::~WinnerTakeAllStdMbari()
{
GVX_TRACE(__PRETTY_FUNCTION__);
}

// ######################################################################
void WinnerTakeAllStdMbari::reset1()
{
GVX_TRACE(__PRETTY_FUNCTION__);
  itsNeurons.freeMem();
  itsGleak = 1.0e-8F;
  itsGinh = 1.0e-2F;
  itsGinput = 5.0e-8F;
  itsT = SimTime::ZERO();

  WinnerTakeAllAdapter::reset1();
}
//! Returns the maxval in the saliency map for the last evolve
float WinnerTakeAllStdMbari::getMaxVal()
{
  return itsMaxVal;
}

// ######################################################################
void WinnerTakeAllStdMbari::input(const Image<float>& in)
{
GVX_TRACE(__PRETTY_FUNCTION__);
  if (itsNeurons.initialized() == false) {
    // first input, let's initialize our array
    itsNeurons.resize(in.getDims(), ZEROS);

    Image<LeakyIntFire>::iterator
      nptr = itsNeurons.beginw(), stop = itsNeurons.endw();
    while (nptr != stop)
      {
        nptr->setGleak(itsGleak);
        nptr->setG(0.0F, itsGinh);
        ++nptr;
      }
  }
}

// ######################################################################
void WinnerTakeAllStdMbari::doEvolve(const SimTime& t, Point2D<int>& winner)
{
GVX_TRACE(__PRETTY_FUNCTION__);
  winner.i = -1;
  const int w = itsNeurons.getWidth();
  const int h = itsNeurons.getHeight();

  // the array of neurons receive excitatory inputs from outside.
  // here we update the inputs and let the neurons evolve; here we
  // need to run this loop time step by time step, since this is a
  // feedback network, so we have code similar to that in
  // LeakyIntFire::evolve() to figure out how many time steps to run:
  const SimTime dt =
    SimTime::computeDeltaT((t - itsT), itsGIN.getTimeStep());

  for (SimTime tt = itsT; tt < t; tt += dt)
    {
      Image<LeakyIntFire>::iterator nptr = itsNeurons.beginw();
      Image<float>::const_iterator inptr = itsInputCopy.begin();
      for (int j = 0 ; j < h; j ++)
        for (int i = 0; i < w; i ++)
          {
            nptr->input(itsGinput * (*inptr++));
            if (nptr->integrate(tt)) { winner.i = i; winner.j = j; itsMaxVal = nptr->getV();}
            nptr->setG(0.0F, 0.0F);  // only leak conductance
            ++nptr;
          }

      // if there is a winner, the winner triggers the global inhibition:
      if (winner.i > -1) itsGIN.setG(itsGleak * 10.0F, 0.0F);

      // when the global inhibition fires, it triggers inhibitory
      // conductances for one time step in the array of excited
      // neurons, shuts off excitatory conductances, and turns itself
      // off:
      if (itsGIN.integrate(tt)) inhibit();
    }
  itsT = t;
}

// ######################################################################
Image<float> WinnerTakeAllStdMbari::getV() const
{
GVX_TRACE(__PRETTY_FUNCTION__);
  Image<float> result(itsNeurons.getDims(), NO_INIT);

  Image<float>::iterator dptr = result.beginw(), stop = result.endw();
  Image<LeakyIntFire>::const_iterator nptr = itsNeurons.begin();
  while(dptr != stop) *dptr++ = (nptr++)->getV();

  return result;
}

// ######################################################################
void WinnerTakeAllStdMbari::inhibit()
{
GVX_TRACE(__PRETTY_FUNCTION__);
  Image<LeakyIntFire>::iterator
    nptr = itsNeurons.beginw(), stop = itsNeurons.endw();
  while(nptr != stop) (nptr++)->setG(0.0F, itsGinh);
  itsGIN.setG(0.0F, 0.0F);
  LDEBUG("WTA inhibition firing...");
}

// ######################################################################
void WinnerTakeAllStdMbari::saccadicSuppression(const bool on)
{
GVX_TRACE(__PRETTY_FUNCTION__);
  if (itsUseSaccadicSuppression.getVal() == false) return;
  if (on) inhibit();
  LINFO("------- WTA saccadic suppression %s -------", on ? "on":"off");
}

// ######################################################################
void WinnerTakeAllStdMbari::blinkSuppression(const bool on)
{
GVX_TRACE(__PRETTY_FUNCTION__);
  if (itsUseBlinkSuppression.getVal() == false) return;
  if (on) inhibit();
  LINFO("------- WTA blink suppression %s -------", on ? "on":"off");
}


// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
