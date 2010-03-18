/*
 * @(#)FrameOutRangeException.java   10/03/17
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



package org.mbari.aved.ui.exceptions;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.model.EventObjectContainer;

public class FrameOutRangeException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -7688845951231445186L;

    public FrameOutRangeException(EventObjectContainer event, int framenum) {
        super("Frame " + framenum + "is out of range for event" + event.getObjectId() + "valid range: "
              + event.getStartFrame() + "-" + event.getEndFrame(), new Throwable("Frame number is invalid"));
    }
}
