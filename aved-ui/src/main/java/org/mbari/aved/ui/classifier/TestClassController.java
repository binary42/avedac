/*
 * @(#)TestClassController.java
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import org.mbari.aved.ui.userpreferences.UserPreferences;

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
    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (getView() != null) {
            if (actionCommand.equals("classNameComboBoxChanged")) {
                JComboBox  box   = ((JComboBox) e.getSource());
                ClassModel model = (ClassModel) box.getSelectedItem();                
                
                getView().loadClassModel(model); 
                
            } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
                JComboBox  box           = ((JComboBox) e.getSource());
                ColorSpace newColorSpace = (ColorSpace) box.getSelectedItem();                
                String     lastSelection = UserPreferences.getModel().getTrainingLibrarySelection();
                
                UserPreferences.getModel().setColorSpace(newColorSpace);   
            
                // Populate the libraries in the new color space
                if (getView() != null) {
                    getView().populateTrainingLibraryList(newColorSpace, lastSelection); 
                }
            } else if (actionCommand.equals("libraryNameComboBoxChanged")) {
                JComboBox     box   = ((JComboBox) e.getSource());
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
                    final ClassModel    classModel    = getView().getClassModel().copy();
                    final TrainingModel trainingModel = getView().getTrainingModel().copy();

                    if (classModel == null) {
                        String                message = "Please select a class to test";
                        NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                        dialog.setVisible(true);

                        if (dialog.answer()) {
                            return;
                        }
                    }

                    if (trainingModel == null) {
                        String                message = "Please select a test library";
                        NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                        dialog.setVisible(true);

                        if (dialog.answer()) {
                            return;
                        }
                    }

                    // Count the number of unique events in this model to use
                    // to allocate the arrays to pass to the Matlab function
                    ArrayList<String> fileName = classModel.getRawImageFileListing();

                    if (fileName.isEmpty()) {
                        NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                           "Error - cannot find the images for this class");

                        dialog.setVisible(true);

                        return;
                    }

                    final SwingWorker worker = Classifier.getController().getWorker();

                    task = new RunTestClassTask(classModel, trainingModel);

                    // / Create a progress display thread for monitoring this task
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            BufferedReader  br              = Classifier.getController().getBufferedReader();
                            ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                                                                  "Testing class " + classModel.getName() + " against "
                                                                  + trainingModel.getName());

                            progressDisplay.getView().setVisible(true);
                            
                            // Redirect err/out to progress display
                            System.setOut(new PrintStream(progressDisplay, true));
                            System.setErr(new PrintStream(progressDisplay, true));

                            ProgressDisplayStream progressDisplayStream = new ProgressDisplayStream(progressDisplay,
                                                                              br);

                            progressDisplayStream.execute();
                            Classifier.getController().addQueue(task);
                            getView().setRunButton(false);
                            getView().setStopButton(true);

                            while (!task.isCancelled() &&!task.isFini()) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }

                            getView().setRunButton(true);
                            getView().setStopButton(false);
                            
                            progressDisplayStream.done();
                            progressDisplay.getView().dispose();

                            if (task.isFini()) {
                                try {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                       trainingModel.getName() + " test finished");

                                    dialog.setVisible(true);
                                    task.controller.getView().pack();
                                    task.controller.getView().setVisible(true);
                                } catch (Exception ex) {
                                    Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                if (task.isCancelled()) {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                       trainingModel.getName() + " test stopped");

                                    dialog.setVisible(true);
                                }
                            }
                        }
                    };

                    thread.start();
                } catch (Exception ex) {
                    Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);

                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), ex.getMessage());

                    dialog.setVisible(true);
                }
            }
        }
    }

    /**
     * Model listener. Reacts to changes in the
     * {@link org.mbari.aved.ui.classifier.model}
     * and  {@link org.mbari.aved.ui.classifier.ClassifierModel}
     */
    @Override
    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            ColorSpace colorSpace = (ColorSpace) getView().getClassColorSpace();

            switch (event.getID()) {

            // When the database root directory changes, update the available libraries
            case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED : 
                getView().setColorSpace(UserPreferences.getModel().getColorSpace());
                break;

            case ClassifierModel.ClassifierModelEvent.TRAINING_MODELS_UPDATED :
                getView().populateTrainingLibraryList(colorSpace, UserPreferences.getModel().getTrainingLibrarySelection());               
                break;

            case ClassifierModel.ClassifierModelEvent.CLASS_MODELS_UPDATED :
                getView().populateClassList(colorSpace, UserPreferences.getModel().getClassName());

                break;
            }
        }
    }

    private class RunTestClassTask extends ClassifierLibraryJNITask {
        private TableController     controller = null;
        private final ClassModel    classModel;
        private TableModel          tableModel;
        private final TrainingModel trainingModel;

        public RunTestClassTask(ClassModel classModel, TrainingModel trainingModel) throws Exception {
            super(classModel.getName());
            this.classModel    = classModel.copy();
            this.trainingModel = trainingModel.copy();
        }

        @Override
        protected void run(ClassifierLibraryJNI library) {

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
                        numEvents = (int) Math.round(0.10 * (fileName.size()));
                    } else {
                        numEvents = fileName.size();
                    }

                    // Allocate the arrays for storing the results
                    int[]    classIndex       = new int[numEvents];
                    float[]  probability      = new float[numEvents];
                    String[] eventFilenames   = new String[numEvents];
                    float    minProbThreshold = 0.8f;

                    Logger.getLogger(TestClassController.class.getName()).log(Level.INFO,
                                     "Testing " + classModel.getName() + " against training library "
                                     + trainingModel.getName() + " with minimum probability:" + minProbThreshold
                                     + ". Saving to dbroot: " + classModel.getDatabaseRootdirectory().toString());

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
                    int      columns    = trainingModel.getNumClasses() + 1;
                    int      rows       = columns;
                    String[] columnName = new String[columns];
                    int[][] statistic = new int[rows][columns];
                    int sum[] = new int[trainingModel.getNumClasses() + 1];

            
                    // Create hash map for look-up of the class index by name  
                    HashMap<String, Integer> map = new HashMap<String, Integer>(columns, 0.75f);

                    map.put(TrainingModel.UNKNOWN_CLASS_LABEL, new Integer(0));

                    for (int j = 1; j < columns; j++) {
                        map.put(trainingModel.getClassModel(j - 1).getName(), new Integer(j));
                    }
 
                    // Format the column name and sums for display in a JTable
                    for (int j = 0; j < columns; j++) {
                        if (j > 0) {
                            columnName[j] = trainingModel.getClassModel(j - 1).getName();
                        } else {
                            columnName[0] = TrainingModel.UNKNOWN_CLASS_LABEL;
                        }
                    } 
 
                    int j = map.get(classModel.getName());
                            
                    for (int k = 0; k < numEvents; k++) {
                        int i = classIndex[k] - 1; 

                        sum[i]++;
                        statistic[i][j]++;

                    }

                    // Put the statistic and column names in a TableModel
                    tableModel = new TableModel(columnName, statistic, sum);
                    controller = new TableController(getModel(), tableModel, "testclass" + classModel.getName());
                    controller.getView().setTitle(classModel.getName());
                    controller.getView().setDescription("Confusion Matrix for " + classModel.getName()
                            + ", Probability Threshold: " + minProbThreshold);
 
                    this.setFini();
                }
            } catch (Exception ex) {
                Logger.getLogger(TestClassController.class.getName()).log(Level.SEVERE, null, ex);
                this.setFini();

                NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                   "Class " + classModel.getName() + " out of sync. Recreate class "
                                                   + "and associated training libraries containing the class");

                dialog.setVisible(true);
            }
        }
    }
}
