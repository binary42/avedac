/*
 * @(#)ProgressDisplayStream.java
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



package org.mbari.aved.ui.progress;

//~--- non-JDK imports --------------------------------------------------------

import org.jdesktop.swingworker.SwingWorker;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.IOException; 

import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Displays the output from a @{link InputStreamReader}
 * to a @{link org.mbari.aved.ui.progress.ProgressDisplay}
 *
 * @author dcline
 */
public class ProgressDisplayStream extends SwingWorker {
    private Boolean          isDone = false;
    private BufferedReader  br;
    private OutputStream display; 

    public ProgressDisplayStream(OutputStream display, BufferedReader br) {
        this.br      = br;
        this.display = display; 
    }

    @Override
    protected Object doInBackground() throws Exception {
        try {
            String s;

            while (!isDone) {
                if (br.ready()) {
                    while ((s = br.readLine()) != null && s.length() > 0) {
                        String copy = s.substring(0, s.length());
                        display.write(copy.getBytes());
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ProgressDisplayStream.class.getName()).log(Level.SEVERE, null, ex);
        }

        return this;
    }

    public void setBufferedReader(BufferedReader br) {
        this.br = br;
    }
    
    @Override
    public void done() {
        isDone = true;
    }
}
