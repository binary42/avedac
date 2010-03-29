/*
 * @(#)TrainingLibraryPopupMenu.java
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

import org.mbari.aved.ui.ApplicationInfo;

//~--- JDK imports ------------------------------------------------------------

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Creates a popupMenu menu. This is intended to be used with a
 * JList by adding a  MouseListener to the JLists in the
 * {@link  org.mbari.aved.ui.classifier.CreateTrainingLibraryView}
 *
 * @author D.Cline
 */
public class TrainingLibraryPopupMenu {
    private static String SELECT_ALL = "Select all";

    /** Defines the strings to use in the created mainMenu */
    private static String                   UNSELECT_ALL = "Unselect all";
    private int                             acceleratorMask;
    private JList                           invoker;
    private JMenuItem                       selectAllMenuItem;
    private JMenuItem                       unselectAllMenuItem;
    private final CreateTrainingLibraryView view;

    public TrainingLibraryPopupMenu(CreateTrainingLibraryView view) {
        this.view = view;

        String s = System.getProperty("os.name").toLowerCase();

        if ((s.indexOf("linux") != -1) || (s.indexOf("windows") != -1)) {
            acceleratorMask = (InputEvent.CTRL_MASK | (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        } else if (s.indexOf("mac") != -1) {
            acceleratorMask = (InputEvent.META_MASK | (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
    }

    /**
     * Shows the popupMenu in the same position as the invoker (e.g. mouse click postiion)
     * @param invoker
     * @param x
     * @param y
     */
    public void show(JList invoker, int x, int y) {
        this.invoker = invoker;

        ActionHandler handler = new ActionHandler();

        // Create the popupMenu menu.
        JPopupMenu popupMenu = new JPopupMenu(ApplicationInfo.getName() + " - Edit");

        unselectAllMenuItem = new JMenuItem(UNSELECT_ALL);
        unselectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, acceleratorMask));
        unselectAllMenuItem.addActionListener(handler);
        popupMenu.add(unselectAllMenuItem);
        selectAllMenuItem = new JMenuItem(SELECT_ALL);
        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, acceleratorMask));
        selectAllMenuItem.addActionListener(handler);
        popupMenu.add(selectAllMenuItem);
        popupMenu.show(invoker, x, y);
    }

    public class ActionHandler implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Thread change = new Thread(new Runnable() {
                        public void run() {
                            String actionCommand = e.getActionCommand();

                            if (invoker.equals(view.getAvailableList())) {
                                if (actionCommand.equals(TrainingLibraryPopupMenu.UNSELECT_ALL)) {
                                    view.clearAllAvailable();
                                } else if (actionCommand.equals(TrainingLibraryPopupMenu.SELECT_ALL)) {
                                    view.selectAllAvailable();
                                }
                            }

                            if (invoker.equals(view.getIncludeList())) {
                                if (actionCommand.equals(TrainingLibraryPopupMenu.UNSELECT_ALL)) {
                                    view.clearAllSelected();
                                } else if (actionCommand.equals(TrainingLibraryPopupMenu.SELECT_ALL)) {
                                    view.selectAll();
                                }
                            }
                        }
                    });

                    change.start();
                }
            });
        }
    }
}
