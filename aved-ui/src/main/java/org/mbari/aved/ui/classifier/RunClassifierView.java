/*
 * @(#)RunClassifierView.java   10/03/17
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

import com.jgoodies.binding.adapter.ComboBoxAdapter;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;

public class RunClassifierView extends JFrameView {

    /*
     *  Component names in the ClassifierRun form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    private static final String ID_AVAILBABLE_CLASSES_JLIST = "available";               // javax.swing.JLIST
    private static final String ID_AVAIL_LIBRARY_NAME_COMBO = "availLibraryName";        // javax.swing.JComboBox
    private static final String ID_COLORSPACE_TEXT          = "colorspace";              // javax.swing.JLabel
    private static final String ID_PROBABILITY_THRESH_TEXT  = "probabilitythreshold";    // javax.swing.JTextField
    private static final String ID_RUN_BUTTON               = "run";                     // javax.swing.JButton
    private static final String ID_STOP_BUTTON              = "stop";                    // ""
    private final JComboBox     availLibraryNameComboBox;

    /* Some frequently accessed variables */
    private final JList      classesInLibraryList;
    private final JLabel     colorSpaceLabel;
    private final JTextField probThresholdTextField;

    public RunClassifierView(ClassifierModel model, RunClassifierController controller) {
        super("org/mbari/aved/ui/forms/ClassifierRun.xml", model, controller);
        classesInLibraryList     = getForm().getList(ID_AVAILBABLE_CLASSES_JLIST);
        availLibraryNameComboBox = getForm().getComboBox(ID_AVAIL_LIBRARY_NAME_COMBO);
        probThresholdTextField   = getForm().getTextField(ID_PROBABILITY_THRESH_TEXT);
        colorSpaceLabel          = getForm().getLabel(ID_COLORSPACE_TEXT);

        ActionHandler actionHandler = getActionHandler();

        availLibraryNameComboBox.addActionListener(actionHandler);
        probThresholdTextField.addActionListener(actionHandler);
        getForm().getButton(ID_RUN_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_STOP_BUTTON).addActionListener(actionHandler);

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Run classifier library");
        this.pack();
    }

    /**
     * Get the available class list
     * @return the class list
     */
    public JList getAvailableList() {
        return this.classesInLibraryList;
    }

    /**
     * Loads and displays the class list and colorspace associated with the given
     * <code>trainingModel</code>
     *
     * @param trainingModel the training model
     */
    public void loadModel(TrainingModel trainingModel) {
        int            size      = trainingModel.getNumClasses();
        ArrayListModel listModel = new ArrayListModel();

        for (int i = 0; i < size; i++) {
            listModel.add(trainingModel.getClassModel(i));
        }

        colorSpaceLabel.setText(trainingModel.getColorSpace().toString());
        classesInLibraryList.setModel(listModel);

        ClassModelListRenderer renderer = new ClassModelListRenderer(listModel);

        classesInLibraryList.setCellRenderer(renderer);
    }

    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            ClassifierModel.ClassifierModelEvent e = (ClassifierModel.ClassifierModelEvent) event;

            switch (event.getID()) {

            // When the database root directory changes, update the available
            // libraries
            case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED :
            case ClassifierModel.ClassifierModelEvent.TRAINING_MODELS_UPDATED :
                loadModels();

                break;
            }
        }
    }

    /**
     * Enables/disables the Run JButton in this view
     *  @param b  true to enable the button, otherwise false
     */
    void setRunButton(Boolean b) {
        getForm().getButton(ID_RUN_BUTTON).setEnabled(b);
    }

    /**
     * Enables/disables the Stop JButton in this view
     *  @param b  true to enable the button, otherwise false
     */
    void setStopButton(Boolean b) {
        getForm().getButton(ID_STOP_BUTTON).setEnabled(b);
    }

    /**
     * Returns the probability threshold defined in this view
     * Converts the percent threshold to the decimal equivalent
     * @return a probability threshold between 0 and 90%
     */
    float getProbabilityThreshold() {
        String  text = probThresholdTextField.getText();
        Integer i    = Integer.valueOf(text);

        return i.floatValue() / 100f;
    }

    /**
     * Loads the training models into this view
     */
    private void loadModels() {
        ClassifierModel model              = (ClassifierModel) getModel();
        ArrayListModel  modelList          = model.getTrainingModels();
        ListModel       listModel          = new ArrayListModel(modelList);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        availLibraryNameComboBox.setModel(new ComboBoxAdapter(selectionInList));

        if (!selectionInList.isEmpty()) {
            int    index         = 0;
            String lastSelection = UserPreferences.getModel().getLastTrainingLibrarySelection();

            for (int i = 0; i < modelList.size(); i++) {
                TrainingModel m = (TrainingModel) modelList.get(i);

                if (m.getName().equals(lastSelection)) {
                    index = i;

                    break;
                }
            }

            availLibraryNameComboBox.setSelectedIndex(index);
        }
    }
}
