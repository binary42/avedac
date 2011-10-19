/*
 * @(#)EventImageCache.java
 * 
 * Copyright 2011 MBARI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */



package org.mbari.aved.ui.model;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.BoundingBox;
import aved.model.EventObject;

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.ui.exceptions.FrameOutRangeException;
import org.mbari.aved.ui.exceptions.MissingFrameException;

//~--- JDK imports ------------------------------------------------------------

import com.sun.media.jai.codec.JPEGEncodeParam;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;

/**
 * Singleton class that executes a SwingWorker to grab
 * the best BufferedImage for each AVED event.
 * Depending on how many images, this can take
 * a long time to load so it's put in a separate
 * SwingWorker class
 */
public class EventImageCache {
    int cacheNextIndex = 0;

    /**
     * Used to kill the SwingWorker
     */
    boolean                           iKeepRunning       = true;
    private List<EventImageCacheData> imageCacheDataList = null;
    int[]                             indexsToCache      = null;
    int                               loadingCacheIndex  = 0;
    int                               loadingIndex       = -1;
    final String                      syncArrays         = "syncArrays";
    private ImageLoader               thread             = null;
    int                               totalLoaded        = 0;

    /**
     * Event List model for retrieving event images
     * to populate the cache with.
     */
    EventListModel eventListModel;

    /**
     * Constructor. Resets the cache
     */
    public EventImageCache() {
        reset();
    }

    /**
     * Resets the cache
     *
     *  Call within block synced by: syncArrays
     */
    public void reset() {
        synchronized (syncArrays) {
            iKeepRunning = false;

            if (thread != null) {
                thread.cancel(true);
            }

            cacheNextIndex    = 0;
            loadingCacheIndex = 0;
            totalLoaded       = 0;

            if (imageCacheDataList != null) {
                imageCacheDataList.clear();
                imageCacheDataList = null;
            }

            loadingIndex  = -1;
            indexsToCache = null;
        }
    }

