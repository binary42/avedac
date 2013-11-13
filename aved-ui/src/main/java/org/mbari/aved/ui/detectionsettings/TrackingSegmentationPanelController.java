/*
 * @(#)TrackingSegmentationPanelController.java
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
