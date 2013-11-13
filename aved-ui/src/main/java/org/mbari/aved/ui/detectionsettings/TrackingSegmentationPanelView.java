/*
 * @(#)TrackingSegmentationPanelView.java
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

import org.mbari.aved.mbarivision.api.MbarivisionOptions;
import org.mbari.aved.ui.appframework.Controller;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

public class TrackingSegmentationPanelView extends JFrameView {
    public static final String ID_CACHESIZE_TEXTFIELD             = "cachesize";    // javax.swing.JFormattedTextField
    public static final String ID_CLOSE_BUTTON                    = "close";           // javax.swing.JButton
    public static final String ID_RESET_DEFAULTS_BUTTON           = "reset";           // javax.swing.JButton
    public static final String ID_SEGMENTATION_ALGORITHM_COMBOBOX = "segmentation";    // javax.swing.JComboBox

    // Component names in the DetectionSettingsTrackingAndSegmentation form
    public static final String ID_TRACKING_ALGORITHM_COMBOBOX = "tracking";    // javax.swing.JComboBox
    public static final int    MAX_CACHE                      = 120;

    // Minimum and maximum cache size. These are arbitrarily set, but the cache should
    // at least be 1, and 120 is 4 seconds for a 30 fps input source
    public static final int MIN_CACHE = 1;

    // Frequently accessed components
    private JFormattedTextField myCacheSize = null;

    public TrackingSegmentationPanelView(DetectionSettingsModel model, Controller controller) {
        super("org/mbari/aved/ui/forms/DetectionSettingsTrackingAndSegmentation.xml", model, controller);

        ActionHandler actionHandler = getActionHandler();

        // Add action handler to panel button and combo boxes
        getForm().getComboBox(ID_TRACKING_ALGORITHM_COMBOBOX).addActionListener(actionHandler);
        getForm().getComboBox(ID_SEGMENTATION_ALGORITHM_COMBOBOX).addActionListener(actionHandler);
        getForm().getButton(ID_CLOSE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_RESET_DEFAULTS_BUTTON).addActionListener(actionHandler);

        // Initialize frequently accesssed components
        myCacheSize = (JFormattedTextField) getForm().getComponentByName(ID_CACHESIZE_TEXTFIELD);

        // Create text field and formatter for the cache settings
        java.text.NumberFormat numberFormat = java.text.NumberFormat.getIntegerInstance();
        NumberFormatter        formatter    = new NumberFormatter(numberFormat);

        formatter.setMinimum(new Integer(MIN_CACHE));
        formatter.setMaximum(new Integer(MAX_CACHE));
        myCacheSize.setFormatterFactory(new DefaultFormatterFactory(formatter));

        // Populate and set the default index for all combo boxes
        JComboBox cb = getForm().getComboBox(ID_TRACKING_ALGORITHM_COMBOBOX);

        cb.setModel(new DefaultComboBoxModel(MbarivisionOptions.TrackingMode.values()));
        cb = getForm().getComboBox(ID_SEGMENTATION_ALGORITHM_COMBOBOX);
        cb.setModel(new DefaultComboBoxModel(MbarivisionOptions.SegmentationAlgorithm.values()));
        loadModel(model);
    }

    public void loadModel(DetectionSettingsModel model) {

        // Set the default index for all combo boxes
        JComboBox cb = getForm().getComboBox(ID_TRACKING_ALGORITHM_COMBOBOX);

        cb.setSelectedItem(model.getTrackingMode());
        cb = getForm().getComboBox(ID_SEGMENTATION_ALGORITHM_COMBOBOX);
        cb.setSelectedItem(model.getSegmentationAlgorithm());

        // Set selection for cache size
        myCacheSize.setValue(new Integer(model.getCacheSize()));
    }

    public void modelChanged(ModelEvent event) {

        // TODO Auto-generated method stub
    }

    public JFormattedTextField getCacheTextField() {
        return myCacheSize;
    }
}
