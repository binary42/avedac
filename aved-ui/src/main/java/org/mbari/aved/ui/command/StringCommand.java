/*
 * @(#)StringCommand.java   10/03/17
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

public class StringCommand extends AbstractCommand {
    private ArrayList<EventObjectContainer> container;
    private EventListModel                  model;
    private Memento                         state;

    public StringCommand() {}

    EventListModel getModel() {
        return model;
    }

    ArrayList<EventObjectContainer> getContainer() {
        return container;
    }

    /**
     * Initializer
     * @param container the list of <code>EventObjectContainers</code> to operate on
     * @param model the model
     */
    @Override
    public void initialize(ArrayList<EventObjectContainer> containers, EventListModel model) {
        this.model     = model;
        this.container = containers;

        // Create a memento for undo
        state = new Memento();
    }

    /**
     * Sets the state of the EventObjectContainers
     */
    @Override
    public void execute() {
        if ((container != null) && (model != null)) {

            // Get a copy of the original container
            state.setState(container);
        }
    }

    /**
     * Performs the undo to a className command
     * by removing the old <code>EventObjectContainers</code>
     * and replacing with the originals
     */
    @Override
    public void unexecute() {
        if ((state != null) && (model != null)) {

            // Get the container back
            ArrayList<EventObjectContainer> c = state.getState();

            // Delete then add originals back into the model
            model.delete(container);
            model.add(c);
            container = c;
        }
    }
}
