/*
 * @(#)MessageController.java
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



package org.mbari.aved.ui.message;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

//A simple class to override the behavior of the close button on the MessageDisplayNoHeaderController
//Otherwise, the embedded form cannot close the parent form. There is probably a better way to do 
//this, but this works ok.
public class MessageController implements KeyListener {
    private OverrideController controller;
    private MessageView        view; 

    public MessageController(String headerdescription) {
        view       = new MessageView(headerdescription);
        controller = new OverrideController(view);

        // Replace the main panel with the MessageDisplayNoHeader form
        view.replaceMainPanel(controller.getView().getForm());

        // To handle shortcut keys
        view.addKeyListener(this);
    }

    public JFrame getView() {
        return view;
    }

    // Display string in view 
    public void display(String str) {
         controller.display(str);
    }

    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {
        String s = System.getProperty("os.name").toLowerCase();

        if (((s.indexOf("linux") != -1) || (s.indexOf("windows") != -1)) && (e.getKeyCode() == KeyEvent.VK_C)) {
            getView().setVisible(false);
        } else if ((s.indexOf("mac") != -1) && (e.getKeyCode() == KeyEvent.VK_W)) {
            getView().setVisible(false);
        }
    }

    public void keyReleased(KeyEvent e) {}


    class OverrideController extends TextDisplayController {
        private JFrame view;

        OverrideController(MessageView view) {
            this.view = view;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Close")) {
                view.setVisible(false);
            } else {
                super.actionPerformed(e);
            }
        }
    }
}
