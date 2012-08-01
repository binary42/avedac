/*
 * @(#)MainMenu.java
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

import com.vlsolutions.swing.docking.DockingDesktop;

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.classifier.ClassImageDirectoryViewManager;
import org.mbari.aved.ui.classifier.DockingContainer;
import org.mbari.aved.ui.detectionsettings.DetectionSettings;
import org.mbari.aved.ui.message.MessagePrintStream;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.URLUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 *
 */
public class MainMenu implements ModelListener {
    public static String       BATCH_PROCESS           = "Batch process";
    public static String       CLOSE_EVENTS            = "Close events";
    public static String       CREATE_CLASS            = "Create class";
    public static String       CREATE_TRAINING_LIBRARY = "Create training library";
    public static String       DETECTION_SETTINGS      = "Detection Settings";
    public static String       EXIT                    = "Exit";
    public static String       EXPORT_EVENTS           = "Export events";
    public static String       FILE_PROCESS            = "Video file";
    public static String       HELP_CONTENTS           = "Contents";
    public static String       MESSAGES                = "Messages";
    public static String       OPEN_EVENTS             = "Open events";
    public static String       ORGANIZE_CLASSES        = "Organize classes";
    public static String       PREFERENCES             = "Preferences";
    public static String       RUN_CLASSIFIER          = "Run";
    public static String       BATCH_RUN_CLASSIFIER    = "Batch process";
    public static String       SAVE_EVENTS             = "Save events";
    public static String       SAVE_EVENTS_AS          = "Save events as...";
    public static String       STREAM_PROCESS          = "Video stream";
    public static String       TEST_CLASS              = "Test class";
    private static final long  serialVersionUID        = 1L;
    public final static Cursor defaultCursor           = Cursor.getDefaultCursor();

    /** Busy and wait cursor */
    public final static Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

    /** Help menu items */
    public static String           HELP_ABOUT = "About " + ApplicationInfo.getName();
    private static JFrame          imageOrganizer;
    private JMenuItem              closeEventsItem;
    private JMenuItem              createClassItem;
    private JMenuItem              createTrainingLibraryItem;
    private DetectionSettings      detectionSettings;
    private EditMenu               editM;
    private JMenu                  editMenu;
    private JMenuItem              exitItem;
    private JMenuItem              exportExcelEventsItems;
    private JMenuItem              helpContentsMenuItem;
    private final ApplicationModel model;
    private JMenuItem              openClassifierPreferenceItem;
    private JMenuItem              openEventsItem;
    private JMenuItem              organizeClassImagesMenuItem;
    private JMenuItem              runClassifierItem;
    private JMenuItem              saveEventsItemAs;
    private JMenuItem              saveEventsMenuItem;
    private SummaryView            summaryView;
    private JMenuItem              testClassItem;
    private JMenuItem batchRunClassifierItem;

    public MainMenu(ApplicationModel model) {
        this.model = model;
        model.addModelListener(this);
    }

    /**
     * Displays the Classifier class creation interface
     */
    void displayCreateClass() {
        Application.getClassifier().selectCreateClassTabbedView();
        display(Application.getClassifier().getView());
    }

    /**
     * Displays the Classifier test class interface
     */
    void displayTestClass() {
        Application.getClassifier().selectTestClassTabbedView();
        display(Application.getClassifier().getView());
    }

    /**
     * Displays the Classifier training library interface
     */
    private void displayTrainingLibrary() {
        Application.getClassifier().selectTrainingPanelTabbedView();
        display(Application.getClassifier().getView());
    }

    /**
     * Displays the Classifier run interface
     */
    private void displayRunClassifier() {
        Application.getClassifier().selectRunPanelTabbedView();
        display(Application.getClassifier().getView());
    }

    /**
     * Displays the Classifier batch run interface
     */
    private void displayBatchRunClassifier() {
        Application.getClassifier().selectBatchRunPanelTabbedView();
        display(Application.getClassifier().getView());
    }


    /**
     * Displays the Classifier setup interface
     */
    private void displayClassifierSettings() {
        display(Application.getClassifier().getClassifierSetingsView());
    }

