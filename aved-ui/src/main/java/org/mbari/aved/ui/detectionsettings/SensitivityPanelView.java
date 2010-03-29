/*
 * @(#)SensitivityPanelView.java
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

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import java.beans.PropertyChangeListener;

import java.util.*;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

public class SensitivityPanelView extends JFrameView {
    public static final String ID_CLOSE_BUTTON        = "close";                 // javax.swing.JButton
    public static final String ID_FRAMESIZE_COMBOBOX  = "inputframesize";        // javax.swing.JComboBox
    public static final String ID_MAX_EVENT_SLIDER    = "maxeventareaslider";    // javax.swing.JSlider
    public static final String ID_MAX_EVENT_TEXTFIELD = "maxeventtextfield";     // javax.swing.JFormattedTextField

    // Component names in the DetectionEventSensitivity form
    public static final String ID_MIN_EVENT_SLIDER      = "mineventareaslider";    // javax.swing.JSlider
    public static final String ID_MIN_EVENT_TEXTFIELD   = "mineventtextfield";     // javax.swing.JFormattedTextField
    public static final String ID_RESET_DEFAULTS_BUTTON = "reset";                 // javax.swing.JButton

    // Frame sizes. If you add more frame sizes here, make sure they are in the format wxh
    private static final String[] INPUT_FRAME_SIZES = {
        "740x480", "640x480", "320x240", "640x360", "320x180", "720x576", "1440x1080", "1920x1080"
    };
    private JSlider               myMaxSlider;

    // Frequently accessed components
    private JSlider             myMinSlider;
    private JFormattedTextField mySliderMaxTextField;
    private JFormattedTextField mySliderMinTextField;

    public SensitivityPanelView(DetectionSettingsModel model, SensitivityPanelController controller) {
        super("org/mbari/aved/ui/forms/DetectionEventSensitivity.xml", model, controller);

        ActionHandler actionHandler = this.getActionHandler();

        // Add action handler to panel button and combo boxes
        getForm().getComboBox(ID_FRAMESIZE_COMBOBOX).addActionListener(actionHandler);
        getForm().getButton(ID_CLOSE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_RESET_DEFAULTS_BUTTON).addActionListener(actionHandler);

        // Populate and set the default index for the frame size combo box
        JComboBox cb = getForm().getComboBox(ID_FRAMESIZE_COMBOBOX);

        cb.setModel(new DefaultComboBoxModel(INPUT_FRAME_SIZES));
        cb.setSelectedIndex(0);

        // Initialize frequently accesssed components
        mySliderMinTextField = (JFormattedTextField) getForm().getComponentByName(ID_MIN_EVENT_TEXTFIELD);
        mySliderMaxTextField = (JFormattedTextField) getForm().getComponentByName(ID_MAX_EVENT_TEXTFIELD);
        loadModel(model);
    }

    public void loadModel(DetectionSettingsModel model) {
        initializeSliders();
        myMinSlider.setValue(model.getMinEventArea());
        myMaxSlider.setValue(model.getMaxEventArea());
        mySliderMaxTextField.setValue(new Integer(model.getMaxEventArea()));
        mySliderMinTextField.setValue(new Integer(model.getMinEventArea()));
    }

    public void initializeSliders() {

        // Create text field and formatter for max/min slider text fields
        java.text.NumberFormat numberFormat = java.text.NumberFormat.getIntegerInstance();

        // Set range of min slider between 0 and (smaller of .1% of framearea or 750)
        int min = 0;
        int max = (int) ((.001f) * getInputFrameArea());

        if (max > 1000) {
            max = 750;
        }

        myMinSlider = (JSlider) getForm().getComponentByName(ID_MIN_EVENT_SLIDER);
        myMinSlider.setMaximum(max);
        myMinSlider.setMinimum(min);
        myMinSlider.setMajorTickSpacing((max - min) / 5);
        myMinSlider.setMinorTickSpacing((max - min) / 20);
        myMinSlider.setSnapToTicks(false);
        myMinSlider.setPaintLabels(true);

        // set the formatter for the associated text field
        NumberFormatter formattermin = new NumberFormatter(numberFormat);

        formattermin.setMinimum(new Integer(min));
        formattermin.setMaximum(new Integer(max));
        mySliderMinTextField.setFormatterFactory(new DefaultFormatterFactory(formattermin));

        // Set range of max slider between min slider max and (smaller of 5% of framearea or 30000)
        min = max;
        max = (int) ((.05f) * getInputFrameArea());

        if (max > 30000) {
            max = 30000;
        }

        myMaxSlider = (JSlider) getForm().getComponentByName(ID_MAX_EVENT_SLIDER);
        myMaxSlider.setMaximum(max);
        myMaxSlider.setMinimum(min);
        myMaxSlider.setMajorTickSpacing((max - min) / 5);
        myMaxSlider.setPaintLabels(true);
        myMaxSlider.setSnapToTicks(false);

        // set the formatter for the associated text field
        NumberFormatter formattermax = new NumberFormatter(numberFormat);

        formattermax.setMinimum(new Integer(min));
        formattermax.setMaximum(new Integer(max));
        mySliderMaxTextField.setFormatterFactory(new DefaultFormatterFactory(formattermax));
    }

    private int getInputFrameArea() {
        JComboBox cb = getForm().getComboBox(ID_FRAMESIZE_COMBOBOX);
        String    s  = (String) cb.getSelectedItem();
        int       w  = 0;
        int       h  = 0;

        // Create string tokenizer with delimter 'x' and don't return the delimters as tokens
        StringTokenizer st = new StringTokenizer(s, "x", false);

        // First two tokens are the width and height
        w = Integer.valueOf(st.nextToken());
        h = Integer.valueOf(st.nextToken());

        return w * h;
    }

    public void addSliderChangeListeners(ChangeListener controller) {
        myMinSlider.addChangeListener(controller);
        myMaxSlider.addChangeListener(controller);
    }

    public void addSliderTextPropertyChangeListeners(PropertyChangeListener listener) {
        mySliderMaxTextField.addPropertyChangeListener(listener);
        mySliderMinTextField.addPropertyChangeListener(listener);
    }

    public JFormattedTextField getMaxSliderTextField() {
        return mySliderMaxTextField;
    }

    public JFormattedTextField getMinSliderTextField() {
        return mySliderMinTextField;
    }

    public JSlider getMaxSlider() {
        return myMaxSlider;
    }

    public JSlider getMinSlider() {
        return myMinSlider;
    }

    public void modelChanged(ModelEvent event) {

        // TODO Auto-generated method stub
    }

    public void actionPerformed(ActionEvent e) {}
}
