/*
 * @(#)MessagePrintStream.java
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

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

import javax.swing.JFrame;

public class MessagePrintStream extends PrintStream {
    private static final MessagePrintStream INSTANCE   = new MessagePrintStream(System.out);
    MessageController                       controller = null;

    private MessagePrintStream(OutputStream out) {
        super(out);

        String lcOSName = System.getProperty("os.name").toLowerCase();

        if (lcOSName.startsWith("mac os x")) {

            // If mac, use the system menu bar. Set it here because message
            // stream display is a singleton that is displayed before
            // any other displays
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        try {
            controller = new MessageController("Messages");
        } catch (Exception e) {
            e.printStackTrace();

            // TODO: what to do if exception here ?
        }
    }

    public static MessagePrintStream getInstance() {
        return INSTANCE;
    }

    public static JFrame getView() {
        return (JFrame) INSTANCE.controller.getView();
    }

    @Override
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);    
    }

    @Override
    public void write(byte b[], int off, int len) {  
        controller.display(new String(b, off, len));
    } 
   
}
