/*
 * @(#)TableController.java
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

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.classifier.ClassifierModel;
import org.mbari.aved.ui.table.AvedTable;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

public class TableController extends AbstractController implements ModelListener {

    /** True when a popup window is displayed */
    private Boolean hasPopup = false;

    /** The popup */
    private PopupMenu popup;

    /** Customized table for displaying AVED classifier data */
    private final JTable table;

    public TableController(ClassifierModel classifierModel, AbstractTableModel tableModel, String title) {

        // Creates the custom JTable to customize how the table is rendered
        table = new AvedTable();

        // Set the data model for this table
        table.setModel(tableModel);

        // Create the modified JTable for the sorted data
        TableView view = new TableView(classifierModel, table, this);

        // Set the view for this controller
        setView(view);

        // Create the popup menu for this table
        popup = new PopupMenu(table, new File(title + ".xls"));
        table.addMouseListener(new MouseClickTableActionHandler());
    }

    /** Helper function that returns the table */
    public JTable getTable() {
        return table;
    }

    /** Helper function that returns TableView associated with this <code>TableController</code> */
    public TableView getView() {
        return ((TableView) super.getView());
    }

    /** Helper function that returns type cast ApplicationModel */
    public ClassifierModel getModel() {
        return ((ClassifierModel) super.getModel());
    }

    void actionClickTable(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            hasPopup = false;
        } else if ((e.getID() == MouseEvent.MOUSE_PRESSED) || (e.getID() == MouseEvent.MOUSE_RELEASED)) {
            Point pt = e.getPoint();

            // Only show popup if this is really a popup trigger
            if (e.isPopupTrigger()) {
                popup.show((Component) e.getSource(), pt.x, pt.y);
                hasPopup = true;
            }
        }
    }

    /**
     * TODO: add doc here
     */
    public void modelChanged(ModelEvent event) {}

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
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


    /**
     * A small popup menu for handing exporting the table data
     * to a user-defined directory
     */
    class PopupMenu extends JPopupMenu {
        private JTable table;

        public PopupMenu(final JTable table, final File xlsDefaultFile) {
            super(ApplicationInfo.getName());
            this.table = table;

            ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File file = browseForXlsExport(xlsDefaultFile);

                    if (file != null) {
                        try {
                            exportTable(table, file);
                        } catch (IOException ex) {
                            Logger.getLogger(PopupMenu.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            };
            JMenuItem m = new JMenuItem("Export to Excel");

            m.addActionListener(al);
            this.add(m);
        }

        /**
         * Browse for Excel file to save the results to
         *
         * @param file the default file to save
         * @return the file to save exported results to or null if
         * a selection was not made
         */
        private File browseForXlsExport(File file) {

            // Browse for XML to import starting with the last exported directory
            JFileChooser chooser = new JFileChooser();

            chooser.setCurrentDirectory(UserPreferences.getModel().getExportedExcelDirectory());
            chooser.setSelectedFile(file);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setDialogTitle("Choose Excel file to save the results to");

            if (chooser.showDialog(getView(), "Export") == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();

                UserPreferences.getModel().setExportedExcelDirectory(new File(f.getAbsolutePath()));

                return f;
            } else {

                // TODO: print dialog message box with something meaningful here
                return null;
            }
        }

        /**
         * This is a very simplified export function that exports data from a
         * table to a file in a format that Excel can import
         * @param table the table to export
         * @param file the file to export to
         * @throws java.io.IOException is thrown if there is an error writing to
         * the file
         */
        public void exportTable(JTable table, File file) throws IOException {
            TableModel model = table.getModel();
            FileWriter out   = new FileWriter(file);

            for (int i = 0; i < model.getColumnCount(); i++) {
                out.write(model.getColumnName(i) + "\t");
            }

            out.write("\n");

            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 0; j < model.getColumnCount(); j++) {
                    out.write(model.getValueAt(i, j).toString() + "\t");
                }

                out.write("\n");
            }

            out.close();
        }
    }
}
