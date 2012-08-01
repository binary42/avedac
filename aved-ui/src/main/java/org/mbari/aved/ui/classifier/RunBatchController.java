/*
 * @(#)RunBatchController.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.ApplicationController;
import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.ExportXMLWorker;
import org.mbari.aved.ui.ImportXMLWorker;
import org.mbari.aved.ui.VideoTranscodeWorker;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventAbstractTableModel;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.model.TableSorter;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.progress.ProgressDisplayStream;
import org.mbari.aved.ui.table.EventTable;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.ExcelExporter;
import org.mbari.aved.ui.utils.ParseUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableModel;  
import org.mbari.aved.ui.utils.URLUtils;
import org.mbari.aved.ui.utils.VideoUtils;

public class RunBatchController extends AbstractController implements ModelListener {
    private final BatchProcessController controller;
    private ExportXMLWorker              exportXmlWorker;
    private ImportXMLWorker              importXmlWorker;
    private RunWorker                    runClassifierWorker;
    private VideoTranscodeWorker         transcodeWorker;
    private final BatchProcessView       view; 

    RunBatchController(ClassifierModel model, BatchProcessController controller) {
        setModel(model);
        setView(new RunView(model, this));
        this.view       = controller.getView();
        this.controller = controller;  

        // Register as listener to the models
        getModel().addModelListener(this);
    }
 
    /* Just some helper functions to access trainingModel and view */
    @Override
    public ClassifierModel getModel() {
        return ((ClassifierModel) super.getModel());
    }

    @Override
    public RunView getView() {
        return ((RunView) super.getView());
    }


    TrainingModel getTrainingModel() {
        int index = getView().getTrainingModelIndex();
        if ( index != -1) {
            return getModel().getTrainingModel(index); 
        }
        return null;
    } 

    private File findTranscodeSource(SummaryModel model) throws Exception {
        URL url = model.getInputSourceURL();
        File file = null;  

        if (url != null) {

            // If this is a http url reference and not a local file
            // must download to a local directory for transcoding to 
            // work
            if (url.getProtocol().startsWith("http:")) {
                File tmpDir = UserPreferences.getModel().getScratchDirectory();

                // Initialize the transcoder output directory to be the temporary directory
                if (!tmpDir.exists()) {
                    tmpDir.mkdir();
                }

                if (tmpDir != null) {
                    String v = tmpDir.toString() + "/" + ParseUtils.parseFileNameRemoveDirectory(url.getFile());

                    file = new File(v);
                } else {
                    file = new File(ParseUtils.parseFileNameRemoveDirectory(url.getFile()));
                }

                // Download the contents of the url to a local file if it doesn't exist
                if (!file.exists()) {
                    VideoUtils.download(url, file);
                }


            } else if (url.getProtocol().startsWith("file:")) {
                URLUtils.isValidURL(url);
                file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
            } else {
                throw new Exception("Invalid image source: " + url.toString());
            }

            if ((file != null) && file.exists()) {
                return file;
            } else {
                throw new Exception("Invalid image source: " + url.toString());
            }
        }
        else {
            throw new Exception("Image source not defined ");
        }
    }


    /**
     *    Export the results in simple Excel format.
     */
    public void exportProcessedResultsAsXls(File f, JTable table) throws IOException {
        if (f != null) {
            ExcelExporter.exportTable(table, f);
        }
    }
     
    /**
     * Helper method to reset the batch classification cleanly
     * 
     * @param name
     * @param batch
     * @param tmodel 
     */
    private void reset(String name, BatchProcessModel batch, BatchProcessAbstractTableModel tmodel) {

        NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                name
                + " classification stopped");

        batch.setStatus("Stopped");
        tmodel.fireTableDataChanged();
        dialog.setVisible(true);

        if (importXmlWorker != null) {
            importXmlWorker.cancel(false);
        }

        if (transcodeWorker != null) {
            transcodeWorker.reset();
            transcodeWorker = null;
        }

        if (runClassifierWorker != null) {
            runClassifierWorker.setFini();
        }
        
        // Initialize button states
        getView().setRunButton(true);
        getView().setStopButton(false);
    }
  
    private class BatchWorker extends SwingWorker {

        final RunBatchController runController;
        final BatchProcessController batchController;

        BatchWorker(RunBatchController runController, BatchProcessController c) {
            this.runController = runController;
            this.batchController = c;
        }

        @Override
        protected Object doInBackground() throws Exception {

            // Temporarily turn off this user preference to not add all the
            // predicted images to the library during assignment
            final boolean isAddTrainingImages = UserPreferences.getModel().getAddTrainingImages();

            if (isAddTrainingImages) {
                UserPreferences.getModel().setAddTrainingImages(false);
            }

            // Initialize variables used in thread
            final TrainingModel trainingModel = getTrainingModel();
            final VotingMethod method = getView().getVotingMethod();
            final float minProbThreshold = getView().getProbabilityThreshold();
            final BatchProcessAbstractTableModel tmodel = batchController.getAbstractModel();
            final int size = view.getSelectedTable().getRowCount();
            BufferedReader br = Classifier.getController().getBufferedReader();

            final SwingWorker worker = Classifier.getController().getWorker();

            final ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                    "Running classifier with training model "
                    + trainingModel.getName());
            
            // Redirect err/out to progress display
            System.setOut(new PrintStream(progressDisplay, true));
            System.setErr(new PrintStream(progressDisplay, true));

            final ProgressDisplayStream progressDisplayStream =
                  new ProgressDisplayStream(progressDisplay, br);

            progressDisplayStream.execute();
            progressDisplay.getView().setVisible(true);

            for (int i = 0; i < size; i++) {
                importXmlWorker = null;
                transcodeWorker = null;
                runClassifierWorker = null;

                BatchProcessModel batch = (BatchProcessModel) tmodel.getValueAt(i, -1);

                if (!batch.getStatus().equals("Done")) {
                    try {
                        ApplicationModel model = batch.getModel();
                        EventListModel list = model.getEventListModel();
                        File xmlfile = model.getSummaryModel().getXmlFile();

                        // import events xml
                        importXmlWorker = new ImportXMLWorker(xmlfile, model, runController, progressDisplay);
                        batch.setStatus("Importing xml...");
                        tmodel.fireTableDataChanged();

                        importXmlWorker.run();

                        if (importXmlWorker.isCancelled()) {
                            reset(trainingModel.getName(), batch, tmodel);
                            progressDisplay.getView().dispose();
                            return null;
                        }

                        // if no events in this, then just skip
                        if (list.getSize() > 0) {

                            // transcode
                            SummaryModel summary = model.getSummaryModel();

                            File file = findTranscodeSource(summary); 

                            transcodeWorker = new VideoTranscodeWorker(runController, model, file, progressDisplay);
                            transcodeWorker.setMaxFrame(importXmlWorker.getMaxEventFrame());
                            batch.setStatus("Transcoding...");
                            tmodel.fireTableDataChanged();
                            transcodeWorker.run();

                            if (transcodeWorker.isCancelled() || !transcodeWorker.isInitialized()) {
                                reset(trainingModel.getName(), batch, tmodel);
                                progressDisplay.getView().dispose();
                                return null;
                            }

                            // make the test directory  a subdirectory of transcoding.
                            // Putting this test images in a subdirectory makes for easy deletion later
                            File s = summary.getFrameSourceDir();
                            File testingDir = new File(s + "/testimages_" + s.getName());

                            if (!testingDir.exists()) {
                                testingDir.mkdir();
                            }

                            summary.setTestImageDir(testingDir);

                            // classify
                            runClassifierWorker = new RunWorker(trainingModel, minProbThreshold,
                                    testingDir, list, method, progressDisplay);
                            batch.setStatus("Classifying...");
                            tmodel.fireTableDataChanged();

                            Classifier.getController().addQueue(runClassifierWorker);

                            while (!runClassifierWorker.isCancelled() && !runClassifierWorker.isFini()) {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(RunBatchController.class.getName()).log(
                                            Level.SEVERE, null, ex); 
                                }
                            }

                            if (!runClassifierWorker.isFini()) {
                                reset(trainingModel.getName(), batch, tmodel);
                                return null;
                            }

                            TableModel cmTableModel = runClassifierWorker.getTableModel();
                            int rowCnt = cmTableModel.getRowCount();

                            // get predicted totals from confusion matrix
                            for (int j = 0; j < rowCnt - 2; j++) {
                                batch.setClassTotal(j, cmTableModel.getValueAt(j, 1));
                            }

                            tmodel.fireTableDataChanged();
                            batch.setStatus("Export...");
                            tmodel.fireTableDataChanged();

                            File exportXmlFile = new File(batchController.getOutputDir() + "/"
                                    + xmlfile.getName());

                            exportXmlWorker = new ExportXMLWorker(exportXmlFile, runController, summary,
                                    summary.getEventDataStream());
                            exportXmlWorker.run();

                            if (exportXmlWorker.isCancelled()) {
                                reset(trainingModel.getName(), batch, tmodel);
                                progressDisplay.getView().dispose();
                                return null;
                            }

                            String title = ParseUtils.removeFileExtension(xmlfile.getName());
                            String exportFilename = title + ".xls";

                            RunController.createTableControllerView(exportFilename, getModel(),
                                    runClassifierWorker.getTableModel(), title, "Confusion Matrix for " + title
                                    + ", Probability Threshold: " + minProbThreshold + " , Voting Method: "
                                    + method.name(), false);


                            // Save the results of class assignments by events in a xls table
                            File exportXlsFile =
                                    new File(exportXmlFile.getParent() + "/"
                                    + ParseUtils.removeFileExtension(exportXmlFile.getName()) + ".xls");

                            // Creates the custom event JTable to customize how the table is rendered
                            EventTable eventTable = new EventTable();

                            // Get the row selection model and register this as a selection
                            // listener so we can translated from the sorted table to the
                            // real model table.
                            eventTable.setSelectionModel(model.getListSelectionModel());

                            EventAbstractTableModel tablemodel =
                                    new EventAbstractTableModel(model.getEventListModel());

                            // Create the sorter and intialize it in the model
                            TableSorter sorter = new TableSorter(tablemodel.getTableModel());

                            // getModel().initializeSorter(sorter);
                            // Initialize the table headers, so the sorter knows what to sort on
                            sorter.setTableHeader(eventTable.getTableHeader());

                            // Set the model in the event table
                            eventTable.setModel(sorter);
                            exportProcessedResultsAsXls(exportXlsFile, eventTable);

                            // reset will reset and clean-up any created files
                            batch.setStatus("Cleanup...");
                            tmodel.fireTableDataChanged();
                            transcodeWorker.reset(); 
                        }

                        batch.setStatus("Done");
                        tmodel.fireTableDataChanged();
                    } catch (Exception ex) {  
                        batch.setStatus("Failed"); 
                        Logger.getLogger(RunBatchController.class.getName()).log(Level.SEVERE, null,
                                ex);
                    }
                }
            }
            

            // Reset the user preference
            UserPreferences.getModel().setAddTrainingImages(isAddTrainingImages);
            
            // Close the progress display and reset button states
            progressDisplayStream.done();
            progressDisplay.getView().dispose();
            getView().setRunButton(true);
            getView().setStopButton(false);
            return this;
        }
    }

    public void run() {
        JTable table = view.getSelectedTable();

        // Check files and training model before starting
        if (table.getRowCount() == 0) {
            NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                    "Selected file list empty. Please select available files");
            dialog.setVisible(true);
            return;
        }
            
 
        if (getTrainingModel() == null) {
             NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                    "Please select training library");
            dialog.setVisible(true);
            return;
        }

        // Check output directory before starting
        if (!controller.getOutputDir().exists()) {
            NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                    controller.getOutputDir()
                    + " does not exist");
            dialog.setVisible(true);
            return;
        }

        if (!controller.getOutputDir().canWrite()) {
            NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                    " cannot write to "
                    + controller.getOutputDir());
            dialog.setVisible(true);
            return;
        }

        try {

            // Set button states
            getView().setRunButton(false);
            getView().setStopButton(true);

            BatchWorker worker = new BatchWorker(this, controller);
            worker.execute();

        } catch (Exception ex) {
            // Reset user preference
            Logger.getLogger(RunBatchController.class.getName()).log(Level.SEVERE, null,
                    ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("Run")) { 
            run();
        } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
            JComboBox  box           = ((JComboBox) e.getSource());
            ColorSpace newColorSpace = (ColorSpace) box.getSelectedItem();
        
            UserPreferences.getModel().setColorSpace(newColorSpace);  
        
            String lastSelection = UserPreferences.getModel().getTrainingLibrarySelection();

            if (getView() != null && newColorSpace != null) {
                getView().populateTrainingLibraryList(newColorSpace, lastSelection);
            }
        } else if (actionCommand.equals("Stop")) {
            if (importXmlWorker != null) {
                importXmlWorker.cancel(false);
            }

            if (transcodeWorker != null) {
                transcodeWorker.cancel(false);
                transcodeWorker.reset();
            }

            if (runClassifierWorker != null) {
                Classifier.getController().kill(runClassifierWorker);
            }

            if (exportXmlWorker != null) {
                exportXmlWorker.cancel(false);
            }

            getView().setRunButton(true);
            getView().setStopButton(false);
        } else if (actionCommand.equals("availLibraryNameComboBoxChanged")) {  
            JComboBox     box           = ((JComboBox) e.getSource());
            TrainingModel m             = (TrainingModel) box.getSelectedItem(); 
            
            getView().loadModel(m);
 
            UserPreferences.getModel().setTrainingLibrarySelection(m.getName());

            getModel().notifyTrainingModelChanged(m.getName());
        }
    }

    @Override
    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {

            // When the database root directory change or the models are updated
            // reset the color space
            case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED :
                ColorSpace c = UserPreferences.getModel().getColorSpace();
                getView().selectColorSpace(c);
                
            case ClassifierModel.ClassifierModelEvent.TRAINING_MODELS_UPDATED :  
                ColorSpace cs = getView().getColorSpace();
            String lastSelection = UserPreferences.getModel().getTrainingLibrarySelection();
                getView().populateTrainingLibraryList(cs, lastSelection); 
            break; 
            }
        }
    } 
}
