/*
 * @(#)EventImageCacheData.java
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

import aved.model.EventObject;

import org.mbari.aved.mbarivision.api.utils.Utils;
import org.mbari.aved.ui.exceptions.FrameOutRangeException;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

/**
 * Container images of AVED Events.
 *
 * @author dcline
 */
public class EventImageCacheData {
    private File                 eventImageFile;
    private EventObjectContainer eventObjectContainer;
    private ImageIcon            imageIcon;
    private ImageIcon            missingImageIcon;

    /**
     * Default constructor. Throws exception if a valid
     * missingframeexception cannot be initialized
     * @param data
     */
    public EventImageCacheData(EventObjectContainer data) throws Exception {
        if (data == null) {
            throw new Exception("invalid EventImageCacheData");
        }

        this.eventObjectContainer = data;

        if (missingImageIcon == null) {

            // Get a default image
            URL url = getClass().getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

            if (url == null) {
                throw new Exception("Cannot find missingframeexception.jpg");
            }

            missingImageIcon = new ImageIcon(url);
        }
    }

    /**
     * Initialize the data EventObjectContainer reference
     * @param bestFrameNo the best frame number to initialize the image from
     * @throws Exception
     * @return true if the frame source that corresponds to the <code>bestFrameNo</code>
     */
    public boolean initialize(int bestFrameNo) {
        if (bestFrameNo >= 0) {
            File source = eventObjectContainer.getFrameSource(bestFrameNo);

            eventObjectContainer.setBestImageFrame(bestFrameNo);

            if ((source != null) && source.exists()) {

                // Create name for the cropped jpeg thumbnail
                // image using the event identifier
                String filename = source.getParent() + "/" + Utils.getNameWithoutExtension(source) + "evt"
                                             + eventObjectContainer.getObjectId() + ".jpg";

                eventImageFile = new File(filename);

                // System.out.println("###DEBUG initializing object " +
                // filename + " " + Long.toString(getObjectId()) + "/" + this.toString());
                return true;
            }
        }

        return false;
    }

    /**
     * Initialize the data EventObjectContainer reference
     * @param rootDirectory the root directory to save the event image to
     * @param append a string to append to the event image
     * @param bestFrameNo the best frame number to initialize the image from
     * @throws Exception
     * @return true if the frame source that corresponds to the <code>bestFrameNo</code>
     */
    public boolean initialize(File rootDirectory, String append, int bestFrameNo) {
        if (bestFrameNo >= 0) {
            File source = eventObjectContainer.getFrameSource(bestFrameNo);

            eventObjectContainer.setBestImageFrame(bestFrameNo);

            if ((rootDirectory != null) && (source != null) && (append != null) && source.exists()) {
                String filename = String.format("%s/evt%06d%s%s.jpg", rootDirectory,
                                                eventObjectContainer.getObjectId(),
                                                Utils.getNameWithoutExtension(source), append);

                eventImageFile = new File(filename);

                // System.out.println("###DEBUG initializing object " +
                // filename + " " + Long.toString(getObjectId()) + "/" + this.toString());
                return true;
            }
        }

        return false;
    }

    /**
     * Helper function to test for valid image file
     * @return  true if a valid image exists, otherwise false if
     * a default image was initialized, or none at all
     */
    public Boolean isValidImageFile() {
        if ((eventImageFile != null) && eventImageFile.exists() && eventImageFile.canRead()
                && (eventImageFile.length() > 0)) {
            return true;
        }

        return false;
    }

    /**
     * Returns the Object identifier for this data.
     */
    public long getObjectId() {
        if (eventObjectContainer != null) {
            return eventObjectContainer.getObjectId();
        }

        return -1;
    }

    /**
     * Returns the ImageIcon contained in this class
     * @return the ImageIcon.
     */
    public ImageIcon getImage() {
        if (imageIcon == null) {
            if (isValidImageFile()) {
                try {
                    imageIcon = new ImageIcon(eventImageFile.getAbsoluteFile().toURL());
                } catch (MalformedURLException ex) {
                    Logger.getLogger(EventImageCacheData.class.getName()).log(Level.SEVERE, null, ex);
                }

                return imageIcon;
            }
        } else {
            return imageIcon;
        }

        return missingImageIcon;
    }

    /**
     * Returns the event object that corresponds to this thumbanil
     * @return best event object
     *
     * @throws FrameOutRangeException
     */
    public EventObject getEvent() throws FrameOutRangeException {

        // Get the best frame number and event that corresponds to it
        int bestFrameNum = eventObjectContainer.getBestEventFrame();

        return eventObjectContainer.getEventObject(bestFrameNum);
    }

    /**
     * Returns the EventObjectContainer contained in this
     *
     * @return the EventObjectContainer
     */
    public EventObjectContainer getEventObjectContainer() {
        return eventObjectContainer;
    }

    /**
     * Returns the image source for this thumbnail
     * @return file with image source.
     */
    public File getImageSource() {
        return eventImageFile;
    }

    /**
     * Returns the raw image source from which this thumbnail came from
     * @return the raw image source name or null if none found.
     */
    public File getRawImageSource() {

        // Get the best frame number and event that corresponds to it
        int bestFrameNum = eventObjectContainer.getBestEventFrame();

        return eventObjectContainer.getFrameSource(bestFrameNum);
    }
}
