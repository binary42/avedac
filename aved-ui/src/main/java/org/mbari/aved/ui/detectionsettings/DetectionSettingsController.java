/*
 * @(#)DetectionSettingsController.java   10/03/17
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



package org.mbari.aved.ui.detectionsettings;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.components.panel.FormPanel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

public class DetectionSettingsController extends DetectionAbstractController {
    ResultsPanelController              detectionResultsController;
    SensitivityPanelController          sensitivityController;
    TrackingSegmentationPanelController trackingSegmentationController;
    VideoMaskPanelController            videoMaskController;

    public DetectionSettingsController() {

        // Create common model for all controllers/views to use
        DetectionSettingsModel model = new DetectionSettingsModel();
        DetectionSettingsView  view  = new DetectionSettingsView(model, this);

        // Need to create the model and view before creating the controllers
        // Need to create all the controllers before setting the model
        sensitivityController          = new SensitivityPanelController(view, model);
        videoMaskController            = new VideoMaskPanelController(view, model);
        trackingSegmentationController = new TrackingSegmentationPanelController(view, model);
        detectionResultsController     = new ResultsPanelController(view, model);
        setView(view);
        setModel(model);

        // Add action listeners for the view menu
        getView().getLeftMenuTree().addTreeSelectionListener(new MenuAction());

        // Replace the view with the detection results. This should be the first menu
        // item listed in the left panel.
        getView().replaceRightPanel(getPanel(DetectionSettingsView.MENU_DETECTION_RESULTS));
    }

    /** Helper function to type cast the view */
    public DetectionSettingsView getView() {
        return (DetectionSettingsView) super.getView();
    }

    private FormPanel getPanel(String description) {
        if (description.equals(DetectionSettingsView.MENU_SENSITIVITY)) {
            return getSensitivityPanel();
        } else if (description.equals(DetectionSettingsView.MENU_VIDEO_MASK)) {
            return getMaskPanel();
        } else if (description.equals(DetectionSettingsView.MENU_TRACKING)) {
            return getTrackingSegmentationPanel();
        } else if (description.equals(DetectionSettingsView.MENU_DETECTION_RESULTS)) {
            return getDetectionResultsPanel();
        } else {
            return getDetectionResultsPanel();
        }
    }

    public void setModel(DetectionSettingsModel model) {
        super.setModel(model);
        sensitivityController.setModel(model);
        videoMaskController.setModel(model);
        trackingSegmentationController.setModel(model);
        detectionResultsController.setModel(model);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Exit")) {
            ((DetectionSettingsView) getView()).setVisible(false);
        }
    }

    public FormPanel getSensitivityPanel() {
        return ((SensitivityPanelView) sensitivityController.getView()).getForm();
    }

    public FormPanel getMaskPanel() {
        return ((VideoMaskPanelView) videoMaskController.getView()).getForm();
    }

    public FormPanel getTrackingSegmentationPanel() {
        return ((TrackingSegmentationPanelView) trackingSegmentationController.getView()).getForm();
    }

    public FormPanel getDetectionResultsPanel() {
        return ((ResultsPanelView) detectionResultsController.getView()).getForm();
    }

    // Responds to user selection in tree
    public class MenuAction implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent evt) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) getView().getLeftMenuTree().getLastSelectedPathComponent();

            if (node == null) {
                return;
            }

            if (node.isLeaf()) {
                FormPanel panel = getPanel(node.toString());

                getView().replaceRightPanel(panel);
            }
        }
    }
}
