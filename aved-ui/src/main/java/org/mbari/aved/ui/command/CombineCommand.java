/*
 * @(#)CombineCommand.java   10/03/17
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

public class CombineCommand extends AbstractCommand {
    private ArrayList<EventObjectContainer> containers;
    private EventListModel                  model;
    private EventObjectContainer            newContainer;
    private Memento                         state;

    public CombineCommand() {}

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
     * Combines the EventObjectContainers
     */
    @Override
    public void execute() {
        if ((containers != null) && (model != null)) {
            state.setState(containers);

            // Combine the EventObjectContainers and get back the result
            newContainer = model.combine(containers);
        }
    }

    /**
     * Performs the undo to a combine command
     * by first deleting the merged EventObjectContainer,
     * then adding back in the original, individual
     * EventObjectContainers
     */
    @Override
    public void unexecute() {
        if ((state != null) && (model != null) && (newContainer != null)) {

            // Get the containers back
            ArrayList<EventObjectContainer> oldContainers = state.getState();

            // Delete the original
            model.deleteElement(newContainer);

            // Add the old back into the model
            model.add(oldContainers);

            // Reset the saved containers
            containers = oldContainers;
        }
    }
}
