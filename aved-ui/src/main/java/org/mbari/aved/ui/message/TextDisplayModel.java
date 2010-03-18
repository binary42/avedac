/*
 * @(#)TextDisplayModel.java   10/03/17
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
