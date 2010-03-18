/*
 * @(#)LabelCommand.java   10/03/17
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

public class LabelCommand extends AbstractCommand {
    private String                          id = new String("");
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
            tag = new String("");
        }

        if (id == null) {
            id = new String("");
        }

        if (className == null) {
            className = new String("");
        }

        if (predictedClassName == null) {
            predictedClassName = new String("");
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
