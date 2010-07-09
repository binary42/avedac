/*
 * @(#)Classifier.java
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
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import org.mbari.aved.classifier.ClassModel;

/**
 * The AVED Classifier. This handles creating the components needed
 * for creating classes, testing classes, and running the classifier.
 * The underlying AVED Classifier is Matlab code, and that code is
 * accessed through a JNI layer in the {@link org.mbari.aved.classifier.ClassifierLibraryJNI}
 * @author dcline
 */
public class Classifier {

    private static InputStreamReader isr;
    private static final ClassifierLibraryJNI jniLibrary = new ClassifierLibraryJNI();;
    private final ClassifierController controller;
    private final Preferences settings;
    private static boolean isInitialized = false;

    public Classifier(ApplicationModel model) {
        controller = new ClassifierController(model.getEventListModel(), model.getSummaryModel());

        ClassifierModel m = controller.getModel();

        settings = new Preferences(m);
        
        try {
            getLibrary();
        } catch (Exception ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public JFrame getClassifierSetingsView() {
        return settings.getView();
    }

    public JFrame getView() {
        return controller.getView();
    }

    /**
     * Brings the create class tabbed view in the Classifier to the front
     * and visible
     */
    public void selectCreateClassTabbedView() {
        controller.getView().setClassCreateTabbedViewVisible();
    }

    /**
     * Brings the create class tabbed view in the Classifier to the front
     * and visible
     */
    public void selectTestClassTabbedView() {
        controller.getView().setClassTestTabbedViewVisible();
    }

    /**
     * Brings the training tabbed view in the Classifier to the front
     * and visible
     */
    public void selectTrainingPanelTabbedView() {
        controller.getView().setTrainingPanelViewVisible();
    }

    /**
     * Brings the run tabbed view in the Classifier to the front
     * and visible
     */
    public void selectRunPanelTabbedView() {
        controller.getView().setRunPanelVisible();
    }

    /**
     * Gets the {@link java.io.InputStreamReader} associated with the Matlab log file
     * This is intended for use for redirecting the Matlab text log ouput
     * to a display.
     *
     * @return the InputStreamReader
     */
    static InputStreamReader getInputStreamReader() {
        return isr;
    }

    /**
     * Returns the singleton <code>ClassifierLibraryJNI</code>
     * <p> An exception may be thrown if the Matlab log file does not exist,
     * which indicates there is something  wrong with the Matlab library
     * initialization. This is likely caused by an invalid matlab log directory
     *
     * Call within block synced by: <code>sync</code>
     *
     * @return a {@link org.mbari.aved.classifier.ClassifierLibraryJNI} singleton
     */
    public static synchronized ClassifierLibraryJNI getLibrary() throws Exception {
        if (isInitialized == false) {

            File dbDir = UserPreferences.getModel().getDefaultScratchDirectory();
            String dbRoot = dbDir.getAbsolutePath();
            File logFile = new File(dbRoot + "/matlablog.txt");

            try {
                jniLibrary.initLib(logFile.getAbsolutePath());
                Thread.sleep(5000);
                isInitialized = true;
            } catch (Exception e) {
                Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, e);
            }

            if (logFile.exists() && logFile.canRead()) {
                FileInputStream fis = new FileInputStream(logFile);
                isr = new InputStreamReader(fis);
            } 
        }

        if (isInitialized == true) {
            return jniLibrary;
        }

        return null;
    }

    /**
     * Closes the library. If this is called, the library will be reopened
     * during the first getLibrary() call
     *
     * @see     getLibrary()
     */
    public static synchronized void closeLibrary() {
        try {
            if (isInitialized == true) {
                jniLibrary.closeLib();
                isInitialized = false;
            }

        } catch (Exception ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test main. This build a classifier for testing ouside
     * of the main application.
     * @param args
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    Classifier c = new Classifier(new ApplicationModel());
                    c.getView().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    c.getView().setVisible(true);
                } catch (Exception ex) {
                    Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
