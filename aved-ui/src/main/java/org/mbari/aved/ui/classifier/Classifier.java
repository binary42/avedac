/*
 * @(#)Classifier.java
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

import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.ApplicationView;
import org.mbari.aved.ui.message.NonModalMessageDialog;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

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
    private static Preferences          settings   = new Preferences();

    public Classifier(ApplicationModel model) throws Exception {
        try {
            controller = new ClassifierController(model.getEventListModel(), model.getSummaryModel());

            ClassifierModel m = controller.getModel();

            settings = new Preferences(m);
        } catch (Exception ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex); 
            String msg = "Cannot create classifier. This is either "
                                    + "because the classifier libraries are missing, or your environment variables"
                                    + " MCR_ROOT/LD_LIBRARY_PATH/XAPPLRESDIR are incorrect";
            NonModalMessageDialog dialog;

            dialog = new NonModalMessageDialog((ApplicationView) Application.getView(), msg);
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
     * Brings the batch run tabbed view in the Classifier to the front
     * and visible
     */
    public void selectBatchRunPanelTabbedView() {
        controller.getView().setBatchRunPanelVisible();
    }

    /**
     * Test main. This build a classifier for testing ouside
     * of the main application.
     * @param args
     */
    /*public static void main(String[] args) {
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
    }*/
}
