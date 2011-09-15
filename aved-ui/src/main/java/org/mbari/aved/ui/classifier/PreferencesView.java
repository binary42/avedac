/*
 * @(#)PreferencesView.java
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

import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
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
    public static final String ID_BROWSE_BUTTON_TRAINING   = "browsetraining";
    public static final String ID_CLOSE_BUTTON             = "close";
    public static final String ID_TRAINING_DIRECTORY_COMBO = "trainingdir";
    private JCheckBox          addTrainingCheckBox;

    // Frequently accessed view variables
    private JComboBox trainingRootDirCombo;

    public PreferencesView(ClassifierModel model, PreferencesController controller) {
        super("org/mbari/aved/ui/forms/ClassifierPreferences.xml", model, controller);
        model = (ClassifierModel) getModel();

        // Initialize frequently accessed fields
        trainingRootDirCombo = getForm().getComboBox(ID_TRAINING_DIRECTORY_COMBO);
        addTrainingCheckBox  = getForm().getCheckBox(ID_ADD_TRAINING_RADIO);

        JButton browseTrainingDirButton, browseDbrootDirButton, closeButton;

        closeButton             = (JButton) getForm().getButton(ID_CLOSE_BUTTON);
        browseTrainingDirButton = (JButton) getForm().getButton(ID_BROWSE_BUTTON_TRAINING);

        // Add handler to buttons and combo boxes
        ActionHandler actionHandler = getActionHandler();

        browseTrainingDirButton.addActionListener(actionHandler);
        closeButton.addActionListener(actionHandler);
        addTrainingCheckBox.addActionListener(actionHandler);
        trainingRootDirCombo.addActionListener(actionHandler);
        loadModel(model);

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Classifier Preferences");

        this.getRootPane().setDefaultButton((JButton) getForm().getButton(ID_CLOSE_BUTTON));
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

        updateTrainingComboBox(newTrainingDir);
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

}
