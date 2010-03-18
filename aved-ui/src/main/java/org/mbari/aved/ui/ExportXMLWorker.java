/*
 * @(#)ExportXMLWorker.java   10/03/17
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

import aved.model.EventDataStream;
import aved.model.xml.Mapper;

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.progress.ProgressDisplay;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JFrame;

/**
 * Helper class to export results. This will write a new XML file with the
 * contents of the editor.
 */
class ExportXMLWorker extends SwingWorker {
    private ApplicationController controller;

    /**
     * Handle to data stream mapped to the current editor. This is reset
     * every time a new XML schema is loaded
     */
    private EventDataStream eventDataStream;

    // Simple display to show import progress
    private ProgressDisplay progessDisplay;
    private File            xmlFile;

    /**
     * Import the results in the XML file and put in hash map
     *
     * @param file
     *            XML file to import/edit
     */
    public ExportXMLWorker(File file, ApplicationController controller, EventDataStream eds) throws Exception {

        // TODO: need null error checking
        eventDataStream = eds;
        this.controller = controller;
        xmlFile         = file;
        progessDisplay  = new ProgressDisplay(this, "Exporting " + xmlFile.getName() + "...");
    }

    /**
     * Executed after the {@code doInBackground} method is finished. Closes
     * the progress display
     *
     * @see #doInBackground
     * @see #isCancelled()
     * @see #get
     */
    protected void done() {
        progessDisplay = null;
    }

    /*
     * Application task. Executed in background thread. This execute the importXML
     * function, which can take a while for large AVED event sets
     */
    @Override
    protected Object doInBackground() throws Exception {
        progessDisplay.display("Running export " + xmlFile.getName() + " now...");

        // Initialize progress property and set the cursor
        setProgress(0);
        Application.getView().setBusyCursor();

        // Export the XML file using Brian S. AVED DB files
        try {
            FileOutputStream outputStream = new FileOutputStream(xmlFile);

            progessDisplay.display("Exporting XML file now...");
            Mapper.marshall(outputStream, eventDataStream);
            outputStream.close();

            // Set the progress bar to 100% and reset the cursor
            setProgress(100);
            Application.getView().setDefaultCursor();

            SummaryModel model = controller.getModel().getSummaryModel();

            // Update the XML file only after successfully exporting it
            model.setXmlFile(xmlFile);

            return eventDataStream;
        } catch (java.io.FileNotFoundException e) {
            String message = new String("Error - cannot open file: " + xmlFile.getName() + "\nmessage:"
                                        + e.getMessage());
            NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) controller.getView(), message);

            dialog.setVisible(true);
        }

        // Set the progress bar to 100% and reset the cursor
        setProgress(100);
        Application.getView().setDefaultCursor();

        return null;
    }
}