    private void displayMessages() {
        display(MessagePrintStream.getView());
    }

    private void displayPreferences() {
        display(UserPreferences.getInstance().getView());
    }

    private void displaySettings() {
        display(detectionSettings.getView());
    }

    /**
     * This must be public to be registed with the OSXAdapter
     */
    public void showAboutBox() {
        new ApplicationInfoView().setVisible(true);
    }

    /**
     * This must be public to be registed with the OSXAdapter
     */
    public void exitApp() {
        try {
            Application.getController().shutdown();
        } catch (Exception ex) {
            Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.exit(0);
    }

    /**
     * Displays the Classifier class image organizer
     */
    void displayClassImageOrganizer() {
        if (imageOrganizer == null) {
            imageOrganizer = new JFrame("AVEDac Class Image Organizer");

            // TODO: push this logic into the ApplicationController class
            // Don't do anything; require the program to handle the operation in the
            // windowClosing  method of a registered WindowListener object.
            imageOrganizer.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            imageOrganizer.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    DockingContainer dockContainer = (DockingContainer) imageOrganizer.getContentPane();
                    DockingDesktop   desktop       = dockContainer.getDesktop();

                    desktop.clear();
                    imageOrganizer.dispose();
                    imageOrganizer = null;
                }
            });
            imageOrganizer.addKeyListener(new KeyListener() {
                public void keyTyped(KeyEvent e) {}
                public void keyPressed(KeyEvent e) {
                    String s = System.getProperty("os.name").toLowerCase();

                    if (((s.indexOf("linux") != -1) || (s.indexOf("windows") != -1))
                            && (e.getKeyCode() == KeyEvent.VK_C)) {
                        imageOrganizer.setVisible(false);
                    } else if ((s.indexOf("mac") != -1) && (e.getKeyCode() == KeyEvent.VK_W)) {
                        imageOrganizer.setVisible(false);
                    }
                }
                public void keyReleased(KeyEvent e) {}
            });

            try {
                ClassImageDirectoryViewManager viewmgr = new ClassImageDirectoryViewManager(Application.getClassifier().getModel());

                imageOrganizer.setContentPane(new DockingContainer(viewmgr));
                imageOrganizer.pack();
                imageOrganizer.setBounds(200, 200, 800, 800);
                imageOrganizer.setVisible(true);
            } catch (Exception ex) {
                Logger.getLogger(DockingContainer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            imageOrganizer.setVisible(true);
        }
    }

    /**
     * Sets the mneumonic based on ths OS
     * @param item
     * @param mnemonic
     */
    private void setMenuItemMneumonic(JMenuItem item, int mnemonic) {
        String s = System.getProperty("os.name").toLowerCase();

        if ((s.indexOf("linux") != -1)) {
            item.setAccelerator(KeyStroke.getKeyStroke(mnemonic, Event.CTRL_MASK));
        } else if (s.indexOf("mac") != -1) {
            item.setAccelerator(KeyStroke.getKeyStroke(mnemonic,
                    (java.awt.event.InputEvent.META_MASK | (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()))));
        }
    }

