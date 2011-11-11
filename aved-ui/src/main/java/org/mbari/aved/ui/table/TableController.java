/*
 * @(#)TableController.java
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



package org.mbari.aved.ui.table;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.EventPopupMenu;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.model.EventAbstractTableModel;
import org.mbari.aved.ui.model.EventAbstractTableModel.CustomTableModel;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.TableSorter;
import org.mbari.aved.ui.player.PlayerManager;
import org.mbari.aved.ui.utils.ImageUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel; 
import org.mbari.aved.ui.EventImagePopupMenu;

public class TableController extends AbstractController implements ModelListener {

    /** True when a popup Menu window is displayed */
    private Boolean hasPopupMenu = false;

    /** Customized table for displaying AVED data */
    private final EventTable eventTable;

    /** The popup */
    private final EventPopupMenu popupMenu;

    /** Defines the table model for displaying event data in a table */
    private final EventAbstractTableModel tablemodel;

    public TableController(ApplicationModel model) throws Exception {

        // Listen to changes in the model
        setModel(model);
        model.addModelListener(this);

        // Creates the custom event JTable to customize how the table is rendered
        eventTable = new EventTable();

        // Initialize the mouse action handler for the actual table
        eventTable.addMouseListener(new MouseClickTableActionHandler());

        // Get the row selection model and register this as a selection
        // listener so we can translated from the sorted table to the
        // real model table.
        eventTable.setSelectionModel(model.getListSelectionModel());
        tablemodel = new EventAbstractTableModel(model.getEventListModel());

        // Create the sorter and intialize it in the model
        TableSorter sorter = new TableSorter(tablemodel.getTableModel());

        getModel().initializeSorter(sorter);

        // Initialize the table headers, so the sorter knows what to sort on
        sorter.setTableHeader(eventTable.getTableHeader());

        // Set the model in the event table
        eventTable.setModel(sorter);

        // Create the modified JTable for the sorted data
        TableView view = new TableView(model, eventTable, this);

        // Set the view for this controller
        setView(view);

        // Create the popup for this table
        popupMenu = new EventPopupMenu(getModel());
    }

    /** Helper function that returns the table */
    public JTable getTable() {
        return eventTable;
    }

    /** Helper function that returns TableView associated with this <code>TableController</code> */
    public TableView getView() {
        return ((TableView) super.getView());
    }

    /** Helper function that returns type cast ApplicationModel */
    public org.mbari.aved.ui.ApplicationModel getModel() {
        return ((ApplicationModel) super.getModel());
    }

    void actionClickTable(MouseEvent e) {
        ApplicationModel model = getModel();

        JTable table = (JTable) e.getSource();

        // Get the event in the table
        Point pt = e.getPoint();
        
        int row = eventTable.rowAtPoint(pt);
        
        // Translated the row index into the real model index
        // through the sorter, since the table may be sorted
        TableSorter sorter = getModel().getSorter();

        int index = sorter.modelIndex(row);

        EventObjectContainer c = model.getEventListModel().getElementAt(index);
        if (e.getID() == MouseEvent.MOUSE_CLICKED) { 

            if (sorter != null) {

                // On double click or single click, but not while a popup menu is showing
                if (!hasPopupMenu) {

                    // On double click launch an Event Player
                    if (e.getClickCount() == 2) {
                        PlayerManager.getInstance().openView(c, model);
                    } else {
                        EventImagePopupMenu imagePopup = new EventImagePopupMenu(c);
                        imagePopup.show((Component) e.getSource(), pt.x, pt.y);
                    }
                }
                hasPopupMenu = false;

            }
        } else if ( ((e.getID() == MouseEvent.MOUSE_PRESSED) || (e.getID() == MouseEvent.MOUSE_RELEASED)) &&
            e.isPopupTrigger()) {
                popupMenu.show((Component) e.getSource(), pt.x, pt.y);
                hasPopupMenu = true; 
        }
    } 

    /*
     * Forces fireTableChanged of the table display
     * TODO: map a function key, e.g. F5 key to this function
     */
    public void fireTableChanged() {
        ((AbstractTableModel) eventTable.getModel()).fireTableDataChanged();
    }

    /**
     * TODO: replace comments here
     */
    public void modelChanged(ModelEvent event) {
        if (event instanceof EventListModelEvent) {
            EventListModelEvent e         = (EventListModelEvent) event;
            EventListModel      listmodel = getModel().getEventListModel();

            switch (event.getID()) {
            case EventListModel.EventListModelEvent.CURRENT_PAGE_CHANGED :
                if (!listmodel.getValueIsAdjusting() && (eventTable != null)) {

                    // TODO: don't we need model to view translation here ?
                    // int row = listmodel.getValue();
                    // eventTable.scrollRectToVisible(eventTable.getCellRect(row, 1, true));
                }

                break;

            // Most of the events are handled through the JTable so these
            // are here just as placeholders in case some special handling
            // is required for these
            case EventListModel.EventListModelEvent.LIST_RELOADED :
                tablemodel.getTableModel().replace(listmodel);

                break;

            // This will scroll the table to the last loaded image
            case EventListModel.EventListModelEvent.NUM_LOADED_IMAGES_CHANGED :
                
                if (!listmodel.getValueIsAdjusting()) {  
                    tablemodel.getTableModel().fireTableDataChanged();  
                }
                // uncomment if you want the table to scroll as it is loading
                // int numchanged = e.getFlag();
                // eventTable.scrollRectToVisible(eventTable.getCellRect(numchanged, 1, true));
                break;

            case EventListModel.EventListModelEvent.ONE_ENTRY_REMOVED :
                CustomTableModel   model        = tablemodel.getTableModel();
                ArrayList<Integer> modelIndexes = e.getModelIndexes();
                ArrayList<Integer> rowIndexes   = getTranslatedIndexes(modelIndexes);

                if (rowIndexes.size() > 0) {
                    int firstRow = rowIndexes.get(0);
                    int lastRow  = firstRow;

                    model.fireTableRowsDeleted(firstRow, lastRow);
                }

                break;

            case EventListModel.EventListModelEvent.MULTIPLE_ENTRIES_CHANGED :
                if (!listmodel.getValueIsAdjusting()) {  
                    tablemodel.getTableModel().fireTableDataChanged();  
                }
                break;

            case EventListModel.EventListModelEvent.LIST_CLEARED :

                // TODO: put some logic to detect if editing and haven't saved results
                // recently before closing
                getView().setCursor(ImageUtils.busyCursor);
                tablemodel.getTableModel().clear();
                getView().setCursor(ImageUtils.defaultCursor);

                break;

            default :
                break;
            }
        }
    }

    /**
     * Translated the real model index  into the  row index
     * through the sorter, since the table may be sorted
     * @return the translated rows
     */
    private ArrayList<Integer> getTranslatedIndexes(ArrayList<Integer> modelIndexes) {
        ArrayList<Integer> selections = new ArrayList<Integer>();
        int                iMin       = modelIndexes.get(0);
        int                iMax       = iMin;

        if (modelIndexes.size() > 0) {
            iMax = modelIndexes.get(modelIndexes.size() - 1);
        }

        if ((iMin == -1) || (iMax == -1)) {
            return selections;
        }

        TableSorter sorter = getModel().getSorter();

        if (sorter != null) {
            selections.add(new Integer(sorter.viewIndex(iMin)));
            selections.add(new Integer(sorter.viewIndex(iMax)));
        }

        return selections;
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Searches the table for text under a column matching the description
     * This returns -1 if the column description doesn't match one
     * in the table or no match is found.  If a match is found,
     * the row the match is found is returned
     * @param searchColumnDescription the column description
     * @param searchText the text to search
     */
    public int search(final String searchColumnDescription, final Object searchText) {
        int searchColumn = -1;

        if (searchText.toString().length() == 0) {
            return -1;
        }

        int size = EventAbstractTableModel.columnNames.length - 1;

        for (int i = 0; i < size; i++) {
            if (EventAbstractTableModel.columnNames[i].equals(searchColumnDescription)) {
                searchColumn = i;

                break;
            }
        }

        if (searchColumn == -1) {
            return -1;
        }

        String search = searchText.toString().toLowerCase();

        for (int row = 0; row < eventTable.getRowCount(); row++) {
            Object val   = eventTable.getValueAt(row, searchColumn);
            String value = (val != null)
                           ? val.toString()
                           : "";

            if (value.toLowerCase().startsWith(search) || value.contains(search)) {
                return row;
            }
        }

        return -1;
    }

    class MouseClickTableActionHandler implements MouseListener {
        public void mouseClicked(MouseEvent e) {
            actionClickTable(e);
        }

        public void mouseEntered(MouseEvent e) {
            actionClickTable(e);
        }

        public void mouseExited(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {
            actionClickTable(e);
        }

        public void mouseReleased(MouseEvent e) {
            actionClickTable(e);
        }
    }
}
