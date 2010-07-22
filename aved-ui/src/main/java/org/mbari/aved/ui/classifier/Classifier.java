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

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationView;
import org.mbari.aved.ui.message.NonModalMessageDialog;

/**
 * The AVED Classifier. This handles creating the components needed
 * for creating classes, testing classes, and running the classifier.
 * The underlying AVED Classifier is Matlab code, and that code is
 * accessed through a JNI layer in the
 * {@link org.mbari.aved.classifier.ClassifierLibraryJNI}
 * @author dcline
 */
public final class Classifier {

    private static ClassifierController controller = new ClassifierController();
    private static Preferences settings = new Preferences();

    public Classifier(ApplicationModel model) throws Exception {
        try {
            controller = new ClassifierController(model.getEventListModel(), model.getSummaryModel());
            ClassifierModel m = controller.getModel();
            settings = new Preferences(m);
        } catch (Exception ex) {
            String msg = new String("Cannot create classifier. This is either " +
                    "because the classifier libraries are missing, or your environment variables" +
                    " MCR_ROOT/LD_LIBRARY_PATH/XAPPLRESDIR are incorrect");
            NonModalMessageDialog dialog;
            dialog = new NonModalMessageDialog((ApplicationView) Application.getView(),
                    msg);
            dialog.setVisible(true);
            throw new Exception("Cannot create Classifier");
        }
    }

    public static ClassifierController getController() {
        return controller;
    }

    public JFrame getClassifierSetingsView() {
        return settings.getView();
    }

    public JFrame getView() {
        return controller.getView();
    }

    /**
     * Does a clean shutdown of the classifier
     */
    public void shutdown() {
        controller.shutdown();
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
