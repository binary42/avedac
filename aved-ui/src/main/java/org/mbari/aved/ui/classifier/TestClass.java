/*
 * @(#)TestClass.java
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
                    ClassifierLibraryJNI library  = new ClassifierLibraryJNI(this);
                    ClassifierModel      model    = new ClassifierModel();
                    File                 dbDir    = UserPreferences.getModel().getClassDatabaseDirectory();
                    String               dbRoot   = dbDir.getAbsolutePath();
                    String               lcOSName = System.getProperty("os.name").toLowerCase();

                    // If running from Mac
                    if (lcOSName.startsWith("mac os x")) {
                        library.initLib(dbDir.getAbsolutePath(), 1);
                    } else {
                        library.initLib(dbDir.getAbsolutePath(), 0);
                    }

                    library.initLib(dbDir.getAbsolutePath(), 0);

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
