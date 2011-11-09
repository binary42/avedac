/*
 * @(#)RunClassifierControllerBatch.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableModel; 

public class RunClassifierControllerBatch extends AbstractController implements ModelListener {
    private final ClassifierBatchProcessController controller;
    private ExportXMLWorker                        exportXmlWorker;
    private ImportXMLWorker                        importXmlWorker;
    private RunClassifierWorker                    runClassifierWorker;
    private VideoTranscodeWorker                   transcodeWorker;
    private final ClassifierBatchProcessView       view;

    RunClassifierControllerBatch(ClassifierModel model, ClassifierBatchProcessController controller) {
        setModel(model);
        setView(new RunClassifierView(model, this));
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
    public RunClassifierView getView() {
        return ((RunClassifierView) super.getView());
    }

    private void findTranscodeSource(SummaryModel model) throws Exception {
        URL url = model.getInputSourceURL();

        if (url != null) {

            // If this is a http url reference and not a local file
            if (url.getProtocol().startsWith("http:")) {
                File file   = null;
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

                try {

                    // Download the contents of the url to a local file if it doesn't exist
                    if (!file.exists()) {
                        ApplicationController.download(url, file);
                    }

                    if ((file != null) && file.exists()) {
                        model.setTranscodeSource(file);
                    } else {
                        throw new Exception("Cannot find transcode source");
                    }
                } catch (Exception ex) {
                    throw new Exception("Cannot find transcode source");
                }
            } else {

                // Convert to to a local file reference
                File xml  = model.getXmlFile();
                File file = new File(url.getPath());
                File localFile;

                if (xml.getParent() != null) {
                    localFile = new File(xml.getParent() + "/" + file.getName());
                } else {
                    localFile = file;
                }

                // If there is no root path in the source identifier
                // assume it is in the same path as the XML,
                // and set its root to the same path as the XML
                if (localFile.exists()) {
                    model.setTranscodeSource(localFile);
                } else {
                    throw new Exception("Cannot find transcode source");
                }
            }
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

    public void run() {
        JTable table = view.getSelectedTable();

        if (table != null) {

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
                      " cannot write to " +                      
                      controller.getOutputDir());
              dialog.setVisible(true);    
              return;
            }
            
            // Temporarily turn off this user preference to not add all the
            // predicted images to the library during assignment
            final boolean isAddTrainingImages = UserPreferences.getModel().getAddTrainingImages();

            if (isAddTrainingImages) {
                UserPreferences.getModel().setAddLabeledTrainingImages(false);
            }
            
            // Initialize button states
            getView().setRunButton(false);
            getView().setStopButton(true);

            // Initialize variables used in thread
            final VotingMethod                     method          = getView().getVotingMethod();
            final RunClassifierControllerBatch     runController   = this;
            final TrainingModel                    trainingModel   = getTrainingModel().copy();
            final ClassifierBatchProcessController batchController = this.controller;
            final float minProbThreshold = getView().getProbabilityThreshold();
            final BatchProcessAbstractTableModel   tmodel          = batchController.getAbstractModel();
            final int                              size            = view.getSelectedTable().getRowCount();

            // Create a progress display thread for monitoring this task
            Thread thread = new Thread() {
                @Override
                public void run() {
                    final SwingWorker worker = Classifier.getController().getWorker();
                    BufferedReader    br     = Classifier.getController().getBufferedReader();

                    for (int i = 0; i < size; i++) {
                        importXmlWorker     = null;
                        transcodeWorker     = null;
                        runClassifierWorker = null;

                        BatchProcessDataModel batch = (BatchProcessDataModel) tmodel.getValueAt(i, -1);

                        if (!batch.getStatus().equals("Done")) {
                            try {
                                ApplicationModel model   = batch.getModel();
                                EventListModel   list    = model.getEventListModel();
                                File             xmlfile = model.getSummaryModel().getXmlFile();

                                // import events xml
                                importXmlWorker = new ImportXMLWorker(xmlfile, model, runController, false);
                                batch.setStatus("Importing xml...");
                                tmodel.fireTableDataChanged();
                                importXmlWorker.run();

                                if (importXmlWorker.isCancelled()) {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                       trainingModel.getName()
                                                                       + " classification stopped");

                                    batch.setStatus("Stopped");
                                    tmodel.fireTableDataChanged();
                                    dialog.setVisible(true);

                                    return;
                                }

                                // if no events in this, then just skip
                                if (list.getSize() > 0) {

                                    // transcode
                                    SummaryModel summary = model.getSummaryModel();

                                    findTranscodeSource(summary);

                                    File file = summary.getTranscodeSource();

                                    transcodeWorker = new VideoTranscodeWorker(runController, model, file, false);
                                    transcodeWorker.setMaxFrame(importXmlWorker.getMaxEventFrame());
                                    batch.setStatus("Transcoding...");
                                    tmodel.fireTableDataChanged();
                                    transcodeWorker.run();

                                    if (transcodeWorker.isCancelled()) {
                                        NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                           trainingModel.getName()
                                                                           + " classification stopped");

                                        batch.setStatus("Stopped");
                                        tmodel.fireTableDataChanged();
                                        dialog.setVisible(true);
                                        transcodeWorker.reset();

                                        return;
                                    }

                                    // make the test directory  a subdirectory of transcoding.
                                    // Putting this test images in a subdirectory makes for easy deletion later
                                    File s          = summary.getFrameSourceDir();
                                    File testingDir = new File(s + "/testimages_" + s.getName());

                                    if (!testingDir.exists()) {
                                        testingDir.mkdir();
                                    }

                                    summary.setTestImageDir(testingDir);

                                    // classify
                                    runClassifierWorker = new RunClassifierWorker(trainingModel, minProbThreshold,
                                            testingDir, list, method);
                                    batch.setStatus("Classifying...");
                                    tmodel.fireTableDataChanged();
                                    Classifier.getController().addQueue(runClassifierWorker);

                                    ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                                                                          "Running classifier with training model "
                                                                          + trainingModel.getName());

                                    progressDisplay.getView().setVisible(true);

                                    ProgressDisplayStream progressDisplayStream =
                                        new ProgressDisplayStream(progressDisplay, br);

                                    progressDisplayStream.execute();

                                    while (!runClassifierWorker.isCancelled() &&!runClassifierWorker.isFini()) {
                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException ex) {
                                            Logger.getLogger(RunClassifierControllerBatch.class.getName()).log(
                                                Level.SEVERE, null, ex);

                                            NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                               trainingModel.getName()
                                                                               + " classification stopped "
                                                                               + ex.getMessage());

                                            batch.setStatus("Stopped");
                                            tmodel.fireTableDataChanged();
                                            dialog.setVisible(true);
                                            transcodeWorker.reset();
                                        }
                                    }

                                    if (runClassifierWorker.isCancelled()) {
                                        NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                           trainingModel.getName()
                                                                           + " classification stopped");

                                        dialog.setVisible(true);
                                        batch.setStatus("Stopped");
                                        tmodel.fireTableDataChanged();
                                        transcodeWorker.reset();

                                        return;
                                    }

                                    TableModel cmTableModel = runClassifierWorker.getTableModel();
                                    int        rowCnt       = cmTableModel.getRowCount();

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
                                        NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                           trainingModel.getName()
                                                                           + " classification stopped");

                                        batch.setStatus("Stopped");
                                        tmodel.fireTableDataChanged();
                                        transcodeWorker.reset();
                                        dialog.setVisible(true);

                                        return;
                                    }

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
                                    progressDisplay.getView().dispose();
                                }

                                batch.setStatus("Done");
                                tmodel.fireTableDataChanged();
                            } catch (Exception ex) {
                                batch.setStatus(ex.getMessage());
                                Logger.getLogger(RunClassifierControllerBatch.class.getName()).log(Level.SEVERE, null,
                                                 ex);
                            }
                        }
                    }

                    getView().setRunButton(true);
                    getView().setStopButton(false);
                }
            };

            thread.run();

            // Reset the user preference
            UserPreferences.getModel().setAddLabeledTrainingImages(isAddTrainingImages);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("Run")) {
            importXmlWorker     = null;
            transcodeWorker     = null;
            runClassifierWorker = null;
            exportXmlWorker     = null;
            run();
        } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
            JComboBox  box           = ((JComboBox) e.getSource());
            ColorSpace newColorSpace = (ColorSpace) box.getSelectedItem();
            String lastSelection = UserPreferences.getModel().getTrainingLibrarySelection();

            if (getView() != null) {
                // Populate the libraries in the new color space
                if (newColorSpace != null) {
                    getView().populateTrainingLibraryList(newColorSpace);
                }

                // Set the library
                if (getView().selectLibrary(lastSelection) == false) {
                    getView().clearClassModelList();
                }
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
            TrainingModel trainingModel = (TrainingModel) box.getSelectedItem();

            if (trainingModel != null) {
                String selection = trainingModel.getName();
                UserPreferences.getModel().setTrainingLibrarySelection(selection);     
                getModel().notifyTrainingModelChanged(trainingModel.getName());
            }
        }
    }

    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {

            // When the database root directory change or the models are updated
            // reset the color space
            case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED :
            case ClassifierModel.ClassifierModelEvent.TRAINING_MODELS_UPDATED :
                TrainingModel trainingModel = getTrainingModel();

                if (trainingModel != null) {
                    getView().populateTrainingLibraryList(trainingModel.getColorSpace());
                } else {
                    getView().populateTrainingLibraryList(ColorSpace.RGB);
                }

                break;
            }
        }
    }

    TrainingModel getTrainingModel() {
        return getView().getTrainingModel();
    }
}
