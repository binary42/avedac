/*
 * @(#)CombineCommand.java
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
