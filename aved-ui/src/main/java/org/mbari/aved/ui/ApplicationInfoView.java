/*
 * @(#)ApplicationInfoView.java
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class ApplicationInfoView extends JFrame {

    /*
     *  Component names in the ApplicationInfo form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_OKAY_BUTTON = "okay";    // javax.swing.JButton
    public FormPanel           form;

    public ApplicationInfoView() {
        super("Help about " + ApplicationInfo.getName());

        // Initialize the Abeille form
        form = new FormPanel("org/mbari/aved/ui/forms/ApplicationInfo.xml");
        form.getButton(ID_OKAY_BUTTON).addActionListener(new OkayAction());

        JLabel appinfo = (JLabel) form.getLabel("appinfo");
        String s       = "<html>" + ApplicationInfo.getName() + "<p><p>" + "Version: " + ApplicationInfo.getVersion()
                         + "<p><p>" + "(C) Copyright 2008 MBARI <p>"
                         + "MBARI Proprietary Information. All rights reserved" + "<p><p>"
                         + "For information on the AVED project see " + "<a href> http://www.mbari.org/aved</a></html>";

        appinfo.setText(s);
        this.setContentPane(form);
        this.setTitle(ApplicationInfo.getName() + "-" + "About");
        this.pack();
    }

    public void close() {
        this.setVisible(false);
    }

    class OkayAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }
}
