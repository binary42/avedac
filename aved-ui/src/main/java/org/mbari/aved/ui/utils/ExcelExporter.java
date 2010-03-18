/*
 * @(#)ExcelExporter.java   10/03/17
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



package org.mbari.aved.ui.utils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.*;

import java.io.*;

import javax.swing.*;
import javax.swing.table.*;

/**
 *
 * @author dcline
 */
public class ExcelExporter {
    public ExcelExporter() {}

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
        System.out.println("write out to: " + file);
    }

    public static void main(String[] args) {
        String[][] data = {
            { "Housewares", "$1275.00" }, { "Pets", "$125.00" }, { "Electronics", "$2533.00" },
            { "Mensware", "$497.00" }
        };
        String[]          headers = { "Department", "Daily Revenue" };
        JFrame            frame   = new JFrame("JTable to Excel Hack");
        DefaultTableModel model   = new DefaultTableModel(data, headers);
        final JTable      table   = new JTable(model);
        JScrollPane       scroll  = new JScrollPane(table);
        JButton           export  = new JButton("Export");

        export.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    ExcelExporter exp = new ExcelExporter();

                    exp.exportTable(table, new File("results.xls"));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        frame.getContentPane().add("Center", scroll);
        frame.getContentPane().add("South", export);
        frame.pack();
        frame.setVisible(true);
    }
}
