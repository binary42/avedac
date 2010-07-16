/*
 * @(#)ClassifierView.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.components.panel.FormPanel;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.event.ChangeEvent;

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 *
 * @author dcline
 */
public class ClassifierView extends JFrameView {

    /** TabbedView indexes */
    private final static int CREATE_CLASS_PANEL_INDEX = 0;
    private final static int RUN_PANEL_INDEX          = 3;
    private final static int TEST_CLASS_PANEL_INDEX   = 2;
    private final static int TRAINING_PANEL_INDEX     = 1;

    ClassifierView(ClassifierModel model, ClassifierController controller) {
        super("org/mbari/aved/ui/forms/Classifier.xml", model, controller);

        // TODO: make this a function of screen size
        Dimension size = new Dimension(900, 480);
        this.setMinimumSize(size);
        this.setResizable(true);
        
        final JTabbedPane tabs = getTabbedPane();

        final Dimension originalTabsDim = tabs.getPreferredSize();

        tabs.addChangeListener(new ChangeListener()  {

            public void stateChanged(ChangeEvent e) {
                Component p = ((JTabbedPane) e.getSource()).getSelectedComponent();
                Dimension panelDim = p.getPreferredSize();

                Dimension nd = new Dimension(
                        originalTabsDim.width - panelDim.width,
                        originalTabsDim.height - panelDim.height);

                tabs.setPreferredSize(originalTabsDim);
                pack();
            }
        });
 
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
        ;
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
     * Gets the tabbed pane in the form
     * @return the tabbed pane
     */
    private JTabbedPane getTabbedPane() {
        FormPanel   panel = getForm();
        JTabbedPane c     = panel.getTabbedPane("tabbedPane");

        return c;
    }
}
