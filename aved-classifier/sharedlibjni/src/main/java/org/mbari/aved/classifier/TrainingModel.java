/*
 * @(#)TrainingModel.java
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



package org.mbari.aved.classifier;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
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
    private String                description         = "-";
    private String                name                = "-";
    private File                  dbrootDirectory     = new File("");
    private ArrayList<ClassModel> classModels         = new ArrayList<ClassModel>();
    final String                  syncArrays          = "syncArrays";

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
     * Returns a copy of the training model
     */
    public TrainingModel copy() {
        TrainingModel m = new TrainingModel();

        synchronized (syncArrays) {
            m.color           = this.color;
            m.name            = this.name;
            m.description     = this.description;
            m.dbrootDirectory = this.dbrootDirectory;

            for (ClassModel model : classModels) {
                m.classModels.add(model.copy());
            }

            Collections.sort(m.classModels);
        }

        return m;
    }

    /**
     * Add the class model to this training model
     *
     * @param model
     *
     * @return false if the class model was not added,
     * otherwise true
     */
    public void addClassModel(ClassModel model) {
        synchronized (syncArrays) {
            checkClassExists(model);
            classModels.add(model);
            Collections.sort(classModels);
        }
    }

    /**
     * Checks if the model exists and remove if so
     * @param model the class model to check
     */
    private void checkClassExists(ClassModel newModel) {
        Iterator<ClassModel> i = classModels.iterator();

        while (i.hasNext()) {
            ClassModel model = i.next();

            if (newModel.getName().equals(model.getName()) && newModel.getColorSpace().equals(model.getColorSpace())) {
                classModels.remove(model);
            }
        }
    }

    /**
     * Gets the class model at the given index
     *
     * @return the class model , or null if none exist at the
     * given index
     */
    public ClassModel getClassModel(int i) {
        synchronized (syncArrays) {
            if (i < classModels.size()) {
                return classModels.get(i);
            }
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
        synchronized (syncArrays) {
            return classModels.remove(model);
        }    
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
        String s = "\n======TRAINING CLASS=====\n" + "Class name: " + name + "\n" + "Description: " + description
                   + "\n" + "Color space: " + color + "\n" + "dbroot: " + dbrootDirectory.getName() + "\n";

        s = s + "\n======CLASS MODELS INCLUDED IN THIS TRAINING CLASS=====\n";

        for (int i = 0; i < classModels.size(); i++) {
            s = s + "," + classModels.get(i).toString();
        }

        s = s + "\n======END=====\n";

        return s;
    }
}
