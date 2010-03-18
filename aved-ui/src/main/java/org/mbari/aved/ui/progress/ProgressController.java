/*
 * @(#)ProgressController.java   10/03/17
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
