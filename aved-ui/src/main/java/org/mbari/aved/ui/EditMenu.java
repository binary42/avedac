/*
 * @(#)EditMenu.java
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

import org.mbari.aved.ui.classifier.ConceptTreePanel;
import org.mbari.aved.ui.command.AbstractCommand;
import org.mbari.aved.ui.command.BiologicalClassificationCommand;
import org.mbari.aved.ui.command.CombineCommand;
import org.mbari.aved.ui.command.CommandHistory;
import org.mbari.aved.ui.command.CommandHistory.UndoRedoState;
import org.mbari.aved.ui.command.DeleteCommand;
import org.mbari.aved.ui.command.Execute;
import org.mbari.aved.ui.command.IdCommand;
import org.mbari.aved.ui.command.TagCommand;
import org.mbari.aved.ui.message.ModalYesNoNeverDialog;
import org.mbari.aved.ui.model.EventAbstractTableModel;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.TableSorter;
import org.mbari.aved.ui.player.PlayerManager;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Creates and manages a mainMenu, either as a mainMenu mainMenu
 * or a JMenu for editing AVEDac results
 * @author D.Cline
 */
public class EditMenu extends JFrame {
    private static String ASSIGN           = "Assign last ";
    private static String CLASS_MAINMENU   = "Class";
    private static String CLASS_NEW_CUSTOM = "Custom";
    private static String CLEAR            = "Clear";
    private static String CLEAR_LIST       = "Clear list";
    private static String COMBINE_EVENTS   = "Combine";
    private static String DELETE_EVENT     = "Delete";

    /** Defines the strings to use in the dynamically created mainMenu */
    private static String                 GOTO           = "Goto";
    private static String                 ID_MAINMENU    = "Identity reference";
    private static String                 ID_REPEAT      = "Assign last identity ";
    private static String                 LABEL_EVENT    = "Label event";
    private static String                 NEW            = "New";
    private static String                 OPEN_EVENT     = "Open event editor";
    private static String                 REDO           = "Redo";
    private static String                 TAG_MAINMENU   = "Tag";
    private static String                 TAG_REPEAT     = "Assign last tag ";
    private static String                 UNDO           = "Undo";
    private static String                 UNSELECT       = "Unselect";
    private static String                 VARS_KB        = "VARS Knowledge Base";
    private ArrayList<JMenuItem>          idMenuItems    = new ArrayList<JMenuItem>(UserPreferencesModel.MAX_NUM_IDS);
    private ArrayList<JMenuItem>          tagMenuItems   = new ArrayList<JMenuItem>(UserPreferencesModel.MAX_NUM_TAGS);
    private ArrayList<JMenuItem>          gotoMenuItems  = new ArrayList<JMenuItem>();
    private ArrayList<JMenuItem>          classMenuItems =
        new ArrayList<JMenuItem>(UserPreferencesModel.MAX_NUM_CLASS_NAMES);
    private int                           acceleratorMask;
    private JMenuItem                     classClearMenuItem;
    private JFrame                        classNameTextEntryFrame;
    private TextEntryPanel                classNameTextEntryPanel;
    private JMenuItem                     classNewCustomMenuItem;
    private JMenuItem                     classNewKbMenuItem;
    private JMenuItem                     classRepeatMenuItem;
    private JMenuItem                     combineMenuItem;
    public JFrame                         conceptTreeFrame;
    public ConceptTreePanel               conceptTreePanel;
    private JMenuItem                     deleteMenuItem;
    private GotoDialog                    gotoDialog;
    private JFrame                        gotoFrame;
    private JFrame                        idFrame;
    private TextEntryPanel                idTextEntryPanel;
    private Point                         lastInvokerPoint;
    private MenuItemListSelectionListener listSelectionListener;
    private ApplicationModel              mainModel;
    private JMenuItem                     menuItemClassMenuClear;
    private JMenuItem                     menuItemGoto;
    private JMenuItem                     menuItemIdClear;
    private JMenuItem                     menuItemIdMenuClear;
    private JMenuItem                     menuItemIdNew;
    private JMenuItem                     menuItemLabelAll;
    private JMenuItem                     menuItemRedo;
    private JMenuItem                     menuItemTagClear;
    private JMenuItem                     menuItemTagMenuClear;
    private JMenuItem                     menuItemTagNew;
    private JMenuItem                     menuItemUndo;
    private JMenu                         subMenuClass;
    private JMenu                         subMenuClassNew;
    private JMenu                         subMenuId;
    private JMenu                         subMenuTag;
    private JFrame                        tagFrame;
    private TextEntryPanel                tagTextEntryPanel;
    private JMenuItem                     unselectMenuItem;