    /**
     * This method creates a menuBar
     *
     * @return javax.swing.JMenuBar
     */
    public JMenuBar buildJJMenuBar() {

        // Build the menus

        /** **************File menu ************** */
        JMenu         fileMenu = new JMenu("File");
        ActionHandler l        = new ActionHandler();

        openEventsItem = new JMenuItem(OPEN_EVENTS);
        setMenuItemMneumonic(openEventsItem, KeyEvent.VK_O);
        openEventsItem.addActionListener(l);
        fileMenu.add(openEventsItem);
        saveEventsMenuItem = new JMenuItem(SAVE_EVENTS);
        setMenuItemMneumonic(saveEventsMenuItem, KeyEvent.VK_S);
        saveEventsMenuItem.addActionListener(l);
        fileMenu.add(saveEventsMenuItem);

        // This is not enabled, until there is something to save
        saveEventsMenuItem.setEnabled(false);
        saveEventsItemAs = new JMenuItem(SAVE_EVENTS_AS);
        saveEventsItemAs.addActionListener(l);
        fileMenu.add(saveEventsItemAs);

        // This is not enabled, until there is something to save
        saveEventsItemAs.setEnabled(false);
        closeEventsItem = new JMenuItem(CLOSE_EVENTS);

        // Export to Excel file
        // This is not enabled, until there is something to export
        exportExcelEventsItems = new JMenuItem(EXPORT_EVENTS);
        exportExcelEventsItems.setEnabled(false);
        exportExcelEventsItems.addActionListener(l);
        fileMenu.add(exportExcelEventsItems);

        String s = System.getProperty("os.name").toLowerCase();

        if ((s.indexOf("linux") != -1) || (s.indexOf("windows") != -1)) {
            setMenuItemMneumonic(closeEventsItem, KeyEvent.VK_C);
        } else if (s.indexOf("mac") != -1) {
            setMenuItemMneumonic(closeEventsItem, KeyEvent.VK_W);
        }

        closeEventsItem.addActionListener(l);
        fileMenu.add(closeEventsItem);
        closeEventsItem.setEnabled(false);

        /**
         * Create the exit item, even if not using as
         * it is used to shutdown the same way on any platform
         */
        exitItem = new JMenuItem(EXIT);
        exitItem.addActionListener(l);

        /*
         *  Add the mneumonic control x and Exit option in the File menu
         * if running  linux or windows
         */
        if ((s.indexOf("linux") != -1) || (s.indexOf("windows") != -1)) {
            setMenuItemMneumonic(exitItem, KeyEvent.VK_X);
            fileMenu.add(exitItem);
        }

        /** **************Classifier menu ************** */
        JMenu classifierMenu = new JMenu("Classifier");

        runClassifierItem = new JMenuItem(RUN_CLASSIFIER);
        classifierMenu.add(runClassifierItem);
        batchRunClassifierItem = new JMenuItem(BATCH_RUN_CLASSIFIER);
        classifierMenu.add(batchRunClassifierItem);
        batchRunClassifierItem.addActionListener(l);
        runClassifierItem.addActionListener(l);
        createClassItem = new JMenuItem(CREATE_CLASS);
        classifierMenu.add(createClassItem);
        createClassItem.addActionListener(l);
        createTrainingLibraryItem = new JMenuItem(CREATE_TRAINING_LIBRARY);
        classifierMenu.add(createTrainingLibraryItem);
        createTrainingLibraryItem.addActionListener(l);
        testClassItem = new JMenuItem(TEST_CLASS);
        classifierMenu.add(testClassItem);
        testClassItem.addActionListener(l);
        classifierMenu.addSeparator();
        organizeClassImagesMenuItem = new JMenuItem(ORGANIZE_CLASSES);
        classifierMenu.add(organizeClassImagesMenuItem);
        organizeClassImagesMenuItem.addActionListener(l);
        classifierMenu.addSeparator();
        openClassifierPreferenceItem = new JMenuItem(PREFERENCES);
        classifierMenu.add(openClassifierPreferenceItem);
        openClassifierPreferenceItem.addActionListener(l);

        /** **************Help menu ************** */
        JMenu helpMenu = new JMenu("Help");

        helpContentsMenuItem = new JMenuItem(HELP_CONTENTS);
        helpMenu.add(helpContentsMenuItem);

        // Initalize listener for the JavaHelp system
        try {
            URL         url    = Application.class.getResource("/org/mbari/aved/ui/HelpSet.hs");
            ClassLoader loader = Application.class.getClassLoader();
            HelpSet     hs     = new HelpSet(loader, url);
            HelpBroker  hb     = hs.createHelpBroker();

            addHelpActionListener(new CSH.DisplayHelpFromSource(hb));
        } catch (Exception e) {
            e.printStackTrace();
        }

        JMenu toolsMenu = new JMenu("Tools");

        // If mac, handling preferences and about menu differently
        if (s.indexOf("mac") != -1) {
            try {

                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("exitApp", (Class[]) null));
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAboutBox", (Class[]) null));
                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("showPrefsDialog", (Class[]) null));
                OSXAdapter.setFileHandler(this,
                                          getClass().getDeclaredMethod("openEvent", new Class[] { String.class }));
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        } else {
            JMenuItem item = new JMenuItem(PREFERENCES);

            setMenuItemMneumonic(item, KeyEvent.VK_P);
            item.addActionListener(l);
            toolsMenu.add(item);
            item = new JMenuItem(HELP_ABOUT);
            setMenuItemMneumonic(item, KeyEvent.VK_A);
            item.addActionListener(l);
            helpMenu.add(item);
        }

