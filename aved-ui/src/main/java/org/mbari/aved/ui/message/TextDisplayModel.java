/*
 * @(#)TextDisplayModel.java
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



package org.mbari.aved.ui.message;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.*;

public class TextDisplayModel extends AbstractModel {
    String myText  = "";
    State  myState = State.SET;

    public enum State {
        RESET(0), SET(1);

        public final int myState;

        State(int state) {
            this.myState = state;
        }
    }

    public void setText(String text) {
        myText  = text;
        myState = State.SET;

        ModelEvent e = new ModelEvent(this, myText.length(), myText);

        notifyChanged(e);
    }

    public State getState() {
        return myState;
    }

    public String getText() {
        return myText;
    }

    public void clear() {
        myText  = "";
        myState = State.RESET;

        ModelEvent e = new ModelEvent(this, 0, myText);

        notifyChanged(e);
    }
}
