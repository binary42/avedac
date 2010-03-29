/*
 * @(#)DeleteCommand.java
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

import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;

public class DeleteCommand extends AbstractCommand {
    private ArrayList<EventObjectContainer> containers;
    private EventListModel                  model;
    private Memento                         state;

    public DeleteCommand() {}

    /**
     * Alternative constuctor for initializing only one event
     * @param event the single <code>EventObjectContainers</code> to operate on
     * @param model the model
     */
    public DeleteCommand(EventObjectContainer event, EventListModel model) {
        ArrayList<EventObjectContainer> c = new ArrayList<EventObjectContainer>();

        c.add(event);
        initialize(c, model);
    }

    /**
     * Initializer
     * @param containers the list of <code>EventObjectContainers</code> to operate on
     * @param model the model
     */
    @Override
    public void initialize(ArrayList<EventObjectContainer> containers, EventListModel model) {
        this.model      = model;
        this.containers = containers;

        // Create a memento for undo
        state = new Memento();
    }

    /**
     * Deletes the EventObjectContainers
     */
    @Override
    public void execute() {
        if ((containers != null) && (model != null)) {
            state.setState(containers);

            // Delete the EventObjectContainers
            model.delete(containers);
        }
    }

    /**
     * Performs the undo to a delete command
     * by adding the containers back into the model
     */
    @Override
    public void unexecute() {
        if ((state != null) && (model != null)) {

            // Get the containers back
            ArrayList<EventObjectContainer> c = state.getState();

            // Add back into the model
            model.add(c);
            containers = c;
        }
    }
}
