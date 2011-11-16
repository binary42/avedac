/*
 * @(#)ProcessDisplay.java
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



package org.mbari.aved.ui.process;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.message.MessageController;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;
import java.awt.Toolkit;

import java.io.*;

import javax.swing.JFrame;

public class ProcessDisplay extends OutputStream {
    private MessageController controller = null;
    
    public ProcessDisplay(String headerdescription) {

        // Create message controller and view
        controller = new MessageController(headerdescription);

        // Bring the display to the front right
        Toolkit   kit        = controller.getView().getToolkit();
        Dimension screenSize = kit.getScreenSize();

        // Display window in right of screen
        int x = (int) (screenSize.width - getView().getWidth());
        int y = (int) (screenSize.height - getView().getHeight());

        getView().setLocation(x, y);
        getView().setVisible(false);
    }

    /** Helper function that returns view for this display */
    public JFrame getView() {
        return controller.getView();
    }

    @Override
    public void write(int b) throws IOException {

        // TODO Auto-generated method stub
    }

    public void write(byte b[]) throws IOException {
        String s = "";

        for (int i = 0; i < b.length; i++) {
            s += (char) b[i];
        }

        controller.display(s);
    }
}
