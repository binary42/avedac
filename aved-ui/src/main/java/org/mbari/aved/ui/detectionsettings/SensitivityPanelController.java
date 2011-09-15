/*
 * @(#)SensitivityPanelController.java
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



package org.mbari.aved.ui.detectionsettings;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.userpreferences.UserPreferencesModel;
import org.mbari.aved.ui.userpreferences.UserPreferencesView;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SensitivityPanelController extends DetectionAbstractController
        implements ChangeListener, PropertyChangeListener {
    private JFrame myParentFrame = null;

    public SensitivityPanelController(JFrame parent, DetectionSettingsModel model) {
        myParentFrame = parent;
        setModel(model);
        setView(new SensitivityPanelView(model, this));
        getView().addSliderChangeListeners(this);
        getView().addSliderTextPropertyChangeListeners(this);

        // Implement key enter check on text field
        Action checkmaxslidertext = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JFormattedTextField tf = (JFormattedTextField) e.getSource();

                if (!tf.isEditValid()) {    // The text is invalid.
                    Toolkit.getDefaultToolkit().beep();
                    tf.selectAll();
                } else {
                    try {
                        tf.commitEdit();
                    } catch (java.text.ParseException exc) {}
                }
            }
        };

        // Implement key enter check on text field
        Action checkminslidertest = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JFormattedTextField tf = (JFormattedTextField) e.getSource();

                if (!tf.isEditValid()) {    // The text is invalid.
                    Toolkit.getDefaultToolkit().beep();
                    tf.selectAll();
                } else {
                    try {
                        tf.commitEdit();
                    } catch (java.text.ParseException exc) {}
                }
            }
        };

        // Add action handler for enter to slider text field
        getView().getMaxSliderTextField().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        getView().getMaxSliderTextField().getActionMap().put("check", checkmaxslidertext);
        getView().getMinSliderTextField().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        getView().getMinSliderTextField().getActionMap().put("check", checkminslidertest);
    }

    public SensitivityPanelView getView() {
        return (SensitivityPanelView) super.getView();
    }

    public DetectionSettingsModel getModel() {
        return (DetectionSettingsModel) super.getModel();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("Close")) {
            myParentFrame.setVisible(false);
        } else if (actionCommand.equals("ResetDefaults") && (getView() != null)) {

            // Delegate this back to the view to reload its model
            getView().loadModel(getSnapshot());
        } else if (actionCommand.equals("InputFrameComboBoxChanged") && (getView() != null)) {
            getView().initializeSliders();
        }
    }

    public void stateChanged(ChangeEvent e) {
        JSlider s     = (JSlider) e.getSource();
        int     value = s.getValue();

        if (s.getValueIsAdjusting() && (getView() != null) && (getModel() != null)) {    // done adjusting
            if (s == getView().getMaxSlider()) {
                getView().getMaxSliderTextField().setText(String.valueOf(value));
                getModel().setMaxEventArea(value);
            } else if (s == getView().getMinSlider()) {
                getView().getMinSliderTextField().setText(String.valueOf(value));
                getModel().setMinEventArea(value);
            } else {}
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        if ("value".equals(e.getPropertyName())) {
            Number              value = (Number) e.getNewValue();
            JFormattedTextField t     = (JFormattedTextField) e.getSource();

            if (value != null) {
                if (t == getView().getMaxSliderTextField()) {
                    getView().getMaxSlider().setValue(value.intValue());
                    getModel().setMaxEventArea(value.intValue());
                } else if (t == getView().getMinSliderTextField()) {
                    getView().getMinSlider().setValue(value.intValue());
                    getModel().setMinEventArea(value.intValue());
                } else {}
            }
        }
    }
}
