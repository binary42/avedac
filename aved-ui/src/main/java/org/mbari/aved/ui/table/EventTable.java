/*
 * @(#)EventTable.java   10/03/17
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



package org.mbari.aved.ui.table;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.model.EventAbstractTableModel;
import org.mbari.aved.ui.model.TableSorter;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

/**
 *
 * @author dcline
 */
public class EventTable extends AvedTable {

    /**
     *    Custom override of tableChanged method to preserve the
     *    ListSelectionModel upon table sorting. This ultimately
     *    calls the super.tableChanged() method, but creates
     *    a copy of the ListSelectionModel and restores it after
     *    calling tableChanged()
     *    @param e
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.getSource().getClass().equals(TableSorter.class)
                || e.getSource().getClass().equals(EventAbstractTableModel.CustomTableModel.class)) {
            ListSelectionModel        lsm  = this.getSelectionModel();
            DefaultListSelectionModel dlsm = new DefaultListSelectionModel();

            dlsm.setAnchorSelectionIndex(lsm.getAnchorSelectionIndex());
            dlsm.setLeadSelectionIndex(lsm.getLeadSelectionIndex());

            // Get number of selections
            int iMin = lsm.getMinSelectionIndex();
            int iMax = lsm.getMaxSelectionIndex();

            // This fixes a bug in the tableChanged method that incorrectly
            // calculates the dirty region for variable row height tables
            TableModelEvent evt = new TableModelEvent(((TableModel) e.getSource()), e.getFirstRow());

            super.tableChanged(evt);

            if ((iMin == -1) || (iMax == -1)) {
                dlsm.setSelectionInterval(iMin, iMax);
            } else {
                for (int i = iMin; i <= iMax; i++) {
                    if (lsm.isSelectedIndex(i)) {
                        dlsm.addSelectionInterval(i, i);
                    }
                }
            }

            lsm.setValueIsAdjusting(true);

            if ((iMin == -1) || (iMax == -1)) {
                lsm.setSelectionInterval(iMin, iMax);
            } else {
                for (int i = iMin; i <= iMax; i++) {
                    if (dlsm.isSelectedIndex(i)) {
                        lsm.addSelectionInterval(i, i);
                    }
                }
            }

            lsm.setValueIsAdjusting(false);
        } else {
            super.tableChanged(e);
        }
    }
}
