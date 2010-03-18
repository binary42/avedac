/*
 * @(#)DetectionSettingsModel.java   10/03/17
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



package org.mbari.aved.ui.detectionsettings;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.mbarivision.api.MbarivisionOptions;
import org.mbari.aved.ui.appframework.AbstractModel;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

public class DetectionSettingsModel extends AbstractModel {
    public static int DETECTION_SETTINGS_CHANGED = 1;
    public static int EVENT_IMAGE_DIR_UPDATED    = 0;

    /** Defines the directory to store the event centered images to */
    public File myEventImageDir = null;

    /** Define the MPEG file to create of results */
    public File myMpegFile = null;

    /** Define the options to use with the mbarivision exeutable */
    private MbarivisionOptions myOptions = new MbarivisionOptions();

    DetectionSettingsModel() {}

    public DetectionSettingsModel cloneData() {
        DetectionSettingsModel d = new DetectionSettingsModel();

        d.myOptions       = myOptions.clone();
        d.myEventImageDir = (myEventImageDir != null)
                            ? myEventImageDir
                            : null;
        d.myMpegFile      = (myMpegFile != null)
                            ? myMpegFile
                            : null;

        return d;
    }

    public void setEventImageDirectory(File file) {
        myEventImageDir = file;
        UserPreferences.getModel().setEventImageDirectory(file);

        ModelEvent e = new ModelEvent(this, EVENT_IMAGE_DIR_UPDATED, "setEventImageDirectory");

        notifyChanged(e);
    }

    public File getVideoMaskFile() {
        return myOptions.maskFile;
    }

    public File getEventImageDir() {
        return myEventImageDir;
    }

    public void setMarkEventStyle(MbarivisionOptions.MarkEventStyle style) {
        if (myOptions.eventStyle != style) {
            myOptions.eventStyle = style;
            notifyChange("MarkEventStyle changed to" + style);
        }
    }

    public MbarivisionOptions.MarkEventStyle getMarkEventStyle() {
        return myOptions.eventStyle;
    }

    public void setSegmentationAlgorithm(MbarivisionOptions.SegmentationAlgorithm algorithm) {
        if (myOptions.segmentAlgorithm != algorithm) {
            myOptions.segmentAlgorithm = algorithm;
            notifyChange("SegmentationAlgorithm changed to" + algorithm);
        }
    }

    public MbarivisionOptions.SegmentationAlgorithm getSegmentationAlgorithm() {
        return myOptions.segmentAlgorithm;
    }

    public void setTrackingMode(MbarivisionOptions.TrackingMode mode) {
        if (myOptions.trackingMode != mode) {
            myOptions.trackingMode = mode;
            notifyChange("TrackingMode changed to" + mode);
        }
    }

    public MbarivisionOptions.TrackingMode getTrackingMode() {
        return myOptions.trackingMode;
    }

    public File getXMLFile() {
        return myOptions.eventxml;
    }

    public void setVideoMask(File file) {
        if (file != null) {

            // If option is not set, or is set and this is a different file, change the option and notify listeners
            if ((myOptions.maskFile == null)
                    || ((myOptions.maskFile != null) && (myOptions.maskFile.toString() != file.toString()))) {
                myOptions.maskFile = file;
                notifyChange("VideoMask file changed to" + file.toString());
            }
        }
    }

    public File getSummaryFile() {
        return myMpegFile;
    }

    public File getMpeg() {
        return myMpegFile;
    }

    public void setMpeg(File file) {
        if (file != null) {
            myMpegFile = file;
        }
    }

    public void setEventLabels(boolean b) {

        // API logic is reversed here, so invert argument before test
        boolean b2 = !b;

        // If option is not set, or is set and this is a different file, change the option and notify listeners
        if ((myOptions.noLabelEvents == null)
                || ((myOptions.noLabelEvents != null) && (myOptions.noLabelEvents != b2))) {
            myOptions.noLabelEvents = b2;
            notifyChange("No write event labels set to " + ((b2 == true)
                    ? "true"
                    : "false"));
        }
    }

    public void setSaveXML(File file) {
        if (file != null) {

            // If option is not set, or is set and this is a different file, change the option and notify listeners
            if ((myOptions.eventxml == null)
                    || ((myOptions.eventxml != null) && (myOptions.eventxml.toString() != file.toString()))) {
                myOptions.eventxml = file;
            }

            notifyChange("Saving XML to " + file.toString());
        }
    }

    public void disableSaveXML() {
        myOptions.eventxml = null;

        // TODO: do something with the error - if not saving XML, make sure saving
        // event summary or mpeg of results at least.
        notifyChange("ERROR --> NOT Saving XML. Warn users about this");
    }

    public void disableTextSummary() {
        myOptions.eventSummary = null;

        // TODO: if not saving summary, make sure saving
        // event xml or mpeg of results
        notifyChange("ERROR --> NOT Saving Text Summary. Warn users about this");
    }

    public void disableSaveMpeg() {
        myMpegFile = null;

        // TODO Auto-generated method stub
        // TODO: if not mpeg, make sure saving
        // event xml or summary of results
        notifyChange("ERROR --> NOT Saving Mpeg. Warn users about this");
    }

    public void setSaveTextSummary(File file) {
        if (file != null) {

            // If option is not set, or is set and this is a different file, change the option and notify listeners
            if ((myOptions.eventSummary == null)
                    || ((myOptions.eventSummary != null) && (myOptions.eventSummary.toString() != file.toString()))) {
                myOptions.eventSummary = file;
                notifyChange("Saving Event Summary to " + file.toString());
            }
        }
    }

    public void setSaveEventImages(boolean b) {

        // API logic uses string here for argument - "all" map to true, and anything else is false.
        // This options supports more complicated event outputs, but don't bring this out in this UI so
        // don't implement logic here
        if ((myOptions.saveEventClip == null)
                || (((myOptions.saveEventClip != null) && ((myOptions.saveEventClip == "all") && (b == false)))
                    || ((myOptions.saveEventClip != "all") && (b == true)))) {
            myOptions.saveEventClip = ((b == true)
                                       ? new String("all")
                                       : null);
            notifyChange("Event centered images saved : " + ((b == true)
                    ? "true"
                    : "false"));
        }
    }

    public void setSaveOnlyInterestingEvents(boolean b) {

        // If option is not set, or is set and this is a different file, change the option and notify listeners
        if ((myOptions.saveOnlyInteresting == null)
                || ((myOptions.saveOnlyInteresting != null) && (myOptions.saveOnlyInteresting != b))) {
            myOptions.saveOnlyInteresting = b;
            notifyChange("Save only interesting events : " + ((b == true)
                    ? "true"
                    : "false"));
        }
    }

    public void markInterestingCandidates(boolean b) {

        // API logic is reversed here, so invert argument before test
        boolean b2 = !b;

        // If option is not set, or is set and this is a different file, change the option and notify listeners
        if ((myOptions.noMarkCandidate == null)
                || ((myOptions.noMarkCandidate != null) && (myOptions.noMarkCandidate != b2))) {
            myOptions.noMarkCandidate = b2;
            notifyChange("No mark interesting candidates: " + ((b2 == true)
                    ? "true"
                    : "false"));
        }
    }

    public void setCacheSize(int value) {
        if (myOptions.cacheSize != value) {
            int oldsize = myOptions.cacheSize;

            myOptions.cacheSize = value;
            notifyChange("Cache size changed from" + oldsize + " to " + value);
        }
    }

    public void setMaxEventArea(int value) {
        if (myOptions.maxEventArea != value) {
            int oldarea = myOptions.maxEventArea;

            myOptions.maxEventArea = value;
            notifyChange("Max event area changed from" + oldarea + " to " + value);
        }
    }

    public void setMinEventArea(int value) {
        if (myOptions.minEventArea != value) {
            int oldarea = myOptions.minEventArea;

            myOptions.minEventArea = value;
            notifyChange("Min event area changed from" + oldarea + " to " + value);
        }
    }

    public int getMaxEventArea() {
        return myOptions.maxEventArea;
    }

    public int getMinEventArea() {
        return myOptions.minEventArea;
    }

    public int getCacheSize() {
        return myOptions.cacheSize;
    }

    public boolean isCreateMpeg() {
        return (myMpegFile != null)
               ? true
               : false;
    }

    public boolean isWriteEventLabels() {
        return ((myOptions.noLabelEvents != null) && (myOptions.noLabelEvents == true))
               ? false
               : true;
    }

    public boolean isSaveEventsXML() {
        return (myOptions.eventxml != null)
               ? true
               : false;
    }

    public boolean isSaveEventTextSummary() {
        return (myOptions.eventSummary != null)
               ? true
               : false;
    }

    public boolean isSaveEventCenteredImages() {
        return (myOptions.saveEventClip != null)
               ? true
               : false;
    }

    public boolean isSaveOnlyInteresting() {
        return ((myOptions.saveOnlyInteresting != null) && (myOptions.saveOnlyInteresting == true))
               ? true
               : false;
    }

    public boolean isMarkCandidates() {

        // API logic is reversed
        return ((myOptions.noMarkCandidate != null) && (myOptions.noMarkCandidate == true))
               ? false
               : true;
    }

    private void notifyChange(String description) {
        ModelEvent e = new ModelEvent(this, DETECTION_SETTINGS_CHANGED, description);

        notifyChanged(e);
    }
}
