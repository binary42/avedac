/*
 * @(#)AbstractModel.java
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
