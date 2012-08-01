/*
 * @(#)BatchProcessAbstractTableModel.java
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

import org.mbari.aved.classifier.TrainingModel;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.table.AbstractTableModel;

/**
 * AbstractTableModel encapsulating ArrayList of BatchProcessModel 
 * objects and customizes TableModel columns to reflect classes
 * in given TrainingModel
 * 
 * @author dcline
 */
public class BatchProcessAbstractTableModel extends AbstractTableModel {
    private LinkedList<BatchProcessModel>      list        = new LinkedList<BatchProcessModel>();
    private ArrayList<String>                      columnNames = new ArrayList<String>();
    private HashMap<String, BatchProcessModel> hmapFilenames = new HashMap<String, BatchProcessModel>();
        
    public BatchProcessAbstractTableModel() { 
        columnNames.add("File");    
        columnNames.add("Status");
        fireTableStructureChanged();
    }

    public void changeClassColumns(TrainingModel trainingModel) {
        columnNames.clear();

        columnNames.add("File");
        columnNames.add("Status"); 

        if (trainingModel != null && trainingModel.getNumClasses() > 0) {
            int i = trainingModel.getNumClasses() + 1;

            columnNames.add("Unknown");

            for (i = 0; i < trainingModel.getNumClasses(); i++) {
                String name = trainingModel.getClassModel(i).getPredictedName();
                columnNames.add(name);
            }
        }

        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int c) {
        return columnNames.get(c);
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param       rowIndex        the row whose value is to be queried
     * @param       columnIndex     the column whose value is to be queried
     * @return      the value Object at the specified cell
     *
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == -1) {
            return list.get(rowIndex);
        } else if (columnIndex == 0) {
            return list.get(rowIndex).getModel().getSummaryModel().getXmlFile().getName();
        } else if (columnIndex == 1) {
            return list.get(rowIndex).getStatus();
        } else if (columnIndex > 1) {
            return list.get(rowIndex).getClassTotal(columnIndex - 2);
        }

        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void remove(ArrayList<Integer> indexes) {
     
        ArrayList<BatchProcessModel> toRemove = new ArrayList<BatchProcessModel>();
        for (int i = 0; i < indexes.size(); i++) {
            BatchProcessModel m = list.get(indexes.get(i));
            toRemove.add(m);
            hmapFilenames.remove(m.getFile().getAbsolutePath());
        }   
        
        list.removeAll(toRemove);
        fireTableDataChanged();
    } 

    void add(BatchProcessModel m) {
        // Don't add duplicates. Use a hash map to speed up the lookup
        if (!hmapFilenames.containsKey(m.getFile().getAbsolutePath())) {
            hmapFilenames.put(m.getFile().getAbsolutePath(), m);
            list.add(m);
        } 
    }

    public void clear() {
        list.clear();
        fireTableDataChanged();
    }

    public Object getEntry(int rowIndex) {
        return list.get(rowIndex);
    }

    void clearResults() {

        Iterator<BatchProcessModel> itr = list.iterator();
        while (itr.hasNext()) {
            BatchProcessModel m = itr.next();
            m.setStatus("");
            for (int i = 2; i < this.getColumnCount(); i++) {
                m.setClassTotal(i - 2, "");
            }
        }        
        fireTableDataChanged();
    }

 
}
