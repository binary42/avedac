/*
 * @(#)BatchProcessController.java
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

import com.jgoodies.binding.list.ArrayListModel;
 
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.ExcelExporter;
import org.mbari.aved.ui.utils.ParseUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JTable;
import org.mbari.aved.ui.model.TableSorter;

/**
 *
 * @author dcline
 */
public class BatchProcessController extends AbstractController
        implements ModelListener, PropertyChangeListener {
    private EventXmlDirectoryModel                 xmlDirModel       = new EventXmlDirectoryModel(); 
    private File                                   outputDir;
    private RunClassifierBatch                     runClassifierBatch;
    private BatchProcessAbstractTableModel         abstractTableModel = new BatchProcessAbstractTableModel();
    private final TableSorter                      sorter;

    BatchProcessController(ClassifierModel model) {
        setModel(model);
        setView(new BatchProcessView(model, this)); 

        // Register as listener to the models
        getModel().addModelListener(this);
        xmlDirModel.addPropertyChangeListener(this);
        getView().addMouseClickListener(new MouseClickJListActionHandler());
        runClassifierBatch = new RunClassifierBatch(model, this);
        getView().replaceClassifierRunPanel(runClassifierBatch.getView().getForm());

        File importDir = UserPreferences.getModel().getXmlImportDirectory();

        xmlDirModel.setDirectory(importDir);

        File exportDir = UserPreferences.getModel().getXmlExportDirectory();

        getView().setExportDirectory(exportDir);
        outputDir = exportDir;         
    
        // Create the sorter and intialize it in the model
        sorter = new TableSorter(abstractTableModel); 

        // getModel().initializeSorter(sorter);
        // Initialize the table headers, so the sorter knows what to sort on
        sorter.setTableHeader(getView().getSelectedTable().getTableHeader());

        // Set the model
        getView().getSelectedTable().setModel(sorter); 
                             
    }

    @Override
    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    @Override
    public BatchProcessView getView() {
        return (BatchProcessView) super.getView();
    }

    /**
     * Operation handler for handling actions initiated in the view
     *
     * @param actionCommand A semantic event which indicates that a
     * component-defined action occurred.
     */
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("BrowseImportDir")) {
            try {
                browseForEventsDir();
            } catch (Exception ex) {
                Logger.getLogger(BatchProcessController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (actionCommand.equals("BrowseExportDir")) {
            try {
                browseForExportDir();
            } catch (Exception ex) {
                Logger.getLogger(BatchProcessController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (actionCommand.equals("<<")) {
            removeItemFromSelected();
        } else if (actionCommand.equals(">>")) {
            copyItemToSelected();
        } else if (actionCommand.equals("SelectAllAvailable")) {
            int size = getView().getAvailableList().getModel().getSize();

            getView().getAvailableList().setSelectionInterval(0, size - 1);
        } else if (actionCommand.equals("ResetAllSelected")) {
            clearSelectedTable();
        } else if (actionCommand.equals("ClearAllAvailable")) {
            getView().getAvailableList().clearSelection();
        } else if (actionCommand.equals("RemoveAllSelected")) {
            JTable t    = getView().getSelectedTable();
            int    size = t.getRowCount();

            t.getSelectionModel().setSelectionInterval(0, size - 1);
            removeItemFromSelected();
        } else if (actionCommand.equals("ExportExcel")) {
            exportTableAsXls();
        }
    }

    /**
     * Export the results in simple Excel format.
     * This will prompt the user first to browse for
     * a suitable file
     */
    void exportTableAsXls() {
        JTable table = getView().getSelectedTable();

        if (table.getRowCount() > 0) {
            BatchProcessModel batch = (BatchProcessModel) table.getValueAt(0, -1);
            File                  dir   = UserPreferences.getModel().getExportedExcelDirectory();
            File                  xml   = batch.getFile();
            File                  exportFile;

            if (xml.getParent() != null) {
                File tmp = new File(dir + "/" + xml.getParent() + "_class_summary.xls");

                exportFile = ExcelExporter.browseForXlsExport(tmp, getView());
            } else {
                File tmp = new File(dir + "/" + ParseUtils.removeFileExtension(xml.getName()) + "_class_summary.xls");

                exportFile = ExcelExporter.browseForXlsExport(tmp, getView());
            }

            try {
                ExcelExporter.exportTable(table, exportFile);
            } catch (IOException ex) {
                Logger.getLogger(BatchProcessController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /** Browse for event import directory */
    private void browseForEventsDir() throws Exception {
        File setCurrentDirectory = new File(UserPreferences.getModel().getImportVideoDir().toString());
        File f;

        // Add a custom file filter and disable the default
        JFileChooser chooser = new JFileChooser();

        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose Event Directory");
        chooser.setCurrentDirectory(setCurrentDirectory);

        if (chooser.showOpenDialog((BatchProcessView) getView()) == JFileChooser.APPROVE_OPTION) {
            f = chooser.getSelectedFile();
            UserPreferences.getModel().setImportXmlDirectory(f);
            getView().setBusyCursor();
            xmlDirModel.setDirectory(f);
        } else {

            // TODO: print dialog message box with something meaningful here
            System.out.println("No Selection ");
        }
    }

    /** Browse for directory to save output to */
    private void browseForExportDir() throws Exception {
        File setCurrentDirectory = new File(UserPreferences.getModel().getImportVideoDir().toString());
        File f;

        // Add a custom file filter and disable the default
        JFileChooser chooser = new JFileChooser();

        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose Output Directory");
        chooser.setCurrentDirectory(setCurrentDirectory);

        if (chooser.showOpenDialog((BatchProcessView) getView()) == JFileChooser.APPROVE_OPTION) {
            f = chooser.getSelectedFile();
            UserPreferences.getModel().setExportXmlDirectory(f);
            getView().setBusyCursor();
            getView().setExportDirectory(f);
            outputDir = f;
        } else {

            // TODO: print dialog message box with something meaningful here
            System.out.println("No Selection ");
        }
    }

    @Override
    public void modelChanged(ModelEvent event) {
        /**
         * When the classifier training library changes, update the columns names in the table 
         */
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {
                case ClassifierModel.ClassifierModelEvent.TRAINING_MODEL_SELECTION_CHANGE:
                    TrainingModel model = runClassifierBatch.getController().getTrainingModel();
                    abstractTableModel.changeClassColumns(model);
                    getView().repaint();
            }
        }
    }

    /**
     * Handles property changes on the directory browser
     *
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("fileList")) {
                 
            abstractTableModel.clear();  
            abstractTableModel.fireTableDataChanged();
            
            List<File>     l         = xmlDirModel.getFileList();
            ArrayListModel listModel = new ArrayListModel();

            listModel.addAll(l);
            getView().populateAvailableClassList(listModel);
            getView().setDefaultCursor();
        } else if (evt.getPropertyName().equals("directory")) {
            getView().setImportDirectory(xmlDirModel.getDirectory());
        }
    }

    /**
     * Keeps the contents of the table but clears the results
     */
    private void clearSelectedTable() { 
        abstractTableModel.clearResults(); 
    }

    /**
     * Copies an item from available to the selected list
     */
    private void copyItemToSelected() {
        if (!getView().getAvailableList().isSelectionEmpty()) {
  
            // getClassTotal user selected items and indices
            Object item[] = getView().getAvailableList().getSelectedValues();

            // add to the model
            for (int i = 0; i < item.length; i++) {
                File f = (File) item[i];
                abstractTableModel.add(new BatchProcessModel(f));
            } 
            
            abstractTableModel.fireTableDataChanged(); 

        }
    }

    /**
     * Remove selected items from selected class JList
     */
    private void removeItemFromSelected() {
        if (getView().getSelectedTable().getSelectedRowCount() > 0) { 
  
            // getClassTotal user selected items and indices
            int item[] = getView().getSelectedTable().getSelectedRows(); 
                    
            ArrayList<Integer>             indexes = new ArrayList<Integer>(); 
                    
            for (int i = 0; i < item.length; i++) {
                indexes.add(item[i]);
            }

            // remove from the model   
            abstractTableModel.remove(indexes);    
        }
    }

    /**
     * Manages mouse clicks on the available and batch process
     * JLists
     * <p>
     * A double mouse-click either adds or removes a selection,
     * depending on what JList it originates from
     * <p>
     */
    void actionClickList(MouseEvent e) {
        JList list = (JList) e.getSource();

        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            if (e.getClickCount() == 2) {

                // On a double click, add the event
                if (list.equals(getView().getAvailableList())) {
                    copyItemToSelected();
                } else {
                    removeItemFromSelected();
                }
            } else {

                // On a single click do nothing
            }
        }
    }

    File getOutputDir() {
        return outputDir;
    }

    /**
     * Helper function to return the abstract table model
     * @return 
     */
    BatchProcessAbstractTableModel getAbstractModel() {
        return this.abstractTableModel;
    }
 

    /**
     * Subclass to handles mouse clicks in
     * {@link org.mbari.aved.ui.classifier.CreateTrainingLibraryView}
     */
    class MouseClickJListActionHandler implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            actionClickList(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            actionClickList(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            actionClickList(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            actionClickList(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            actionClickList(e);
        }
    }


    private class RunClassifierBatch {
        private final RunBatchController controller;

        public RunClassifierBatch(ClassifierModel model, BatchProcessController c) {
            controller = new RunBatchController(model, c);
        }

        public RunView getView() {
            return controller.getView();
        }

        public RunBatchController getController() {
            return controller;
        } 
    }
}
