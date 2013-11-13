/*
 * @(#)StringCommand.java
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
