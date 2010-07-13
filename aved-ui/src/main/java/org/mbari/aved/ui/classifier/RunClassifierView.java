/*
 * @(#)RunClassifierView.java
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

import org.mbari.aved.classifier.ColorSpace;
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
    private static final String ID_CLASSES_IN_LIBRARY_JLIST = "classes";                 // javax.swing.JLIST
    private static final String ID_COLORSPACE_COMBOBOX = "colorspace";              // javax.swing.JComboBox
    private static final String ID_LIBRARY_NAME_COMBO = "libraryname";             // javax.swing.JComboBox
    private static final String ID_PROBABILITY_THRESH_TEXT = "probabilitythreshold";    // javax.swing.JTextField
    private static final String ID_RUN_BUTTON = "run";                     // javax.swing.JButton
    private static final String ID_STOP_BUTTON = "stop";                    // ""

    /* Some frequently accessed variables */
    private final JList classesInLibraryList;
    private final JComboBox colorSpaceComboBox;
    private final JComboBox libraryNameComboBox;
    private final JTextField probThresholdTextField;

    public RunClassifierView(ClassifierModel model, RunClassifierController controller) {
        super("org/mbari/aved/ui/forms/ClassifierRun.xml", model, controller);
        classesInLibraryList = getForm().getList(ID_CLASSES_IN_LIBRARY_JLIST);
        libraryNameComboBox = getForm().getComboBox(ID_LIBRARY_NAME_COMBO);
        probThresholdTextField = getForm().getTextField(ID_PROBABILITY_THRESH_TEXT);
        colorSpaceComboBox = getForm().getComboBox(ID_COLORSPACE_COMBOBOX);

        ActionHandler actionHandler = getActionHandler();

        colorSpaceComboBox.addActionListener(actionHandler);
        libraryNameComboBox.addActionListener(actionHandler);
        probThresholdTextField.addActionListener(actionHandler);
        getForm().getButton(ID_RUN_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_STOP_BUTTON).addActionListener(actionHandler);

        // Populate the color space combo box
        ArrayListModel list = new ArrayListModel();

        list.add(ColorSpace.GRAY);
        list.add(ColorSpace.RGB);
        list.add(ColorSpace.YCBCR);

        ListModel listModel = new ArrayListModel(list);
        ValueModel selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList = new SelectionInList(listModel, selectedItemHolder);

        colorSpaceComboBox.setModel(new ComboBoxAdapter(selectionInList));

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Run classifier library");
        this.pack();
    }

    public void modelChanged(ModelEvent event) {
    }

    /**
     * Selects the given color space in the combobox
     * @param colorSpace
     */
    public void selectColorSpace(ColorSpace colorSpace) {
        colorSpaceComboBox.setSelectedItem(colorSpace);
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
        String text = probThresholdTextField.getText();
        Integer i = Integer.valueOf(text);

        return i.floatValue() / 100f;
    }

    /**
     * Loads the training models into this view in the correct color space
     */
    public void populateTrainingLibraryList(ColorSpace colorSpace) {
        loadModel(null);

        ClassifierModel model = (ClassifierModel) getModel();
        ArrayListModel modelList = model.getTrainingModels();
        ArrayListModel newList = new ArrayListModel();

        for (int i = 0; i < modelList.size(); i++) {
            TrainingModel m = (TrainingModel) modelList.get(i);

            // Only add those libraries in this color space
            if (m.getColorSpace().equals(colorSpace)) {
                newList.add(m);
            }
        }

        ListModel listModel = new ArrayListModel(newList);
        ValueModel selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList = new SelectionInList(listModel, selectedItemHolder);

        libraryNameComboBox.setModel(new ComboBoxAdapter(selectionInList));

        // Select the first one
        if (!selectionInList.isEmpty()) {
            libraryNameComboBox.setSelectedIndex(0);
        }
    }

    /**
     * Loads and displays the classes in the given  <code>trainingModel</code>
     *
     * @param trainingModel the training model
     */
    public void loadModel(TrainingModel trainingModel) {
        if (trainingModel != null) {
            int size = trainingModel.getNumClasses();
            ArrayListModel listModel = new ArrayListModel();

            for (int i = 0; i < size; i++) {
                listModel.add(trainingModel.getClassModel(i));
            }

            classesInLibraryList.setModel(listModel);

            ClassModelListRenderer renderer = new ClassModelListRenderer(listModel);

            classesInLibraryList.setCellRenderer(renderer);
        } else {
            // Set an empty model
            ArrayListModel listModel = new ArrayListModel();
            classesInLibraryList.setModel(listModel);
        }
    }

    /**
     * 
     */
    void clearClasses() {
        ArrayListModel listModel = new ArrayListModel();
        classesInLibraryList.setModel(listModel);
    }

    /**
     * Selects the library by name.
     * @param name the name of the library
     * @return false if the library is not found, otherwise true
     */
    boolean selectLibrary(String name) {
        int index = -1;
        int size = libraryNameComboBox.getModel().getSize();

        if (size > 0) {
            index = 0;
        }

        // Look for the library by name in this color space
        for (int i = 0; i < size; i++) {
            TrainingModel m = (TrainingModel) libraryNameComboBox.getItemAt(index);

            // If the last user selection is found by name in this color space
            // keep the index to set it later
            if (m.getName().equals(name)) {
                index = i;
            }
        }

        if ((index >= 0) && (index < libraryNameComboBox.getItemCount())) {
            libraryNameComboBox.setSelectedIndex(index);

            return true;
        } else {
            return false;
        }
    }
}
