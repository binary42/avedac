/*
 * @(#)Memento.java
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

import org.mbari.aved.ui.model.EventObjectContainer;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Iterator;

class Memento {
    private ArrayList<EventObjectContainer> state = null;

    public Memento() {}

    public void setState(ArrayList<EventObjectContainer> containers) {
        state = new ArrayList<EventObjectContainer>();

        Iterator<EventObjectContainer> i = containers.iterator();

        while (i.hasNext()) {
            EventObjectContainer e = i.next().clone();

            state.add(e);
        }
    }

    public ArrayList<EventObjectContainer> getState() {
        return state;
    }
}
