/*
 * @(#)RunController.java
 * 
 * Copyright 2013 MBARI
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

import java.io.IOException;

import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.classifier.table.TableController;
import org.mbari.aved.ui.classifier.table.TableModel;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.SummaryModel;
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
import org.mbari.aved.ui.process.ProcessDisplay;

public class RunController extends AbstractController implements ModelListener {
    private EventListModel      eventListModel;
    private SummaryModel        summaryModel;
    private RunWorker task;

    RunController(ClassifierModel model) {
        setModel(model);
        setView(new RunView(model, this));

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
    public RunView getView() {
        return ((RunView) super.getView());
    }

    public void run() {
        if ((eventListModel != null) && (summaryModel != null)) {
            if (eventListModel.getSize() == 0) {
                String                msg = "Please open a valid XML file ";
                NonModalMessageDialog dialog;

                dialog = new NonModalMessageDialog(getView(), msg);
                dialog.setVisible(true);
                getView().setStopButton(false);
                getView().setRunButton(true);

                return;
            } else {
                try { 
                    final String        name             = getView().getTrainingModelName();
                    final float         minProbThreshold = getView().getProbabilityThreshold(); 
                    final TrainingModel trainingModel    = getModel().getTrainingModel(getView().getTrainingModelName());
                    
                    // Create a progress display thread for monitoring this 
                    final ProcessDisplay display = new ProcessDisplay(
                            "Running classifier with training model "
                            + trainingModel.getName());
                     
                    display.getView().setVisible(true);
                    
                    // Temporarily turn off this user preference to not add all the
                    // predicted images to the library during assignment
                    final boolean isAddTrainingImages = UserPreferences.getModel().getAddTrainingImages();

                    if (isAddTrainingImages) {
                        UserPreferences.getModel().setAddTrainingImages(false);
                    }

                    // Get the voting method used for determining the winner
                    final VotingMethod method  = getView().getVotingMethod();
                    File               testDir = summaryModel.getTestImageDirectory();

                    // Create worker to put in the Matlab queue later
                    task = new RunWorker(trainingModel, minProbThreshold, testDir, eventListModel, method, display);
                    

                    BufferedReader br = Classifier.getController().getBufferedReader();
                    final ProgressDisplayStream matlabDisplayStream = new ProgressDisplayStream(display,
                            br);

                    matlabDisplayStream.execute();


                    Thread thread = new Thread() {

                        @Override
                        public void run() {

                            Classifier.getController().addQueue(task);
                            getView().setRunButton(false);
                            getView().setStopButton(true);

                            while (!task.isCancelled() && !task.isFini()) {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException ex) {
                                }
                            }

                            getView().setRunButton(true);
                            getView().setStopButton(false);

                            display.getView().dispose();

                            // Reset the user preference
                            UserPreferences.getModel().setAddTrainingImages(isAddTrainingImages);

                            // Add the results only after successfully run
                            if (task.isFini() && (getModel() != null)) {
                                try {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                       trainingModel.getName()
                                                                       + " classification finished");

                                    dialog.setVisible(true);

                                    // Make the title the same as the XML file name
                                    String          xmlFilename= summaryModel.getXmlFile().getName();
                                    String          title      = ParseUtils.removeFileExtension(xmlFilename);
                                    String          exportFilename = title + ".xls";
                                    VotingMethod    method     = getView().getVotingMethod();  
                                       
                                    createTableControllerView(exportFilename, getModel(), task.getTableModel(), title, "Confusion Matrix for " + title
                                                + ", Probability Threshold: " + minProbThreshold + " , Voting Method: "
                                                + method.name(), true);
                                } catch (IOException ex) {
                                    Logger.getLogger(RunController.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                               
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
                            
                            matlabDisplayStream.done();
                        }
                    };

                    thread.start();
                } catch (Exception ex) {
                    Logger.getLogger(RunController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Helper method to create confusion matrix from table results and export to a file if not displayed
     * 
     * @param exportFilename
     * @param model
     * @param tableModel
     * @param title
     * @param description
     * @param b
     * @throws IOException 
     */
    public static void createTableControllerView(String exportFilename, ClassifierModel model, 
            TableModel tableModel, String title, String description, boolean b) throws IOException {
        TableController controller = new TableController(model, tableModel,
                "testclass" + title);

        controller.getView().setTitle(title);
        controller.getView().setDescription(description);
        controller.getView().pack();
        controller.getView().setVisible(b);
         
        File f = new File(UserPreferences.getModel().getExportedExcelDirectory() + "/" + exportFilename); 
        
        if (!b) { 
            TableController.exportTable(controller.getTable(), f );
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
              getView().populateTrainingLibraryList(newColorSpace, lastSelection); 
	    }
        } else if (actionCommand.equals("Stop")) {
            if (task != null) {
                getView().setRunButton(true);
                getView().setStopButton(false);
                Classifier.getController().kill(task);
            }
        } else if (actionCommand.equals("availLibraryNameComboBoxChanged")) {
            JComboBox box = ((JComboBox) e.getSource());
            TrainingModel trainingModel = (TrainingModel) box.getSelectedItem();

            getView().loadModel(trainingModel);

            String selection = trainingModel.getName();

            UserPreferences.getModel().setTrainingLibrarySelection(selection);
        }
    }
    
    @Override
    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            String     lastSelection = UserPreferences.getModel().getTrainingLibrarySelection();
            switch (event.getID()) {

            // When the database root directory change or the models are updated
            // reset the color space
            case ClassifierModel.ClassifierModelEvent.CLASSIFIER_IMAGE_DIR_MODEL_CHANGED  :
                ColorSpace c = UserPreferences.getModel().getColorSpace();
                getView().populateTrainingLibraryList(c, lastSelection);
                getView().setColorSpace(c);
                break;
            case ClassifierModel.ClassifierModelEvent.TRAINING_MODELS_UPDATED :
                ColorSpace cs = getView().getColorSpace();
                getView().populateTrainingLibraryList(cs, lastSelection);             

                break;
            }
        }
    }
}
