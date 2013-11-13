/*
 * @(#)ExcelExporter.java
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



package org.mbari.aved.ui.utils;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.table.*;
import org.mbari.aved.ui.userpreferences.UserPreferences;

/**
 *
 * @author dcline
 */
public final class ExcelExporter {
    public static void exportTable(JTable table, File file) throws IOException {
        TableModel model = table.getModel();
        FileWriter out   = new FileWriter(file);

        for (int i = 0; i < model.getColumnCount(); i++) {
            out.write(model.getColumnName(i) + "\t");
        }

        out.write("\n");

        for (int i = 0; i < model.getRowCount(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                Object obj = model.getValueAt(i, j);

                if (obj != null) {
                    out.write(obj.toString() + "\t");
                } else {
                    out.write("" + "\t");
                }
            }

            out.write("\n");
        }

        out.close();
        System.out.println("write out to: " + file);
    }


    /**
     * Export the results in simple Excel format.
     * This will prompt the user first to browse for
     * a suitable file
     */
    public static void exportProcessedResultsAsXls(File xml, JTable table, JFrame view) {
        File   dir   = UserPreferences.getModel().getExportedExcelDirectory();
        File   tmp   = new File(dir + "/" + ParseUtils.removeFileExtension(xml.getName()) + ".xls");
        File   f     = browseForXlsExport(tmp, view);

        if (f != null) {
            try {
                ExcelExporter.exportTable(table, f);
            } catch (IOException ex) {
                Logger.getLogger(ExcelExporter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     *    Browse for Excel file to save the results to
     *
     *    @param file the default file to save
     *    @return the file to save exported results to or null if
     *    a selection was not made
     */
    public static File browseForXlsExport(File file, JFrame view) {

        // Browse for XML to import starting with the last exported directory
        JFileChooser chooser = new JFileChooser();

        chooser.setCurrentDirectory(UserPreferences.getModel().getExportedExcelDirectory());
        chooser.setSelectedFile(file);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Choose Excel file to save the results to");

        if (chooser.showDialog(view, "Export") == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();

            UserPreferences.getModel().setExcelExportDirectory(new File(f.getParent()));

            return f;
        } else {

            // TODO: print dialog message box with something meaningful here
            return null;
        }
    }
 
}
