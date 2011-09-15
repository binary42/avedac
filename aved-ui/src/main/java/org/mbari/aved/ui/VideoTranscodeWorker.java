/*
 * @(#)VideoTranscodeWorker.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.mbarivision.api.AvedRuntimeException;
import org.mbari.aved.mbarivision.api.TranscodeProcess;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.process.ProcessDisplay;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.ParseUtils;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

/**
 * Helper worker to handle starting image handling
 * tasks like starting the transcoding script
 * and the image loader
 *
 * @author dcline
 */
public class VideoTranscodeWorker extends SwingWorker {

    /**
     * Flag used to control cancellation messages
     * when cancel requested by the user
     */
    private boolean isUserCancel = false;

    /** Flag to indicate whether to display progress or not */
    private boolean displayProgress = true;

    /** The controller container parent view and model data */
    private AbstractController controller;
    private ApplicationModel   model;

    /** Simple display to show process status */
    ProcessDisplay processDisplay;

    /** The underlying transcoder process */
    private TranscodeProcess transcodeProcess;

    /** *Video to transcode  using this worker */
    private File video;

    /**
     * Import the results in the XML file and put in hash map
     *
     * @param controller the controller that
     * @param videoSource the local file that contains the video source to transcode
     *
     */
    public VideoTranscodeWorker(AbstractController controller, ApplicationModel model, File videoSource,
                                boolean displayProgress) {
        try {
            if ((controller != null) && (videoSource != null) && (model != null)) {
                this.model           = model;
                this.controller      = controller;
                this.displayProgress = displayProgress;
                video                = videoSource;

                if (displayProgress) {
                    processDisplay = new ProcessDisplay("Video transcoding " + videoSource.toString() + "...");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(VideoTranscodeWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Executed after the {@code doInBackground} method is finished.
     * Kills running transcode process if it still running
     *
     * @see #doInBackground
     * @see #isCancelled()
     * @see #get
     */
    @Override
    protected void done() {

        // If this was cancelled, kill the transcode process if it is running
        if (isCancelled() && (transcodeProcess != null)) {
            if (transcodeProcess.isRunning()) {
                transcodeProcess.kill();
            }
        }

        // Close the window when done
        if (displayProgress && (processDisplay != null)) {
            processDisplay.getView().dispose();
        }
    }

    /*
     * Application task. Executed in background thread. This execute the transcode
     * process, which can take a while for large video clilps
     *
     * @throws Exception
     */
    @Override
    protected Object doInBackground() throws Exception {
        final JFrame view = (JFrame) controller.getView();

        Application.getView().setBusyCursor();

        if (displayProgress) {
            processDisplay.getView().setVisible(true);
        }

        // If already a running process then stop it
        if ((transcodeProcess != null) && transcodeProcess.isRunning()) {
            transcodeProcess.kill();
            Application.getView().setBusyCursor();
            transcodeProcess.clean();
            Application.getView().setDefaultCursor();
        }

        try {
            File clip = video;

            // Create a new transcode process and redirect the output
            transcodeProcess = new TranscodeProcess(clip);

            if (displayProgress) {
                transcodeProcess.setPrintStream(processDisplay);
            }

            // Get the temporary directory and create it if it doesn't exist
            File tmpDir = UserPreferences.getModel().getScratchDirectory();

            // Initialize the transcoder output directory to be the temporary directory
            if (!tmpDir.exists()) {
                tmpDir.mkdir();
            }

            // Now, set the transcode directory by default to the temporary directory
            // appended with the source file file stem
            String srcDir = tmpDir + File.separator + ParseUtils.removeFileExtension(clip.getName().toString())
                            + File.separator;

            // Update the model with the new transcode directory
            model.getSummaryModel().setTranscodeDir(new File(srcDir));
            transcodeProcess.setOutTemporaryStorage(srcDir.toString());
            model.getSummaryModel().setAVEDVideo(transcodeProcess.getOutAVEDVideo());

            // Start the process
            transcodeProcess.run();

            try {
                if (displayProgress) {

                    // Close the window when done
                    processDisplay.getView().dispose();
                }
            } catch (Exception ex) {
                Logger.getLogger(VideoTranscodeWorker.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            NonModalMessageDialog dialog = new NonModalMessageDialog(view, ex.getMessage());

            dialog.setVisible(true);
            dialog.answer();
        } catch (AvedRuntimeException ex) {

            // If this wasn't cancelled by the user
            if (isUserCancel == false) {
                NonModalMessageDialog dialog = new NonModalMessageDialog(view, ex.getMessage());

                dialog.setVisible(true);
                dialog.answer();
            }

            isUserCancel = false;
        }

        Application.getView().setDefaultCursor();

        return null;
    }

    /**
     * Sets a flag to distinguish between user cancel and
     * system cancel to avoid throwing an exception due to
     * the user cancel
     */
    void gracefulCancel() {
        isUserCancel = true;
        this.cancel(false);
    }

    /**
     * Reset the transcode process
     * Kills this worker if running and cleans up the transcoding files
     */
    public void reset() {
        isUserCancel = true;
        this.cancel(true);

        if (transcodeProcess != null) {
            do {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(VideoTranscodeWorker.class.getName()).log(Level.SEVERE, null, ex);
                }
            } while (transcodeProcess.isAlive());

            Application.getView().setBusyCursor();
            transcodeProcess.clean();
            Application.getView().setDefaultCursor();
        }

        if (processDisplay != null) {
            try {
                processDisplay.close();
            } catch (IOException ex) {
                Logger.getLogger(VideoTranscodeWorker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Set the maximum frame range for transcoding
     * @param maxEventFrame
     */
    public void setMaxFrame(int maxEventFrame) {
        if (transcodeProcess != null) {
            transcodeProcess.setTranscodeOpts(" -b 0 -e " + Integer.toString(maxEventFrame));
        }
    }
}
