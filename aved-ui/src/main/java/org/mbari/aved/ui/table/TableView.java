/*
 * @(#)TableView.java
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
import com.jeta.forms.gui.form.FormAccessor;

import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.model.EventAbstractTableModel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Color;
import java.awt.Component;

import javax.swing.*; 
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * A better-looking table than JTable. This is a modified version of
 * the one * @author Elliott Hughes defined.
 *
 * Removed all MacOS references, because we don't care about that.
 * Just want to ability to paint pretty empty rows without inserting
 * dummy data during startup, and incorporated our custom
 * EditorColorTableCellRenderer into this class.
 */
public class TableView extends JFrameView {

    private static final String ID_TABLE = "table";    // javax.swing.JTable
    private static final long serialVersionUID = 1L;
 
    public TableView(ApplicationModel model, EventTable table, TableController controller) {

        // Constructor
        super("org/mbari/aved/ui/forms/Table.xml", model, controller);

        int numcolumns = EventAbstractTableModel.NUM_COLUMNS;

        // Define the variable row height renderer to adjust the height by the thumbnail
        table.getColumnModel().getColumn(EventAbstractTableModel.COL_THUMBNAIL).setCellRenderer(new VariableRowHeightRenderer());

        // Enable column auto resizing
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableCellRenderer s = new DefaultTableCellRenderer();

        s.setHorizontalAlignment(SwingConstants.LEFT);

        // Set columns to 150 pixels wide
        for (int i = 0; i < numcolumns; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);

            col.setPreferredWidth(150);
        }

        // Left-justify the all the columns except the thumbnail column that
        // is handled by the custom renderer
        DefaultTableCellRenderer r = new DefaultTableCellRenderer(); 

        r.setHorizontalAlignment(SwingConstants.LEFT);

        for (int i = 0; i < numcolumns; i++) {
            if (i != EventAbstractTableModel.COL_THUMBNAIL)
                table.getColumnModel().getColumn(i).setCellRenderer(r);
        }

        // Replace the table in the form with the AvedTable
        FormAccessor a = getForm().getFormAccessor();
        JTable tableOld = getForm().getTable(ID_TABLE);

        a.replaceBean(tableOld, table);
    }

    public void modelChanged(ModelEvent event) {
    }

    public class VariableRowHeightRenderer extends JLabel implements TableCellRenderer {

        public VariableRowHeightRenderer() {
            super();
            setOpaque(true);
            setHorizontalAlignment(JLabel.CENTER);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            if (value instanceof ImageIcon) {
                if (isSelected) {
                    setBackground(UIManager.getColor("Table.selectionBackground"));
                } else {
                    setBackground(Color.BLACK);
                }

                if (hasFocus) {
                    setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));

                    if (table.isCellEditable(row, column)) {
                        super.setForeground(UIManager.getColor("Table.focusCellForeground"));
                        super.setBackground(UIManager.getColor("Table.focusCellBackground"));
                    }
                } else {
                    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
                }

                // create the icon from the image and set it on the label
                setIcon((ImageIcon) (value));

                // check if the preferred height of this component is not equal to
                // the row height, then set the preferred height of this component as
                // the row height, else don't do anything.
                if ((getPreferredSize().height != table.getRowHeight(row)) && (getPreferredSize().height > 30)) {
                    table.setRowHeight(row, getPreferredSize().height);
                }
            }

            return this;
        }
    }
}
