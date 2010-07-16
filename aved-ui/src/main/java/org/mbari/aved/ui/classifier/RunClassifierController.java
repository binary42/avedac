/*
 * @(#)RunClassifierController.java
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
import aved.model.EventObject;

import com.jgoodies.binding.list.ArrayListModel;

import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.classifier.table.TableController;
import org.mbari.aved.ui.classifier.table.TableModel;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventImageCache;
import org.mbari.aved.ui.model.EventImageCacheData;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.progress.ProgressDisplayStream;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.ParseUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.io.BufferedReader;

import java.io.File;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import org.jdesktop.swingworker.SwingWorker;

public class RunClassifierController extends AbstractController implements ModelListener {

    /** The AVED XML file to save the results of this classifier run */
    private final File avedXmlFile;
    private final EventListModel eventListModel;
    private final SummaryModel summaryModel;
    private TrainingModel trainingModel;
    private RunClassifierWorker task;

    RunClassifierController(ClassifierModel model, final EventListModel eventListModel, SummaryModel summaryModel) {
        setModel(model);
        setView(new RunClassifierView(model, this));
        this.eventListModel = eventListModel;
        this.summaryModel = summaryModel;
        this.avedXmlFile = new File("");

        // Register as listener to the models
        getModel().addModelListener(this);
        eventListModel.addModelListener(this);
        summaryModel.addModelListener(this);

        // Create an empty model to start with
        trainingModel = new TrainingModel();

        File dbroot = UserPreferences.getModel().getClassDatabaseDirectory();

        try {

            // Create the directory if it doesn't exist
            if (!dbroot.exists()) {
                dbroot.mkdirs();
            }

            trainingModel.setDatabaseRoot(dbroot);

            ColorSpace colorSpace = trainingModel.getColorSpace();

            getView().selectColorSpace(colorSpace);
        } catch (Exception ex) {
            Logger.getLogger(RunClassifierController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* Just some helper functions to access model and view */
    @Override
    public ClassifierModel getModel() {
        return ((ClassifierModel) super.getModel());
    }

    @Override
    public RunClassifierView getView() {
        return ((RunClassifierView) super.getView());
    }

    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("Run")) {
            if (eventListModel.getSize() == 0) {
                String msg = new String("Please open a valid XML file ");
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
                    final SwingWorker worker = Classifier.getController().getWorker();
                    final float minProbThreshold = getView().getProbabilityThreshold();
                    task = new RunClassifierWorker(minProbThreshold);
                    Classifier.getController().addQueue(task);
                    getView().setRunButton(false);
                    getView().setStopButton(true);

                    /// Create a progress display thread for monitoring this task
                    Thread thread = new Thread() {

                        @Override
                        public void run() {
                            BufferedReader br = Classifier.getController().getBufferedReader();
                            ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                                    "Running classifier with training model " + trainingModel.getName());
                            progressDisplay.getView().setVisible(true);

                            ProgressDisplayStream progressDisplayStream = new ProgressDisplayStream(progressDisplay, br);
                            progressDisplayStream.execute();
                            while (!task.isCancelled() && !task.isFini()) {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException ex) {
                                }
                            }
                            getView().setRunButton(true);
                            getView().setStopButton(false);
                            progressDisplay.getView().dispose();

                            // Reset the user preference
                            UserPreferences.getModel().setAddLabeledTrainingImages(isAddTrainingImages);

                            // Add the results only after successfully run
                            if (task.isFini() && getModel() != null) {
                                NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), trainingModel.getName() + " classification finished");
                                dialog.setVisible(true); 

                                // Make the title the same as the XML file name
                                String xmlFile = summaryModel.getXmlFile().getName();
                                String title = ParseUtils.removeFileExtension(xmlFile);
                                TableController controller = new TableController(getModel(), task.tableModel, "testclass" + title);

                                controller.getView().setTitle(title);
                                controller.getView().setDescription("Confusion Matrix for " + title + ", Probability Threshold: "
                                        + task.minProbThreshold);
                                controller.getView().pack();
                                controller.getView().setVisible(true);
                            } else {
                                if (task.isCancelled()) {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), trainingModel.getName() + " classification stopped");
                                    dialog.setVisible(true);
                                }
                            }
                        }
                    };

                    thread.start();

                } catch (Exception ex) {
                    Logger.getLogger(RunClassifierController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
            JComboBox box = ((JComboBox) e.getSource());
            ColorSpace newColorSpace = (ColorSpace) box.getSelectedItem();
            String lastSelection = UserPreferences.getModel().getLastTrainingLibrarySelection();

            // Populate the libraries in the new color space
            getView().populateTrainingLibraryList(newColorSpace); 

            // Set the library 
            if (getView().selectLibrary(lastSelection) == false) {
                getView().clearClasses();
            }

        } else if (actionCommand.equals("Stop")) {
            if (task != null) {
                getView().setRunButton(true);
                getView().setStopButton(false);
                Classifier.getController().kill(task);
            }
        } else if (actionCommand.equals("availLibraryNameComboBoxChanged")) {
            JComboBox box = ((JComboBox) e.getSource());

            trainingModel = (TrainingModel) box.getSelectedItem();

            if (trainingModel != null) {
                getView().loadModel(trainingModel);

                String selection = trainingModel.getName();

                UserPreferences.getModel().setLastTrainingLibrarySelection(selection);
            }
        }
    }

    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {

                // When the database root directory change or the models are updated
                // reset the color space
                case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED:
                case ClassifierModel.ClassifierModelEvent.CLASS_MODELS_UPDATED: 
                    getView().populateTrainingLibraryList(trainingModel.getColorSpace());
                    break;
            }
        }
    }

    private class RunClassifierWorker extends ClassifierLibraryJNITask {

        float minProbThreshold = .09f;
        TableModel tableModel;

        public RunClassifierWorker(float minProbThreshold) throws Exception {
            super(trainingModel.getName());
            this.minProbThreshold = minProbThreshold;
        }

        @Override
        protected void run(ClassifierLibraryJNI library) throws Exception {

            try {
                File testDir = summaryModel.getTestImageDirectory();
                int size = eventListModel.getSize();

                if (!testDir.exists()) {
                    testDir.mkdirs();
                } else {
                    deleteDir(testDir);
                    testDir.mkdirs();
                }

                for (int i = 0; i < size; i++) {
                    EventObjectContainer event = eventListModel.getElementAt(i);

                    for (int frameNo = event.getStartFrame(); frameNo <= event.getEndFrame(); frameNo++) {
                        if (isCancelled()) {
                            return;
                        }

                        // If found a valid frame number
                        if (frameNo >= 0) {

                            int bestFrameNo = frameNo;
                            EventImageCacheData data = new EventImageCacheData(event);

                            // If the event has a class, then rename
                            // the event with an appended name - replacing
                            // all the white spaces with dashes
                            if (event.getClassName().length() > 0) {
                                data.initialize(testDir, event.getClassName(), bestFrameNo);
                            } else {
                                data.initialize(testDir, "", bestFrameNo);
                            }

                            EventObject object = event.getEventObject(bestFrameNo);

                            if (object != null) {
                                EventImageCache.createSquaredImageOfEvent(data, object);
                            }
                        }
                    }
                }

                String dbRoot = trainingModel.getDatabaseRootdirectory().toString();

                // Run test image collection on the data
                // TODO: only do this once, unless the data has changed
                // because this takes a very long time to run for large
                // event sets
                library.collect_tests(this.getCancel(), testDir.getAbsolutePath(), dbRoot, trainingModel.getColorSpace());

                int numEvents = eventListModel.getSize();
                int[] majoritywinnerindex = new int[numEvents];
                int[] probabilitywinnerindex = new int[numEvents];
                float[] probability = new float[numEvents];
                String[] eventids = new String[numEvents];

                library.run_test(this.getCancel(), eventids, majoritywinnerindex, probabilitywinnerindex, probability,
                        testDir.getName(), trainingModel.getName(), minProbThreshold, dbRoot,
                        trainingModel.getColorSpace());

                // Add one column for the Unknown class
                int columns = trainingModel.getNumClasses() + 1;
                int rows = columns;
                String[] columnNames = new String[columns];
                int[][] statistics = new int[rows][columns];

                // Create hash map for look-up of the class index by name and
                // vice-versa
                HashMap<String, Integer> map = new HashMap<String, Integer>(columns, 0.75f);

                map.put(TrainingModel.UNKNOWN_CLASS_LABEL, new Integer(0));

                for (int j = 1; j < columns; j++) {
                    map.put(trainingModel.getClassModel(j - 1).getName(), new Integer(j));
                }

                HashMap<Integer, String> mapbyid = new HashMap<Integer, String>(columns, 0.75f);

                mapbyid.put(new Integer(0), TrainingModel.UNKNOWN_CLASS_LABEL);

                for (int j = 1; j < columns; j++) {
                    mapbyid.put(new Integer(j), trainingModel.getClassModel(j - 1).getName());
                }

                // Put the predicted class in the list model using the majority winner
                for (int i = 0; i < size; i++) {
                    EventObjectContainer event = eventListModel.getElementAt(i);

                    for (int frameNo = event.getStartFrame(); frameNo <= event.getEndFrame(); frameNo++) {
                        if (isCancelled()) {
                            return;
                        }

                        // If found a valid frame number
                        if (frameNo >= 0) {
                            EventObject obj = event.getEventObject(frameNo);

                            // Only populate the classes
                            // that are known. Skip over all the unknown
                            // results - these have an index equal to 1
                            if ((majoritywinnerindex[i] > 1) && (obj != null)) {
                                Float p = obj.getPredictedClassProbability();

                                if (p == null) {
                                    p = new Float(0.0f);
                                }

                                String np = mapbyid.get(new Integer(majoritywinnerindex[i] - 1));
                                Float pp = new Float(probability[i]);

                                // Always choosing the
                                // strongest prediction
                                if (pp > p) {
                                    obj.setPredictedClass(np, pp);
                                }
                            }
                        }
                    }
                }

                int sum[] = new int[trainingModel.getNumClasses() + 1];

                // Format the statistics array for display in a JTable
                for (int j = 0; j < columns; j++) {
                    if (j > 0) {
                        columnNames[j] = trainingModel.getClassModel(j - 1).getName();
                    } else {
                        columnNames[0] = TrainingModel.UNKNOWN_CLASS_LABEL;
                    }
                }

                for (int k = 0; k < numEvents; k++) {
                    EventObjectContainer evt = eventListModel.getElementAt(k);
                    int frame = evt.getBestEventFrame();
                    int j = 0;
                    String actualClassName = evt.getEventObject(frame).getClassName();

                    if (map.containsKey(actualClassName)) {
                        j = map.get(actualClassName);
                    }

                    int i = majoritywinnerindex[k] - 1;

                    sum[i]++;
                    statistics[i][j]++;

                    // Put the statistics and column names in a TableModel
                    TableModel tableModel = new TableModel(columnNames, statistics, sum);
                }
            } catch (RuntimeException ex) {
                Logger.getLogger(RunClassifierController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(RunClassifierController.class.getName()).log(Level.SEVERE, null, ex);

            }
        }

        /**
         * Deletes all files and subdirectories under dir.
         * @param dir
         * @return <code>true</code> if all deletions were successful.
         */
        public boolean deleteDir(File dir) {
            if (dir.isDirectory()) {
                String[] children = dir.list();

                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));

                    if (isCancelled()) {
                        return false;
                    }

                    if (!success) {
                        return false;
                    }
                }
            }

            // The directory is now empty so delete it
            return dir.delete();
        }
    }
}
