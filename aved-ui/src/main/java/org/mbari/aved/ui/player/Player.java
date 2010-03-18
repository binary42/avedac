/*
 * @(#)Player.java   10/03/17
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
