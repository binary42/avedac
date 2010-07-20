/*
 * @(#)ClassifierModel.java
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
import com.jgoodies.binding.list.ArrayListModel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.appframework.AbstractModel;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.Iterator;

/**
 *
 * @author dcline
 */
public class ClassifierModel extends AbstractModel {

    public static final String DBROOT = "databaseRoot";
    // a list of available models under this database root
    private final ArrayListModel classModelList = new ArrayListModel();
    private final ArrayListModel trainingModelList = new ArrayListModel();
    private File lastClassTrainingDirectory = new File("");
    private File dbrootDirectory = new File("");
    final String syncArrays = "syncArrays";

    /**
     * Constructor.
     */
    public ClassifierModel() {
    }

    /**
     * Get the current training image directory. This is where
     * class images are stored.
     *
     * @return the training image directory
     */
    public File getClassTrainingImageDirectory() {
        return lastClassTrainingDirectory;
    }

    /**
     * Sets the class training image directory
     * 
     * @param dir the training image directory
     */
    public void setClassTrainingImageDirectory(File directory) {

        UserPreferences.getModel().clearDockingImagesDirectories();
        if (changedName(lastClassTrainingDirectory, directory)) {
            lastClassTrainingDirectory = directory;
            UserPreferences.getModel().setClassTrainingImageDirectory(directory);
            ClassifierModelEvent e = new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_CLASS_DIR_UPDATED,
                    "setTrainingClassDirectory" + directory.toString());
            notifyChanged(e);
        }
    }

    /**
     * Get a training model at the given index. Returns an empty
     * <code>TrainingModel</code> if no model exist at that index
     *
     * @return a TrainingModel
     */
    public TrainingModel getTrainingModel(int index) {

        synchronized (syncArrays) {
            try {
                TrainingModel model = (TrainingModel) trainingModelList.get(index);
                return model;
            } catch (IndexOutOfBoundsException ex) {
            }
            return new TrainingModel();
        }
    }

    /**
     * Sets the root class database directory
     * @param f the root directory to set
     */
    public void setDatabaseRoot(File directory) {

        if (changedName(dbrootDirectory, directory)) {
            dbrootDirectory = directory;
            UserPreferences.getModel().setClassDatabaseDirectory(directory);
            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED,
                    dbrootDirectory.toString()));
        }
    }

    /**
     * Sets the model event to tell listeners a jni task has completed
     * @param f the root directory to set
     */
    public void setJniTaskComplete(int taskId) {
        notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.JNI_TASK_COMPLETED,
                Integer.toString(taskId)));
    }

    /**
     * Null safe check if the files have the same
     * @returns True is the files are the same
     */
    private static Boolean changedName(File a, File b) {
        String oldfile = ((a != null)
                ? a.toString()
                : new String(""));
        String newfile = ((b != null)
                ? b.toString()
                : new String(""));

        return !oldfile.equals(newfile);
    }

    /**
     * Get a class models at the given index. Returns an empty
     * <code>ClassModel</code> if no model exist at that index
     *
     * @return a ClassModel
     */
    public ClassModel getClassModel(int index) {
        synchronized (syncArrays) {
            try {
                ClassModel model = (ClassModel) classModelList.get(index);
                return model;
            } catch (IndexOutOfBoundsException ex) {
            }
            return new ClassModel();
        }
    }

    /**
     * Adds an array of class model to the list
     * @param models class models to add
     */
    public void addClassModels(ClassModel model[]) throws Exception {
        if (model == null) {
            throw new Exception("class model cannot be null");
        }


        synchronized (syncArrays) {
            for (int i = 0; i < model.length; i++) {
                ClassModel c = model[i].copy();
                checkClassModel(c);
                classModelList.add(c);
            }
        }

        notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.CLASS_MODELS_UPDATED, "all"));
    }

    /**
     * Adds a class model to the list
     * @param model class model to add
     */
    public void addClassModel(ClassModel model) throws Exception {
        if (model == null) {
            throw new Exception("class model cannot be null");
        }

        synchronized (syncArrays) {
            ClassModel m = model.copy();
            checkClassModel(m);
            classModelList.add(m);

            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.CLASS_MODELS_UPDATED, m.getName()));
        }
    }

    /**
     * Adds a training model to the list
     * @param model training model to add
     */
    public void addTrainingModel(TrainingModel model) throws Exception {
        if (model == null) {
            throw new Exception("training model cannot be null");
        }

        synchronized (syncArrays) {
            checkTrainingModel(model);
            this.trainingModelList.add(model);
            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_MODELS_UPDATED, model.getName()));
        }
    }

    /**
     * Adds training models to the list
     * @param model training models to add
     */
    void addTrainingModels(TrainingModel[] training) throws Exception {
        if (training == null) {
            throw new Exception("training model array cannot be null");
        }

        synchronized (syncArrays) {
            for (int i = 0; i < training.length; i++) {
                try {
                    TrainingModel t = training[i];
                    checkTrainingModel(t);
                    this.trainingModelList.add(t);
                } catch (Exception ex) {
                    Logger.getLogger(ClassifierModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_MODELS_UPDATED, "all"));
        }
    }

    /**
     * Checks if the model exists
     * @param model the class model to check
     * @return true if it exists
     */
    public boolean checkClassExists(ClassModel newModel) {

        synchronized (syncArrays) {
            Iterator<ClassModel> i = classModelList.iterator();


            while (i.hasNext()) {
                ClassModel model = i.next();

                if (newModel.getName().equals(model.getName())
                        && newModel.getColorSpace().equals(model.getColorSpace())
                        && newModel.getDatabaseRootdirectory().equals(model.getDatabaseRootdirectory())
                        && newModel.getDescription().equals(model.getDescription())
                        && newModel.getRawImageDirectory().equals(model.getRawImageDirectory())
                        && newModel.getVarsClassName().equals(model.getVarsClassName())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if the training model exists and delete it if so
     * @param m the training model to check 
     */
    public void checkTrainingModel(TrainingModel model) throws Exception {
        if (model == null) {
            throw new Exception("training model cannot be null");
        }

        synchronized (syncArrays) {
            Iterator<TrainingModel> i = trainingModelList.iterator();

            while (i.hasNext()) {
                TrainingModel m = i.next();

                if (m != null
                        && m.getName().equals(model.getName()) && m.getColorSpace().equals(model.getColorSpace())
                        && m.getDatabaseRootdirectory().equals(model.getDatabaseRootdirectory())
                        && m.getDescription().equals(model.getDescription())) {
                    trainingModelList.remove(model);
                    break;
                }
            }
        }
    }

    /**
     * Checks if the class model exists and delete it if so
     * @param m the class model to check 
     */
    public void checkClassModel(ClassModel m) throws Exception {
        if (m == null) {
            throw new Exception("class model cannot be null");
        }

        synchronized (syncArrays) {
            Iterator<ClassModel> i = classModelList.iterator();

            while (i.hasNext()) {
                ClassModel model = i.next();

                if (m.getName().equals(model.getName()) && m.getColorSpace().equals(model.getColorSpace())
                        && m.getDatabaseRootdirectory().equals(model.getDatabaseRootdirectory())
                        && m.getDescription().equals(model.getDescription())) {
                    classModelList.remove(model);
                    break;
                }
            }
        }
    }

    /**
     * Gets the class model with the given class name.
     *
     * @param className
     * @return the class model if found, otherwise throws exception
     */
    public ClassModel getClassModel(String className) throws Exception {

        synchronized (syncArrays) {
            Iterator<ClassModel> i = classModelList.iterator();

            while (i.hasNext()) {
                ClassModel model = i.next();

                if (model.getName().equals(className)) {
                    return model;
                }
            }

            throw new Exception("Class Model with name: " + className + " not found");
        }
    }

    /**
     *
     * @return the database root directory this classsifier will store
     * trained data from this class in
     */
    public File getDatabaseRoot() {
        return dbrootDirectory;
    }

    /**
     *
     * @return the number of class models
     */
    public int getNumClassModels() {
        synchronized (syncArrays) {
            return classModelList.getSize();
        }
    }

    /**
     *
     * @return the number of training models
     */
    public int getNumTrainingModels() {
        synchronized (syncArrays) {
            return trainingModelList.getSize();
        }
    }

    /**
     *
     * @author dcline
     */
    public class ClassifierModelEvent extends ModelEvent {

        /**
         * Indicates that the class model has changed
         */
        public static final int CLASSIFIER_DBROOT_MODEL_CHANGED = 0;
        public static final int CLASS_MODELS_UPDATED = 2;
        public static final int TRAINING_CLASS_DIR_UPDATED = 1;
        public static final int TRAINING_MODELS_UPDATED = 3;
        public static final int JNI_TASK_COMPLETED = 4;

        /**
         * Constructor for this custom ModelEvent. Basically just like ModelEvent.
         * This is the default constructor for events that don't need to set the
         * any contained variables.
         * @param obj  the object that originated the event
         * @param type    an integer that identifies the ModelEvent type
         * @param message a message about the event
         */
        public ClassifierModelEvent(Object obj, int type, String message) {
            super(obj, type, "ClassifierModelEvent:" + message);
        }
    }
}
