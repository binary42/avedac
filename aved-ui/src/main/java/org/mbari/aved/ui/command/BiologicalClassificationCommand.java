/*
 * @(#)BiologicalClassificationCommand.java
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

/**
 *
 * @author dcline
 */
public class BiologicalClassificationCommand extends StringCommand {
    private final String className;

    public BiologicalClassificationCommand(String command) {
        this.className = command;
    }

    /**
     * Sets the classes in the EventObjectContainers
     */
    @Override
    public void execute() {
        super.execute();

        EventListModel                  model     = getModel();
        ArrayList<EventObjectContainer> container = getContainer();

        model.setClassAll(container, className);
    }
}
