/*!@file MbariImageCache.H implements an image cache based on the iLab 
 * Neuromorphic Vision C++ Toolkit. This one cache 
 */
// //////////////////////////////////////////////////////////////////// //
// The iLab Neuromorphic Vision C++ Toolkit - Copyright (C) 2000-2003   //
// by the University of Southern California (USC) and the iLab at USC.  //
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
// Primary maintainer for this file: Dirk Walther <walther@caltech.edu>
// $HeadURL: svn://iLab.usc.edu/trunk/saliency/src/Image/ImageCache.H $
// $Id: MbariImageCache.H,v 1.1 2007/09/26 23:06:54 dcline Exp $
//

#ifndef MBARIIMAGECACHE_H_DEFINED
#define MBARIIMAGECACHE_H_DEFINED

#include "Util/Promotions.H"

#include "Image/MbariImage.H"

#include <deque>


// ######################################################################
//! base class for image caches that do computations on the fly
/*! This base class has no op doWhenAdd and doWhenRemove functions
  that should be overridden in classes derived from this one.*/
template <class T>
class MbariImageCache
{
public:
  //! Constructor
  /*! @param maxSize the maximum size of the cache. If this size is exceeded,
    images are popped off the front of the cache and disregarded for the
    computation of the mean. If maxSize = 0, the cache is not limited.*/
  MbariImageCache(uint maxSize = 0);

  //! Destructor
  virtual ~MbariImageCache();

  //! Set maximum number of images in the cache
  /*! If the cache currently has more images than the new specified
    maximum, it will be truncated to that maximum. */
  inline void setMaxSize(const uint maxSize);

  //! Get maximum number of images in the cache
  inline uint getMaxSize() const;

  //! add image to cache - if the cache gets to big, old images are popped off
  inline void push_back(const MbariImage<T>& img);

  //! pop the front Image (oldest) off the cache and return it
  inline MbariImage<T> pop_front();

  //! access the last Image (newest) in the queue
  inline MbariImage<T>& back() const;

  //! access the first Image (oldest) in the queue
  inline MbariImage<T>& front();

  //! Get image from a given level.
  inline const MbariImage<T> getImage(const uint lev) const;

  //! Get image from a given level (shorthand for getImage()).
  inline const MbariImage<T> operator[](const uint lev) const;

  //! return the current size of the cache
  /*! This may be smaller than the maximum size specified at
    construction or with setMaxSize(), if the cache is not full yet. */
  inline uint size() const;

  //! true if the cache contains no elements
  inline bool empty() const;

  //! clear the cache, preserving its maxSize
  inline void clear();

protected:
  //! checks whether the cache has become too big and removes old entries
  inline void popOffOld();

  //! called when an image is added - override in your derived classes!
  /*! in ImageCache, this function is no op*/
  inline void doWhenAdd(const MbariImage<T>& img);

  //! called when an image is removed - override in your derived classes
  /*! in ImageCache, this function is no op*/
  inline void doWhenRemove(const MbariImage<T>& img);

  //! the maximum size of images to be stored
  uint itsMaxSize;

  //! the cache of the images
  std::deque< MbariImage<T> > itsCache;
};

// ######################################################################
// ##### Implementation of ImageCache<T>
// ######################################################################
template <class T> inline
MbariImageCache<T>::MbariImageCache(uint maxSize)
  : itsMaxSize(maxSize)
{}

// ######################################################################
template <class T> inline
MbariImageCache<T>::~MbariImageCache()
{}

// ######################################################################
template <class T> inline
void MbariImageCache<T>::setMaxSize(const uint maxSize)
{
  itsMaxSize = maxSize;
  // truncate if necessary:
  popOffOld();
}

// ######################################################################
template <class T> inline
uint MbariImageCache<T>::getMaxSize() const
{ return itsMaxSize; }

// ######################################################################
template <class T> inline
void MbariImageCache<T>::push_back(const MbariImage<T>& img)
{
  doWhenAdd(img);
  itsCache.push_back(img);
  popOffOld();
  return;
}

// ######################################################################
template <class T> inline
void MbariImageCache<T>::popOffOld()
{
  // now pop off old images
  if (itsMaxSize == 0) return;

  while (itsCache.size() > itsMaxSize)
    pop_front();

  return;
}

// ######################################################################
template <class T> inline
MbariImage<T> MbariImageCache<T>::pop_front()
{
  ASSERT(!itsCache.empty());
  MbariImage<T> ret = itsCache.front();
  doWhenRemove(ret);
  itsCache.pop_front();
  return ret;
}

// ######################################################################
template <class T> inline
MbariImage<T>& MbariImageCache<T>::back() const
{
  ASSERT(!itsCache.empty());
  return itsCache.back();
}

// ######################################################################
template <class T> inline
MbariImage<T>& MbariImageCache<T>::front()
{
  ASSERT(!itsCache.empty());
  return itsCache.front();
}

// ######################################################################
template <class T> inline
const MbariImage<T> MbariImageCache<T>::getImage(const uint lev) const
{
  ASSERT(lev < itsCache.size());
  return itsCache[lev];
}

// ######################################################################
template <class T> inline
const MbariImage<T> MbariImageCache<T>::operator[](const uint lev) const
{ return getImage(lev); }

// ######################################################################
template <class T> inline
uint MbariImageCache<T>::size() const
{ return itsCache.size(); }

// ######################################################################
template <class T> inline
bool MbariImageCache<T>::empty() const
{ return itsCache.empty(); }

// ######################################################################
template <class T> inline
void MbariImageCache<T>::clear()
{
  // hopefully, overloads of pop_front() will take care of updating any
  // additional data members (like the sliding average image in
  // ImageCache):
  while(size()) this->pop_front();
}

// ######################################################################
template <class T> inline
void MbariImageCache<T>::doWhenAdd(const MbariImage<T>& img)
{ }

// ######################################################################
template <class T> inline
void MbariImageCache<T>::doWhenRemove(const MbariImage<T>& img)
{ }

#endif

// ######################################################################
/* So things look consistent in everyone's emacs... */
/* Local Variables: */
/* indent-tabs-mode: nil */
/* End: */
