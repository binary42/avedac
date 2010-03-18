/*
 * @(#)TestMessagStreamDisplay.java   10/03/17
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
