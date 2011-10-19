/*
 * @(#)ModalYesNoDialog.java
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

import com.jeta.forms.components.panel.FormPanel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 *
 * @author dcline
 */
public class ModalYesNoDialog extends JDialog implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private boolean           isAnswered       = false,
                              isYes            = false;

    public ModalYesNoDialog(JFrame frame, String question) {
        super(frame, true /* modal */);

        final FormPanel formPanel = new FormPanel("org/mbari/aved/ui/forms/MessageYesNo.xml");

        this.setContentPane(formPanel);
        formPanel.getTextComponent("message").setText(question);
        formPanel.getButton("yes").addActionListener(this);
        formPanel.getButton("no").addActionListener(this);
        pack();
        setResizable(false);

        Toolkit   kit        = frame.getToolkit();
        Dimension screenSize = kit.getScreenSize();

        // Make button get the focus whenever frame is activated.
        frame.addWindowListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                formPanel.getButton("no").requestFocusInWindow();
            }
        });

        // Display window in center of screen
        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x, y);
    }

    // Use synchronization to insure only one
    // thread can call answer at one time
    synchronized public boolean answer() {
        while (!isAnswered) {

            // Use wait() to give up lock while waiting for
            // button to be clicked.
            try {
                wait();
            } catch (InterruptedException e) {    /* error TODO print exception */
            }
        }

        return isYes;
    }

    @Override
    public void setVisible(boolean bln) {
        pack();
        setResizable(false);
        super.setVisible(bln);
    }

    // Also use synchronization to insure only one
    // one thread can call this method.
    synchronized public void actionPerformed(ActionEvent e) {
        isYes = e.getActionCommand().equals("Yes");

        // Tell other threads in wait() to wake up.
        isAnswered = true;
        notifyAll();
        dispose();
    }
}
