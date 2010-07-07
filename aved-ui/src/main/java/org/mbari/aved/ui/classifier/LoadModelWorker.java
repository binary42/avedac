/*
 * @(#)LoadLibraryWorker.java
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
import org.jdesktop.swingworker.SwingWorker;


//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

/**
 *
 * @author dcline
 */
public class LoadModelWorker extends SwingWorker {
    private final ClassifierModel  model;

    public LoadModelWorker(ClassifierModel model) {
        this.model           = model; 
    }

    @Override
    protected Object doInBackground() throws Exception { 

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

            if (classes != null) {
                model.addClassModels(classes);
            }

            // Create the training class directory if it doesn't exist
            File trainingDir = new File(dbRoot + "/training/class");
            if (!trainingDir.exists()) {
                trainingDir.mkdir();
            }

            // Get the training classes in this root directory
            TrainingModel[] training = library.get_training_classes(dbRoot);

            if (training != null) {
                model.addTrainingModels(training);
            }
 
        } catch (RuntimeException ex) {
            Logger.getLogger(LoadModelWorker.class.getName()).log(Level.SEVERE, null, ex);

        } catch (Exception ex) {
            Logger.getLogger(LoadModelWorker.class.getName()).log(Level.SEVERE, null, ex);
        } 
         return this;
    }
}
