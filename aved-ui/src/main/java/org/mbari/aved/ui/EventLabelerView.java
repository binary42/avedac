/*
 * @(#)EventLabelerView.java
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

import com.google.inject.Injector;
import javax.swing.JButton;

import org.jdesktop.swingx.JXTree;

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.classifier.knowledgebase.KnowledgeBaseUtil;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

import vars.ToolBelt;

import vars.knowledgebase.Concept;
import vars.knowledgebase.ui.Lookup;

import vars.shared.ui.tree.ConceptTreeCellRenderer;
import vars.shared.ui.tree.ConceptTreeModel;
import vars.shared.ui.tree.ConceptTreeNode;
import vars.shared.ui.tree.ConceptTreePanel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.MouseListener;

import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

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
    private static final String ID_SPECIES_NAME_COMBO     = "speciesname";
    private static final String ID_TAG_COMBO              = "tag";

    /** Frequently accessed components */
    private JComboBox        classComboBox, speciesClassComboBox;
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

        speciesClassComboBox = getForm().getComboBox(ID_SPECIES_NAME_COMBO);
        classComboBox        = getForm().getComboBox(ID_CLASS_NAME_COMBO);
        classComboBox.setModel(new DefaultComboBoxModel(list.toArray()));
        classComboBox.addActionListener(actionHandler);
        classComboBox.setSelectedIndex(-1);
        getForm().getButton(ID_APPLY_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_CLOSE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_CLASS_BROWSE_KB_BUTTON).addActionListener(actionHandler);


        this.getRootPane().setDefaultButton((JButton) getForm().getButton(ID_APPLY_BUTTON));
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
        if ((conceptTreePanel != null)) {
            JTree    tree          = conceptTreePanel.getJTree();
            TreePath selectionPath = tree.getSelectionPath();

            if (selectionPath != null) {
                ConceptTreeNode node    = (ConceptTreeNode) selectionPath.getLastPathComponent();
                Concept         concept = (Concept) node.getUserObject();

                return concept.getPrimaryConceptName().getName();
            }
        }

        return null;
    }

    /**
     * Override the view dispose to also close the conceptTreePanel
     */
    @Override
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
     * Sets the selected item in the speciesClassName combo box display area
     * to the object in the argument.
     *
     * @param speciesClassName  the object to display
     */
    void setSpeciesCombo(String speciesClassName) {
        speciesClassComboBox.setSelectedItem(speciesClassName);
    }

    public void displayConceptTree() {
        MouseListener listener = (MouseListener) getController();

        // TODO: put check for knowledge base existence
        if (KnowledgeBaseUtil.isKnowledgebaseAvailable()) {
            Injector injector = (Injector) Lookup.getGuiceInjectorDispatcher().getValueObject();
            ToolBelt toolBelt = injector.getInstance(ToolBelt.class);

            conceptTreePanel = new ConceptTreePanel(toolBelt.getKnowledgebaseDAOFactory());

            final ConceptTreeModel treeModel = new ConceptTreeModel(toolBelt.getKnowledgebaseDAOFactory());
            JXTree                 tree      = new JXTree(treeModel);

            tree.setCellRenderer(new ConceptTreeCellRenderer());
            conceptTreePanel.setJTree(tree);
            conceptTreePanel.setOpaque(true);

            // Only build the conceptTreePanel when the window is opened
            // conceptTreePanel.buildPanel();
            conceptTreeFrame = new JFrame(ApplicationInfo.getName() + " - " + "VARS Knowledge Base Lookup");
            conceptTreeFrame.setContentPane(conceptTreePanel);
            conceptTreeFrame.setFocusable(true);
            conceptTreeFrame.setSize(400, 600);
            conceptTreeFrame.setVisible(true);
            conceptTreePanel.setVisible(true);
            conceptTreePanel.addMouseListener(listener);
        }
    }

    public void modelChanged(ModelEvent event) {}
}
