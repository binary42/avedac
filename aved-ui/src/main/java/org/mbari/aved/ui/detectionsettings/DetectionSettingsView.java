/*
 * @(#)DetectionSettingsView.java
 * 
 * Copyright 2013 MBARI
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



package org.mbari.aved.ui.detectionsettings;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.components.panel.FormPanel;
import com.jeta.forms.gui.form.FormAccessor;

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Dimension;

import java.util.Iterator;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class DetectionSettingsView extends JFrameView {

    /*
     *  Component names in the DetectionSettings form
     * If any of the component names are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_SETTINGS_TREE       = "DetectionSettingsTree";    // javax.swing.JTree
    public static final String MENU_DETECTION_RESULTS = "Detection Results";

    /* Menu names for left panel menu tree */
    public static final String MENU_SENSITIVITY = "Event Sensitivity";
    public static final String MENU_TRACKING    = "Tracking and Segmentation";
    public static final String MENU_VIDEO_MASK  = "Video Mask";
    private Component          nestedForm;

    /* Helpers for managing the Abeille views */
    private FormAccessor rightPaneAccessor;

    public DetectionSettingsView(DetectionSettingsModel model, DetectionSettingsController controller) {
        super("org/mbari/aved/ui/forms/DetectionSettings.xml", model, controller);
        setTitle("Detection Settings");
        setContentPane(getForm());

        DefaultTreeModel newModel = createNewTreeModel();

        getLeftMenuTree().setModel(newModel);

        // Set default size and title
        setTitle("AVED Settings");
        this.pack();

        Dimension dims = this.getSize();

        dims.width = 1200;
        this.setSize(dims);
        this.setResizable(true);
        rightPaneAccessor = getForm().getFormAccessor("right.panel");
        nestedForm        = findNestedForm();
        loadModel(model);
    }

    public void loadModel(DetectionSettingsModel model) {

        // Initialize preferences from previous application instance
        // UserPreferencesModel prefs = UserPreferences.getModel();
        // The model loading happens in the individual panels when created.
        // provide public method here in case external class needs to control loading
    }

    public JTree getLeftMenuTree() {
        return getForm().getTree(ID_SETTINGS_TREE);
    }

    /**
     * Builds the side bar menus
     */
    private DefaultTreeModel createNewTreeModel() {
        DefaultMutableTreeNode parent           = new DefaultMutableTreeNode("Settings");
        DefaultMutableTreeNode eventResultsNode = new DefaultMutableTreeNode(MENU_DETECTION_RESULTS);

        parent.add(eventResultsNode);

        DefaultMutableTreeNode eventTrackingNode = new DefaultMutableTreeNode(MENU_TRACKING);

        parent.add(eventTrackingNode);

        DefaultMutableTreeNode eventSensitivityNode = new DefaultMutableTreeNode(MENU_SENSITIVITY);

        parent.add(eventSensitivityNode);

        DefaultMutableTreeNode videoMaskNode = new DefaultMutableTreeNode(MENU_VIDEO_MASK);

        parent.add(videoMaskNode);

        DefaultTreeModel newmodel = new DefaultTreeModel(parent);

        return newmodel;
    }

    private Component findNestedForm() {
        Iterator iter = rightPaneAccessor.beanIterator();

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

    public void replaceRightPanel(FormPanel panel) {
        if (nestedForm != null) {
            rightPaneAccessor.replaceBean(nestedForm, panel);
            nestedForm = panel;
        }
    }

    public void modelChanged(ModelEvent event) {
        DetectionSettingsModel model = (DetectionSettingsModel) getModel();
    }
}
