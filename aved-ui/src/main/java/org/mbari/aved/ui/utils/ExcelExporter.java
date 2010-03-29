/*
 * @(#)ExcelExporter.java
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

import java.io.*;

import javax.swing.*;
import javax.swing.table.*;

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
}
