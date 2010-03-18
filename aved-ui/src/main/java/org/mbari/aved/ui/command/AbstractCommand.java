/*
 * @(#)AbstractCommand.java   10/03/17
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
 * The root of the Command class hierarchy is the AbstractCommandr class.
 * This class defines all the basic setter/getter methods to implement
 * the Command
 * <p>
 */
public abstract class AbstractCommand implements Command {
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void unexecute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void initialize(ArrayList<EventObjectContainer> containers, EventListModel model) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
