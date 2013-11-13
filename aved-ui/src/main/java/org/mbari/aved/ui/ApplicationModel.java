/*
 * @(#)ApplicationModel.java
 * 
 * Copyright 2013 MBARI
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

import org.mbari.aved.ui.appframework.AbstractModel;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.model.TableSorter;

//~--- JDK imports ------------------------------------------------------------

import java.net.MalformedURLException;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

public class ApplicationModel extends AbstractModel {

    /** Table sorter decorator to control how cells are sorted in the table view */
    private TableSorter sorter = null;

    /** Summary model */
    private final SummaryModel summary = new SummaryModel();

    /** List selection model for selecting this model data */
    private final ListSelectionModel listSelectionModel = new DefaultListSelectionModel();

    /** Custom event list model */
    private final EventListModel list = new EventListModel();

    /** Constructor. Initializes the model components */
    public ApplicationModel() throws Exception {}

    public void initializeSorter(TableSorter sorter) {
        this.sorter = sorter;
    }

    /**
     * Add a ModelListener to the list of objects interested in EditorModelEvents.
     */
    @Override
    public void addModelListener(ModelListener l) {
        super.addModelListener(l);
        list.addModelListener(l);
        summary.addModelListener(l);
    }

    /** Returns the customized table sorter */
    public TableSorter getSorter() {
        return sorter;
    }

    /**
     * Reset this model. This will reset all contained members
     * and notify all ModelListeners
     */
    public void reset() {
        list.reset();
        listSelectionModel.clearSelection();

        try {
            summary.reset();
        } catch (MalformedURLException ex) {
            Logger.getLogger(ApplicationModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Wrapper to add entries to the enclosed list
     * @param entries
     */
    public void add(LinkedList<EventObjectContainer> entries) throws Exception {
        list.add(entries);
    }

    /**
     *
     * @return the list selection model
     */
    public ListSelectionModel getListSelectionModel() {
        return listSelectionModel;
    }

    /**
     *
     * @return the event list selection model
     */
    public EventListModel getEventListModel() {
        return list;
    }

    /**
     *
     * @return the event list selection model
     */
    public SummaryModel getSummaryModel() {
        return summary;
    }
}
