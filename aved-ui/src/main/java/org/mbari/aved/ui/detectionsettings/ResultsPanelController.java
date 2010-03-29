/*
 * @(#)ResultsPanelController.java
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

import org.mbari.aved.mbarivision.api.MbarivisionOptions;
import org.mbari.aved.mbarivision.api.MbarivisionOptions.MarkEventStyle;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class ResultsPanelController extends DetectionAbstractController {
    private JFrame myParentFrame = null;

    public ResultsPanelController(JFrame parent, DetectionSettingsModel model) {
        myParentFrame = parent;
        setModel(model);
        setView(new ResultsPanelView(model, this));
    }

    private void browseForEventImageDirectory() {
        DetectionSettingsModel model   = ((DetectionSettingsModel) getModel());
        JFileChooser           chooser = new JFileChooser();

        // Add a custom file filter and disable the default
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Set chooser to last setting in model
        chooser.setCurrentDirectory(model.getEventImageDir());
        chooser.setDialogTitle("Choose directory to store event-centered images to");

        if (chooser.showOpenDialog(getView()) == JFileChooser.APPROVE_OPTION) {
            model.setEventImageDirectory(chooser.getSelectedFile());

            if (getView() != null) {
                getView().getEventImagesComboBox();
            }
        } else {
            System.out.println("No Selection ");
        }
    }

    public void addEventComboBox(File f) {
        getModel().setEventImageDirectory(f);

        if (f != null) {
            JComboBox cb = getView().getEventImagesComboBox();

            if (cb.getItemCount() == 0) {
                cb.addItem(f);
            } else {
                int     num   = cb.getItemCount();
                boolean found = false;

                for (int i = 0; i < num; i++) {
                    String s = ((File) cb.getItemAt(i)).toString();

                    if (s.equals(f.toString())) {
                        found = true;

                        break;
                    }
                }

                if (!found) {
                    cb.insertItemAt(f, 0);
                    cb.setSelectedIndex(0);
                }
            }
        }
    }

    public ResultsPanelView getView() {
        return (ResultsPanelView) super.getView();
    }

    public DetectionSettingsModel getModel() {
        return ((DetectionSettingsModel) super.getModel());
    }

    /**
     * Operation handler for handling actions initiated in the view
     *
     * @param actionCommand A semantic event which indicates that a
     * component-defined action occurred.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        Object obj           = e.getSource();

        // System.out.println(actionCommand);
        if (actionCommand.equals("Browse")) {
            browseForEventImageDirectory();
        } else if (actionCommand.equals("EventImageDirComboBoxChanged") && (getModel() != null)
                   && (getView() != null)) {
            JComboBox cb = (JComboBox) obj;

            addEventComboBox((File) cb.getSelectedItem());
        } else if (actionCommand.equals("MarkEventStyleComboBoxChanged") && (getModel() != null)) {
            getModel().setMarkEventStyle((MarkEventStyle) ((JComboBox) obj).getSelectedItem());
        } else if (actionCommand.equals("CreateMPEG") && (getView() != null)) {
            JTextField f = getView().getMpegTextField();

            // If a valid description in MPEG file: field update model
            if ((f.getText() != "") && ((JCheckBox) obj).isSelected()) {
                getModel().setMpeg(new File(f.getText()));
            } else {
                getModel().disableSaveMpeg();
            }
        } else if (actionCommand.equals("WriteEventLabels")) {
            getModel().setEventLabels(((JCheckBox) obj).isSelected());
        } else if (actionCommand.equals("SaveXMLResults")) {
            JTextField f = getView().getMpegTextField();

            if ((f.getText() != "") && ((JCheckBox) obj).isSelected()) {
                getModel().setSaveXML(new File(f.getText()));
            } else {
                getModel().disableSaveXML();
            }
        } else if (actionCommand.equals("SaveTextSummary") && (getView() != null) && (getModel() != null)) {
            JTextField f = getView().getResultsSummaryTextField();

            if ((f.getText() != "") && ((JCheckBox) obj).isSelected()) {
                getModel().setSaveTextSummary(new File(f.getText()));
            } else {
                getModel().disableTextSummary();
            }
        } else if (actionCommand.equals("SaveEventImages") && (getModel() != null)) {
            if (((JCheckBox) obj).isSelected()) {
                File f = getModel().getEventImageDir();

                if (f == null) {

                    // TODO: display error message here about setting the directory
                    getModel().setSaveEventImages(false);
                } else if (!f.isDirectory()) {

                    // TODO: display error message here about setting the directory
                    getModel().setSaveEventImages(false);
                } else {
                    getModel().setSaveEventImages(true);
                }
            }
        } else if (actionCommand.equals("SaveInterestingEventsOnly")) {
            if (((JRadioButton) obj).isSelected() && (getModel() != null)) {
                getModel().setSaveOnlyInterestingEvents(true);
            }
        } else if (actionCommand.equals("SaveNonInterestingToo")) {
            if (((JRadioButton) obj).isSelected()) {
                getModel().setSaveOnlyInterestingEvents(false);
            }
        } else if (actionCommand.equals("MarkInterestingCandidates")) {
            getModel().markInterestingCandidates(((JCheckBox) obj).isSelected());
        } else if (actionCommand.equals("ResetDefaults")) {
            DetectionSettingsModel snapshot = getSnapshot();

            getView().loadModel(snapshot);
            getModel().setEventImageDirectory(snapshot.getEventImageDir());
            getModel().setMarkEventStyle(snapshot.getMarkEventStyle());
            getModel().setEventLabels(snapshot.isWriteEventLabels());

            if (snapshot.isCreateMpeg()) {
                getModel().setMpeg(snapshot.getMpeg());
            } else {
                getModel().disableSaveMpeg();
            }

            if (getModel().isSaveEventsXML()) {
                getModel().setSaveXML(snapshot.getXMLFile());
            } else {
                getModel().disableSaveXML();
            }

            if (snapshot.isSaveEventTextSummary()) {
                getModel().setSaveTextSummary(snapshot.getSummaryFile());
            } else {
                getModel().disableTextSummary();
            }

            getModel().setSaveOnlyInterestingEvents(snapshot.isSaveOnlyInteresting());
            getModel().markInterestingCandidates(snapshot.isMarkCandidates());
        } else if (actionCommand.equals("Close")) {
            myParentFrame.setVisible(false);
        } else {

            // TODO: display error message here to console
        }
    }
}