    /**
     * Starts the image loader thread to load new image cache data
     */
    public void loadImageCache(EventListModel eventListModel, boolean loadByFrame) throws Exception {
        reset();

        try {
            if (eventListModel.getSize() > 0) {

                // System.out.println("Reloading image cache: " + eventListModel.getSize());
                this.eventListModel = eventListModel;
                indexsToCache       = new int[eventListModel.getSize()];

                int size = eventListModel.getSize();

                synchronized (syncArrays) {
                    imageCacheDataList = new ArrayList<EventImageCacheData>();

                    for (int i = 0; i < size; i++) {
                        EventObjectContainer c    = (EventObjectContainer) this.eventListModel.getElementAt(i);
                        EventImageCacheData  data = new EventImageCacheData(c);

                        // Add the new data to my cache
                        imageCacheDataList.add(data);
                    }
                }

                update();
                iKeepRunning = true;
                thread       = new ImageLoader(this, eventListModel);
                thread.loadByFrame = loadByFrame;
                thread.execute();
            } else {
                System.out.println("Image cache empty - no images to load");
            }
        } catch (Exception ex) {
            Logger.getLogger(EventImageCache.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     *  Remove cache at index.
     * @param zero-based index to remove
     */
    public void remove(int index) {
        if ((index >= 0) && (index < imageCacheDataList.size())) {
            imageCacheDataList.remove(index);
        }
    }

    /*
     *  Add cache at index.
     * @param zero-based index to add
     * @param element element to add
     */
    public void add(int index, EventImageCacheData element) {
        synchronized (syncArrays) {
            imageCacheDataList.add(index, element);

            try {
                grabImage(index);
            } catch (FrameOutRangeException ex) {
                Logger.getLogger(EventImageCache.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /** Remove cache using an arraylist of indexes */
    public void removeIndexes(ArrayList<Integer> l) {
        iKeepRunning = false;

        Iterator<Integer>              i          = l.iterator();
        ArrayList<EventImageCacheData> collection = new ArrayList<EventImageCacheData>(l.size());

        synchronized (syncArrays) {
            try {
                while (i.hasNext()) {
                    EventImageCacheData c = imageCacheDataList.get((int) i.next());

                    collection.add(c);
                }
            } catch (IndexOutOfBoundsException exception) {
                exception.printStackTrace();
            }

            imageCacheDataList.removeAll(collection);
        }

        iKeepRunning = true;
        update();
        System.out.println("BufferedImageCache removed new size:" + imageCacheDataList.size() + " collection size:"
                           + l.size());
    }

    void update() {

        // Clear out all the old stuff to cache.
        synchronized (syncArrays) {
            for (int i = 0; i < indexsToCache.length; i++) {

                // Negative means don't cache.
                // so assign an index to cache new images in case
                // we need to
                indexsToCache[i] = i;
            }

            // Reset for the real running loop.
            cacheNextIndex = 0;
        }
    }

    /**
     * Creates the best cropped image of an event. Saves the event
     * to a jpg on disk. An exception is thrown if the image file
     * cannot be found
     * @param data
     * @return true if the cropped image is created
     * @throws org.mbari.aved.ui.exceptions.MissingFrameException
     */
    private boolean createBestCroppedImageOfEvent(EventImageCacheData data)
            throws MissingFrameException, FrameOutRangeException {
        PlanarImage original = loadImage(data.getRawImageSource());

        if (original != null) {
            return createCroppedImageOfEvent(original, data, data.getEvent());
        }

        return false;
    }

    /**
     * Creates the best cropped image of an event. Saves the event
     * to a jpg on disk. An exception is thrown if the image file
     * cannot be found
     * @param data image cache data to store the image data in
     * @param bestEvtObj Object to crop
     * @return
     * @throws org.mbari.aved.ui.exceptions.MissingFrameException
     */
    public static boolean createSquaredImageOfEvent(EventImageCacheData data, EventObject bestEvtObj)
            throws MissingFrameException, FrameOutRangeException {

        // Get the best frame number and event that corresponds to it
        File source = data.getRawImageSource();

        // Load the image that corresponds to the best frame if it exists
        if ((source != null) && source.exists() && source.getAbsoluteFile().canRead()) {
            try {

                // This is a brute force way to check if the file
                // is still being written to because of file locking
                // inconsistencies
                long i = source.length();

                Thread.sleep(4);

                long j = source.length();

                if (i != j) {
                    return false;
                }

                // Load the image that corresponds to the best frame
                PlanarImage original = JAI.create("fileload", source.toString());

                // Create a ParameterBlock with information for the cropping.
                ParameterBlock pb = new ParameterBlock();

                pb.addSource(original);

                // Calculate the cropping coordinates from the bounding box
                BoundingBox b       = bestEvtObj.getBoundingBox();
                int         xorigin = b.getLowerLeftX();
                int         yorigin = b.getUpperRightY();
                int         width   = b.getUpperRightX() - b.getLowerLeftX();
                int         height  = b.getLowerLeftY() - b.getUpperRightY();

                // If the clip bounds are beyond the original image size, adjust
                if (xorigin + width > original.getWidth()) {
                    width = original.getWidth() - xorigin;
                }

                if (yorigin + height > original.getHeight()) {
                    height = original.getHeight() - yorigin;
                }

                // If width or height is zero, adjust to 1 to avoid cropping error
                if (width == 0) {
                    width = 1;
                }

                if (height == 0) {
                    height = 1;
                }

                pb.add((float) xorigin);    // x origin
                pb.add((float) yorigin);    // y origin
                pb.add((float) width);      // width
                pb.add((float) height);     // height

                File outputFile = data.getImageSource();

                // If the file already exists, then return
                if (outputFile.exists()) {
                    return true;
                }

                PlanarImage output = null;

                // Create the output image by cropping the input image.JAI
                output = JAI.create("crop", pb, null);

                // Now square up the image for classification
                float yScale = 0.f;
                float xScale = 0.f;

                width  = output.getWidth();
                height = output.getHeight();

                if (width > height) {
                    xScale = 1.0f;
                    yScale = (float) width / (float) height;
                } else {
                    xScale = (float) height / (float) width;
                    yScale = 1.0f;
                }

                ParameterBlockJAI paramScale = new ParameterBlockJAI("Scale");

                paramScale.addSource(output);
                paramScale.setParameter("xScale", xScale);
                paramScale.setParameter("yScale", yScale);
                paramScale.setParameter("interpolation", Interpolation.getInstance(Interpolation.INTERP_NEAREST));

                // hint with border extender
                RenderingHints hint = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                          BorderExtender.createInstance(BorderExtender.BORDER_COPY));

                // scale
                PlanarImage scaledImg = JAI.create("Scale", paramScale, hint);

                // Store the cropped image in jpg format in the best quality
                JPEGEncodeParam param = new JPEGEncodeParam();

                param.setQuality(1.0f);

                if ((outputFile != null)
                        && (JAI.create("filestore", scaledImg.getAsBufferedImage(), outputFile.toString(), "jpeg",
                                       param) != null)) {
                    output.dispose();

                    return true;
                }
            } catch (InterruptedException ex) {
                return false;
            } catch (IllegalArgumentException e) {}
        }

        return false;
    }

    /**
     * Load the image from a given file source if it exists
     * @param source file source
     * @return image or null if doesn't exist of cannot be read
     */
    public static PlanarImage loadImage(File source) {

        // 
        if ((source != null) && source.exists() && source.getAbsoluteFile().canRead()) {
            try {

                // This is a brute force way to check if the file
                // is still being written to because of file locking
                // inconsistencies
                long i = source.length();

                Thread.sleep(4);

                long j = source.length();

                if (i != j) {
                    return null;
                }

                // Load the image that corresponds to the best frame
                PlanarImage original = JAI.create("fileload", source.toString());

                return original;
            } catch (InterruptedException ex) {
                Logger.getLogger(EventImageCache.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException e) {}
        }

        return null;
    }

    /**
     * Creates the best cropped image of an event. Saves the event
     * to a jpg on disk. An exception is thrown if the image file
     * cannot be found
     * @param original planar image to crop event image from
     * @param data image cache data to store the image data in
     * @param bestEvtObj Object to crop
     * @return
     * @throws org.mbari.aved.ui.exceptions.MissingFrameException
     */
    public static boolean createCroppedImageOfEvent(PlanarImage original, EventImageCacheData data,
            EventObject bestEvtObj)
            throws MissingFrameException, FrameOutRangeException {

        // Load the image that corresponds to the best frame
        try {
            File outputFile = data.getImageSource();

            // If the file already exists, then return
            if (outputFile.exists()) {
                return true;
            }

            // Create a ParameterBlock with information for the cropping.
            ParameterBlock pb = new ParameterBlock();

            pb.addSource(original);

            // Calculate the cropping coordinates from the bounding box
            BoundingBox b       = bestEvtObj.getBoundingBox();
            int         xorigin = b.getLowerLeftX();
            int         yorigin = b.getUpperRightY();
            int         width   = b.getUpperRightX() - b.getLowerLeftX();
            int         height  = b.getLowerLeftY() - b.getUpperRightY();

            // If the clip bounds are beyond the original image size, adjust
            if (xorigin + width > original.getWidth()) {
                width = original.getWidth() - xorigin;
            }

            if (yorigin + height > original.getHeight()) {
                height = original.getHeight() - yorigin;
            }

            // If width or height is zero, adjust to 1 to avoid cropping error
            if (width == 0) {
                width = 1;
            }

            if (height == 0) {
                height = 1;
            }

            pb.add((float) xorigin);    // x origin
            pb.add((float) yorigin);    // y origin
            pb.add((float) width);      // width
            pb.add((float) height);     // height

            // Create the output image by cropping the input image.JAI
            PlanarImage output = JAI.create("crop", pb, null);

            // Store the cropped image in jpg format in the best quality
            JPEGEncodeParam param = new JPEGEncodeParam();

            param.setQuality(1.0f);

            if ((outputFile != null)
                    && (JAI.create("filestore", output.getAsBufferedImage(), outputFile.toString(), "jpeg", param)
                        != null)) {
                output.dispose();

                return true;
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(EventImageCache.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    private int grabEventsByFrame(PlanarImage original, int bestFrame, int index)
            throws MissingFrameException, FrameOutRangeException {
        int numEventsInFrame = 0;

        synchronized (syncArrays) {
            EventObjectContainer ec = null;

            do {
                if (index >= eventListModel.getSize()) {
                    return numEventsInFrame;
                }

                EventImageCacheData data = null;

                // Get the EventImageCacheData at this index
                data = imageCacheDataList.get(index);
                ec   = data.getEventObjectContainer();

                // If the best frame match, initialize and crop
                if (ec.getBestEventFrame() == bestFrame) {
                    data.initialize(bestFrame);
                    ec.setIsBlackChecked();

                    if (createCroppedImageOfEvent(original, data, data.getEvent())) {
                        ec.setEventImageCacheData(data);
                        index++;
                        numEventsInFrame++;
                    }
                } else {
                    return numEventsInFrame;
                }
            } while (ec.getBestEventFrame() == bestFrame);
        }

        return numEventsInFrame;
    }

    /**
     * Grabs image to populate the cache list and given index
     * @param index
     * @return false if the index is out of range of the list
     * or any other exception occurs, otherwise returns true
     * if found an image to represent the index. Note, this
     * doesn't not necessary mean that the image is valid. This
     * could return a default image to give some visual
     * indication to the user of the error.
     *
     *  Call within block synced by: syncArrays
     *
     */
    private boolean grabImage(int index) throws FrameOutRangeException {
        if (index >= eventListModel.getSize()) {
            return false;
        }

        synchronized (syncArrays) {
            EventImageCacheData data = null;

            try {

                // Get the EventImageCacheData at this index
                data = imageCacheDataList.get(index);

                if (data != null) {
                    EventObjectContainer ec        = data.getEventObjectContainer();
                    int                  bestFrame = ec.getBestEventFrame();

                    data.initialize(bestFrame);

                    if (createBestCroppedImageOfEvent(data) == true) {
                        if (ec.isBlackChecked() == false) {

                            // Load the image that corresponds to the best frame
                            PlanarImage original = JAI.create("fileload", data.getImageSource().toString());
                            int         mean     = meanValue(original.getAsBufferedImage());
                            int         length   = ec.getEndFrame() - ec.getStartFrame();

                            /*
                             * System.out.println("Mean: " + mean +
                             * " for ObjectID: " + ec.getObjectId() +
                             * " bestFrame: " + bestFrame);
                             */

                            // If the mean is nearly black, then assume this is a bogus
                            // image and select the next best frame.
                            // This may not be true for all cases, but this is true
                            // for the still-images processed in Station M when
                            // the strobe mis-fires, resulting in a black image
                            if ((mean < 5) && (length > 0)) {

                                // Get the next best frame
                                int nextBestFrame = ec.findNextBestFrame();

                                if (nextBestFrame != bestFrame) {

                                    // if found a next best frame, reinitialize
                                    // the EventImageCacheData
                                    ec.setBestImageFrame(nextBestFrame);
                                    data.initialize(nextBestFrame);
                                    System.out.println("Found alternative best frame" + " for ObjectID: "
                                                       + ec.getObjectId() + " bestFrame: " + nextBestFrame);
                                    ec.setIsBlackChecked();
                                    ec.setEventImageCacheData(data);

                                    return true;
                                }
                            } else {
                                ec.setIsBlackChecked();
                                ec.setEventImageCacheData(data);

                                return true;
                            }
                        } else {
                            ec.setEventImageCacheData(data);

                            return true;
                        }
                    }
                }
            } catch (MissingFrameException e) {
                System.out.println(e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                System.out.println(e.getMessage());

                return false;
            }
        }

        return false;
    }

    /**
     * Returns the mean value of an image
     * @param image
     * @return the mean value
     */
    public static int meanValue(BufferedImage image) {
        Raster raster = image.getRaster();
        double sum    = 0.0;

        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                sum += (raster.getSample(x, y, 0) + raster.getSample(x, y, 1) + raster.getSample(x, y, 2)) / 3;
            }
        }

        return (int) (sum / (image.getWidth() * image.getHeight()));
    }

    /**
     * Grabs and loads events by frame
     * Returns the number of total loaded
     */
    public int loadNextFrame() {
        if ((cacheNextIndex < indexsToCache.length) && iKeepRunning) {
            int         index     = -1;
            int         bestFrame = -1;
            PlanarImage original  = null;

            index = indexsToCache[cacheNextIndex];

            /*
             *  System.out.println("EventImageCache total loaded: " + totalLoaded + " total needed:"
             *                  + imageCacheDataList.size() + " Indexes to cache: " + indexsToCache.length
             *                  + " cacheNextIndex:" + cacheNextIndex);
             */

            if (index > -1) {
                synchronized (syncArrays) {

                    // Get the EventImageCacheData at this index
                    EventImageCacheData data = imageCacheDataList.get(index);

                    if (data != null) {
                        EventObjectContainer ec = data.getEventObjectContainer();

                        // initialize a new best frame
                        if (bestFrame != ec.getBestEventFrame()) {
                            bestFrame = ec.getBestEventFrame();
                            original  = loadImage(data.getRawImageSource());

                            if (original != null) {
                                int i = 0;

                                try {
                                    i = grabEventsByFrame(original, bestFrame, index);
                                } catch (FrameOutRangeException ex) {}
                                catch (MissingFrameException ex) {}
                                catch (Exception ex) {
                                    Logger.getLogger(EventImageCache.class.getName()).log(Level.SEVERE, null, ex);
                                }

                                if (i > 0) {

                                    // If grabbed all images in frame, increment the loaded count appropriately
                                    for (int j = 0; j < i; j++) {
                                        indexsToCache[cacheNextIndex++] = -1;
                                        totalLoaded++;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // If loaded all the images then end the SwingWorker
            if (totalLoaded >= indexsToCache.length) {
                iKeepRunning   = false;
                cacheNextIndex = 0;
            }

            // If exceeded the total to cache and haven't loaded everything
            // then reset the next index back to zero. This will continue
            // this threads attempts to load images.
            if ((cacheNextIndex >= indexsToCache.length) && (totalLoaded < indexsToCache.length)) {
                iKeepRunning   = true;
                cacheNextIndex = 0;
            }
        }

        return totalLoaded;
    }

    /**
     * Grabs and loads the next needed index
     * Returns the number of total loaded
     */
    private int loadNextIndex() {
        if ((cacheNextIndex < indexsToCache.length) && iKeepRunning) {
            int index = -1;

            index = indexsToCache[cacheNextIndex];

            /*
             * System.out.println("EventImageCache total loaded: " + totalLoaded +
             *       " total needed:" + imageCacheDataList.size() +
             *       " Indexes to cache: " + indexsToCache.length +
             *       " cacheNextIndex:" + cacheNextIndex);
             */
            if (index > -1) {
                synchronized (syncArrays) {
                    try {
                        for (int retries = 0; retries < 2; retries++) {

                            // If grabbed an image, increment the loaded count
                            if (grabImage(index) == true) {
                                indexsToCache[cacheNextIndex] = -1;
                                totalLoaded++;

                                break;
                            }
                        }
                    } catch (FrameOutRangeException ex) {
                        Logger.getLogger(EventImageCache.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            cacheNextIndex++;
        }

        // If loaded all the images then end the SwingWorker
        if (totalLoaded >= indexsToCache.length) {
            iKeepRunning   = false;
            cacheNextIndex = 0;
        }

        // If exceeded the total to cache and haven't loaded everything
        // then reset the next index back to zero. This will continue
        // this threads attempts to load images.
        if ((cacheNextIndex >= indexsToCache.length) && (totalLoaded < indexsToCache.length)) {
            iKeepRunning   = true;
            cacheNextIndex = 0;
        }

        return totalLoaded;
    }

    /**
     * Returns the size of the image cache. This should be
     * the same size as the EventListModel with one image
     * per every event
     *
     * @return cache size
     */
    public int size() {
        synchronized (syncArrays) {
            return imageCacheDataList.size();
        }
    }

    /**
     * Public method to reset the cache
     */
    public void clear() {
        reset();
    }

    /**
     * Helper class for reporting the stats of loading
     * to the View classes in the SwingWorker thread
     */
    private static class ImageLoadStats {
        private final int numloaded, total;

        ImageLoadStats(int numloaded, int total) {
            this.numloaded = numloaded;
            this.total     = total;
        }
    }


    /**
     * Worker to load the images into the EventImageCache
     * @author dcline
     *
     */
    private class ImageLoader extends SwingWorker<Void, ImageLoadStats> {
        boolean         loadByFrame = false;
        EventImageCache cache;
        EventListModel  model;

        public ImageLoader(EventImageCache cache, EventListModel model) throws Exception {
            if ((cache != null) && (cache.size() > 0)) {
                this.cache = cache;
            } else {
                throw new Exception("Invalid EventImageCache");
            }

            if ((model != null) && (model.getSize() > 0)) {
                this.model = model;
            } else {
                throw new Exception("Invalid EventListModel");
            }
        }

        /**
         * Because publish is invoked very frequently, a lot of totals will
         * probably be accumulated before process is invoked in the event
         * dispatch thread; process is only interested in the last value
         * reported each time, using it to update the GUI:
         */
        @Override
        protected void process(List list) {
            ImageLoadStats s = (ImageLoadStats) list.get(list.size() - 1);

            model.notifyImagesChanged(s.numloaded);
        }

        @Override
        protected Void doInBackground() throws Exception {

            /*
             *  Runnable implementation
             */
            if (cache != null) {
                int ttl        = cache.size();
                int ttllast    = 0;
                int refreshcnt = 7;

                while (cache.iKeepRunning) {
                    try {
                        int totalLoaded = 0;

                        if (loadByFrame) {
                            totalLoaded = cache.loadNextFrame();
                        } else {
                            totalLoaded = cache.loadNextIndex();
                        }

                        if ((totalLoaded - ttllast > refreshcnt) || (totalLoaded == ttl)) {
                            publish(new ImageLoadStats(totalLoaded, ttl));
                            ttllast = totalLoaded;
                            System.out.println("publishing  totalLoaded: " + totalLoaded);
                        }

                        if (totalLoaded == ttl) {
                            return null;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }

            return null;
        }
    }
}
