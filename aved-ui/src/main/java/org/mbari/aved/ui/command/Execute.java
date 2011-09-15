/*
 * @(#)Execute.java
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



package org.mbari.aved.ui.command;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.TableSorter;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 *
 * @author dcline
 */
public class Execute {

    /**
     * Returns a string description of the objects in the list selection
     * model. This currently only lists the object id, otherwise it is
     * probably too verbose a message, depending on the number of objects
     *
     * @param command
     */
    public static String getObjectIdDescription() {
        ApplicationModel   model       = Application.getModel();
        ArrayList<Integer> selections  = getTranslatedRows();
        EventListModel     list        = model.getEventListModel();
        String             description = "";
        int                j           = 0;

        if (selections.size() > 0) {
            Iterator<Integer> i = selections.iterator();

            if (selections.size() > 1) {
                description = "object ids: ";
            } else {
                description = "object id: ";
            }

            while (i.hasNext()) {
                description += list.getElementAt(i.next()).getObjectId();

                // When more than 5 objects, append ... and return
                if (j > 5) {
                    description += "...";

                    break;
                }

                if (i.hasNext()) {
                    description += ",";
                }

                j++;
            }
        }

        return description;
    }

    /**
     * Executes the command on the current list selection model
     * and stores it in the command history for future
     * undo/redo operationes
     *
     * @param command
     */
    public static void run(final AbstractCommand command) {
        ApplicationModel   model      = Application.getModel();
        ArrayList<Integer> selections = getTranslatedRows();
        EventListModel     list       = model.getEventListModel();

        if (selections.size() > 0) {

            // Get a list of event objects for later undo operation after delete
            ArrayList<EventObjectContainer> containers = new ArrayList<EventObjectContainer>();
            Iterator<Integer>               i          = selections.iterator();

            while (i.hasNext()) {
                containers.add(list.getElementAt(i.next().intValue()));
            }

            // Create the command, add it to the history, then execute
            command.initialize(containers, list);

            CommandHistory history = CommandHistory.getInstance();

            history.addCommand(command);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Thread change = new Thread(new Runnable() {
                        public void run() {
                            Application.getView().setBusyCursor();
                            command.execute();
                            Application.getView().setDefaultCursor();
                        }
                    });

                    change.start();
                }
            });
        }

        Application.getView().setDefaultCursor();
    }

    /**
     * Translated the row index into the real model index
     * through the sorter, since the table may be sorted
     * @return the translated rows
     */
    public static ArrayList<Integer> getTranslatedRows() {
        ApplicationModel   model      = Application.getModel();
        ListSelectionModel lsm        = model.getListSelectionModel();
        ArrayList<Integer> selections = new ArrayList<Integer>();
        int                iMin       = lsm.getMinSelectionIndex();
        int                iMax       = lsm.getMaxSelectionIndex();

        if ((iMin == -1) || (iMax == -1)) {
            return selections;
        }

        TableSorter sorter = model.getSorter();

        for (int i = iMin; i <= iMax; i++) {
            if (lsm.isSelectedIndex(i)) {
                if (sorter != null) {
                    selections.add(new Integer(sorter.modelIndex(i)));
                }
            }
        }

        return selections;
    }
}
