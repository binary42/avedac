/*
 * @(#)TrackingSegmentationPanelController.java   10/03/17
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

import org.mbari.aved.mbarivision.api.MbarivisionOptions.SegmentationAlgorithm;
import org.mbari.aved.mbarivision.api.MbarivisionOptions.TrackingMode;
import org.mbari.aved.ui.appframework.AbstractController;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

public class TrackingSegmentationPanelController extends DetectionAbstractController implements PropertyChangeListener {
    private JFrame myParentFrame = null;

    public TrackingSegmentationPanelController(JFrame view, DetectionSettingsModel model) {
        myParentFrame = view;
        setModel(model);
        setView(new TrackingSegmentationPanelView(model, this));

        // Implement key enter check on text field
        Action checkcachesize = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JFormattedTextField f = (JFormattedTextField) (e.getSource());

                if (!f.isEditValid()) {    // The text is invalid.
                    Toolkit.getDefaultToolkit().beep();
                    f.selectAll();
                } else {
                    try {
                        f.commitEdit();
                    } catch (java.text.ParseException exc) {}
                }
            }
        };

        // Add action handler for enter to slider text field
        getView().getCacheTextField().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        getView().getCacheTextField().getActionMap().put("check", checkcachesize);
        getView().getCacheTextField().addPropertyChangeListener(this);
    }

    public TrackingSegmentationPanelView getView() {
        return (TrackingSegmentationPanelView) super.getView();
    }

    public DetectionSettingsModel getModel() {
        return (DetectionSettingsModel) super.getModel();
    }

    public void propertyChange(PropertyChangeEvent e) {
        if ("value".equals(e.getPropertyName())) {
            Number value = (Number) e.getNewValue();

            getModel().setCacheSize(value.intValue());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("Close")) {
            myParentFrame.setVisible(false);
        } else if (actionCommand.equals("ResetDefaults")) {

            // Delegate this back to the view to reload its model
            if (getView() != null) {
                DetectionSettingsModel snapshot = getSnapshot();

                getView().loadModel(snapshot);
                getModel().setCacheSize(snapshot.getCacheSize());
                getModel().setSegmentationAlgorithm(snapshot.getSegmentationAlgorithm());
                getModel().setTrackingMode(snapshot.getTrackingMode());
            }
        } else if (actionCommand.equals("SegmentationComboBoxChanged")) {
            getModel().setSegmentationAlgorithm((SegmentationAlgorithm) ((JComboBox) e.getSource()).getSelectedItem());
        } else if (actionCommand.equals("TrackingComboBoxChanged")) {
            getModel().setTrackingMode((TrackingMode) ((JComboBox) e.getSource()).getSelectedItem());
        }
    }
}
