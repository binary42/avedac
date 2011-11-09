/*
 * @(#)RunClassifierController.java
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
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.classifier.table.TableController;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.progress.ProgressDisplayStream;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.ParseUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import java.io.BufferedReader;
import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;

public class RunClassifierController extends AbstractController implements ModelListener {
    private EventListModel      eventListModel;
    private SummaryModel        summaryModel;
    private RunClassifierWorker task;

    RunClassifierController(ClassifierModel model) {
        setModel(model);
        setView(new RunClassifierView(model, this));

        // Register as listener to the models
        getModel().addModelListener(this);
    }

    public void setEventListModel(final EventListModel eventListModel) {
        this.eventListModel = eventListModel;
        this.eventListModel.addModelListener(this);
    }

    public void setSummaryModel(final SummaryModel summaryModel) {
        this.summaryModel = summaryModel;
        this.summaryModel.addModelListener(this);
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

    public void run() {
        if ((eventListModel != null) && (summaryModel != null)) {
            if (eventListModel.getSize() == 0) {
                String                msg = "Please open a valid XML file ";
                NonModalMessageDialog dialog;

                dialog = new NonModalMessageDialog(getView(), msg);
                dialog.setVisible(true);
                getView().setStopButton(false);

                return;
            } else {
                try {

                    // Temporarily turn off this user preference to not add all the
                    // predicted images to the library during assignment
                    final boolean isAddTrainingImages = UserPreferences.getModel().getAddTrainingImages();

                    if (isAddTrainingImages) {
                        UserPreferences.getModel().setAddLabeledTrainingImages(false);
                    }

                    // If at least one class then create
                    final SwingWorker   worker           = Classifier.getController().getWorker();
                    final float         minProbThreshold = getView().getProbabilityThreshold();
                    final TrainingModel trainingModel    = getView().getTrainingModel().copy();

                    // Get the voting method used for determining the winner
                    final VotingMethod method  = getView().getVotingMethod();
                    File               testDir = summaryModel.getTestImageDirectory();

                    task = new RunClassifierWorker(trainingModel, minProbThreshold, testDir, eventListModel, method);

                    // / Create a progress display thread for monitoring this task
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            BufferedReader  br              = Classifier.getController().getBufferedReader();
                            ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                                                                  "Running classifier with training model "
                                                                  + trainingModel.getName());

                            progressDisplay.getView().setVisible(true);

                            ProgressDisplayStream progressDisplayStream = new ProgressDisplayStream(progressDisplay,
                                                                              br);

                            progressDisplayStream.execute();
                            Classifier.getController().addQueue(task);
                            getView().setRunButton(false);
                            getView().setStopButton(true);

                            while (!task.isCancelled() &&!task.isFini()) {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException ex) {}
                            }

                            getView().setRunButton(true);
                            getView().setStopButton(false);
                            progressDisplay.getView().dispose();

                            // Reset the user preference
                            UserPreferences.getModel().setAddLabeledTrainingImages(isAddTrainingImages);

                            // Add the results only after successfully run
                            if (task.isFini() && (getModel() != null)) {
                                NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                   trainingModel.getName()
                                                                   + " classification finished");

                                dialog.setVisible(true);

                                // Make the title the same as the XML file name
                                String          xmlFile    = summaryModel.getXmlFile().getName();
                                String          title      = ParseUtils.removeFileExtension(xmlFile);
                                TableController controller = new TableController(getModel(), task.getTableModel(),
                                                                 "testclass" + title);
                                VotingMethod method = getView().getVotingMethod();

                                controller.getView().setTitle(title);
                                controller.getView().setDescription("Confusion Matrix for " + title
                                        + ", Probability Threshold: " + minProbThreshold + " , Voting Method: "
                                        + method.name());
                                controller.getView().pack();
                                controller.getView().setVisible(true);
                            } else {
                                if (task.isCancelled()) {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                       trainingModel.getName()
                                                                       + " classification stopped");

                                    dialog.setVisible(true);
                                } else {
                                    getView().setRunButton(true);
                                    getView().setStopButton(false);
                                }
                            }
                        }
                    };

                    thread.start();
                } catch (Exception ex) {
                    Logger.getLogger(RunClassifierController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
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
            String     lastSelection = UserPreferences.getModel().getTrainingLibrarySelection();
            
            // Populate the libraries in the new color space
	    if (getView() != null) { 
              getView().populateTrainingLibraryList(newColorSpace);

              // Set the library
              if (getView().selectLibrary(lastSelection) == false) {
                getView().clearClassModelList();
              }
	    }
        } else if (actionCommand.equals("Stop")) {
            if (task != null) {
                getView().setRunButton(true);
                getView().setStopButton(false);
                Classifier.getController().kill(task);
            }
        } else if (actionCommand.equals("availLibraryNameComboBoxChanged")) {
            JComboBox     box           = ((JComboBox) e.getSource());
            TrainingModel trainingModel = (TrainingModel) box.getSelectedItem();

            if (trainingModel != null) {
                getView().loadModel(trainingModel);

                String selection = trainingModel.getName();

                UserPreferences.getModel().setTrainingLibrarySelection(selection);
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
                if (getView().getTrainingModel() != null) {
                    getView().populateTrainingLibraryList(getView().getTrainingModel().getColorSpace());
                } else {
                    getView().populateTrainingLibraryList(ColorSpace.RGB);
                }

                break;
            }
        }
    }
}
