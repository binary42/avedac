/*
 * @(#)MissingFrameException.java
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

public class MissingFrameException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -8807932826648295488L;

    public MissingFrameException(String frame) {
        super("Frame " + frame + " is missing - invalid or missing",
              new Throwable("Either file is invalid, or doesn't exist"));
    }
}
