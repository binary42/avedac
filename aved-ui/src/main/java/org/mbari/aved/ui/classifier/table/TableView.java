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



package org.mbari.aved.ui.classifier.table;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.gui.form.FormAccessor;

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.classifier.ClassifierModel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
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
    private static final String ID_DESCRIPTION   = "description";    // javax.swing.JLabel
    private static final String ID_TABLE         = "table";          // javax.swing.JTable
    private static final long   serialVersionUID = 1L;

    public TableView(ClassifierModel model, JTable table, TableController controller) {

        // Constructor
        super("org/mbari/aved/ui/forms/ClassifierTable.xml", model, controller);

        int numcolumns = table.getColumnCount();

        // Proportionately resize all columns
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setCellSelectionEnabled(false);

        for (int i = 0; i < numcolumns; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);

            col.setPreferredWidth(150);
        }

        // Left justify the all the columns
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();

        r.setHorizontalAlignment(SwingConstants.LEFT);

        for (int i = 0; i < numcolumns; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(r);
        }

        // Replace the table in the form with the AvedTable
        FormAccessor accessor = getForm().getFormAccessor("main.form");
        JComponent   tableOld = (JComponent) getForm().getComponentByName(ID_TABLE);

        // Put table in scroll pane  this allows for display of
        // column headings
        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scrollPane.setPreferredSize(new Dimension(300, 200));
        accessor.replaceBean(tableOld, scrollPane);
    }

    /**
     * TODO: add doc here
     * @param event
     */
    public void modelChanged(ModelEvent event) {}

    /**
     * Sets the description label in this view. This should
     * describe what data is contained in this table in enough detail
     * to be meaningful. This description is exported to the excel
     * format.
     * @param string the description
     */
    public void setDescription(String string) {
        getForm().getLabel(ID_DESCRIPTION).setText(string);
    }
}
