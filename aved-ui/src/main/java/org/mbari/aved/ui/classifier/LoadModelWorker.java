/*
 * @(#)LoadModelWorker.java
 * 
 * Copyright 2013 MBARI
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

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads the classifier matlab data from the training library database
 * The database is a collection of matlab formatted data, and not really
 * a database but retains the name database to be consistent with
 * the  convention used in the Matlab code
 * @author dcline
 */
public class LoadModelWorker extends ClassifierLibraryJNITask {
    private final ClassifierModel model;

    public LoadModelWorker(ClassifierModel model) throws Exception {
        super("");
        this.model = model;
    }

    @Override
    protected void run(ClassifierLibraryJNI library) {
        try {
            model.clear();
            
            File   dir  = UserPreferences.getModel().getClassImageDirectory();
            String dbRoot = dir.getAbsolutePath();

            // Create the collected class directory if it doesn't exist
            File featuresDir = new File(dbRoot + "/features/class");

            if (!featuresDir.exists()) {
                featuresDir.mkdirs();
            }

            // Get the collected classes in this root directory
            ClassModel[] classes = library.get_collected_classes(dbRoot);

            // Create the training class directory if it doesn't exist
            File trainingDir = new File(dbRoot + "/training/class");

            if (!trainingDir.exists()) {
                trainingDir.mkdirs();
            }

            // Get the training classes in this root directory
            TrainingModel[] training = library.get_training_classes(dbRoot);

            if (classes != null) {
               model.addClassModels(classes);
            }

            if (training != null) {
               model.addTrainingModels(training);
            }
        } catch (RuntimeException ex) {
            Logger.getLogger(LoadModelWorker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LoadModelWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
