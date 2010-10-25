/*!@file Neuro/WinnerTakeAll.H Header file for winner-take-all network */

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
// $HeadURL: svn://iLab.usc.edu/trunk/saliency/src/Neuro/WinnerTakeAll.H $
// $Id: WinnerTakeAll.H 10316 2008-03-11 18:34:52Z lior $
//

#ifndef WINNERTAKEALLSTDMBARI_H_DEFINED
#define WINNERTAKEALLSTDMBARI_H_DEFINED

#include "Component/ModelComponent.H"
#include "Component/ModelParam.H"
#include "Image/Image.H"
#include "Image/LevelSpec.H"
#include "Image/Point2DT.H"
#include "Neuro/LeakyIntFire.H"
#include "Neuro/LeakyIntFireAdp.H"
#include "Neuro/WTAwinner.H"
#include "Neuro/WinnerTakeAll.H"
#include "Simulation/SimModule.H"
#include "VFAT/segmentImageMC2.H"
#include "Util/SimTime.H"
 
// ######################################################################
//! The standard winner-take-all
// ######################################################################
/*! This is a trivial winner-take-all implementation, based on a 2D
  layer of LeakyIntFire neurons and a unique global inhibitory
  neuron. All neurons in the layer charge up in parallel; whenever one
  reaches threshold, it activates the inhibitory interneuron (which is
  connected to all neurons in the layer), which in turn resets all
  neurons in the layer.  WinnerTakeAllStdMbari is an Image<LeakyIntFire>
  and inherits all the standard methods of class Image. To avoid
  confusion, we here add explicit input() and getV() methods (rather
  than providing conversion functions between LeakyIntFire and float,
  which could make the process more transparent but also possibly more
  confusing). The user has the responsibility of calling evolve()
  often to ensure that the difference equations in the LeakyIntegrator
  neurons run well. */
class WinnerTakeAllStdMbari : public WinnerTakeAllAdapter
{
public:
  //! Ininitialized constructor
  /*! The WTA will be resized and initialized the first time input() is
    called */
  WinnerTakeAllStdMbari(OptionManager& mgr,
                   const std::string& descrName = "Winner-Take-All Std",
                   const std::string& tagName = "WinnerTakeAllStdMbari");

  //! Destructor
  virtual ~WinnerTakeAllStdMbari();

  //! Reset to initial state just after construction
  virtual void reset1();
 
  //! Set new input currents for all neurons
  /*! This will initialize and resize the network if the network is
    currently uninitialized (e.g., just after construction or reset()). */
  virtual void input(const Image<float>& in);

  //! Return all our membrane potential voltages as an Image<float>
  virtual Image<float> getV() const;

  //! Turn saccadic suppression on/off
  virtual void saccadicSuppression(const bool on);

  //! Turn blink suppression on/off
  virtual void blinkSuppression(const bool on);

  //! Integrate inputs until time t (in s) and update membrane potentials
  virtual void doEvolve(const SimTime& t, Point2D<int>& winner);

  //! Returns the maxval in the saliency map for the last evolve
  float getMaxVal();

  Image<LeakyIntFire> itsNeurons;
  LeakyIntFire itsGIN;               // global inhibition neuron
  SimTime itsT;                      // keep track of time
  float itsGleak, itsGinh;           // in Siemens
  float itsGinput;                   // in Siemens
  float itsMaxVal;	
  void inhibit();                    // inhibit the whole layer
};


#endif

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
