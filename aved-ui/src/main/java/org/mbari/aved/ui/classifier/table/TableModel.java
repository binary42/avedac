/*
 * @(#)TableModel.java
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



package org.mbari.aved.ui.classifier.table;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.table.AbstractTableModel;

/**
 * Abstract table model class that contains a custom
 * table model for displaying and storing AVED Classifier
 * results in a Confusion Matrix
 * @author dcline
 */
public class TableModel extends AbstractTableModel {
    private static final String EMPTY_STRING     = new String("");
    private static final long   serialVersionUID = 1L;
    private static String       columnNames[];

    // Pad the rows by 2 for inserting the precision/recall numbers
    private final int  PAD = 2;
    private final int  rows, columns;
    private Object[][] table;

    public TableModel(String[] headerNames, int[][] statistics, int[] totalEvents) {
        rows    = headerNames.length + PAD;
        columns = headerNames.length + 1;

        int sum[] = new int[columns];

        table       = new Object[rows][columns];
        columnNames = new String[columns];

        // Format the header names for the actual class
        columnNames[0] = EMPTY_STRING;

        for (int j = 1; j < columns; j++) {
            columnNames[j] = new String("Actual " + headerNames[j - 1]);
        }

        // Format the header names for the predicted class
        for (int i = 0; i < headerNames.length; i++) {
            table[i][0] = new String("Predicted " + headerNames[i]);
        }

        table[rows - 2][0] = new String("Recall");
        table[rows - 1][0] = new String("Precision");

        // Sum up all the columns
        for (int j = 0; j < headerNames.length; j++) {
            for (int i = 0; i < headerNames.length; i++) {
                sum[j]          += statistics[i][j];
                table[i][j + 1] = new Integer(statistics[i][j]);
            }
        }

        float recall    = 0.f;
        float precision = 0.f;

        // Calculate the recall/precision numbers and insert into the fields
        for (int j = 0; j < totalEvents.length; j++) {
            if (totalEvents[j] > 0) {
                recall = ((float) statistics[j][j]) / ((float) totalEvents[j]);
            } else {
                recall = 0.f;
            }

            table[rows - 2][j + 1] = new Float(recall);

            if (sum[j] > 0) {
                precision = ((float) statistics[j][j]) / ((float) sum[j]);
            } else {
                precision = 0.f;
            }

            table[rows - 1][j + 1] = new Float(precision);
        }
    }

    /**
     * Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     *
     */
    public int getColumnCount() {
        return columns;
    }

    @Override
    public String getColumnName(int c) {
        return (String) columnNames[c];
    }

    /**
     * Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     *
     */
    public int getRowCount() {
        return rows;
    }

    public Object getEntry(int rowIndex) {
        if (rowIndex < columns) {
            return table[rowIndex][0];
        }

        return EMPTY_STRING;
    }

    /**
     * Returns the most specific superclass for all the cell values
     * in the column.  This is used by the <code>JTable</code> to set up a
     * default renderer and editor for the column.
     *
     * @param columnIndex  the index of the column
     * @return the common ancestor class of the object values in the model.
     */
    @Override
    public Class getColumnClass(int columnIndex) {
        return getValueAt(0, columnIndex).getClass();
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param   rowIndex        the row whose value is to be queried
     * @param   columnIndex     the column whose value is to be queried
     * @return  the value Object at the specified cell
     *
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        return table[rowIndex][columnIndex];
    }
}