    public EditMenu(ApplicationModel model) {
        this.mainModel = model;

        String s = System.getProperty("os.name").toLowerCase();

        if ((s.indexOf("linux") != -1) || (s.indexOf("windows") != -1)) {
            acceleratorMask = (InputEvent.CTRL_MASK | (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        } else if (s.indexOf("mac") != -1) {
            acceleratorMask = (InputEvent.META_MASK | (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
    }

    /**
     *    Checks if the text is a previously defined in the list
     *    @param text
     *    @return true if the text is a class name
     */
    private static boolean listContains(ArrayList<String> list, String text) {
        Iterator<String> i = list.iterator();

        while (i.hasNext()) {
            if (text.equals(i.next())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Dynamically creates a menu based on the number of selections
     * @param menu the top-level menu  to populate with edit <code>JMenuItem</code>
     * @return  the populated edit menu
     */
    public Component create(Component rootmenu) {
        return create(rootmenu, null, 0, 0);
    }

    /**
     * Dynamically creates a menu based on the number of selections
     * @param menu the top-level menu  to populate with edit <code>JMenuItem</code>
     * @return  the populated edit menu
     */
    public Component create(Component rootmenu, Component invoker, int x, int y) {
        if (invoker != null) {

            // Store the last position of the invoker
            if (invoker.isShowing()) {
                Point invokerOrigin = invoker.getLocationOnScreen();

                lastInvokerPoint = new Point(invokerOrigin.x + x, invokerOrigin.y + y);
            } else {
                Dimension screenSize = invoker.getSize();

                lastInvokerPoint = new Point(screenSize.width / 2, screenSize.height / 2);
            }
        }

        return build(rootmenu);
    }

    /**
     * Dynamically builds a menu based on the number of selections
     * @param menu the top-level menu  to populate with edit <code>JMenuItem</code>
     * @return  the populated edit menu
     */
    private Component build(Component rootmenu) {
        ListSelectionModel lsm = mainModel.getListSelectionModel();

        listSelectionListener = new MenuItemListSelectionListener();
        lsm.addListSelectionListener(listSelectionListener);

        MenuItemActionHandler menuItemHandler = new MenuItemActionHandler();

        initialize();

        /** **************Goto section/************** */
        menuItemGoto = new JMenu(GOTO);
        menuItemGoto.setEnabled(true);

        int size = EventAbstractTableModel.columnNames.length - 1;

        for (int i = 0; i < size; i++) {
            gotoMenuItems.add(new JMenuItem(EventAbstractTableModel.columnNames[i]));
            menuItemGoto.add(gotoMenuItems.get(i));
            gotoMenuItems.get(i).addActionListener(menuItemHandler);
        }

        // Add quick key to object ID goto only
        gotoMenuItems.get(0).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, acceleratorMask));
        addMenuItem(rootmenu, menuItemGoto);

        /** **************Undo/Redo section/************** */
        addSeparator(rootmenu);
        menuItemUndo = new JMenuItem(UNDO);
        menuItemUndo.setEnabled(false);
        menuItemUndo.addActionListener(menuItemHandler);
        menuItemUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, acceleratorMask));
        addMenuItem(rootmenu, menuItemUndo);
        menuItemRedo = new JMenuItem(REDO);
        menuItemRedo.setEnabled(false);
        menuItemRedo.addActionListener(menuItemHandler);
        menuItemRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, acceleratorMask));
        addMenuItem(rootmenu, menuItemRedo);
        addSeparator(rootmenu);

        /** **************Delete/Combine/Unselect section/************** */

        // Change the mainMenu settings, depending on the number of selected items
        deleteMenuItem = new JMenuItem(DELETE_EVENT);
        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, acceleratorMask));
        deleteMenuItem.addActionListener(menuItemHandler);
        addMenuItem(rootmenu, deleteMenuItem);
        combineMenuItem = new JMenuItem(COMBINE_EVENTS);
        combineMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, acceleratorMask));
        combineMenuItem.addActionListener(menuItemHandler);

        if (getNumSelected() > 1) {
            combineMenuItem.setEnabled(true);
        } else {
            combineMenuItem.setEnabled(false);
        }

        addMenuItem(rootmenu, combineMenuItem);
        unselectMenuItem = new JMenuItem(UNSELECT);
        unselectMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, acceleratorMask));
        unselectMenuItem.addActionListener(menuItemHandler);
        addMenuItem(rootmenu, unselectMenuItem);
        addSeparator(rootmenu);

        /** **************Class Menu************** */
        ArrayList<String> userClassNameList = UserPreferences.getModel().getClassNameList();

        subMenuClass = new JMenu(CLASS_MAINMENU);

        if (userClassNameList.size() > 0) {
            classRepeatMenuItem = new JMenuItem(ASSIGN + " class");
            classRepeatMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, acceleratorMask));
            classRepeatMenuItem.addActionListener(menuItemHandler);
            subMenuClass.add(classRepeatMenuItem);
        }

        subMenuClassNew = new JMenu(NEW + " class");
        subMenuClass.add(subMenuClassNew);
        classNewKbMenuItem = new JMenuItem(VARS_KB);
        classNewKbMenuItem.addActionListener(menuItemHandler);
        subMenuClass.add(classNewKbMenuItem);
        classNewCustomMenuItem = new JMenuItem(CLASS_NEW_CUSTOM);
        classNewCustomMenuItem.addActionListener(menuItemHandler);
        subMenuClassNew.add(classNewCustomMenuItem);
        classClearMenuItem = new JMenuItem(CLEAR);
        classClearMenuItem.addActionListener(menuItemHandler);
        subMenuClass.add(classClearMenuItem);

        if (userClassNameList.size() > 0) {
            subMenuClass.addSeparator();

            /**
             * Populate a submenu of tag events
             * with previously user-defined tags
             */
            Iterator<String> i = userClassNameList.iterator();

            while (i.hasNext()) {
                JMenuItem item = new JMenuItem(i.next());

                subMenuClass.add(item);
                classMenuItems.add(item);
                item.addActionListener(menuItemHandler);
            }

            subMenuClass.addSeparator();
            menuItemClassMenuClear = new JMenuItem(CLEAR_LIST);
            menuItemClassMenuClear.addActionListener(menuItemHandler);
            subMenuClass.add(menuItemClassMenuClear);
        }

        addMenu(rootmenu, subMenuClass);

        /** **************Tag Menu************** */
        ArrayList<String> userTagList = UserPreferences.getModel().getTagList();

        subMenuTag = new JMenu(TAG_MAINMENU);

        if (userTagList.size() > 0) {
            JMenuItem m = new JMenuItem(TAG_REPEAT);

            m.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, acceleratorMask));
            m.addActionListener(menuItemHandler);
            subMenuTag.add(m);
        }

        menuItemTagNew = new JMenuItem(NEW);
        menuItemTagNew.addActionListener(menuItemHandler);
        subMenuTag.add(menuItemTagNew);
        menuItemTagClear = new JMenuItem(CLEAR);
        menuItemTagClear.addActionListener(menuItemHandler);
        subMenuTag.add(menuItemTagClear);

        if (userTagList.size() > 0) {
            subMenuTag.addSeparator();

            /**
             * Populate a submenu of tag events
             * with previously user-defined tags
             */
            Iterator<String> i = userTagList.iterator();

            while (i.hasNext()) {
                JMenuItem item = new JMenuItem(i.next());

                subMenuTag.add(item);
                tagMenuItems.add(item);
                item.addActionListener(menuItemHandler);
            }

            subMenuTag.addSeparator();
            menuItemTagMenuClear = new JMenuItem(CLEAR_LIST);
            menuItemTagMenuClear.addActionListener(menuItemHandler);
            subMenuTag.add(menuItemTagMenuClear);
        }

        addMenu(rootmenu, subMenuTag);

        /** **************Identity Reference************** */
        ArrayList<String> userIdList = UserPreferences.getModel().getIdList();

        subMenuId = new JMenu(ID_MAINMENU);

        if (userIdList.size() > 0) {
            JMenuItem menuItem = new JMenuItem(ID_REPEAT);

            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, acceleratorMask));
            menuItem.addActionListener(menuItemHandler);
            subMenuId.add(menuItem);
        }

        menuItemIdNew = new JMenuItem(NEW);
        menuItemIdNew.addActionListener(menuItemHandler);
        subMenuId.add(menuItemIdNew);
        menuItemIdClear = new JMenuItem(CLEAR);
        menuItemIdClear.addActionListener(menuItemHandler);
        subMenuId.add(menuItemIdClear);

        if (userIdList.size() > 0) {
            subMenuId.addSeparator();

            /**
             * Populate a submenu of tag events
             * with previously user-defined tags
             */
            Iterator<String> i = userIdList.iterator();

            while (i.hasNext()) {
                JMenuItem item = new JMenuItem(i.next());

                subMenuId.add(item);
                idMenuItems.add(item);
                item.addActionListener(menuItemHandler);
            }

            subMenuId.addSeparator();
            menuItemIdMenuClear = new JMenuItem(CLEAR_LIST);
            menuItemIdMenuClear.addActionListener(menuItemHandler);
            subMenuId.add(menuItemIdMenuClear);
        }

        addMenu(rootmenu, subMenuId);

        /** **************Label Event MenuItem************** */
        menuItemLabelAll = new JMenuItem(LABEL_EVENT);
        menuItemLabelAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, acceleratorMask));
        menuItemLabelAll.addActionListener(menuItemHandler);
        addMenuItem(rootmenu, menuItemLabelAll);
        listSelectionListener.valueChanged(null);

        // Enable the undo/redo options according to the state of the last action
        // and the history size
        CommandHistory history = CommandHistory.getInstance();
        UndoRedoState  state   = history.getState();

        if (((history.getSize() > 0) && (state == UndoRedoState.INIT_STATE)) || (state == UndoRedoState.UNDO_STATE)
                || (state == UndoRedoState.UNDO_REDO_STATE)) {
            menuItemUndo.setEnabled(true);
        }

        // Enable the undo/redo options according to the state of the last action
        // and the history size
        if ((state == UndoRedoState.REDO_STATE) || (state == UndoRedoState.UNDO_REDO_STATE)) {
            menuItemRedo.setEnabled(true);
        }

        return rootmenu;
    }

    /**
     * Helper function to add a <code>JMenu</code> to either a <code>PopupMenu</code>
     * or a <code>JMenu</code>.
     * @param component a JPopupMenu or JMenu
     * @param item the <code>JMenu</code> item to add to the component
     */
    private void addMenu(Component component, JMenu item) {

        // Type-cast the top-level menu, depending on whether its a Popup or not
        if (component instanceof JPopupMenu) {
            ((JPopupMenu) (component)).add(item);
        } else if (component instanceof JMenu) {
            ((JMenu) component).add(item);
        }
    }

    /**
     * Helper function to add a <code>JMenuItem</code> to either a <code>PopupMenu</code>
     * or a <code>JMenu</code>.
     * @param component a JPopupMenu or JMenu
     * @param item the <code>JMenu</code> item to add to the component
     */
    private void addMenuItem(Component component, JMenuItem item) {

        // Type-cast the top-level menu, depending on whether its a Popup or not
        if (component instanceof JPopupMenu) {
            ((JPopupMenu) (component)).add(item);
        } else if (component instanceof JMenu) {
            ((JMenu) component).add(item);
        }
    }

    private void addSeparator(Component componen) {

        // Type-cast the top-level menu, depending on whether its a Popup or not
        if (componen instanceof JPopupMenu) {
            ((JPopupMenu) (componen)).addSeparator();
        } else if (componen instanceof JMenu) {
            ((JMenu) componen).addSeparator();
        }
    }

    /** Returns the number of selected items */
    private int getNumSelected() {
        ListSelectionModel lsm = mainModel.getListSelectionModel();

        // Get number of selections
        int iMin = lsm.getMinSelectionIndex();
        int iMax = lsm.getMaxSelectionIndex();

        if ((iMin == -1) || (iMax == -1)) {
            return 0;
        }

        int numSelections = 0;

        for (int i = iMin; i <= iMax; i++) {
            if (lsm.isSelectedIndex(i)) {
                numSelections++;
            }
        }

        return numSelections;
    }

    /**
     * Displays the goto dialog at a given point location
     * @param point point to display the dialog
     */
    private void displayGoto(String field, Point point) {
        gotoFrame = new JFrame(GOTO + field);

        try {
            gotoDialog = new GotoDialog(gotoFrame, point);
        } catch (Exception ex) {
            Logger.getLogger(EditMenu.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Displays the VARS Knowledge Base concept tree at a given point location
     * and attaches a listener to the tree
     * @param listener the listener to attach to the concept tree
     * @param point the point to display the tree
     */
    private void displayConceptTree(MouseListener listener, Point point) {
        if (conceptTreePanel != null) {
            MouseListener[] listeners = conceptTreePanel.getMouseListeners();

            for (int i = 0; i < listeners.length; i++) {
                conceptTreePanel.removeListener(listeners[i]);
            }

            conceptTreePanel.replaceListener(listener);
        } else {

            // TODO: put check for knowledge base existence
            conceptTreePanel = new ConceptTreePanel(listener);

            // Only build the conceptTreePanel when the window is opened
            conceptTreePanel.buildPanel();
            conceptTreeFrame = new JFrame(ApplicationInfo.getName() + " - " + VARS_KB + " Lookup");
            conceptTreeFrame.setContentPane(conceptTreePanel);
            conceptTreeFrame.setFocusable(true);
        }

        if (point != null) {
            conceptTreeFrame.setLocation(point);
        }

        conceptTreeFrame.setSize(400, 600);
        conceptTreeFrame.setVisible(true);
        conceptTreePanel.setVisible(true);
        conceptTreePanel.addMouseListener(listener);
    }

    /**
     * Initializes the components needed to activate when mainMenu options are selected
     */
    private void initialize() {
        try {
            ComboBoxActionHandler handler = new ComboBoxActionHandler();

            // Create and set up the window.
            tagFrame = new JFrame(ApplicationInfo.getName() + " - " + "Tag Entry");

            // Create and set up the content pane.
            ArrayList<String> userTagList = UserPreferences.getModel().getTagList();

            tagTextEntryPanel = new TextEntryPanel(handler, "tag", userTagList, "");
            tagTextEntryPanel.setOpaque(true);    // content panes must be opaque
            tagFrame.setContentPane(tagTextEntryPanel);
            tagFrame.setVisible(false);
            tagFrame.pack();

            // Create and set up the window.
            idFrame = new JFrame(ApplicationInfo.getName() + " - " + " Entry");

            // Create and set up the content pane.
            ArrayList<String> userIdList = UserPreferences.getModel().getIdList();

            idTextEntryPanel = new TextEntryPanel(handler, "identity reference", userIdList, "");
            idTextEntryPanel.setOpaque(true);    // content panes must be opaque
            idFrame.setContentPane(idTextEntryPanel);
            idFrame.setVisible(false);
            idFrame.pack();

            // Create and set up the window.
            classNameTextEntryFrame = new JFrame(ApplicationInfo.getName() + " - " + "Class Name Entry");

            // Create and set up the content pane.
            ArrayList<String> classNameList = UserPreferences.getModel().getClassNameList();

            classNameTextEntryPanel = new TextEntryPanel(handler, "class name", classNameList, "");
            classNameTextEntryPanel.setOpaque(true);
            classNameTextEntryFrame.setContentPane(classNameTextEntryPanel);
            classNameTextEntryFrame.setVisible(false);
            classNameTextEntryFrame.pack();
        } catch (Exception ex) {
            Logger.getLogger(EditMenu.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Opens the last selected event in an EventPlayer
     *
     * This is called from either the <code>actionPerformed()</code> or
     * <code>keyPressed()</code> handlers
     */
    private void openEvent() {

        // Translated the row index into the real mainModel index
        // through the sorter, since the table may be sorted
        TableSorter        sorter = mainModel.getSorter();
        ListSelectionModel lsm    = mainModel.getListSelectionModel();
        EventListModel     model  = mainModel.getEventListModel();

        if (sorter != null) {
            int                  index = sorter.modelIndex(lsm.getMinSelectionIndex());
            EventObjectContainer event = model.getElementAt(index);

            PlayerManager.getInstance().openView(event, mainModel, true);
        }
    }

    public void keyReleased(KeyEvent e) {}

    /**
     * Redoes the last command
     */
    private void redo() {
        Application.getView().setBusyCursor();

        // Redo the last command
        CommandHistory history = CommandHistory.getInstance();

        try {
            AbstractCommand command = history.getPrevCommand();

            command.execute();
        } catch (NoSuchElementException exception) {}

        Application.getView().setDefaultCursor();
    }

    /**
     * Undoes the last command
     */
    private void undo() {
        Application.getView().setBusyCursor();

        // Undo the last command
        CommandHistory history = CommandHistory.getInstance();

        try {
            AbstractCommand command = history.getNextCommand();

            command.unexecute();
        } catch (NoSuchElementException exception) {}

        Application.getView().setDefaultCursor();
    }

    /**
     * Implements actions when combox items in each TextEntryPanel are selected
     */
    class ComboBoxActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ListSelectionModel lsm = mainModel.getListSelectionModel();

            // If this was an action on the TextField entry
            // get the text, initiate the action and then hide the display
            if (!lsm.isSelectionEmpty()) {
                JComboBox source = (JComboBox) (e.getSource());

                if ((tagTextEntryPanel != null) && source.equals(tagTextEntryPanel.getComboBox())) {
                    String tagname = tagTextEntryPanel.getText();

                    Execute.run(new TagCommand(tagname));
                    tagFrame.setVisible(false);
                } else if ((classNameTextEntryPanel != null) && source.equals(classNameTextEntryPanel.getComboBox())) {
                    String className = classNameTextEntryPanel.getText();

                    Execute.run(new BiologicalClassificationCommand(className));
                    classNameTextEntryFrame.setVisible(false);
                } else if ((idTextEntryPanel != null) && source.equals(idTextEntryPanel.getComboBox())) {
                    String id = idTextEntryPanel.getText();

                    Execute.run(new IdCommand(id));
                    idFrame.setVisible(false);
                }
            }
        }
    }


    /**
     * Implements actions when menu items are selected
     */
    class MenuItemActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JMenuItem source = (JMenuItem) (e.getSource());

            if (gotoMenuItems.contains(source)) {
                displayGoto(source.getText(), lastInvokerPoint);

                if (gotoDialog.answer() == true) {
                    Object value = gotoDialog.getValue();

                    /**
                     * This searches the table, instead of the EventListModel
                     * to work around the table view to model translation
                     * when the list is sorted.
                     */
                    int            row  = Application.getController().getTableController().search(source.getText(),
                                              value);
                    EventListModel elsm = mainModel.getEventListModel();

                    elsm.setValue(row);
                }
            } else if (source.equals(menuItemUndo)) {
                undo();
            } else if (source.equals(menuItemRedo)) {
                redo();
            } else if (source.equals(unselectMenuItem)) {
                ListSelectionModel lsm = mainModel.getListSelectionModel();

                lsm.clearSelection();
            } else if (source.getText().equals(OPEN_EVENT)) {
                openEvent();
            } else if (source.equals(deleteMenuItem)) {
                UserPreferencesModel prefs = UserPreferences.getModel();

                if (prefs.getAskBeforeDelete() == true) {
                    String question = new String("Are you sure you want to delete" + Execute.getObjectIdDescription()
                                                 + " ?");
                    ModalYesNoNeverDialog dialog;

                    try {
                        dialog = new ModalYesNoNeverDialog(Application.getView(), question);
                        dialog.setVisible(true);

                        if (dialog.isNever() == true) {
                            prefs.setAskBeforeDelete(false);
                        }

                        if (dialog.answer() == true) {
                            Execute.run(new DeleteCommand());

                            ListSelectionModel lsm = mainModel.getListSelectionModel();

                            lsm.clearSelection();
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(EditMenu.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    Execute.run(new DeleteCommand());

                    ListSelectionModel lsm = mainModel.getListSelectionModel();

                    lsm.clearSelection();
                }
            } else if (source.equals(combineMenuItem)) {
                Execute.run(new CombineCommand());
            } else if (source.equals(classRepeatMenuItem)) {
                String lastClassName = UserPreferences.getModel().getLastUsedClassName();

                Execute.run(new BiologicalClassificationCommand(lastClassName));
            } else if (source.equals(classNewCustomMenuItem)) {
                classNameTextEntryFrame.setLocation(lastInvokerPoint);
                classNameTextEntryFrame.setVisible(true);
            } else if (source.equals(classNewKbMenuItem)) {
                MouseAdapter adapter = new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if ((conceptTreePanel != null) && (e.getClickCount() == 2)) {
                            String conceptName = conceptTreePanel.getSelectedConceptName();

                            if (conceptName.length() > 0) {
                                Execute.run(new BiologicalClassificationCommand(conceptName));
                                conceptTreeFrame.dispose();
                            }
                        }
                    }
                };

                displayConceptTree(adapter, lastInvokerPoint);
            } else if (source.equals(classClearMenuItem)) {
                Execute.run(new BiologicalClassificationCommand(""));
            } else if (source.equals(menuItemIdMenuClear)) {
                UserPreferences.getModel().clearIdList();
            } else if (source.equals(menuItemClassMenuClear)) {
                UserPreferences.getModel().clearClassList();
            } else if (source.equals(menuItemTagMenuClear)) {
                UserPreferences.getModel().clearTagList();
            } else if (source.equals(menuItemTagNew)) {
                tagFrame.setLocation(lastInvokerPoint);
                tagFrame.setVisible(true);
            } else if (source.equals(menuItemTagClear)) {
                Execute.run(new TagCommand(""));
            } else if (source.getText().equals(TAG_REPEAT)) {
                String tag = UserPreferences.getModel().getLastUsedTag();

                Execute.run(new TagCommand(tag));
            } else if (source.equals(menuItemIdNew)) {
                idFrame.setLocation(lastInvokerPoint);
                idFrame.setVisible(true);
            } else if (source.equals(menuItemIdClear)) {
                Execute.run(new IdCommand(""));
            } else if (source.getText().equals(ID_REPEAT)) {
                String id = UserPreferences.getModel().getLastUsedId();

                Execute.run(new IdCommand(id));
            } else if (tagMenuItems.contains(source)) {
                String tag = source.getText();

                Execute.run(new TagCommand(tag));
            } else if (idMenuItems.contains(source)) {
                String id = source.getText();

                Execute.run(new IdCommand(id));
            } else if (classMenuItems.contains(source)) {
                String className = source.getText();

                Execute.run(new BiologicalClassificationCommand(className));
            } else if (source.equals(menuItemLabelAll)) {
                EventLabelerController classLabelController = new EventLabelerController(mainModel.getEventListModel());

                classLabelController.getView().setVisible(true);
            } else {
                String s = "Item event detected." + "    Event source: " + source.getText();

                System.out.println(s);
            }
        }
    }


    class MenuItemListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel lsm = mainModel.getListSelectionModel();

            if (lsm.isSelectionEmpty()) {
                deleteMenuItem.setEnabled(false);
                combineMenuItem.setEnabled(false);
                unselectMenuItem.setEnabled(false);
                menuItemLabelAll.setEnabled(false);

                int cnt = subMenuTag.getItemCount();

                for (int j = 0; j < cnt; j++) {
                    if (subMenuTag.getItem(j) != null) {
                        subMenuTag.getItem(j).setEnabled(false);
                    }
                }

                cnt = subMenuClass.getItemCount();

                for (int j = 0; j < cnt; j++) {
                    if (subMenuClass.getItem(j) != null) {
                        subMenuClass.getItem(j).setEnabled(false);
                    }
                }

                cnt = subMenuId.getItemCount();

                for (int j = 0; j < cnt; j++) {
                    if (subMenuId.getItem(j) != null) {
                        subMenuId.getItem(j).setEnabled(false);
                    }
                }
            } else {
                menuItemLabelAll.setEnabled(true);
                deleteMenuItem.setEnabled(true);

                if (getNumSelected() > 1) {
                    combineMenuItem.setEnabled(true);
                } else {
                    combineMenuItem.setEnabled(false);
                }

                unselectMenuItem.setEnabled(true);

                int cnt = subMenuTag.getItemCount();

                for (int j = 0; j < cnt; j++) {
                    if (subMenuTag.getItem(j) != null) {
                        subMenuTag.getItem(j).setEnabled(true);
                    }
                }

                cnt = subMenuClass.getItemCount();

                for (int j = 0; j < cnt; j++) {
                    if (subMenuClass.getItem(j) != null) {
                        subMenuClass.getItem(j).setEnabled(true);
                    }
                }

                cnt = subMenuId.getItemCount();

                for (int j = 0; j < cnt; j++) {
                    if (subMenuId.getItem(j) != null) {
                        subMenuId.getItem(j).setEnabled(true);
                    }
                }

                ArrayList<Integer> selections            = Execute.getTranslatedRows();
                EventListModel     model                 = mainModel.getEventListModel(); 
                boolean            hasClassName          = false;
                boolean            hasTag                = false;
                boolean            hasId                 = false;

                if (selections.size() > 0) {
                    Iterator<Integer> i = selections.iterator();
 

                    while (i.hasNext()) {
                        EventObjectContainer obj = model.getElementAt(i.next().intValue());

                        if (obj != null) {
                            String classname = obj.getClassName();

                            if ((classname != null) && (classname.length() > 0)) {
                                hasClassName = true;

                                break;
                            }
                        }
                    }

                    i = selections.iterator();

                    while (i.hasNext()) {
                        EventObjectContainer obj = model.getElementAt(i.next().intValue());

                        if (obj != null) {
                            String id = obj.getIdentityReference();

                            if ((id != null) && (id.length() > 0)) {
                                hasId = true;

                                break;
                            }
                        }
                    }

                    i = selections.iterator();

                    while (i.hasNext()) {
                        EventObjectContainer obj = model.getElementAt(i.next().intValue());

                        if (obj != null) {
                            String tag = obj.getTag();

                            if ((tag != null) && (tag.length() > 0)) {
                                hasTag = true;

                                break;
                            }
                        }
                    }
                }

                if (hasClassName) {
                    classClearMenuItem.setEnabled(true);
                } else {
                    classClearMenuItem.setEnabled(false);
                }

                if (hasId) {
                    menuItemIdClear.setEnabled(true);
                } else {
                    menuItemIdClear.setEnabled(false);
                }

                if (hasTag) {
                    menuItemTagClear.setEnabled(true);
                } else {
                    menuItemTagClear.setEnabled(false);
                }
            }

            // Enable the undo/redo options according to the state of the last action
            // and the history size
            CommandHistory history = CommandHistory.getInstance();
            UndoRedoState  state   = history.getState();

            if (((history.getSize() > 0) && (state == UndoRedoState.INIT_STATE)) || (state == UndoRedoState.UNDO_STATE)
                    || (state == UndoRedoState.UNDO_REDO_STATE)) {
                menuItemUndo.setEnabled(true);
            }

            // Enable the undo/redo options according to the state of the last action
            // and the history size
            if ((state == UndoRedoState.REDO_STATE) || (state == UndoRedoState.UNDO_REDO_STATE)) {
                menuItemRedo.setEnabled(true);
            }
        }
    }
}
