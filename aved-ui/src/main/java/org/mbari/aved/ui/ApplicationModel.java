/*
 * @(#)ApplicationModel.java
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
    private SummaryModel summary = new SummaryModel();

    /** List selection model for selecting this model data */
    private ListSelectionModel listSelectionModel = new DefaultListSelectionModel();

    /** Custom event list model */
    private EventListModel list = new EventListModel();

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
