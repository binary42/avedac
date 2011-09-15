/*
 * @(#)AbstractCommand.java
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
