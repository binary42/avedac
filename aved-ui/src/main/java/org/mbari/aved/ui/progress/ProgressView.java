/*
 * @(#)ProgressView.java
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

import com.jeta.forms.components.panel.FormPanel;
import com.jeta.forms.gui.form.FormAccessor;

import org.mbari.aved.ui.ApplicationInfo;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Dimension;

import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class ProgressView extends JFrame {
    public static final String ID_HEADER_LABEL = "header";    // javax.swing.JLabel

    /*
     *  Component names in the ProgressPanel form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    private static final String ID_PROGRESS_BAR = "progressbar";    // javax.swing.JProgressBar
    private FormPanel           formPanel;

    /* Helpers for managing the Abeille view */
    private FormAccessor mainViewAccessor;
    private Component    nestedForm;
    private JProgressBar progressBar;

    public ProgressView(String headerdescription) {

        // Initialize the Abeille form
        formPanel = new FormPanel("org/mbari/aved/ui/forms/Progress.xml");
        this.setContentPane(formPanel);

        // Initialize components for setting/getting
        progressBar      = formPanel.getProgressBar(ID_PROGRESS_BAR);
        mainViewAccessor = formPanel.getFormAccessor("main.form");
        nestedForm       = findNestedForm();

        // Initialize components for setting/getting
        JLabel header = formPanel.getLabel(ID_HEADER_LABEL);

        // Set.getName() of display and size it
        header.setText(headerdescription);
        setTitle(ApplicationInfo.getName() + "-" + headerdescription);
        this.pack();
        this.setVisible(true);
    }

    public void setValue(int progress) {
        progressBar.setIndeterminate(false);
        progressBar.setValue(progress);
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
            this.setResizable(false);
        }

        // TODO: throw exception here if no nestedpanel to replace
    }
}
