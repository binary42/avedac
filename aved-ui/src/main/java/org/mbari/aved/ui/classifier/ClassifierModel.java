/*
 * @(#)ClassifierModel.java   10/03/17
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

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.appframework.AbstractModel;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel;

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
    private ArrayListModel classModelList             = new ArrayListModel();
    private ArrayListModel trainingModelList          = new ArrayListModel();
    private File           lastClassTrainingDirectory = new File("");
    private File           dbrootDirectory            = new File("");

    /**
     * Constructor. Initializes frequently accessed variables.
     */
    public ClassifierModel() {
        UserPreferencesModel m = UserPreferences.getModel();

        lastClassTrainingDirectory = m.getClassTrainingImageDirectory();
        dbrootDirectory            = m.getClassDatabaseDirectory();
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
     * @param dir the training image directory
     */
    public void setClassTrainingImageDirectory(File dir) {
        lastClassTrainingDirectory = dir;
        UserPreferences.getModel().setClassTrainingImageDirectory(lastClassTrainingDirectory);
        UserPreferences.getModel().clearDockingImagesDirectories();

        ClassifierModelEvent e = new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_CLASS_DIR_UPDATED,
                                     "setTrainingClassDirectory" + dir.toString());

        notifyChanged(e);
    }

    /**
     * Get the list of already trained models. Returns an empty
     * ArrayListModel if no models exist, otherwise returns
     * the available training models in the database root directory
     *
     * @return the list of already trained models
     */
    public ArrayListModel getTrainingModels() {
        return trainingModelList;
    }

    /**
     * Sets the root class database directory
     * @param f the root directory to set
     */
    public void setDatabaseRoot(File directory) {
        dbrootDirectory = directory;
        UserPreferences.getModel().setClassDatabaseDirectory(directory);
        notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED,
                dbrootDirectory.toString()));
    }

    /**
     * Gets the available class models. These are the classes
     * that have been successfully collected and trained.
     * @return
     */
    public ArrayListModel getClassModels() {
        return this.classModelList;
    }

    /**
     * Adds a class model to the list
     * @param model class model to add
     */
    public void addClassModel(ClassModel model) {
        if (classExists(model)) {
            classModelList.remove(model);
        }

        classModelList.add(model);
        notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.CLASS_MODELS_UPDATED, model.getName()));
    }

    /**
     * Adds a training model to the list
     * @param model training model to add
     */
    public void addTrainingModel(TrainingModel model) {
        if (trainingClassExists(model)) {
            trainingModelList.remove(model);
        }

        trainingModelList.add(model);
        notifyChanged(new ClassifierModelEvent(this, ClassifierModelEvent.TRAINING_MODELS_UPDATED, model.getName()));
    }

    /**
     * Checks if the model exists
     * @param model the class model to check
     * @return true if it exists
     */
    public boolean classExists(ClassModel newModel) {
        Iterator<ClassModel> i = classModelList.iterator();

        while (i.hasNext()) {
            ClassModel model = i.next();

            if (newModel.getName().equals(model.getName()) && newModel.getColorSpace().equals(model.getColorSpace())
                    && newModel.getDatabaseRootdirectory().equals(model.getDatabaseRootdirectory())
                    && newModel.getDescription().equals(model.getDescription())
                    && newModel.getRawImageDirectory().equals(model.getRawImageDirectory())
                    && newModel.getVarsClassName().equals(model.getVarsClassName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the training model exists
     * @param model the training model to check
     * @return true if it exists
     */
    public boolean trainingClassExists(TrainingModel newModel) {
        Iterator<TrainingModel> i = trainingModelList.iterator();

        while (i.hasNext()) {
            TrainingModel model = i.next();

            if (newModel.getName().equals(model.getName()) && newModel.getColorSpace().equals(model.getColorSpace())
                    && newModel.getDatabaseRootdirectory().equals(model.getDatabaseRootdirectory())
                    && newModel.getDescription().equals(model.getDescription())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a model with the given classname exists
     * @param className the class name to check
     * @return true if it exists
     */
    public boolean classExists(String className) {
        Iterator<ClassModel> i = classModelList.iterator();

        while (i.hasNext()) {
            ClassModel model = i.next();

            if (model.getName().equals(className)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the class model with the given class name.
     *
     * @param className
     * @return the class model if found, otherwise throws exception
     */
    public ClassModel getClassModel(String className) throws Exception {
        Iterator<ClassModel> i = classModelList.iterator();

        while (i.hasNext()) {
            ClassModel model = i.next();

            if (model.getName().equals(className)) {
                return model;
            }
        }

        throw new Exception("Class Model with name: " + className + " not found");
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
     * @author dcline
     */
    public class ClassifierModelEvent extends ModelEvent {

        /**
         * Indicates that the class model has changed
         */
        public static final int CLASSIFIER_DBROOT_MODEL_CHANGED = 0;
        public static final int CLASS_MODELS_UPDATED            = 2;
        public static final int TRAINING_CLASS_DIR_UPDATED      = 1;
        public static final int TRAINING_MODELS_UPDATED         = 3;

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
