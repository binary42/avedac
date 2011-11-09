/*
 * @(#)ClassifierModel.java
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

import com.jgoodies.binding.list.ArrayListModel;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.appframework.AbstractModel;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dcline
 */
public class ClassifierModel extends AbstractModel {
    public static final String DBROOT = "databaseRoot";

    // a list of available models under this database root
    private final ArrayListModel classModelList             = new ArrayListModel();
    final String                 syncArrays                 = "syncArrays";
    private final ArrayListModel trainingModelList          = new ArrayListModel();
    private File                 lastClassTrainingDirectory = new File("");
    private File                 dbrootDirectory            = new File("");

    /**
     * Constructor.
     */
    public ClassifierModel() {}

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

            ClassifierModelEvent e = new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_DIR_UPDATED,
                                         "setTrainingClassDirectory" + directory.toString());

            notifyChanged(e);
        }
    }

    /**
     * Get a training model at the given index. Returns an empty
     * <code>TrainingModel</code> if no model exists at that index
     *
     * @return a TrainingModel
     */
    public TrainingModel getTrainingModel(int index) {
        synchronized (syncArrays) {
            try {
                TrainingModel model = (TrainingModel) trainingModelList.get(index);

                return model;
            } catch (IndexOutOfBoundsException ex) {}

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
                          : "");
        String newfile = ((b != null)
                          ? b.toString()
                          : "");

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
            } catch (IndexOutOfBoundsException ex) {}

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

                addClassModelToList(c);
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

            addClassModelToList(m);
            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.CLASS_MODELS_UPDATED, m.getName()));
        }
    }
    
    /**
     * Notify listeners that the specific class model has been updated
     * @param model 
     */
    public void notifyChanged(ClassModel model) {
          synchronized (syncArrays) { 
            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.CLASS_MODELS_UPDATED, model.getName()));
        }
    }

    /**
     * Remove a class model from the list
     * @param model class model to delete
     */
    public void removeClassModel(ClassModel model) throws Exception {
        if (model == null) {
            throw new Exception("class model cannot be null");
        }

        synchronized (syncArrays) {
            Iterator<ClassModel> i = classModelList.iterator();

            while (i.hasNext()) {
                ClassModel m = i.next();

                if (m.getName().equals(model.getName()) && m.getColorSpace().equals(model.getColorSpace())) {
                    classModelList.remove(m);

                    break;
                }
            }

            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_MODELS_UPDATED,
                    model.getName()));
        }
    }

    /**
     * Remove a training model from the list
     * @param model training model to delete
     */
    public void removeTrainingModel(TrainingModel model) throws Exception {
        if (model == null) {
            throw new Exception("training model cannot be null");
        }

        synchronized (syncArrays) {
            Iterator<TrainingModel> i = trainingModelList.iterator();

            while (i.hasNext()) {
                TrainingModel m = i.next();

                if ((m != null) && (model != null) && m.getName().equals(model.getName())
                        && m.getColorSpace().equals(model.getColorSpace())) {
                    trainingModelList.remove(model);

                    break;
                }
            }

            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.CLASS_MODELS_UPDATED, model.getName()));
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
            addTrainingModelToList(model);
            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_MODELS_UPDATED,
                    model.getName()));
        }
    }

    /**
     * Checks if the training model exists and delete it if so
     * @param m the training model to check
     */
    public void addTrainingModelToList(TrainingModel model) throws Exception {
        if (model == null) {
            throw new Exception("training model cannot be null");
        }

        synchronized (syncArrays) {
            Iterator<TrainingModel> i = trainingModelList.iterator();

            while (i.hasNext()) {
                TrainingModel m = i.next();

                if ((m != null) && (model != null) && m.getName().equals(model.getName())
                        && m.getColorSpace().equals(model.getColorSpace())) {
                    trainingModelList.remove(m);
                    trainingModelList.add(model);

                    return;
                }
            }

            trainingModelList.add(model);
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

                    addTrainingModelToList(t);
                } catch (Exception ex) {
                    Logger.getLogger(ClassifierModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_MODELS_UPDATED, "all"));
        }
    }

    /**
     * Checks if the class model exists and delete it if so
     * @param m the class model to check
     */
    public void addClassModelToList(ClassModel m) throws Exception {
        if (m == null) {
            throw new Exception("class model cannot be null");
        }

        synchronized (syncArrays) {
            Iterator<ClassModel> i = classModelList.iterator();

            while (i.hasNext()) {
                ClassModel model = i.next();

                if (m.getName().equals(model.getName()) && m.getColorSpace().equals(model.getColorSpace())) {
                    classModelList.remove(model); 
                    break;
                }
            }

            classModelList.add(m);
        }
    }

    /**
     * Gets the class model with the given class name.
     *
     * @param className
     * @return the class model if found, otherwise returns null
     */
    public ClassModel getClassModel(String className) {
        synchronized (syncArrays) {
            Iterator<ClassModel> i = classModelList.iterator();

            while (i.hasNext()) {
                ClassModel model = i.next();

                if (model.getName().equals(className)) {
                    return model;
                }
            }

            return null;
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
     * notify model listeners the training model selected has changed
     * @param name 
     */
    void notifyTrainingModelChanged(String name) {
        this.notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_MODEL_SELECTION_CHANGE, name));
    }

    /**
     *
     * @author dcline
     */
    public class ClassifierModelEvent extends ModelEvent {
 
        public static final int CLASSIFIER_DBROOT_MODEL_CHANGED = 0;
        public static final int CLASS_MODELS_UPDATED            = 2;
        public static final int CLASS_MODELS_FILE_ADDED         = 5;
        public static final int JNI_TASK_COMPLETED              = 4;
        public static final int TRAINING_DIR_UPDATED            = 1;
        public static final int TRAINING_MODELS_UPDATED         = 3;
        public static final int TRAINING_MODEL_SELECTION_CHANGE = 6;

        /**
         * Constructor for this custom ModelEvent. Basically just like ModelEvent.
         * This is the default constructor for events that don't need to set the
         * any contained variables.
         * @param obj  the object that originated the event
         * @param type    an integer that identifies the ModelEvent type
         * @param message a message about the event
         */
        public ClassifierModelEvent(Object obj, int type, String message) {
            super(obj, type, message);
        }
    }
}
