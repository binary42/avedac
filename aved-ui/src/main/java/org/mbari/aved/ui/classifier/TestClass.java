/*
 * @(#)TestClass.java
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

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

public class TestClass {
    private final TestClassController controller;

    public TestClass(ClassifierModel model) {
        controller = new TestClassController(model);
    }

    public TestClassView getView() {
        return controller.getView();
    }

    /**
     * Test main. This build a TestClass for testing ouside
     * of the main application.
     * @param args
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    ClassifierLibraryJNI library = new ClassifierLibraryJNI();
                    ClassifierModel      model   = new ClassifierModel();
                    File                 dbDir   = UserPreferences.getModel().getClassDatabaseDirectory();
                    String               dbRoot  = dbDir.getAbsolutePath();

                    library.initLib(dbDir.getAbsolutePath());

                    // Get the collected classes in this root directory
                    ClassModel[] classes = library.get_collected_classes(dbRoot);

                    if (classes != null) {
                        for (int i = 0; i < classes.length; i++) {
                            model.addClassModel(classes[i]);
                        }
                    }

                    // Get the training classes in this root directory
                    TrainingModel[] training = library.get_training_classes(dbRoot);

                    if (classes != null) {
                        for (int i = 0; i < training.length; i++) {
                            model.addTrainingModel(training[i]);
                        }
                    }

                    TestClass c = new TestClass(model);

                    model.setDatabaseRoot(dbDir);
                    c.getView().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    c.getView().setVisible(true);
                } catch (Exception ex) {
                    Logger.getLogger(TestClass.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
