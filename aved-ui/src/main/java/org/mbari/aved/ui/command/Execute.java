/*
 * @(#)Execute.java
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
        String             description = new String("");
        int                j           = 0;

        if (selections.size() > 0) {
            Iterator<Integer> i = selections.iterator();

            if(selections.size() > 1)
                description = "object ids: ";
            else
                description = "object id: ";
            
            while (i.hasNext()) {
                description +=  list.getElementAt(i.next()).getObjectId();

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
