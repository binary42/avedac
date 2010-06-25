/*
 * @(#)NonModalMessageDialog.java
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

import com.jeta.forms.components.panel.FormPanel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.text.JTextComponent;

public class NonModalMessageDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;
    private boolean           isAnswered       = false,
                              isOkay           = false;

    public NonModalMessageDialog(JFrame frame, String message) {
        super(frame, "Message", false /* non modal */);

        FormPanel form = new FormPanel("org/mbari/aved/ui/forms/MessageOkay.xml");

        this.setContentPane(form);

        JTextComponent text   = form.getTextComponent("message");
        final JButton  button = (JButton) form.getButton("okay");

        text.setText(message);
        button.addActionListener(this);

        // Make button get the focus whenever frame is activated.
        frame.addWindowListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                button.requestFocusInWindow();
            }
        });

        Toolkit   kit        = frame.getToolkit();
        Dimension screenSize = kit.getScreenSize();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setResizable(false);

        // Display window in center of screen
        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x, y);
        button.requestFocusInWindow();
        this.setVisible(true);
    }

    // Use synchronization to insure only one
    // thread can call answer at one time
    synchronized public boolean answer() {
        while (!isAnswered)                       // Use wait() to give up lock while waiting for

        // button to be clicked.
        {
            try {
                wait();
            } catch (InterruptedException e) {    /* error TODO print exception */
            }
        }

        return isOkay;
    }

    // Also use synchronization to insure only one
    // one thread can call this method.
    synchronized public void actionPerformed(ActionEvent e) {
        isOkay = e.getActionCommand().equals("Okay");

        // Tell other threads in wait() to wake up.
        isAnswered = true;
        notifyAll();
        dispose();
    }
}
