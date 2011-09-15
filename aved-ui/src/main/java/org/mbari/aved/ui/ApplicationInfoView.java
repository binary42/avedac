/*
 * @(#)ApplicationInfoView.java
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
                         + "<p><p>" + "(C) Copyright 2010 MBARI <p>"
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