        JMenu edit = new JMenu("Edit");

        editM    = new EditMenu(this.model);
        editMenu = (JMenu) editM.create(edit);

        JMenu     viewMenu = new JMenu("View");
        JMenuItem item     = new JMenuItem(MESSAGES);

        setMenuItemMneumonic(item, KeyEvent.VK_M);
        item.addActionListener(l);
        viewMenu.add(item);

        JMenuBar menubar = new JMenuBar();

        // Now add the menus to the menu bar
        menubar.add(fileMenu);
        menubar.add(editMenu);
        menubar.add(classifierMenu);

        // menubar.add(processMenu);
        menubar.add(viewMenu);

        // If not mac then add the tools menu
        if (s.indexOf("mac") == -1) {
            menubar.add(toolsMenu);
        }

        menubar.add(helpMenu);
        runDisplayLogic();

        return menubar;
    }

    /** Displays the help system */
    public void addHelpActionListener(ActionListener l) {
        if (helpContentsMenuItem != null) {
            helpContentsMenuItem.addActionListener(l);
        }
    }

    /**
     * Updates the menu items enabled/disabled state according
     * to the number of list items. This should be run when this
     * is first created, and whenever the model changes
     */
    private void runDisplayLogic() {
        EventListModel listModel = model.getEventListModel();

        // If there is more than one elements in the list model
        // enable the save/close functions and disable
        // the open functions in the menu
        if (listModel.getMaximum() != -1) {
            openEventsItem.setEnabled(false);
            saveEventsMenuItem.setEnabled(true);
            saveEventsItemAs.setEnabled(true);
            exportExcelEventsItems.setEnabled(true);
            closeEventsItem.setEnabled(true);
        } else {
            openEventsItem.setEnabled(true);
            closeEventsItem.setEnabled(false);
            saveEventsMenuItem.setEnabled(false);
            saveEventsItemAs.setEnabled(false);
            exportExcelEventsItems.setEnabled(false);
        }
    }

    /** Handles the model changes from the ApplicationModel */
    public void modelChanged(ModelEvent event) {
        if (event instanceof EventListModel.EventListModelEvent) {
            switch (event.getID()) {
            case EventListModel.EventListModelEvent.LIST_RELOADED :
                runDisplayLogic();

                break;

            case EventListModelEvent.LIST_CLEARED :

                // Disable save/close function
                openEventsItem.setEnabled(true);
                closeEventsItem.setEnabled(false);
                saveEventsMenuItem.setEnabled(false);
                saveEventsItemAs.setEnabled(false);
                exportExcelEventsItems.setEnabled(false);

                break;

            default :
                break;
            }
        }
    }

    public SummaryView getSummaryView() {
        return summaryView;
    }

    /**
     * Sends exit action event to shutdown application. This is only used
     *  in the Mac version
     */
    public void sendExit() {
        ActionListener l[] = exitItem.getActionListeners();
        ActionEvent    e   = new ActionEvent(this, 0, EXIT);

        if (l.length > 0) {
            l[0].actionPerformed(e);
        }
    }

    /**
     * This must be public to be registed with the OSXAdapter
     */
    public void showPrefsDialog() {
        UserPreferences.getInstance().getView().setVisible(true);
    }

    private void display(JFrame v) {
        Application.getView().display(v);
    }

    private void openEvent(String path) {

        // TODO: Add function to ask user close events, then re-open new event
    }

    private void openEvents() {

        // TODO: if events are currently open and modified,
        // ask user if want to save the changes made before
        // allowing to open and thrash existing data
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Thread change = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Application.getController().importProcessedResults();
                        } catch (Exception e) {

                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });

                change.start();
            }
        });
    }

    public class ActionHandler implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Thread change = new Thread(new Runnable() {
                        public void run() {
                            Object obj           = e.getSource();
                            String actionCommand = e.getActionCommand();

                            if (actionCommand.equals(MainMenu.EXIT)) {
                                exitApp();
                            } else if (obj.equals(createTrainingLibraryItem)) {
                                displayTrainingLibrary();
                            } else if (obj.equals(runClassifierItem)) {
                                displayRunClassifier();}
                            else if (obj.equals(batchRunClassifierItem)) {
                                displayBatchRunClassifier();
                            } else if (obj.equals(openClassifierPreferenceItem)) {
                                displayClassifierSettings();
                            } else if (obj.equals(createClassItem)) {
                                displayCreateClass();
                            } else if (obj.equals(testClassItem)) {
                                displayTestClass();
                            } else if (obj.equals(organizeClassImagesMenuItem)) {
                                displayClassImageOrganizer();
                            } else if (actionCommand.equals(MainMenu.DETECTION_SETTINGS)) {
                                displaySettings();
                            } else if (actionCommand.equals(MainMenu.MESSAGES)) {
                                displayMessages();
                            } else if (actionCommand.equals(MainMenu.CLOSE_EVENTS)) {
                                Application.getController().reset();
                            } else if (actionCommand.equals(MainMenu.OPEN_EVENTS)) {
                                openEvents();
                            } else if (actionCommand.equals(MainMenu.HELP_ABOUT)) {
                                showAboutBox();
                            } else if (obj.equals(saveEventsMenuItem)) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        Thread change = new Thread(new Runnable() {
                                            public void run() {
                                                try {
                                                    Application.getController().saveProcessedResults();
                                                } catch (Exception e) {

                                                    // TODO Auto-generated catch block
                                                    e.printStackTrace();
                                                }
                                            }
                                        });

                                        change.start();
                                    }
                                });
                            } else if (obj.equals(saveEventsItemAs)) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        Thread change = new Thread(new Runnable() {
                                            public void run() {
                                                try {
                                                    Application.getController().saveProcessedResultsAs();
                                                } catch (Exception e) {

                                                    // TODO Auto-generated catch block
                                                    e.printStackTrace();
                                                }
                                            }
                                        });

                                        change.start();
                                    }
                                });
                            } else if (obj.equals(exportExcelEventsItems)) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        Thread change = new Thread(new Runnable() {
                                            public void run() {
                                                try {
                                                    Application.getController().exportProcessedResultsAsXls();
                                                } catch (Exception e) {

                                                    // TODO Auto-generated catch block
                                                    e.printStackTrace();
                                                }
                                            }
                                        });

                                        change.start();
                                    }
                                });

                                // TODO: fill in settings for these
                            } else if (actionCommand.equals(MainMenu.DETECTION_SETTINGS)) {}
                            else if (actionCommand.equals(MainMenu.PREFERENCES)) {
                                displayPreferences();
                            } else if (actionCommand.equals(MainMenu.FILE_PROCESS)) {}
                            else if (actionCommand.equals(MainMenu.STREAM_PROCESS)) {}
                            else if (actionCommand.equals(MainMenu.BATCH_PROCESS)) {
                            
                            }
                            else if (actionCommand.equals(MainMenu.HELP_ABOUT)) {}
                            else if (actionCommand.equals(MainMenu.HELP_CONTENTS)) {

                                // This is already tied to the HelpBroker so no need to handle this here
                            } else {

                                // TODO: throw exception here
                            }
                        }
                    });

                    change.start();
                }
            });
        }
    }


    /**
     * Ce panneau représente l'en-tête d'un formulaire. Un en-tête affiche un logo (dont l'opacité peut
     * être modifiée) et un titre.
     *
     * @author Roguy
     */
    public static class HeaderPanel extends JPanel {
        private static AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
        private Color                 blender;
        private ImageIcon             icon;

        /**
         * Crée un nouvel en-tête affichant l'image sélectionnée, avec l'opacité choisie et le titre
         * spécifié.
         *
         * @param title Le titre de l'en-tête
         * @param icon L'image d'en-tête
         * @param alpha L'opacité de l'image
         */
        public HeaderPanel(String title, ImageIcon icon, float alpha) {
            super(new BorderLayout());
            this.icon    = icon;
            this.blender = new Color(255, 255, 255, (int) (255 * alpha));

            JLabel headerTitle = new JLabel(title);
            Font   font        = new Font("Papyrus", Font.PLAIN, 20);

            // Font font = headerTitle.getFont().deriveFont(Font.PLAIN, 20.0f);
            headerTitle.setFont(font);
            headerTitle.setBorder(new EmptyBorder(0, 0, 0, 4));
            headerTitle.setForeground(new Color(0x04549a));
            add(BorderLayout.EAST, headerTitle);
            setPreferredSize(new Dimension(this.icon.getIconWidth(), this.icon.getIconHeight()));
        }

        /**
         * Récupère la couleur de l'en-tête en fonction du thème choisi.
         *
         * @return Le couleur de fond de l'en-tête
         */
        protected Color getHeaderBackground() {
            Color c = UIManager.getColor("SimpleInternalFrame.activeTitleBackground");

            if (c != null) {
                return c;
            }

            return (c != null)
                   ? c
                   : UIManager.getColor("InternalFrame.activeTitleBackground");
        }

        /**
         * Dessine un dégradé dans le composant.
         *
         * @param g L'objet graphique sur lequel peindre
         */
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (!isOpaque()) {
                return;
            }

            Color      control = new Color(178, 213, 255);
            int        width   = getWidth();
            int        height  = getHeight();
            Graphics2D g2      = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            Paint storedPaint = g2.getPaint();

            g2.setPaint(new GradientPaint(this.icon.getIconWidth(), 0, Color.white, width, 0, control));
            g2.fillRect(0, 0, width, height);
            g2.setPaint(storedPaint);
            g2.drawImage(this.icon.getImage(), 0, 0, this);
            g2.setColor(blender);
            g2.setComposite(composite);
            g2.fillRect(0, 0, this.icon.getIconWidth(), this.icon.getIconHeight());
        }
    }


    public static class SummaryView extends JFrameView {
        public static final String ID_IMPORT_DIRECTORY_LABEL     = "importDir";                   // javax.swing.JTable
        public static final String ID_INPUTSOURCE_URL_LABEL      = "videoSourceURL";              // javax.swing.JLabel
        public static final String ID_MASTER_BROWSE_BUTTON       = "browseMaster";                // javax.swing.JButton
        public static final String ID_MPEG_BROWSE_BUTTON         = "browseMpeg";                  // javax.swing.JButton
        public static final String ID_MPEG_FILE_LABEL            = "mpegFile";                    // javax.swing.JLabel
        public static final String ID_TRANSCODE_OUTPUT_DIR_LABEL = "transcodeSourceOutputDir";    // javax.swing.JLabel
        public static final String ID_TRANSCODE_SOURCE_LABEL     = "transcodeSource";             // javax.swing.JLabel

        /*
         *  Component names in the EditorSummary.xml form
         * If any of the component name are changed in the Abeille form designer, they
         * should be modified here too
         */
        public static final String ID_XML_FILE_LABEL = "xmlFile";    // javax.swing.JLabel

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public SummaryView(ApplicationModel model, ApplicationController controller) {

            // Constructor
            super("org/mbari/aved/ui/forms/Summary.xml", model, controller);

            // Add action listeners for the button, but disable them from
            // view until there is data to edit
            ActionHandler l = getActionHandler();

            getForm().getButton(ID_MPEG_BROWSE_BUTTON).addActionListener(l);
            getForm().getButton(ID_MASTER_BROWSE_BUTTON).addActionListener(l);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics g = graphics.create();

            g.dispose();
        }

        /**
         * Updates the display logic.
         *  Changes the labels and associated buttons
         *  If all files are in the same directory, sets the import directory label and strips off
         *  the full path names of the results XML and video clips to avoid clutter in the UI.
         *  Otherwise, if files are in different directories, displays the full path names
         */
        private void runDisplayLogic() {
            SummaryModel e               = ((ApplicationModel) getModel()).getSummaryModel();
            File         xml             = e.getXmlFile();
            URL          mpeg            = e.getMpegUrl();
            URL          inputsource     = e.getInputSourceURL();
            File         transcodeDir    = e.getFrameSourceDir(); 

            // If the xml is defined then show all related files/directories
            // relative to it
            if (xml != null) {
                getForm().getLabel(ID_IMPORT_DIRECTORY_LABEL).setText(xml.getParent().toString());
                getForm().getLabel(ID_XML_FILE_LABEL).setText(xml.getName());

                if (mpeg != null) {
                    if (URLUtils.isFileUrl(mpeg.toString())) {
                        File f = new File(mpeg.toString());

                        getForm().getLabel(ID_MPEG_FILE_LABEL).setText("<html><a href>" + f.getName()
                                + "</a></html>");
                    } else {

                        // This is a URL. Since it can be long, so only display the name
                        // not the fullpath
                        File f = new File(mpeg.getPath());

                        getForm().getLabel(ID_MPEG_FILE_LABEL).setText("<html><a href>" + f.getName()
                                + "</a></html>");
                    }
                }

                if (inputsource != null) {
                    if (URLUtils.isFileUrl(inputsource.toString())) {
                        File f = new File(inputsource.toString());

                        getForm().getLabel(ID_INPUTSOURCE_URL_LABEL).setText("<html><a href>" + f.getName()
                                + "</a></html>");
                    } else {

                        /**
                         * This is a URL. Since it can be long, so only display the name
                         * not the fullpath.  Only display the URL if it doesn't end with
                         * a tar tgz, or tar.gz
                         */
                        File f = new File(inputsource.getPath());

                        getForm().getLabel(ID_INPUTSOURCE_URL_LABEL).setText("<html><a href>" + f.getName()
                                + "</a></html>");
                    }
                }

                if (transcodeDir != null) {
                    getForm().getLabel(ID_TRANSCODE_OUTPUT_DIR_LABEL).setText(
                        transcodeDir.getAbsolutePath().toString());
                }
 
            } else {

                // If the xml isn't defined, then show all full path names
                // and html reference if valid, otherwise indicate missing
                // data with "-"
                getForm().getLabel(ID_IMPORT_DIRECTORY_LABEL).setText("-");
                getForm().getLabel(ID_XML_FILE_LABEL).setText("-");

                if (mpeg != null) {
                    getForm().getLabel(ID_MPEG_FILE_LABEL).setText("<html><a href>" + mpeg.toString()
                            + "</a></html>");
                } else {
                    getForm().getLabel(ID_MPEG_FILE_LABEL).setText("-");
                }

                if (inputsource != null) {
                    getForm().getLabel(ID_INPUTSOURCE_URL_LABEL).setText("<html><a href>"
                            + inputsource.toString() + "</a></html>");
                } else {
                    getForm().getLabel(ID_INPUTSOURCE_URL_LABEL).setText("-");
                }

                if (transcodeDir != null) {
                    getForm().getLabel(ID_TRANSCODE_OUTPUT_DIR_LABEL).setText(transcodeDir.getName().toString());
                } else {
                    getForm().getLabel(ID_TRANSCODE_OUTPUT_DIR_LABEL).setText("-");
                }
 
            }
        }

        /** Handles the model changes from the ApplicationModel */
        public void modelChanged(ModelEvent event) {
            if (event instanceof SummaryModel.SummaryModelEvent) {
                runDisplayLogic();
            }
        }
    }
}
