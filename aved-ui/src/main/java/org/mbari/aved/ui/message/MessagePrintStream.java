/*
 * @(#)MessagePrintStream.java
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



package org.mbari.aved.ui.message;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

import javax.swing.JFrame;

public class MessagePrintStream extends PrintStream {
    private static final MessagePrintStream INSTANCE     = new MessagePrintStream(System.out);
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
        String str = new String(b);

        controller.display(str);
    }

    @Override
    public void write(byte b[], int off, int len) {
        String str = new String(b, off, len);

        controller.display(str);
    }
}
