/*
 * @(#)ProgressController.java
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



package org.mbari.aved.ui.progress;

//~--- non-JDK imports --------------------------------------------------------

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.ui.message.TextDisplayController;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;

public class ProgressController implements PropertyChangeListener {
    private OverrideController controller;
    private ProgressView       view;

    public ProgressController(String headerdescription, SwingWorker task) {
        view       = new ProgressView(headerdescription);
        controller = new OverrideController(view);

        // Replace the main panel with the MessageDisplayNoHeader form
        view.replaceMainPanel(controller.getView().getForm());
 
        // Add change listener to this controller. Normally should do this in
        // the view, to follow the view-controller separation rules used in
        // this general  design, but this is such a lightweight application,
        // put here instead of complicating the interface between view and
        // controller
        task.addPropertyChangeListener(this);
    }

    public void display(String str) {
        controller.display(str);
    }

    public ProgressView getView() {
        return view;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("progress")) {
            int progress = (Integer) evt.getNewValue();

            view.setValue(progress);

            // Disable the display when reached 100 %
            if (progress == 100) {
                view.dispose();
                view.setVisible(false);
            }
        }
    }

    class OverrideController extends TextDisplayController {
        private JFrame view;

        OverrideController(JFrame view) {
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
