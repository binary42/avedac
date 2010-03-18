/*
 * @(#)Application.java   10/03/17
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import com.jgoodies.clearlook.ClearLookManager;
import com.jgoodies.plaf.Options;
import com.jgoodies.plaf.plastic.PlasticLookAndFeel;

import org.mbari.aved.ui.appframework.ErrorLog;

//~--- JDK imports ------------------------------------------------------------

import java.awt.*;

import java.io.File;

import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class Application {
    public static ApplicationLookandFeelSettings lookAndFeelSettings = ApplicationLookandFeelSettings.createDefault();
    public static final ErrorLog                 errLog              = ErrorLog.getInstance();
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
     * Helper function to return the <code>ApplicationView</code> associated with
     * this <code>Application</code>
     * @return the ApplicationView singleton
     */
    public static ApplicationView getView() {
        return (ApplicationView) controller.getView();
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
        String lcOSName = System.getProperty("os.name").toLowerCase();

        // If mac, Set Aqua (or future default Apple VM platform look-and-feel
        if (lcOSName.startsWith("mac os x")) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(Application.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {

            // Otherwise, set the look and feel to a metal look
            Options.setDefaultIconSize(new Dimension(18, 18));
            UIManager.put(Options.USE_SYSTEM_FONTS_APP_KEY, lookAndFeelSettings.isUseSystemFonts());
            Options.setGlobalFontSizeHints(lookAndFeelSettings.getFontSizeHints());
            Options.setUseNarrowButtons(lookAndFeelSettings.isUseNarrowButtons());
            Options.setTabIconsEnabled(lookAndFeelSettings.isTabIconsEnabled());
            ClearLookManager.setMode(lookAndFeelSettings.getClearLookMode());
            ClearLookManager.setPolicy(lookAndFeelSettings.getClearLookPolicyName());

            LookAndFeel lookAndFeel = lookAndFeelSettings.getSelectedLookAndFeel();

            if (lookAndFeel instanceof PlasticLookAndFeel) {
                PlasticLookAndFeel.setMyCurrentTheme(lookAndFeelSettings.getSelectedTheme());
                PlasticLookAndFeel.setTabStyle(lookAndFeelSettings.getPlasticTabStyle());
                PlasticLookAndFeel.setHighContrastFocusColorsEnabled(
                    lookAndFeelSettings.isPlasticHighContrastFocusEnabled());
            } else if (lookAndFeel.getClass() == MetalLookAndFeel.class) {
                MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
            }

            // Override tabbed pane colors
            UIManager.put("TabbedPane.selected", Color.WHITE);
            UIManager.put("TableHeader.background", Color.WHITE);
            UIManager.put("TableHeader.cellBorder", Color.WHITE);

            JRadioButton radio = new JRadioButton();

            radio.getUI().uninstallUI(radio);

            JCheckBox checkBox = new JCheckBox();

            checkBox.getUI().uninstallUI(checkBox);

            try {
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (UnsupportedLookAndFeelException e) {
                System.err.println("Can't use the specified look and feel (" + lookAndFeel + ") on this platform.");
                System.err.println("Using the default look and feel.");
            } catch (Exception e) {
                System.err.println("Couldn't get specified look and feel (" + lookAndFeel + "), for some reason.");
                System.err.println("Using the default look and feel.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {

        // Set the look and feel.
        initLookAndFeel();

        final Application app        = new Application();
        JFrame            view       = app.getView();
        Toolkit           kit        = view.getToolkit();
        Dimension         screenSize = kit.getScreenSize();

        // Set window size to 90% of display
        view.setSize((int) (0.90 * screenSize.width), (int) (0.90 * screenSize.height));

        // Display window in center of screen
        int x = (screenSize.width - view.getWidth()) / 2;
        int y = (screenSize.height - view.getHeight()) / 2;

        view.setLocation(x, y);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Thread change = new Thread(new Runnable() {
                    public void run() {
                        try {

                            // URL name = getClass().getResource("/2344_00_32_40_25.events.xml");
                            // URL name = getClass().getResource("/20060808T000000_4912_32_103.events.xml");
                            // URL name = new URL("file:/Volumes/nanomiaRAID-1/JAMSTEC/20080604T063139Z_tests/20080604T063139Z-test1/20080604T063139Z-test1.events.xml");
                            // app.getController().importProcessedResults(new File(name.getFile()));
                        } catch (Exception e) {

                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });

                change.start();
            }
        });
        view.setVisible(true);
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
