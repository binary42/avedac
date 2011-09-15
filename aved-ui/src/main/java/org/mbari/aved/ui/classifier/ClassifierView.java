/*
 * @(#)ClassifierView.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.components.panel.FormPanel;

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.JTabbedPane;

/**
 *
 * @author dcline
 */
public class ClassifierView extends JFrameView {

    /** TabbedView indexes */
    private final static int CREATE_CLASS_PANEL_INDEX = 0;
    private final static int RUN_PANEL_INDEX          = 3;
    private final static int BATCH_RUN_PANEL_INDEX    = 4;
    private final static int TEST_CLASS_PANEL_INDEX   = 2;
    private final static int TRAINING_PANEL_INDEX     = 1;

    ClassifierView(ClassifierModel model, ClassifierController controller) {
        super("org/mbari/aved/ui/forms/Classifier.xml", model, controller);
        this.setResizable(true);
    }

    public void modelChanged(ModelEvent event) {}

    /**
     * Sets the  Create Class panel
     * @param form the form to replace the panel with
     */
    public void setCreateClassPanel(FormPanel form) {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setComponentAt(CREATE_CLASS_PANEL_INDEX, form);
        }
    }

    /**
     * Sets the  Test Class panel
     * @param form the form to replace the panel with
     */
    public void setTestClassPanel(FormPanel form) {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setComponentAt(TEST_CLASS_PANEL_INDEX, form);
        }
    }

    /**
     * Sets the Create Training Library panel
     *
     * @param form the form to replace the panel with
     */
    public void setTrainingPanel(FormPanel form) {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setComponentAt(TRAINING_PANEL_INDEX, form);
        }
        ;
    }

    void setBatchRunPanel(FormPanel form) {
           JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setComponentAt(BATCH_RUN_PANEL_INDEX, form);
        }
    }

    /**
     * Sets the Run Classifier panel
     *
     * @param form the form to replace the panel with
     */
    public void setRunPanel(FormPanel form) {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setComponentAt(RUN_PANEL_INDEX, form);
        }
    }

    /**
     * Selects the Create Class panel
     */
    public void setClassCreateTabbedViewVisible() {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setSelectedIndex(CREATE_CLASS_PANEL_INDEX);
        }
    }

    /**
     * Selects the Test Class panel
     */
    public void setClassTestTabbedViewVisible() {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setSelectedIndex(TEST_CLASS_PANEL_INDEX);
        }
    }

    /**
     * Selects the Create Training Library panel
     */
    public void setTrainingPanelViewVisible() {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setSelectedIndex(TRAINING_PANEL_INDEX);
        }
    }

    /**
     * Selects the Run Classifier panel
     */
    void setRunPanelVisible() {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setSelectedIndex(RUN_PANEL_INDEX);
        }
    }

    /**
     * Selects the Batch Run Classifier panel
     */
    void setBatchRunPanelVisible() {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setSelectedIndex(BATCH_RUN_PANEL_INDEX);
        }
    }

    /**
     * Gets the tabbed pane in the form
     * @return the tabbed pane
     */
    private JTabbedPane getTabbedPane() {
        FormPanel   panel = getForm();
        JTabbedPane c     = panel.getTabbedPane("tabbedPane");

        return c;
    }

}
