/*
 * @(#)CreateTrainingLibraryView.java
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

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.classifier.CreateTrainingLibraryController.MouseClickJListActionHandler;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;

public class CreateTrainingLibraryView extends JFrameView {

    /*
     *  Component names in the CreateClassifierTraining form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    private static final String ID_ADD_BUTTON               = "add";               // javax.swing.JButton
    private static final String ID_AVAILBABLE_CLASSES_JLIST = "available";         // javax.swing.JLIST
    private static final String ID_COLORSPACE_COMBOBOX      = "colorspace";        // javax.swing.JComboBox
    private static final String ID_INCLUDED_CLASSES_JLIST   = "include";           // javax.swing.JLIST
    private static final String ID_NEW_LIBRARY_NAME_TEXT    = "newLibraryName";    // javax.swing.JText
    private static final String ID_REMOVE_BUTTON            = "remove";            // javax.swing.JButton
    private static final String ID_RUN_BUTTON               = "run";               // javax.swing.JButton
    private static final String ID_STOP_BUTTON              = "stop";              // javax.swing.JButton

    /* Some frequently accessed variables */
    private final JList      availableList, selectedList;
    private final JComboBox  colorSpaceComboBox;
    private final JTextField newLibraryNameTextField;

    public CreateTrainingLibraryView(ClassifierModel model, CreateTrainingLibraryController controller) {

        // Constructor
        super("org/mbari/aved/ui/forms/ClassifierCreateTrainingLibrary.xml", model, controller);
        availableList           = getForm().getList(ID_AVAILBABLE_CLASSES_JLIST);
        selectedList            = getForm().getList(ID_INCLUDED_CLASSES_JLIST);
        newLibraryNameTextField = getForm().getTextField(ID_NEW_LIBRARY_NAME_TEXT);
        colorSpaceComboBox      = getForm().getComboBox(ID_COLORSPACE_COMBOBOX);

        ActionHandler actionHandler = getActionHandler();

        colorSpaceComboBox.addActionListener(actionHandler);
        newLibraryNameTextField.addActionListener(actionHandler);
        getForm().getButton(ID_REMOVE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_ADD_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_ADD_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_RUN_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_STOP_BUTTON).addActionListener(actionHandler);

        // Populate the color space combo box
        ArrayListModel list = new ArrayListModel();

        list.add(ColorSpace.GRAY);
        list.add(ColorSpace.RGB);
        list.add(ColorSpace.YCBCR);

        ListModel       listModel          = new ArrayListModel(list);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        colorSpaceComboBox.setModel(new ComboBoxAdapter(selectionInList));

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Create training library"); 
    }

    /**
     * Get the available class list
     * @return the class list
     */
    public JList getAvailableList() {
        return this.availableList;
    }

    /**
     * Get the list of classes to include in this training library
     * @return the class list
     */
    public JList getIncludeList() {
        return this.selectedList;
    }

    public void modelChanged(ModelEvent event) {
        
    }

    /**
     * Populates the available classes in the given
     * @{link org.mbari.aved.classifier.ColorSpace}
     *
     * @param colorSpace
     */
    public void populateAvailableClassList(ColorSpace colorSpace) {
        ClassifierModel model = (ClassifierModel) getModel();

        // Get all available models
        ArrayListModel classModel = model.getClassModels();
        ArrayListModel list       = new ArrayListModel();

        for (int i = 0; i < classModel.getSize(); i++) {
            ClassModel aClassModel = (ClassModel) classModel.get(i);

            if (aClassModel.getColorSpace().equals(colorSpace)) {
                list.add(aClassModel);
            }
        }

        availableList.removeAll();
        availableList.setModel(list);

        ClassModelListRenderer renderer = new ClassModelListRenderer(list);

        availableList.setCellRenderer(renderer);
    }

    /**
     * Selects the given color space in the combobox
     * @param colorSpace
     */
    public void selectColorSpace(ColorSpace colorSpace) {
        colorSpaceComboBox.setSelectedItem(colorSpace);
    }

    /**
     * Clears all items in the available list
     */
    public void clearAllAvailable() {
        availableList.clearSelection();
    }

    /**
     * Get the name for the training class.
     * @return the training class name
     */
    public String getTrainingClassName() {
        return newLibraryNameTextField.getText();
    }

    /**
     * Populates the selected class list with the classes in
     * the <code>trainingModel</code> and highlights those
     * selected in the available classes
     *
     * @param trainingModel the training model
     */
    public void populateSelectedList(TrainingModel trainingModel) {
        int            size      = trainingModel.getNumClasses();
        ArrayListModel listModel = new ArrayListModel();

        for (int i = 0; i < size; i++) {
            listModel.add(trainingModel.getClassModel(i));
        }

        selectedList.setModel(listModel);

        ClassModelListRenderer renderer = new ClassModelListRenderer(listModel);

        selectedList.setCellRenderer(renderer);
        selectedList.setSelectionInterval(0, size - 1);

        ClassifierModel model   = (ClassifierModel) getModel();
        Vector<Integer> indices = new Vector<Integer>();

        for (int i = 0; i < size; i++) {
            ClassModel cls      = trainingModel.getClassModel(i);
            ListModel  list     = availableList.getModel();
            int        numAvail = list.getSize();

            if (model.checkClassExists(cls)) {
                for (int j = 0; j < numAvail; j++) {
                    ClassModel availClass = (ClassModel) list.getElementAt(j);

                    if (availClass.getName().equals(cls.getName())) {
                        indices.add(j);
                    }
                }
            }
        }

        Collections.sort(indices);

        int[] index = new int[indices.size()];

        for (int i = 0; i < index.length; i++) {
            index[i] = indices.get(i);
        }

        availableList.setSelectedIndices(index);
    }

    /**
     * Selects all items in the available list
     */
    public void selectAllAvailable() {
        int size = availableList.getModel().getSize();

        availableList.setSelectionInterval(0, size - 1);
    }

    /**
     * Clears all items in the selected list
     */
    public void clearAllSelected() {
        selectedList.clearSelection();
    }

    /**
     * Selects all items in the selected list
     */
    public void selectAll() {
        int size = selectedList.getModel().getSize();

        selectedList.setSelectionInterval(0, size - 1);
    }

    /**
     * Adds a @{link org.mbari.aved.ui.classifier.CreateTrainingLibraryController.MouseClickJListActionHandler}
     * to the contained lists
     *
     * @param mouseClickJListActionHandler
     */
    void addMouseClickListener(MouseClickJListActionHandler mouseClickJListActionHandler) {
        selectedList.addMouseListener(mouseClickJListActionHandler);
        availableList.addMouseListener(mouseClickJListActionHandler);
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
}
