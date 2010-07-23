/*
 * @(#)VideoTranscodeWorker.java
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
package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------
import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.mbarivision.api.AvedRuntimeException;
import org.mbari.aved.mbarivision.api.TranscodeProcess;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.process.ProcessDisplay;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.ParseUtils;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper worker to handle starting image handling
 * tasks like starting the transcoding script
 * and the image loader
 *
 * @author dcline
 */
class VideoTranscodeWorker extends SwingWorker {

    /**
     * Flag used to control cancellation messages
     * when cancel requested by the user
     */
    private boolean isUserCancel = false;
    /** The controller container parent view and model data */
    private ApplicationController controller;
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
    public VideoTranscodeWorker(ApplicationController controller, File videoSource) {
        try {
            if ((controller != null) && (videoSource != null)) {
                this.controller = controller;
                video = videoSource;
                processDisplay = new ProcessDisplay("Video transcoding " + videoSource.toString() + "...");
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
        if (processDisplay != null) {
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
        ApplicationView view = controller.getView();
        SummaryModel model = controller.getModel().getSummaryModel();

        Application.getView().setBusyCursor();
        processDisplay.getView().setVisible(true); 

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
            transcodeProcess.setPrintStream(processDisplay);

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
            model.setTranscodeDir(new File(srcDir));
            transcodeProcess.setOutTemporaryStorage(srcDir.toString());
            model.setAVEDVideo(transcodeProcess.getOutAVEDVideo());

            // Start the process
            transcodeProcess.run();

            try {
                // Close the window when done
                processDisplay.getView().dispose();
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
    void reset() {
        isUserCancel = true;
        this.cancel(true);

        if (transcodeProcess != null) {
          do{
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(VideoTranscodeWorker.class.getName()).log(Level.SEVERE, null, ex);
                } 
          }while (transcodeProcess.isAlive()) ;

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
}
