/*
 * @(#)Application.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.ErrorLog;
import org.mbari.aved.ui.classifier.Classifier;
import org.mbari.aved.ui.message.NonModalMessageDialog;

//~--- JDK imports ------------------------------------------------------------
 
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import javax.swing.*; 

public class Application {
    public static final ErrorLog                 errLog              = ErrorLog.getInstance();
    private static final Application             INSTANCE            = new Application();
    public static ApplicationLookandFeelSettings lookAndFeelSettings = ApplicationLookandFeelSettings.createDefault();
    private static ApplicationController         controller;

    public Application() {
        try {

            // Create controller which creates the view, and model
            controller = new ApplicationController();
        } catch (Exception ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get the singleton for this object.
     * @return
     */
    public static synchronized Application getInstance() {

        // Prevent two threads from calling this method
        // at the same time
        return INSTANCE;
    }

    /**
     * Helper function to return the <code>ApplicationView</code> associated with
     * this <code>Application</code>
     * @return the ApplicationView singleton
     */
    public static ApplicationView getView() {
        return (ApplicationView) controller.getView();
    }

    /**
     *  Helper function to return the <code>Classifier</code> associated with
     * this <code>Application</code>
     * @return the Classifier singleton
     */
    public static Classifier getClassifier() {
        return controller.getClassifier();
    }

    /**
     * Helper function to return the Model associated with this Application
     * @return the ApplicationModel singleton
     */
    public static ApplicationModel getModel() {
        return (ApplicationModel) controller.getModel();
    }

    /**
     * Helper function to return the Controller associated with this Application
     * @return the ApplicationController singleton
     */
    public static ApplicationController getController() {
        return (ApplicationController) controller;
    }

    /**
     * Apply parameters to the GUI in function of data in settings
     * @author D.Cline
     */
    private static void initLookAndFeel() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Checks for jpg reader
     * @return false if not valid jpg reader is found
     */
    private static boolean checkForJpgReader() {
        ImageIO.scanForPlugins();

        String[] formats = ImageIO.getReaderFormatNames();

        for (int i = 0; i < formats.length; i++) {
            if (formats[i].equalsIgnoreCase("jpg")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {

        // Set the look and feel.
        initLookAndFeel();

        final JFrame view = Application.getView();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Thread change = new Thread(new Runnable() {
                    public void run() {
                        try {

                            // URL name = getClass().getResource("/examples/2344_00_32_40_25.events.small.xml");
                            // URL name = getClass().getResource("/examples/20060808T000000_4912_32_103.events.xml");
                            // URL name = new URL("file:/Volumes/nanomiaRAID-1/JAMSTEC/20080604T063139Z_tests/20080604T063139Z-test1/20080604T063139Z-test1.events.xml");
                            // Application.getController().importProcessedResults(new File(name.getFile()));

                            /*
                             *  Jpeg readers should exist for most platforms through the javax.imageio package.
                             * This is put here just in case this is run on Linux with a pre-packaged
                             * java distribution
                             */
                            if (!checkForJpgReader()) {
                                String message = "You are missing the Java Advanced Imaging\n"
                                                            + "Library needed to view .jpg images. Please download \n"
                                                            + "the package for your platform here:\n"
                                                            + "https://jai.dev.java.net/binary-builds.html\n"
                                                            + "and install version 1.1.3 or better";
                                NonModalMessageDialog dialog = new NonModalMessageDialog(view, message);

                                dialog.setVisible(true);
                                dialog.answer();
                                System.exit(-1);
                            }
                        } catch (Exception e) { 
                            Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, e);
                        }
                    }
                });

                change.start();
            }
        });
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
