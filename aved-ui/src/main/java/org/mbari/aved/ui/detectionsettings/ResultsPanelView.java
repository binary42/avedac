/*
 * @(#)ResultsPanelView.java
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

import org.mbari.aved.mbarivision.api.MbarivisionOptions;
import org.mbari.aved.ui.appframework.Controller;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.Model;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

public class ResultsPanelView extends JFrameView {
    public static final String ID_CLOSE_BUTTON = "close";    // javax.swing.JButton

    // Component names in the DetectionSettingsResults form
    public static final String ID_CREATE_MPEG_CHECKBOX                 = "creatempeg";    // javax.swing.JCheckBox
    public static final String ID_MARK_EVENT_STYLE_COMBOBOX            = "markeventstyle";    // javax.swing.JComboxBox
    public static final String ID_MARK_INTERESTING_CANDIDATES_CHECKBOX = "markinterestingcandidates";    // javax.swing.JCheckBox
    public static final String ID_MPEG_FILE_TEXTFIELD                           = "mpegfile";    // javax.swing.JFormattedTextField
    public static final String ID_RESET_BUTTON                                  = "reset";    // javax.swing.JButton
    public static final String ID_SAVE_EVENT_IMAGES_BROWSE_BUTTON               = "browse";    // javax.swing.JButton
    public static final String ID_SAVE_EVENT_IMAGES_CHECKBOX                    = "saveeventimages";    // javax.swing.JCheckBox
    public static final String ID_SAVE_EVENT_IMAGES_DIR_COMBOBOX                = "saveeventimagesdir";    // javax.swing.JComboBox
    public static final String ID_SAVE_INTERESTING_N_NONINTERESTING_RADIOBUTTON = "savenoninterestingtoo";    // javax.swing.JRadioButton
    public static final String ID_SAVE_INTERESTING_ONLY_RADIOBUTTON = "saveinterestingonly";    // javax.swing.JRadioButton
    public static final String ID_SAVE_SUMMARY_CHECKBOX             = "savesummary";         // javax.swing.JCheckBox
    public static final String ID_SAVE_SUMMARY_FILE_TEXTFIELD       = "savesummaryfile";    // javax.swing.JFormattedTextField
    public static final String ID_SAVE_XML_CHECKBOX                 = "savexml";             // javax.swing.JCheckBox
    public static final String ID_SAVE_XML_FILE_TEXTFIELD           = "savexmlfile";    // javax.swing.JFormattedTextField
    public static final String ID_WRITE_EVENT_LABELS_CHECKBOX       = "writeeventlables";    // javax.swing.JCheckBox

    public ResultsPanelView(Model model, Controller controller) {
        super("org/mbari/aved/ui/forms/DetectionSettingsResults.xml", model, controller);

        ActionHandler actionHandler = getActionHandler();

        // DetectionResults panel button and combo boxes
        getForm().getButton(ID_RESET_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_CLOSE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_SAVE_EVENT_IMAGES_BROWSE_BUTTON).addActionListener(actionHandler);
        getForm().getComboBox(ID_SAVE_EVENT_IMAGES_DIR_COMBOBOX).addActionListener(actionHandler);
        getForm().getComboBox(ID_MARK_EVENT_STYLE_COMBOBOX).addActionListener(actionHandler);
        getForm().getCheckBox(ID_CREATE_MPEG_CHECKBOX).addActionListener(actionHandler);
        getForm().getCheckBox(ID_SAVE_SUMMARY_CHECKBOX).addActionListener(actionHandler);
        getForm().getCheckBox(ID_SAVE_EVENT_IMAGES_CHECKBOX).addActionListener(actionHandler);
        getForm().getCheckBox(ID_MARK_INTERESTING_CANDIDATES_CHECKBOX).addActionListener(actionHandler);
        getForm().getCheckBox(ID_WRITE_EVENT_LABELS_CHECKBOX).addActionListener(actionHandler);
        getForm().getComboBox(ID_MARK_EVENT_STYLE_COMBOBOX).addActionListener(actionHandler);
        getForm().getCheckBox(ID_SAVE_XML_CHECKBOX).addActionListener(actionHandler);
        getForm().getCheckBox(ID_SAVE_XML_CHECKBOX).addActionListener(actionHandler);
        getForm().getButton(ID_SAVE_INTERESTING_ONLY_RADIOBUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_SAVE_INTERESTING_N_NONINTERESTING_RADIOBUTTON).addActionListener(actionHandler);
        loadModel((DetectionSettingsModel) model);
    }

    public void loadModel(DetectionSettingsModel model) {

        // Initialize preferences from previous application instance
        UserPreferencesModel prefs = UserPreferences.getModel();

        model.setEventImageDirectory(prefs.getEventImageDirectory());

        // Insert the last user selected directory for event images into the combobox
        // TODO: check if valid directory before putting in box as selection
        JComboBox cb = getForm().getComboBox(ID_SAVE_EVENT_IMAGES_DIR_COMBOBOX);

        if (cb.getItemCount() == 0) {
            cb.addItem(prefs.getEventImageDirectory());
        } else {
            cb.insertItemAt(prefs.getEventImageDirectory(), 0);
            cb.setSelectedIndex(0);
        }

        // Populate and set the default index for the event style combo box
        cb = getForm().getComboBox(ID_MARK_EVENT_STYLE_COMBOBOX);
        cb.setModel(new DefaultComboBoxModel(MbarivisionOptions.SegmentationAlgorithm.values()));
        cb.setSelectedItem(model.getMarkEventStyle());

        JTextField tf;

        // Populate the remaining text fields
        if (model.getMpeg() != null) {
            tf = getForm().getTextField(ID_MPEG_FILE_TEXTFIELD);
            tf.setText(model.getMpeg().toString());
        }

        if (model.getXMLFile() != null) {
            tf = getForm().getTextField(ID_SAVE_XML_FILE_TEXTFIELD);
            tf.setText(model.getXMLFile().toString());
        }

        if (model.getSummaryFile() != null) {
            tf = getForm().getTextField(ID_SAVE_SUMMARY_FILE_TEXTFIELD);
            tf.setText(model.getSummaryFile().toString());
        }

        getForm().getCheckBox(ID_CREATE_MPEG_CHECKBOX).setSelected(model.isCreateMpeg());
        getForm().getCheckBox(ID_WRITE_EVENT_LABELS_CHECKBOX).setSelected(model.isWriteEventLabels());
        getForm().getCheckBox(ID_SAVE_XML_CHECKBOX).setSelected(model.isSaveEventsXML());
        getForm().getCheckBox(ID_SAVE_SUMMARY_CHECKBOX).setSelected(model.isSaveEventTextSummary());
        getForm().getCheckBox(ID_SAVE_EVENT_IMAGES_CHECKBOX).setSelected(model.isSaveEventCenteredImages());

        if (model.isSaveOnlyInteresting() == true) {
            getForm().getRadioButton(ID_SAVE_INTERESTING_ONLY_RADIOBUTTON).setSelected(true);
        } else {
            getForm().getRadioButton(ID_SAVE_INTERESTING_N_NONINTERESTING_RADIOBUTTON).setSelected(true);
        }

        getForm().getCheckBox(ID_MARK_INTERESTING_CANDIDATES_CHECKBOX).setSelected(model.isMarkCandidates());
    }

    public JComboBox getEventImagesComboBox() {
        return getForm().getComboBox(ID_SAVE_EVENT_IMAGES_DIR_COMBOBOX);
    }

    public void modelChanged(ModelEvent event) {
        DetectionSettingsModel model = (DetectionSettingsModel) getModel();
    }

    public JTextField getMpegTextField() {
        return getForm().getTextField(ID_MPEG_FILE_TEXTFIELD);
    }

    public JTextField getXMLFileTextField() {
        return getForm().getTextField(ID_SAVE_XML_FILE_TEXTFIELD);
    }

    public JTextField getResultsSummaryTextField() {
        return getForm().getTextField(ID_SAVE_SUMMARY_FILE_TEXTFIELD);
    }
}
