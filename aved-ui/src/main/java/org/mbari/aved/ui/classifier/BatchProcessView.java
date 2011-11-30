/*
 * @(#)BatchProcessView.java
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

import com.jeta.forms.components.panel.FormPanel;
import com.jeta.forms.gui.form.FormAccessor;

import com.jgoodies.binding.list.ArrayListModel;
 
import org.mbari.aved.ui.appframework.JFrameView; 
import org.mbari.aved.ui.appframework.ModelEvent; 

//~--- JDK imports ------------------------------------------------------------

import java.awt.Cursor;

import java.io.File;
 

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.mbari.aved.ui.classifier.BatchProcessController.MouseClickJListActionHandler;

/**
 *
 * @author dcline
 */
public class BatchProcessView extends JFrameView {

    /*
     *     Component names in the CreateClassifierTraining form
     *    If any of the component name are changed in the Abeille form designer, they
     *    should be modified here too
     */
    private static final String ID_ADD_BUTTON                  = "add";                   // javax.swing.JButton
    private static final String ID_AVAILABLE_EVENTS_JLIST      = "available";             // javax.swing.JLIST
    private static final String ID_BROWSE_EXPORT_BUTTON        = "browseExportDir";       // javax.swing.JButton
    private static final String ID_BROWSE_IMPORT_BUTTON        = "browseImportDir";       // javax.swing.JButton
    private static final String ID_CLEAR_ALL_AVAILABLE_BUTTON  = "clearAllAvailable";     // javax.swing.JButton
    private static final String ID_EXPORT_DIRECTORY_JLIST      = "exportDirectory";    // javax.swing.JFormattedTextField
    private static final String ID_EXPORT_EXCEL_BUTTON         = "exportXls";             // javax.swing.JButton
    private static final String ID_IMPORT_DIRECTORY_JLIST      = "importDirectory";    // javax.swing.JFormattedTextField
    private static final String ID_REMOVE_ALL_SELECTED_BUTTON  = "removeAllSelected";     // javax.swing.JButton
    private static final String ID_RESET_ALL_SELECTED_BUTTON   = "resetAllSelected";     // javax.swing.JButton
    private static final String ID_REMOVE_BUTTON               = "remove";                // javax.swing.JButton
    private static final String ID_SELECT_ALL_AVAILABLE_BUTTON = "selectAllAvailable";    // javax.swing.JButton
    private static final String ID_SELECT_TABLE                = "selectedTable";         // javax.swing.JTable
    public final static Cursor  defaultCursor                  = Cursor.getDefaultCursor();

    /** Busy and wait cursor */
    public final static Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    private final JList        availableList;
    private final JTextField   importDirectoryTextField, exportDirectoryTextField;
    private final JTable       selectedTable; 

    public BatchProcessView(ClassifierModel model, BatchProcessController controller) {

        // Constructor
        super("org/mbari/aved/ui/forms/ClassifierBatchProcess.xml", model, controller);
        setFocusable(true);
        availableList            = getForm().getList(ID_AVAILABLE_EVENTS_JLIST);
        importDirectoryTextField = getForm().getTextField(ID_IMPORT_DIRECTORY_JLIST);
        exportDirectoryTextField = getForm().getTextField(ID_EXPORT_DIRECTORY_JLIST);
        selectedTable            = getForm().getTable(ID_SELECT_TABLE);

        ActionHandler actionHandler = getActionHandler();

        getForm().getButton(ID_SELECT_ALL_AVAILABLE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_RESET_ALL_SELECTED_BUTTON).addActionListener(actionHandler); 
        getForm().getButton(ID_CLEAR_ALL_AVAILABLE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_REMOVE_ALL_SELECTED_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_ADD_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_REMOVE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_BROWSE_IMPORT_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_BROWSE_EXPORT_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_EXPORT_EXCEL_BUTTON).addActionListener(actionHandler); 
    }

    /** Sets the busy cursor for this view. Use this to disable the user mouse during long operations */
    public void setBusyCursor() {
        requestFocusInWindow();
        setCursor(busyCursor);
    }

    /** Sets the default cursor for this view. Use this when a long operation is done */
    public void setDefaultCursor() {
        requestFocusInWindow();
        setCursor(defaultCursor);
    }

    public void modelChanged(ModelEvent event) {}

    /**
     * Populates the available events XML files
     *
     * @param dir
     */
    void populateAvailableClassList(ArrayListModel listModel) {
        availableList.removeAll();
        availableList.setModel(listModel);
    }

    void setExportDirectory(File directory) {
        exportDirectoryTextField.setText(directory.toString());
    }

    void setImportDirectory(File directory) {
        importDirectoryTextField.setText(directory.toString());
    }

    JList getAvailableList() {
        return availableList;
    }

    /**
     * Adds a @{link org.mbari.aved.ui.classifier.BatchProcessController.MouseClickJListActionHandler}
     * to the contained lists
     *
     * @param mouseClickJListActionHandler
     */
    void addMouseClickListener(MouseClickJListActionHandler mouseClickJListActionHandler) {
        availableList.addMouseListener(mouseClickJListActionHandler);
    }

    public void replaceClassifierRunPanel(FormPanel panel) {
        FormAccessor accessor = getForm().getFormAccessor("main");

        accessor.replaceBean("classifierRun", panel);
    }

    JTable getSelectedTable() {
        return selectedTable;
    }
 
}
