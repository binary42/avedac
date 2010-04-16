/*
 * @(#)TrainingModel.java
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



package org.mbari.aved.classifier;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A <code>TrainingModel</code> is simply a collection
 * of <code>ClassModel</code> objects. It is referenced
 * by its name in the Matlab code.
 *
 * @author dcline
 */
public class TrainingModel {

    /** The class label used in the Matlab code */
    public static final String    UNKNOWN_CLASS_LABEL = "Unknown";
    private ColorSpace            color               = ColorSpace.RGB;
    private String                description         = new String("-");
    private String                name                = new String("-");
    private File                  dbrootDirectory     = new File("");
    private ArrayList<ClassModel> classModels         = new ArrayList<ClassModel>();

    /**
     * Get the number of classes in the model. Note that a training class
     * is valid (trainable) only if it contains one or more classes
     *
     * @return the number of classes
     */
    public int getNumClasses() {
        return classModels.size();
    }

    /**
     * Add the class model to this training model
     * 
     * @param model
     * 
     * @return false if the class model was not added,
     * otherwise true
     */
    public boolean addClassModel(ClassModel model) {
        if (!classModels.contains(model) && 
                !this.checkClassExists(model)) {
            classModels.add(model);
            return true;
        }
        return false;
    }

     /**
     * Checks if the model exists
     * @param model the class model to check
     * @return true if it exists
     */
    private boolean checkClassExists(ClassModel newModel) {
        Iterator<ClassModel> i = classModels.iterator();

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

    /**
     * Gets the class model at the given index
     *
     * @return the class model, or null if none exist at the
     * given index
     */
    public ClassModel getClassModel(int i) {
        if (i < classModels.size()) {
            return classModels.get(i);
        }

        return null;
    }

    /**
     * Remove a class model. Returns <tt>true</tt> if the class is
     * contained in this training class.
     *
     * @param model
     */
    public boolean removeClassModel(ClassModel model) {
        return classModels.remove(model);
    }

    /**
     *
     * @return the color space for this class
     */
    public ColorSpace getColorSpace() {
        return color;
    }

    /**
     *
     * @param color the color space for this class
     */
    public void setColorSpace(ColorSpace color) {
        this.color = color;
    }

    /**
     * Sets the class alias. This is a user-defined alias
     * used to look-up this class during testing
     *
     * @param className a user-defined name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the class alias. This is a user-defined alias
     * used to look-up this class during testing
     *
     * @param className
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description of the class. This is an optional,
     * user-defined description  of what this class represents.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the class. This is an optional,
     * user-defined description  of what this class represents.
     *
     * @param desc the description
     */
    public void setDescription(String desc) {
        this.description = desc;
    }

    /**
     * Gets the database root directory this classsifier will store
     * trained data from this training class in
     *
     * @return the database root directory
     */
    public File getDatabaseRootdirectory() {
        return dbrootDirectory;
    }

    /**
     * Set the database root directory to store this model information to.
     * This directory must exist.
     *
     * @param directory the name of the root directory
     * @throws java.lang.Exception if the directory does not exist
     */
    public void setDatabaseRoot(File directory) throws Exception {
        dbrootDirectory = directory;

        if (dbrootDirectory.isDirectory() == false) {
            throw new Exception(dbrootDirectory + " is not a directory");
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns a verbose formatted string description of the training class.
     * This includes a detailed listing of all the included classes
     *
     * @return the description
     */
    public String description() {
        String s = "======TRAINING CLASS=====\n" + "Class name: " + name + "\n" + "Description: " + description + "\n"
                   + "Color space: " + color + "\n" + "dbroot: " + dbrootDirectory.getName() + "\n";

        s = s + "======CLASS MODELS INCLUDED IN THIS TRAINING CLASS=====\n";

        for (int i = 0; i < classModels.size(); i++) {
            s = s + classModels.get(i).toString();
        }

        s = s + "======END=====\n";

        return s;
    }
}
