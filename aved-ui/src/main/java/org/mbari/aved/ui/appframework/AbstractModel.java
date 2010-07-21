/*
 * @(#)AbstractModel.java
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



package org.mbari.aved.ui.appframework;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Abstract root class of Model hierarchy - provides basic
 * notification behaviour
 */
public abstract class AbstractModel implements Model {
    private final ArrayList listeners = new ArrayList(10);

    /**
     * Method that is called by subclasses of AbstractModel when they want to
     * notify other classes of changes to themselves.
     */
    @Override
     public void notifyChanged(ModelEvent event) {
        synchronized (listeners) {
            ArrayList list = (ArrayList) listeners.clone();
            Iterator  it   = list.iterator();

            while (it.hasNext()) {
                ModelListener ml = (ModelListener) it.next();

                ml.modelChanged(event);
            }
        }
    }

    /**
     * Add a ModelListener to the list of objects interested in ModelEvents.
     */
    public void addModelListener(ModelListener l) {
        listeners.add(l);
    }

    /**
     * Remove a ModelListener from the list of objects interested in ModelEvents
     */
    public void removeModelListener(ModelListener l) {
        listeners.remove(l);
    }
}
