/*
 * @(#)EventLabelerView.java
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

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.classifier.ConceptTreePanel;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.MouseListener;

import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;

/**
 *
 * @author dcline
 */
public class EventLabelerView extends JFrameView {
    private static final String ID_APPLY_BUTTON = "apply";

    /*
     * Component names in the EventLabelerView form If any of the component name
     * are changed in the Abeille form designer, they should be modified here
     * too
     */
    private static final String ID_CLASS_BROWSE_KB_BUTTON = "browseknowledgebase";
    private static final String ID_CLASS_NAME_COMBO       = "classname";
    private static final String ID_CLOSE_BUTTON           = "close";
    private static final String ID_IDENTITY_REF_COMBO     = "identityreference";
    private static final String ID_TAG_COMBO              = "tag";

    /** Frequently accessed components */
    private JComboBox        classComboBox, predictedClassComboBox;
    private JFrame           conceptTreeFrame;
    private ConceptTreePanel conceptTreePanel;

    EventLabelerView(EventListModel model, EventLabelerController controller) {
        super("org/mbari/aved/ui/forms/EventLabeler.xml", model, controller);

        ActionHandler actionHandler = getActionHandler();

        // Create and set up the content pane.
        ArrayList<String> userTagList = UserPreferences.getModel().getTagList();
        JComboBox         tagComboBox = getForm().getComboBox(ID_TAG_COMBO);

        tagComboBox.setModel(new DefaultComboBoxModel(userTagList.toArray()));
        tagComboBox.addActionListener(actionHandler);
        tagComboBox.setSelectedIndex(-1);

        // Create and set up the content pane.
        ArrayList<String> userIdList = UserPreferences.getModel().getIdList();
        JComboBox         idComboBox = getForm().getComboBox(ID_IDENTITY_REF_COMBO);

        idComboBox.setModel(new DefaultComboBoxModel(userIdList.toArray()));
        idComboBox.addActionListener(actionHandler);
        idComboBox.setSelectedIndex(-1);

        // Create and set up the content pane.
        ArrayList<String> list = UserPreferences.getModel().getClassNameList();

        classComboBox = getForm().getComboBox(ID_CLASS_NAME_COMBO);
        classComboBox.setModel(new DefaultComboBoxModel(list.toArray()));
        classComboBox.addActionListener(actionHandler);
        classComboBox.setSelectedIndex(-1);
        getForm().getButton(ID_APPLY_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_CLOSE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_CLASS_BROWSE_KB_BUTTON).addActionListener(actionHandler);
        this.addMouseListener((MouseListener) controller);
        this.pack();
        this.setResizable(false);
    }

    /**
     * Gets the selected concept tree name.
     * This returns the selected concept from the VARS knowledge base.
     * @return the concept tree name
     */
    String getSelectedConceptName() {
        if (conceptTreePanel != null) {
            return conceptTreePanel.getSelectedConceptName();
        }

        return null;
    }

    /**
     * Override the view dispose to also close the conceptTreePanel
     */
    public void dispose() {
        if (conceptTreeFrame != null) {
            conceptTreeFrame.dispose();
        }

        super.dispose();
    }

    /**
     * Sets the selected item in the class combo box display area
     * to the object in the argument.
     *
     * @param className  the object to display
     */
    void setClassCombo(String className) {
        classComboBox.setSelectedItem(className);
    }

    /**
     * Sets the selected item in the predictedClassName combo box display area
     * to the object in the argument.
     *
     * @param predictedClassName  the object to display
     */
    void setSpeciesCombo(String predictedClassName) {
        predictedClassComboBox.setSelectedItem(predictedClassName);
    }

    public void displayConceptTree() {
        MouseListener listener = (MouseListener) getController();

        // TODO: put check for knowledge base existence
        conceptTreePanel = new ConceptTreePanel(listener);

        // Only build the conceptTreePanel when the window is opened
        conceptTreePanel.buildPanel();
        conceptTreeFrame = new JFrame(ApplicationInfo.getName() + " - " + "VARS Knowledge Base Lookup");
        conceptTreeFrame.setContentPane(conceptTreePanel);
        conceptTreeFrame.setFocusable(true);
        conceptTreeFrame.setSize(400, 600);
        conceptTreeFrame.setVisible(true);
        conceptTreePanel.setVisible(true);
        conceptTreePanel.addMouseListener(listener);
    }

    public void modelChanged(ModelEvent event) {}
}
