/*
 * @(#)TableModel.java
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
    private static final String EMPTY_STRING     = "";
    private static final long   serialVersionUID = 1L;
    private static String       columnNames[];

    // Pad the rows by 2 for inserting the precision/recall numbers
    private final int  PAD = 2;
    private final int  rows, columns;
    private int[]      sum;
    private Object[][] table;

    public TableModel(String[] headerNames, int[][] statistics, int[] totalEvents) {
        rows        = headerNames.length + PAD;
        columns     = headerNames.length + 1;
        sum         = new int[columns];
        table       = new Object[rows][columns];
        columnNames = new String[columns];

        // Format the header names for the actual class
        columnNames[0] = EMPTY_STRING;

        for (int j = 1; j < columns; j++) {
            columnNames[j] = "Actual " + headerNames[j - 1];
        }

        // Format the header names for the predicted class
        for (int i = 0; i < headerNames.length; i++) {
            table[i][0] = "Predicted " + headerNames[i];
        }

        table[rows - 2][0] = "Recall";
        table[rows - 1][0] = "Precision";

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

    public int getTotalEvents(int c) {
        if ((sum != null) && (sum.length <= c)) {
            return sum[c];
        }

        return -1;
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
