/*
 * @(#)ExportXMLWorker.java
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
import org.mbari.aved.ui.appframework.Controller;

/**
 * Helper class to export results. This will write a new XML file with the
 * contents of the editor.
 */
public class ExportXMLWorker extends SwingWorker {
    private Controller controller;

    /**
     * Handle to data stream mapped to the current editor. This is reset
     * every time a new XML schema is loaded
     */
    private EventDataStream eventDataStream;

    // Simple display to show import progress
    private ProgressDisplay progessDisplay;
    private File            xmlFile;
    private SummaryModel summary;

    /**
     * Import the results in the XML file and put in hash map
     *
     * @param file
     *            XML file to import/edit
     */
    public ExportXMLWorker(File file, Controller controller, SummaryModel summary, EventDataStream eds) throws Exception {

        // TODO: need null error checking
        eventDataStream = eds;
        this.controller = controller;
        this.summary = summary;
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

            // Update the XML file only after successfully exporting it
            summary.setXmlFile(xmlFile);

            return eventDataStream;
        } catch (java.io.FileNotFoundException e) {
            String message = "Error - cannot open file: " + xmlFile.getName() + "\nmessage:"
                                        + e.getMessage();
            NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) controller.getView(), message);

            dialog.setVisible(true);
        }

        // Set the progress bar to 100% and reset the cursor
        setProgress(100);
        Application.getView().setDefaultCursor();

        return null;
    }
}
