/*
 * @(#)LabelCommand.java
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

public class LabelCommand extends AbstractCommand {
    private String                          id = "";
    private String                          className;
    private ArrayList<EventObjectContainer> containers;
    private EventListModel                  model;
    private String                          predictedClassName;
    private Memento                         state;
    private String                          tag;

    public LabelCommand(String predictedClassName, String className, String tag, String id) {
        this.predictedClassName = predictedClassName;
        this.className          = className;
        this.tag                = tag;
        this.id                 = id;

        if (tag == null) {
            tag = "";
        }

        if (id == null) {
            id = "";
        }

        if (className == null) {
            className = "";
        }

        if (predictedClassName == null) {
            predictedClassName = "";
        }
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
     * Tags the EventObjectContainers
     */
    @Override
    public void execute() {
        if ((containers != null) && (model != null)) {

            // Get a copy of the original containers
            state.setState(containers);
            model.setIdAll(containers, id);
            model.setClassAll(containers, className);
            model.setPredictedClass(containers, predictedClassName, 1.0f);
            model.setTagAll(containers, tag); 
        }
    }

    /**
     * Performs the undo to a id command
     * by removing the old <code>EventObjectContainers</code>
     * and replacing with the originals
     */
    @Override
    public void unexecute() {
        if ((state != null) && (model != null) && (id != null)) {

            // Get the containers back
            ArrayList<EventObjectContainer> c = state.getState();

            // Delete then add originals back into the model
            model.delete(containers);
            model.add(c);
            containers = c;
        }
    } 
}
