/*
 * @(#)MessageView.java
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
import com.jeta.forms.gui.form.FormAccessor;

import org.mbari.aved.ui.ApplicationInfo;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class MessageView extends JFrame {

    /*
     *  Component names in the MessagePanel form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_HEADER_LABEL = "header";    // javax.swing.JLabel
    private FormPanel          form;

    /* Helpers for managing the Abeille views */
    private FormAccessor mainViewAccessor;
    private Component    nestedForm;

    public MessageView(String headerdescription) {

        // Initialize the Abeille form
        form = new FormPanel("org/mbari/aved/ui/forms/Message.xml");
        this.setContentPane(form);

        // TODO: need to throw exception here is form not found
        mainViewAccessor = form.getFormAccessor("main.form");

        // TODO: replace with replaceBean using form ID
        nestedForm = findNestedForm();

        // Initialize components for setting/getting
        JLabel header = form.getLabel(ID_HEADER_LABEL);

        // Set.getName() of display and size it
        header.setText(headerdescription);
        this.setTitle(ApplicationInfo.getName() + "-" + headerdescription);

        // Format the size the message
        this.setVisible(false);
        this.pack();

        Dimension dims = this.getSize();

        dims.width = 750;
        this.setSize(dims);
        this.setResizable(true);
        this.setVisible(true);

        // Setup the window location
        Toolkit   kit        = this.getToolkit();
        Dimension screenSize = kit.getScreenSize();

        // Set default location to center of screen
        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x, y);
    }

    private Component findNestedForm() {
        Iterator iter = mainViewAccessor.beanIterator();

        while (iter.hasNext()) {
            Component comp = (Component) iter.next();

            if (comp instanceof FormAccessor) {
                return comp;

                // found a nested form.
            } else {

                // found a standard Java Bean
            }
        }

        return null;

        // didn't find a nested form. TODO: throw exception here
    }

    public void replaceMainPanel(FormPanel panel) {
        if (nestedForm != null) {
            mainViewAccessor.replaceBean(nestedForm, panel);
            nestedForm = panel;
            this.pack();

            Dimension dims = this.getSize();

            dims.width = 750;
            this.setSize(dims);
            this.setResizable(true);
        }

        // TODO: throw exception here if no nestedpanel to replace
    }
}
