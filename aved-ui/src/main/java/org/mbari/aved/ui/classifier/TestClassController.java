/*
 * @(#)TestClassController.java
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
package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------
import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.classifier.table.TableController;
import org.mbari.aved.ui.classifier.table.TableModel;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.progress.ProgressDisplayStream;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.io.BufferedReader;

import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import org.jdesktop.swingworker.SwingWorker;

class TestClassController extends AbstractController implements ModelListener {

    private RunTestClassTask task;

    TestClassController(ClassifierModel model) {
        setModel(model);
        setView(new TestClassView(model, this));

        // Register as listener to the models
        getModel().addModelListener(this);
    }

    @Override
    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    @Override
    public TestClassView getView() {
        return (TestClassView) super.getView();
    }

    /**
     * Operation handler for handling actions initiated in the view
     *
     * @param actionCommand A semantic event which indicates that a
     * component-defined action occurred.
     */
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (getView() != null) {
            if (actionCommand.equals("classNameComboBoxChanged")) {
                JComboBox box = ((JComboBox) e.getSource());
                ClassModel model = (ClassModel) box.getSelectedItem();

                getView().loadClassModel(model);
            } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
                JComboBox box = ((JComboBox) e.getSource());
                ColorSpace newColorSpace = (ColorSpace) box.getSelectedItem();

                if (newColorSpace != null) {
                    getView().populateTrainingLibraryList(newColorSpace);
                    getView().populateClassList(newColorSpace);
                }
            } else if (actionCommand.equals("libraryNameComboBoxChanged")) {
                JComboBox box = ((JComboBox) e.getSource());
                TrainingModel model = (TrainingModel) box.getSelectedItem();

                getView().loadTrainingModel(model);
            } else if (actionCommand.equals("Stop")) {
                if (task != null) {
                    getView().setRunButton(true);
                    getView().setStopButton(false);
                    Classifier.getController().kill(task);
                }
            } else if (actionCommand.equals("Run")) {
                try {
                    final ClassModel classModel = getView().getClassModel();
                    final TrainingModel trainingModel = getView().getTrainingModel();

                    if (classModel == null) {
                        String message = new String("Please select a class to test");
                        NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                        dialog.setVisible(true);

                        if (dialog.answer()) {
                            return;
                        }
                    }

                    if (trainingModel == null) {
                        String message = new String("Please select a test library");
                        NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                        dialog.setVisible(true);

                        if (dialog.answer()) {
                            return;
                        }
                    }

                    // Count the number of unique events in this model to use
                    // to allocate the arrays to pass to the Matlab function
                    ArrayList<String> fileName = classModel.getRawImageFileListing();

                    if (fileName.size() == 0) {
                        NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                "Error - cannot find the images for this class");

                        dialog.setVisible(true);
                        return;
                    }

                    final SwingWorker worker = Classifier.getController().getWorker();
                    task = new RunTestClassTask(classModel, trainingModel);
                    Classifier.getController().addQueue(task);
                    getView().setRunButton(false);
                    getView().setStopButton(true);


                    /// Create a progress display thread for monitoring this task
                    Thread thread = new Thread() {

                        @Override
                        public void run() {
                            BufferedReader br = Classifier.getController().getBufferedReader();
                            ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                                    "Testing class " + classModel.getName() + " against " + trainingModel.getName());

                            progressDisplay.getView().setVisible(true);

                            ProgressDisplayStream progressDisplayStream = new ProgressDisplayStream(progressDisplay, br);
                            progressDisplayStream.execute();
                            while (!task.isCancelled() && !task.isFini()) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException ex) {
                                }
                            }
                            getView().setRunButton(true);
                            getView().setStopButton(false);
                            progressDisplay.getView().dispose();

                            if (task.isFini()) {
                                try {
                                    // Add to training model when successfully run
                                    getModel().addTrainingModel(trainingModel);
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), trainingModel.getName() + " test finished");
                                    dialog.setVisible(true);
                                    task.controller.getView().pack();
                                    task.controller.getView().setVisible(true);
                                } catch (Exception ex) {
                                    Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                if (task.isCancelled()) {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), trainingModel.getName() + " test stopped");
                                    dialog.setVisible(true);
                                }
                            }
                        }
                    };

                    thread.start();
                } catch (Exception ex) {
                    Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Model listener. Reacts to changes in the
     * {@link org.mbari.aved.ui.classifier.model}
     * and  {@link org.mbari.aved.ui.classifier.ClassifierModel}
     */
    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            ColorSpace colorSpace = (ColorSpace) getView().getClassColorSpace();

            switch (event.getID()) {

                // When the database root directory changes, update the available 
                case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED:
                    getView().populateTrainingLibraryList(colorSpace);
                    getView().populateClassList(colorSpace);

                    break;

                case ClassifierModel.ClassifierModelEvent.CLASS_MODELS_UPDATED:
                    getView().populateClassList(colorSpace);

                    break;

                case ClassifierModel.ClassifierModelEvent.TRAINING_MODELS_UPDATED:
                    if (getView().getClassModel() != null) {
                        getView().populateTrainingLibraryList(colorSpace);
                    } else {
                        getView().populateTrainingLibraryList(ColorSpace.RGB);
                    }

                    break;
            }
        }
    }

    private class RunTestClassTask extends ClassifierLibraryJNITask {

        private final ClassModel classModel;
        private final TrainingModel trainingModel;
        private TableModel tableModel;
        private TableController controller = null;

        public RunTestClassTask(ClassModel classModel, TrainingModel trainingModel) throws Exception {
            super(classModel.getName());
            this.classModel = classModel;
            this.trainingModel = trainingModel;
        }

        @Override
        protected void run(ClassifierLibraryJNI library) throws Exception {

            // Get a input stream on the matlab log file to display in
            // the progress display window
            try {

                // Count the number of unique events in this model to use
                // to allocate the arrays to pass to the Matlab function
                ArrayList<String> fileName = classModel.getRawImageFileListing();

                if (fileName.size() > 0) {

                    int numEvents;

                    // Round up a 10% file size; this is what is sampled
                    // for class testing. This is hardcoded in the Matlab code
                    // so don't change this without changing the Matlab code
                    // and recompiling. If less than 10 files in the class to
                    // test then use all of them
                    if (fileName.size() > 10) {
                        numEvents = (int) (0.10 * (fileName.size()) + 0.5);
                    } else {
                        numEvents = fileName.size();
                    }

                    // Allocate the arrays for storing the results
                    int[] classIndex = new int[numEvents];
                    float[] probability = new float[numEvents];
                    String[] eventFilenames = new String[numEvents];
                    float minProbThreshold = 0.8f;

                    System.out.println("Running test class");
                    System.out.println("Testing " + classModel.getName() + " against training library:"
                            + trainingModel.getName() + " with minimum probability:" + minProbThreshold);

                    // Run the class tests
                    library.test_class(this.getCancel(), eventFilenames, classIndex, probability, classModel.getName(),
                            trainingModel.getName(), minProbThreshold,
                            classModel.getDatabaseRootdirectory().toString(), classModel.getColorSpace());

                    // Dump out some debuging info. TODO: remove this when done
                    // testing
                    if ((classIndex != null) && (probability != null)) {
                        for (int i = 0; i < numEvents; i++) {
                            System.out.println("filename:" + eventFilenames[i] + "\tclassindex:" + classIndex[i]
                                    + "\tprobability in class:" + probability[i]);
                        }
                    }

                    // Add one column for the Unknown class
                    int columns = trainingModel.getNumClasses() + 1;
                    int rows = columns;
                    String[] columnName = new String[columns];
                    int[][] statistic = new int[rows][columns];
                    int sum[] = new int[trainingModel.getNumClasses() + 1];

                    // Format the column name and sums for display in a JTable
                    for (int j = 0; j < columns; j++) {
                        if (j > 0) {
                            columnName[j] = trainingModel.getClassModel(j - 1).getName();
                        } else {
                            columnName[0] = TrainingModel.UNKNOWN_CLASS_LABEL;
                        }
                    }

                    // Sum up all the winners and put into the appropriate bucket
                    for (int k = 0; k < numEvents; k++) {
                        if (classIndex[k] > 0) {
                            sum[classIndex[k] - 1]++;
                        }
                    }

                    // Format the statistic array for display in a JTable
                    for (int j = 0; j < columns; j++) {
                        if (j > 0) {
                            columnName[j] = trainingModel.getClassModel(j - 1).getName();
                        } else {
                            columnName[0] = TrainingModel.UNKNOWN_CLASS_LABEL;
                        }

                        for (int i = 0; i < rows; i++) {
                            if ((i == j) && (i < columns)) {
                                int ttl = 0;

                                for (int k = 0; k < numEvents; k++) {
                                    if (classIndex[k] == (i + 1)) {
                                        ttl++;
                                    }
                                }

                                statistic[i][j] = ttl;
                            } else {
                                statistic[i][j] = 0;
                            }
                        }
                    }

                    // Put the statistic and column names in a TableModel
                    tableModel = new TableModel(columnName, statistic, sum);
                    controller = new TableController(getModel(), tableModel,
                            "testclass" + classModel.getName());

                    controller.getView().setTitle(classModel.getName());

                    controller.getView().setDescription("Confusion Matrix for " + classModel.getName()
                            + ", Probability Threshold: " + minProbThreshold);

                    this.setFini();

                }
            } catch (Exception ex) {
                Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
