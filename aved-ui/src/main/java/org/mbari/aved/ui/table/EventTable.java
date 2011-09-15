/*
 * @(#)EventTable.java
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
            setRowHeight(getRowHeight());

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
