/*
 * @(#)PreferencesController.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.classifier.ClassifierModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

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
        } else if (op.equals("BrowseDbroot")) {
            File newDir = browse(getModel().getDatabaseRoot(), JFileChooser.DIRECTORIES_ONLY,
                                 "Choose class metadata directory");

            try {
                getView().updateDbrootComboBox(newDir);
            } catch (Exception ex) {
                Logger.getLogger(PreferencesController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (op.equals("AddTrainingImages")) {
            boolean state = ((JCheckBox) e.getSource()).isSelected();

            UserPreferences.getModel().setAddLabeledTrainingImages(state);
        } else if (op.equals("TrainingDirComboBoxChanged")) {
            JComboBox box    = ((JComboBox) e.getSource());
            File      newDir = (File) box.getSelectedItem();

            getModel().setClassTrainingImageDirectory(newDir);
        } else if (op.equals("DbrootDirComboBoxChanged")) {
            JComboBox box    = ((JComboBox) e.getSource());
            File      newDir = (File) box.getSelectedItem();

            if (newDir != null) {
                getModel().setDatabaseRoot(newDir);
            }
        } else if (op.equals("Close")) {
            getView().setVisible(false);
        } else {
            System.out.println("#############Something unknown changed in ClassifierSettingsController: " + op);
        }
    }
}
