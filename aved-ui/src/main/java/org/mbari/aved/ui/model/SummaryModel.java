/*
 * @(#)SummaryModel.java
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

import aved.model.EventDataStream;
import aved.model.SourceMetadata;

import org.mbari.aved.mbarivision.api.AvedVideo; 
import org.mbari.aved.ui.appframework.AbstractModel;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author dcline
 */
public class SummaryModel extends AbstractModel {

    /** Defines the default string to put in each label */
    private static final String EMPTY_STRING = "";

    /** Defines the source video transcoded output */
    private AvedVideo avedVideoOut;

    /** Defines the data stream associated with the XML */
    private EventDataStream eventDataStream;

    /** Defines the input video file associated with the results */
    private URL inputSourceURL;

    /** Defines the mpegFile associated with the results */
    private URL mpegFile;

    /** Defines the directory the test images used for classification are */
    private File testImageDir;

    /** Defines the directory the input video file was transcoded to */
    private File transcodeDir;
 
    /** Defines the XML file containing results to edit */
    private File xmlFile;

    void SummaryModel() {
        try {
            reset();
        } catch (MalformedURLException ex) {}
    }

    /**
     * Clears out the list and notifies model event listeners
     */
    public void reset() throws MalformedURLException {
        this.eventDataStream = null;
        this.avedVideoOut    = null;
        this.setXmlFile(new File(EMPTY_STRING));
        this.setInputSourceURL(null, true);
        this.setMpegUrl(null);
        this.setTranscodeDir(new File(EMPTY_STRING));
        this.setTestImageDir(new File(EMPTY_STRING)); 
    }

    /**
     * @return frame source directory.
     * This is the directory where the transcoded output frames
     * associated with the data are.
     */
    public File getFrameSourceDir() {
        return transcodeDir;
    }

    /**
     * @return classifier test image directory.
     * This is the directory the test images used for classification
     * testing are
     */
    public File getTestImageDirectory() {
        return testImageDir;
    }

    /**
     * Returns the video file that is associated with the XML results
     *  This is the original video source used as input to the AVED process
     *  This is not necessarily the video source used for transcoding
     *  @return video source URL
     */
    public URL getInputSourceURL() {
        return inputSourceURL;
    } 

    /**
     * Returns the XML file that contains results to edit
     */
    public File getXmlFile() {
        return xmlFile;
    }

    /**
     * Returns the (optional) URL of the mpeg file associated
     * with these results
     */
    public URL getMpegUrl() {
        return mpegFile;
    }

    /**
     * Sets the object that contains information abou the video output
     * @param video
     */
    public void setAVEDVideo(AvedVideo video) {
        avedVideoOut = video;
    }

    /**
     * Sets the XML file with the AVED results to edit
     * @param file XML AVED results file
     */
    public void setXmlFile(File file) {
        if (changedName(file, xmlFile)) {
            xmlFile = file;

            String name = ((file != null)
                           ? file.toString()
                           : "null");

            notifyChanged(new SummaryModelEvent(this, SummaryModelEvent.XML_FILE_CHANGED, name));
        }
    }

    /**
     * Sets the MPEG  file that is associated with the XML results
     * @param url MPEG URL that contains AVED results
     */
    public void setMpegUrl(URL url) throws MalformedURLException {
        if (changedName(url, mpegFile)) {
            mpegFile = url;

            if (url != null) {

                // Set the last imported parent URL in the preferences
                UserPreferencesModel prefs = UserPreferences.getModel();

                if (url.getProtocol().startsWith("file") || url.getProtocol().startsWith("http")) {
                    prefs.setImportMpegDirectory(url);
                } else {
                    File f = new File(url.getFile());

                    prefs.setImportMpegDirectory(new URL("file://" + f.getParent()));
                }

                notifyChanged(new SummaryModelEvent(this, SummaryModelEvent.MPEG_SOURCE_URL_CHANGED, url.toString()));
            } else {
                notifyChanged(new SummaryModelEvent(this, SummaryModelEvent.MPEG_SOURCE_URL_CHANGED, "null"));
            }
        }
    }

