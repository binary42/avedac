/*
 * @(#)GotoDialog.java   10/03/17
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.components.panel.FormPanel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.text.ParseException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;

public class GotoDialog extends JDialog implements ActionListener {
    private static final long   serialVersionUID = 1L;
    private boolean             isAnswered       = false,
                                isGoto           = false;
    private JFormattedTextField objectIdTextField;
    private Object              value;

    public GotoDialog(JFrame frame, Point location) throws Exception {
        super(frame, true /* modal */);

        FormPanel myForm = new FormPanel("org/mbari/aved/ui/forms/Goto.xml");

        this.setContentPane(myForm);
        objectIdTextField = (JFormattedTextField) myForm.getTextField("goto");
        objectIdTextField.setValue(new String("1"));
        objectIdTextField.setColumns(40);

        final JButton button = (JButton) myForm.getButton("goto2");

        if ((objectIdTextField == null) || (button == null)) {
            throw new Exception("Invalid dialog components");
        }

        button.addActionListener(this);

        // Make button get the focus whenever frame is activated.
        frame.addWindowListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                button.requestFocusInWindow();
            }
        });

        if (location != null) {
            this.setLocation(location);
        } else {
            Toolkit   kit        = frame.getToolkit();
            Dimension screenSize = kit.getScreenSize();

            // Display window in center of screen
            int x = (screenSize.width - this.getWidth()) / 2;
            int y = (screenSize.height - this.getHeight()) / 2;

            this.setLocation(x, y);
        }

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setResizable(false);
        objectIdTextField.requestFocusInWindow();
        this.setVisible(true);
    }

    // Use synchronization to insure only one
    // thread can call answer at one time
    synchronized public boolean answer() {
        while (!isAnswered)    // Use wait() to give up lock while waiting for

        // button to be clicked.
        {
            try {
                wait();
            } catch (InterruptedException e) { /* error TODO print exception */
            }
        }

        return isGoto;
    }

    /**
     * Gets the value of this goto - this could be a String, Integer, etc.
     * @return object the user entered to goto
     */
    synchronized public Object getValue() {
        return value;
    }

    // Also use synchronization to insure only one
    // one thread can call this method.
    synchronized public void actionPerformed(ActionEvent e) {
        isGoto = e.getActionCommand().equals("Goto");

        // Tell other threads in wait() to wake up.
        isAnswered = true;

        try {
            objectIdTextField.commitEdit();
        } catch (ParseException ex) {
            Logger.getLogger(GotoDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

        value = objectIdTextField.getValue();
        notifyAll();
        dispose();
    }

    public static void main(String[] args) {
        JFrame gotoFrame = new JFrame("Goto Object ID");

        try {
            GotoDialog gotoDialog = new GotoDialog(gotoFrame, null);

            gotoDialog.setAlwaysOnTop(true);
        } catch (Exception ex) {
            Logger.getLogger(GotoDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
