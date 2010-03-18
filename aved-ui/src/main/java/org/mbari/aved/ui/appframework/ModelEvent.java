/*
 * @(#)ModelEvent.java   10/03/17
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

import java.awt.event.ActionEvent;

/**
 * Used to notify interested objects of changes in the
 * state of a model
 */
public class ModelEvent extends ActionEvent {
    private int flag;

    // TODO: how to generalize this ?
    public ModelEvent(Object obj, int id, String message) {
        super(obj, id, message);
        this.flag = 0;
    }

    public ModelEvent(Object obj, int id, String message, int flag) {
        super(obj, id, message);
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }
}
