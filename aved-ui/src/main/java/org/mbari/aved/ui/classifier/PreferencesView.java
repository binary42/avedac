/*
 * @(#)PreferencesView.java
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

import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.classifier.ClassifierModel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

public class PreferencesView extends JFrameView {

    /*
     *  Component names in the ApplicationPreferences form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_ADD_TRAINING_RADIO       = "addtrainingimages";
    public static final String ID_BROWSE_BUTTON_DBROOT     = "browsedbroot";
    public static final String ID_BROWSE_BUTTON_TRAINING   = "browsetraining";
    public static final String ID_CLOSE_BUTTON             = "close";
    public static final String ID_DBROOT_DIRECTORY_COMBO   = "dbrootdir";
    public static final String ID_TRAINING_DIRECTORY_COMBO = "trainingdir";
    private JCheckBox          addTrainingCheckBox;

    // Frequently accessed view variables
    private JComboBox trainingRootDirCombo, dbRootDirCombo;

    public PreferencesView(ClassifierModel model, PreferencesController controller) {
        super("org/mbari/aved/ui/forms/ClassifierPreferences.xml", model, controller);
        model = (ClassifierModel) getModel();

        // Initialize frequently accessed fields
        trainingRootDirCombo = getForm().getComboBox(ID_TRAINING_DIRECTORY_COMBO);
        dbRootDirCombo       = getForm().getComboBox(ID_DBROOT_DIRECTORY_COMBO);
        addTrainingCheckBox  = getForm().getCheckBox(ID_ADD_TRAINING_RADIO);

        JButton browseTrainingDirButton, browseDbrootDirButton, closeButton;

        closeButton             = (JButton) getForm().getButton(ID_CLOSE_BUTTON);
        browseTrainingDirButton = (JButton) getForm().getButton(ID_BROWSE_BUTTON_TRAINING);
        browseDbrootDirButton   = (JButton) getForm().getButton(ID_BROWSE_BUTTON_DBROOT);

        // Add handler to buttons and combo boxes
        ActionHandler actionHandler = getActionHandler();

        browseTrainingDirButton.addActionListener(actionHandler);
        browseDbrootDirButton.addActionListener(actionHandler);
        closeButton.addActionListener(actionHandler);
        addTrainingCheckBox.addActionListener(actionHandler);
        dbRootDirCombo.addActionListener(actionHandler);
        trainingRootDirCombo.addActionListener(actionHandler);
        loadModel(model);

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Classifier Preferences");
        this.pack();
        this.setResizable(false);
    }

    @Override
    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    /**
     * Loads the model data into the view components
     *
     * @param model the model to load from
     */
    private void loadModel(ClassifierModel model) {
        boolean isAddTrainingImages = UserPreferences.getModel().getAddTrainingImages();

        addTrainingCheckBox.setSelected(isAddTrainingImages);

        File newTrainingDir = model.getClassTrainingImageDirectory();
        File dbRootDir      = model.getDatabaseRoot();

        updateTrainingComboBox(newTrainingDir);
        updateDbrootComboBox(dbRootDir);
    }

    public void modelChanged(ModelEvent event) {}

    /**
     * Inserts a directory into the training combo box list
     * at the first index.
     * @param dir the directory to insert
     */
    public void updateTrainingComboBox(File dir) {
        trainingRootDirCombo.insertItemAt(dir, 0);
        trainingRootDirCombo.setSelectedItem(dir);
    }

    /**
     * Inserts a directory into the class metadata combo box list
     * at the first index.
     * @param dir the directory to insert
     */
    public void updateDbrootComboBox(File dir) {
        dbRootDirCombo.insertItemAt(dir, 0);
        dbRootDirCombo.setSelectedItem(dir);
    }
}
