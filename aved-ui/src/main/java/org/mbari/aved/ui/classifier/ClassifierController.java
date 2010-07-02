/*
 * @(#)ClassifierController.java
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

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.model.EventImageCache;
import org.mbari.aved.ui.model.EventImageCacheData;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.TrainingModel;

/**
 *
 * @author dcline
 */
public class ClassifierController extends AbstractController implements ModelListener, WindowListener {

    private final CreateClass createClass;
    private final CreateTrainingLibrary createTrainingLib;
    private final EventListModel eventListModel;
    private final RunClassifier runClassifier;
    private final TestClass testClass;

    public ClassifierController(EventListModel list, SummaryModel summaryModel) {
        eventListModel = list;

        // Create common model for all controllers/views to use
        ClassifierModel model = new ClassifierModel();
        ClassifierView view = new ClassifierView(model, this);

        // Need to create the model and view before creating the controllers
        setView(view);
        setModel(model);

        // Register as a window listener to handle closing the shared library
        // gracefully
        getView().addWindowListener(this);

        // Register as listener to the model
        model.addModelListener(this);
 
        loadModels();

        createTrainingLib = new CreateTrainingLibrary(model);
        createClass = new CreateClass(model, list);
        testClass = new TestClass(model);
        runClassifier = new RunClassifier(model, list, summaryModel);

        // Replace the views
        view.setTrainingPanel(createTrainingLib.getView().getForm());
        view.setCreateClassPanel(createClass.getView().getForm());
        view.setTestClassPanel(testClass.getView().getForm());
        view.setRunPanel(runClassifier.getView().getForm());

        // TODO: make this a function of screen size
        view.pack();

        // Initialize the database directory from the user-defined preferences
        File dbDir = UserPreferences.getModel().getClassDatabaseDirectory();
        model.setDatabaseRoot(dbDir);
         
        // Initialize the training image directory
        File trainingDir = UserPreferences.getModel().getClassTrainingImageDirectory();
        model.setClassTrainingImageDirectory(trainingDir);
    }

    @Override
    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    @Override
    public ClassifierView getView() {
        return (ClassifierView) super.getView();
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Loads the available models
     */
    private void loadModels() {
        try {
            ClassifierLibraryJNI library = Classifier.getLibrary();
            File dbDir = UserPreferences.getModel().getClassDatabaseDirectory();
            String dbRoot = dbDir.getAbsolutePath();

            // Create the collected class directory if it doesn't exist
            File featuresDir = new File(dbRoot + "/features/class");
            if (!featuresDir.exists()) {
                featuresDir.mkdir();
            }

            // Get the collected classes in this root directory
            ClassModel[] classes = library.get_collected_classes(dbRoot);
            ClassifierModel model = getModel();

            if (classes != null) {
                for (int i = 0; i < classes.length; i++) {
                    model.addClassModel(classes[i]);
                }
            }

            // Create the training class directory if it doesn't exist
            File trainingDir = new File(dbRoot + "/training/class");
            if (!trainingDir.exists()) {
                trainingDir.mkdir();
            }

            // Get the training classes in this root directory
            TrainingModel[] training = library.get_training_classes(dbRoot);

            if (training != null) {
                for (int i = 0; i < training.length; i++) {
                    getModel().addTrainingModel(training[i]);
                }
            }
        } catch (RuntimeException ex) {
            Logger.getLogger(ClassifierController.class.getName()).log(Level.SEVERE, null, ex);

        } catch (Exception ex) {
            Logger.getLogger(ClassifierController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Model listener. Reacts to changes in the
     * {@link org.mbari.aved.ui.classifier.model}
     * and  {@link org.mbari.aved.ui.model.EventListModel}
     */
    public void modelChanged(ModelEvent event) {

        /**
         * When the classifier training image directory changes, repopulate
         * the persistent list of classes. This requires storing the class information
         * in a directory for now, until this information is actually put
         * in a real database
         */
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {
                case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED:
                    loadModels();
                    break;
            }
        } /**
         * when entries are changed, check if this belongs to a class and whether
         * the user is creating a training classes, and if so add it to the
         * appropriate training class
         */
        else if (event instanceof EventListModel.EventListModelEvent) {
            EventListModel.EventListModelEvent e = (EventListModel.EventListModelEvent) event;

            switch (e.getID()) {
                case EventListModelEvent.MULTIPLE_ENTRIES_CHANGED:
                    ArrayList<Integer> a = e.getModelIndexes();
                    Iterator<Integer> iter = a.iterator();

                    while (iter.hasNext()) {
                        File f = UserPreferences.getModel().getClassTrainingImageDirectory();
                        EventObjectContainer eoc = eventListModel.getElementAt(iter.next());

                        // TODO: add check for user pref to add all classes to training set
                        if ((eoc != null) && (eoc.getClassName().length() > 0)) {
                            try {
                                String className = eoc.getClassName();
                                File dir = new File(f + "/" + className + "//");

                                if (!dir.exists()) {
                                    if (f.canWrite()) {
                                        dir.mkdir();
                                    } else {

                                        // TODO: display error message to user here
                                        return;
                                    }
                                }

                                // create a new model with some reasonable
                                // defaults
                                ClassModel newModel = new ClassModel();

                                newModel.setRawImageDirectory(dir);
                                newModel.setName(className);
                                newModel.setVarsClassName(eoc.getClassName());
                                newModel.setDescription(eoc.getClassName());

                                if (!getModel().checkClassExists(newModel)) {
                                    getModel().addClassModel(newModel);
                                } else {

                                    // get the existing model and add an image to it
                                    ClassModel model = getModel().getClassModel(className);
                                    AddClassImageWorker thread = new AddClassImageWorker(model, eoc);

                                    thread.execute();
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(ClassifierController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                default:
                    break;
            }
        }
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        try {
            Classifier.closeLibrary();
        } catch (Exception ex) {
            Logger.getLogger(ClassifierController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public class AddClassImageWorker extends SwingWorker {

        private final ClassModel classModel;
        private final EventObjectContainer event;
        private ProgressDisplay progressDisplay;

        public AddClassImageWorker(ClassModel classModel, EventObjectContainer event) {
            this.event = event;
            this.classModel = classModel;

            if (progressDisplay != null) {
                progressDisplay = new ProgressDisplay(this, "Adding class image" + classModel.getName());
            }
        }

        @Override
        protected Object doInBackground() throws Exception {
            progressDisplay.display("Adding class " + classModel.getName() + "...");

            boolean addImages = UserPreferences.getModel().getAddTrainingImages();

            if (addImages) {
                File dir = UserPreferences.getModel().getClassTrainingImageDirectory();
                File classDir = new File(dir + "/" + classModel.getName());

                // Add this class to the docking image directories
                UserPreferences.getModel().setLastDockingImageDirectory(classDir);

                for (int frameNo = event.getStartFrame(); frameNo <= event.getEndFrame(); frameNo++) {

                    // If found a valid frame number
                    if (frameNo >= 0) {
                        try {
                            int bestFrameNo = frameNo;
                            EventImageCacheData data = new EventImageCacheData(event);

                            data.initialize(bestFrameNo);

                            EventObject object = event.getEventObject(bestFrameNo);

                            if ((object != null) && EventImageCache.createCroppedImageOfEvent(data, object)) {
                                classModel.addToTrainingSet(data.getImageSource());
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(AddClassImageWorker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }

            progressDisplay.display("Done !");

            return this;
        }
    }
}