    /**
     * Sets the video file in the model if it is different
     * than the one already contained in the model
     * @param urls the url that contains the input source video file
     * @param notify true to notify listeners the model has been updated
     */
    public void setInputSourceURL(URL url, boolean notify) throws MalformedURLException {
        if (changedName(url, inputSourceURL)) {
            inputSourceURL = url;

            // Set the last imported parent URL in the preferences
            UserPreferencesModel prefs = UserPreferences.getModel();


            if (url != null) {                
                if (eventDataStream != null) {
                    // Update the source metadata if there is one
                    SourceMetadata source = eventDataStream.getSourceMetadata();
                    source.setSourceIdentifier(url.toString());
                }
                prefs.setImportSourceUrl(url);
                if (notify)
                    notifyChanged(new SummaryModelEvent(this, SummaryModelEvent.INPUT_SOURCE_URL_CHANGED, url.toString()));
            } else { 
                if (notify)
                    notifyChanged(new SummaryModelEvent(this, SummaryModelEvent.INPUT_SOURCE_URL_CHANGED, "null"));
            }
        }
    } 

    public void setTranscodeDir(File file) {
        if (changedName(file, transcodeDir)) {
            transcodeDir = file;

            if (file != null) {
                notifyChanged(new SummaryModelEvent(this, SummaryModelEvent.TRANSCODE_OUTPUT_DIR_CHANGED,
                        transcodeDir.toString()));
            } else {
                notifyChanged(new SummaryModelEvent(this, SummaryModelEvent.TRANSCODE_OUTPUT_DIR_CHANGED, "null"));
            }
        }
    }

    /**
     * Sets the test image directory to save image files to classification
     * @param directory the directory
     */
    public void setTestImageDir(File directory) {
        testImageDir = directory;
    }

    /** Returns the EventDataStream associated with the XML file currently being edited */
    public EventDataStream getEventDataStream() {
        return eventDataStream;
    }

    /** Returns the EventDataStream associated with the XML file currently being edited */
    public AvedVideo getAvedVideo() {
        return avedVideoOut;
    }

    /**
     * Sets the event data stream associated with the XML file results contained in this list model
     * @param EDS
     */
    public void setEventDataStream(EventDataStream EDS) {
        eventDataStream = EDS;
    }

    /**
     * Null safe check if the URL's have the same
     * @returns True is the URL's are the same
     */
    private static Boolean changedName(URL a, URL b) {
        String oldfile = ((a != null)
                          ? a.toString()
                          : "");
        String newfile = ((b != null)
                          ? b.toString()
                          : "");

        return !oldfile.equals(newfile);
    }

    /**
     * Null safe check if the files have the same
     * @returns True is the files are the same
     */
    private static Boolean changedName(File a, File b) {
        String oldfile = ((a != null)
                          ? a.toString()
                          : "");
        String newfile = ((b != null)
                          ? b.toString()
                          : "");

        return !oldfile.equals(newfile);
    }

    public class SummaryModelEvent extends ModelEvent {

        /**
         * Indicates the Input video source has changed
         */
        public static final int INPUT_SOURCE_URL_CHANGED = 0;

        /**
         * Indicates the MPEG file has changed
         */
        public static final int MPEG_SOURCE_URL_CHANGED = 1;

        /**
         * Indicates the transcoded output directory has changed
         */
        public static final int TRANSCODE_OUTPUT_DIR_CHANGED = 3;

      
        /**
         * Indicates the XML file with AVED results has changed
         */
        public static final int XML_FILE_CHANGED = 2;

        /**
         * Constructor for this custom ModelEvent. Basically just like ModelEvent.
         * This is the default constructor for events that don't need to set the
         * any contained variables.
         * @param obj  the object that originated the event
         * @param type    an integer that identifies the ModelEvent type
         * @param message a message to add to the event description
         */
        public SummaryModelEvent(Object obj, int type, String message) {
            super(obj, type, "EditorSummaryModelEvent:" + type + " " + message);
        }
    }
}
