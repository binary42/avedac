/*
 * @(#)TestMessagStreamDisplay.java
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

import org.mbari.aved.ui.utils.ImageUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;

import javax.swing.JFrame;

public class TestMessagStreamDisplay {
    static public void setCursor(Cursor crsr, Component cmp) {

        // This routine sets the Cursor crsr on Component cmp and all of its children.
        // If cmp is the top level Component, the effect is setting the busy cursor on the
        // entire application.
        cmp.setCursor(crsr);

        if (cmp instanceof Container) {
            Component[] kids = ((Container) cmp).getComponents();

            for (int i = 0; i < kids.length; i++) {
                setCursor(crsr, kids[i]);
            }
        }

        return;
    }

    public static void main(String[] args) {
        MessagePrintStream m     = MessagePrintStream.getInstance();
        JFrame             frame = m.getView();

        frame.setTitle("Message Display Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        System.setOut(m);
        System.setErr(m);
        setCursor(ImageUtils.busyCursor, frame);
        System.out.println("If you see this message, display is redirecting stdout correctly");
        System.err.println("If you see this message, display is redirecting stdin correctly");
    }
}
