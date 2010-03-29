/*
 * @(#)EventImageCacheData.java
 * 
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

import java.util.Formatter;
import java.util.Locale;
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
                String filename = new String(source.getParent() + "/" + Utils.getNameWithoutExtension(source) + "evt"
                                             + eventObjectContainer.getObjectId() + ".jpg");

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
    EventObject getEvent() throws FrameOutRangeException {

        // Get the best frame number and event that corresponds to it
        int bestFrameNum = eventObjectContainer.getBestEventFrame();

        return eventObjectContainer.getEventObject(bestFrameNum);
    }

    /**
     * Returns the EventObjectContainer contained in this
     *
     * @return the EventObjectContainer
     */
    EventObjectContainer getEventObjectContainer() {
        return eventObjectContainer;
    }

    /**
     * Returns the image source for this thumbnail
     * @return file with raw image source.
     */
    public File getImageSource() {
        return eventImageFile;
    }

    /**
     * Returns the raw image source from which this thumbnail came from
     * @return the raw image source name or nul if none found.
     */
    File getRawImageSource() {

        // Get the best frame number and event that corresponds to it
        int bestFrameNum = eventObjectContainer.getBestEventFrame();

        return eventObjectContainer.getFrameSource(bestFrameNum);
    }
}
