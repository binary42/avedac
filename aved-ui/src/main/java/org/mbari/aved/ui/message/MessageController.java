/*
 * @(#)MessageController.java
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
