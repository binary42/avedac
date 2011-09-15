/*
 * @(#)Player.java
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



package org.mbari.aved.ui.player;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.model.EventObjectContainer;

public class Player {
    private long             eventObjectId = -1;
    private PlayerController controller;

    public Player(EventObjectContainer event, ApplicationModel model) throws Exception {

        // Initialize the event to edit
        controller    = new PlayerController(event, model);
        eventObjectId = event.getObjectId();
    }

    public Boolean isEditing(EventObjectContainer event) {
        if (eventObjectId == event.getObjectId()) {
            return true;
        }

        return false;
    }

    public PlayerView getView() {
        return (PlayerView) controller.getView();
    }

    public Boolean isEditing(long id) {
        if (eventObjectId == id) {
            return true;
        }

        return false;
    }
}
