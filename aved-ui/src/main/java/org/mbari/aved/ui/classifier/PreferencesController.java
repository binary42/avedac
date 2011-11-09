/*
 * @(#)PreferencesController.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;

public class PreferencesController extends AbstractController {
    public PreferencesController(ClassifierModel model) {
        setModel(model);
        setView(new PreferencesView(getModel(), this));
    }

    public PreferencesView getView() {
        return (PreferencesView) super.getView();
    }

    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    /**
     * Creates a browser
     * @param dir sets the current directory to start the browser in
     * @param choosermode sets the mode the chooser should be in
     * @param dialogTitle sets the title of the chooser dialog
     * @return null if none found, otherwise user selection
     */
    private File browse(File dir, int choosermode, String dialogTitle) {
        JFileChooser chooser = new JFileChooser();

        chooser.setFileSelectionMode(choosermode);

        // Initialize the chooser with the model directory
        chooser.setCurrentDirectory(dir);
        chooser.setDialogTitle(dialogTitle);

        if (chooser.showOpenDialog(getView()) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {

            // TODO print error message box
            System.out.println("No Selection ");
        }

        return null;
    }

    public void actionPerformed(ActionEvent e) {
        String op = e.getActionCommand();

        if (op.equals("BrowseTraining")) {
            File newDir = browse(getModel().getClassTrainingImageDirectory(), JFileChooser.DIRECTORIES_ONLY,
                                 "Choose training library directory");

            if (newDir != null) {
                getView().updateTrainingComboBox(new File(newDir.getAbsolutePath()));
                getModel().setClassTrainingImageDirectory(newDir);
            }
        } else if (op.equals("AddTrainingImages")) {
            boolean state = ((JCheckBox) e.getSource()).isSelected();

            UserPreferences.getModel().setAddLabeledTrainingImages(state);
        } else if (op.equals("TrainingDirComboBoxChanged")) {
            JComboBox box    = ((JComboBox) e.getSource());
            File      newDir = (File) box.getSelectedItem();

            getModel().setClassTrainingImageDirectory(newDir);
            getModel().setDatabaseRoot(newDir);
        } else if (op.equals("Close")) {
            getView().setVisible(false);
        } else {
            System.out.println("#############Something unknown changed in PreferencesController: " + op);
        }
    }
}
