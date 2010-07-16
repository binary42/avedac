/*
 * @(#)ProgressDisplayStream.java
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



package org.mbari.aved.ui.progress;

//~--- non-JDK imports --------------------------------------------------------

import org.jdesktop.swingworker.SwingWorker;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Displays the output from a @{link InputStreamReader}
 * to a @{link org.mbari.aved.ui.progress.ProgressDisplay}
 *
 * @author dcline
 */
public class ProgressDisplayStream extends SwingWorker {
    public Boolean            isDone = false;
    private BufferedReader    br;
    private ProgressDisplay   display; 

    public ProgressDisplayStream(ProgressDisplay display, BufferedReader br) {
        this.br = br;
        this.display = display;
    }

    @Override
    protected Object doInBackground() throws Exception {
        try {
            String s;

            while (!isDone) {
                if (br.ready()) {
                    while ((s = br.readLine()) != null) {
                        display.display(s);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ProgressDisplayStream.class.getName()).log(Level.SEVERE, null, ex);
        }

        return this;
    }
}
