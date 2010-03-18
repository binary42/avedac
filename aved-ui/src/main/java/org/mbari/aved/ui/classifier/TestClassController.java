/*
 * @(#)TestClassController.java   10/03/17
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

import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;

class TestClassController extends AbstractController implements ModelListener {
    private RunTestClassWorker worker;

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

        if (actionCommand.equals("classNameComboBoxChanged")) {
            JComboBox  box   = ((JComboBox) e.getSource());
            ClassModel model = (ClassModel) box.getSelectedItem();

            if (model != null) {
                getView().loadModel(model);
                getView().populateTrainingLibraryList(model.getColorSpace());
            }
        } else if (actionCommand.equals("libraryNameComboBoxChanged")) {
            JComboBox     box   = ((JComboBox) e.getSource());
            TrainingModel model = (TrainingModel) box.getSelectedItem();

            if (model != null) {
                getView().loadModel(model);
            }
        } else if (actionCommand.equals("Stop")) {
            if ((worker != null) &&!worker.isDone()) {
                worker.cancelWorker(true);
            }
        } else if (actionCommand.equals("Run")) {
            try {
                ClassModel    classModel    = getView().getClassModel();
                TrainingModel trainingModel = getView().getTrainingModel();

                if (classModel == null) {
                    String                message = new String("Please select a class to test");
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (trainingModel == null) {
                    String                message = new String("Please select a test library");
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                worker = new RunTestClassWorker(classModel, trainingModel);
                worker.execute();
            } catch (Exception ex) {
                Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);
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
            switch (event.getID()) {

            // When the database root directory changes, update the available
            // libraries, defaulting to those in the RGB color space.
            case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED :
            case ClassifierModel.ClassifierModelEvent.CLASS_MODELS_UPDATED :
                break;
            }
        }
    }

    private class RunTestClassWorker extends MatlabWorker {
        private final ClassModel      classModel;
        private final ProgressDisplay progressDisplay;
        private final TrainingModel   trainingModel;

        public RunTestClassWorker(ClassModel classModel, TrainingModel trainingModel) throws Exception {
            super(classModel.getName());
            this.classModel      = classModel;
            this.trainingModel   = trainingModel;
            this.progressDisplay = new ProgressDisplay(this, "Testing class " + classModel.getName());
        }

        @Override @SuppressWarnings("empty-statement")
        protected Object doInBackground() throws Exception {
            progressDisplay.display("Testing class ...");

            // Get a input stream on the matlab log file to display in
            // the progress display window
            try {
                ClassifierLibraryJNI  app = Classifier.getLibrary();
                InputStreamReader     isr = Classifier.getInputStreamReader();
                ProgressDisplayStream progressDisplayStream;

                progressDisplayStream = new ProgressDisplayStream(progressDisplay, isr);
                progressDisplayStream.execute();

                // Count the number of unique events in this mocel to use
                // to allocate the arrays to pass to the Matlab function
                ArrayList<String> fileName = classModel.getRawImageFileListing();

                if (fileName.size() == 0) {
                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                       "Error - cannot find the images for this class");

                    dialog.setVisible(true);
                    progressDisplay.getView().dispose();

                    return this;
                }

                int numEvents;

                // Round up a 10% sample size; this is what is sampled
                // for class testing. This is hardcoded in the Matlab code
                // so don't change this without changing the Matlab code
                // and recompiling
                if (fileName.size() > 10) {
                    numEvents = (int) (0.10 * (fileName.size()) + 0.5);
                } else {
                    numEvents = fileName.size();
                }

                // Allocate the arrays for storing the results
                int[]    classIndex       = new int[numEvents];
                float[]  probability      = new float[numEvents];
                String[] eventFilenames   = new String[numEvents];
                float    minProbThreshold = 0.8f;

                System.out.println("Running test class");

                // Run the class tests
                app.test_class(this.getCancel(), eventFilenames, classIndex, probability, classModel.getName(),
                               trainingModel.getName(), minProbThreshold,
                               classModel.getDatabaseRootdirectory().toString());
                System.out.println("Testing " + classModel.getName() + "against training library:"
                                   + trainingModel.getName() + "with  minimum probability:" + minProbThreshold);

                // Dump out some debuging info. TODO: remove this when done
                // testing
                if ((classIndex != null) && (probability != null)) {
                    for (int i = 0; i < numEvents; i++) {
                        System.out.println("filename:" + eventFilenames[i] + "\tclassindex:" + classIndex[i]
                                           + "\tprobability in class:" + probability[i]);
                    }
                }

                // Kill the progress display
                progressDisplayStream.isDone = true;
                progressDisplay.getView().dispose();

                // Add one column for the Unknown class
                int      columns    = trainingModel.getNumClasses() + 1;
                int      rows       = columns;
                String[] columnName = new String[columns];
                int[][]  statistic  = new int[rows][columns];
                int      sum[]      = new int[trainingModel.getNumClasses() + 1];

                // Format the column name and sums for display in a JTable
                for (int j = 0; j < columns; j++) {
                    if (j > 0) {
                        columnName[j] = trainingModel.getClassModel(j - 1).getName();
                    } else {
                        columnName[0] = TrainingModel.UNKNOWN_CLASS_LABEL;
                    }
                }

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
                TableModel      tableModel = new TableModel(columnName, statistic, sum);
                TableController controller = new TableController(getModel(), tableModel,
                                                 "testclass" + classModel.getName());

                controller.getView().setTitle(classModel.getName());;
                controller.getView().setDescription("Confusion Matrix for " + classModel.getName()
                        + ", Probability Threshold: " + minProbThreshold);
                controller.getView().pack();
                controller.getView().setVisible(true);

                NonModalMessageDialog dialog;

                dialog = new NonModalMessageDialog(getView(), classModel.getName() + " testing finished");
                dialog.setVisible(true);
            } catch (Exception ex) {
                Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);

                NonModalMessageDialog dialog;

                dialog = new NonModalMessageDialog(getView(), ex.getMessage());
                dialog.setVisible(true);
                setProgress(0);
            }

            getView().setRunButton(true);
            getView().setStopButton(false);

            return this;
        }
    }
}
