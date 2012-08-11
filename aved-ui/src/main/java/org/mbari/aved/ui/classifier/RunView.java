/*
 * @(#)RunView.java
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

import com.jgoodies.binding.adapter.ComboBoxAdapter;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import java.util.Arrays;
import javax.swing.JButton;

import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;
import org.mbari.aved.ui.appframework.AbstractController;

public class RunView extends JFrameView {
    /*
     *  Component names in the ClassifierRun form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    private static final String ID_VOTING_METHOD_COMBOBOX   = "votingmethod";            // javax.swing.JComboBox
    private static final String ID_CLASSES_IN_LIBRARY_JLIST = "classes";                 // javax.swing.JLIST
    private static final String ID_COLORSPACE_COMBOBOX      = "colorspace";              // javax.swing.JComboBox
    private static final String ID_LIBRARY_NAME_COMBO       = "libraryname";             // javax.swing.JComboBox
    private static final String ID_PROBABILITY_THRESH_TEXT  = "probabilitythreshold";    // javax.swing.JTextField
    private static final String ID_RUN_BUTTON               = "run";                     // javax.swing.JButton
    private static final String ID_STOP_BUTTON              = "stop";                    // ""

    /* Some frequently accessed variables */
    private final JList      classesInLibraryList;
    private final JComboBox  colorSpaceComboBox, votingMethodComboBox, libraryNameComboBox;
    private final JTextField probThresholdTextField;

    public RunView(ClassifierModel model, AbstractController controller) {
        super("org/mbari/aved/ui/forms/ClassifierRun.xml", model, controller);
        votingMethodComboBox   = getForm().getComboBox(ID_VOTING_METHOD_COMBOBOX);
        classesInLibraryList   = getForm().getList(ID_CLASSES_IN_LIBRARY_JLIST);
        libraryNameComboBox    = getForm().getComboBox(ID_LIBRARY_NAME_COMBO);
        probThresholdTextField = getForm().getTextField(ID_PROBABILITY_THRESH_TEXT);
        colorSpaceComboBox     = getForm().getComboBox(ID_COLORSPACE_COMBOBOX);

        ActionHandler actionHandler = getActionHandler();

        votingMethodComboBox.addActionListener(actionHandler);
        colorSpaceComboBox.addActionListener(actionHandler);
        libraryNameComboBox.addActionListener(actionHandler);
        probThresholdTextField.addActionListener(actionHandler);
        getForm().getButton(ID_RUN_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_STOP_BUTTON).addActionListener(actionHandler);

        // Populate the color space combo box
        ArrayListModel list = new ArrayListModel();
        list.addAll(Arrays.asList(ColorSpace.values()));

        ListModel       listModel          = new ArrayListModel(list);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        colorSpaceComboBox.setModel(new ComboBoxAdapter(selectionInList));

        // Populate the voting method combo box
        ArrayListModel listVotingMethod = new ArrayListModel();

        for (VotingMethod method: VotingMethod.values()) {
            listVotingMethod.add(method);
        } 

        ListModel       listVotingModel          = new ArrayListModel(listVotingMethod);
        ValueModel      selectedVotingItemHolder = new ValueHolder();
        SelectionInList selectionVotingInList    = new SelectionInList(listVotingModel, selectedVotingItemHolder);

        votingMethodComboBox.setModel(new ComboBoxAdapter(selectionVotingInList));

        // default to majority
        votingMethodComboBox.setSelectedIndex(0);  
        
        this.getRootPane().setDefaultButton((JButton) getForm().getButton(ID_RUN_BUTTON));
        
        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Run classifier library");
        this.pack();
    }


    VotingMethod getVotingMethod() {
        return (VotingMethod) votingMethodComboBox.getSelectedItem();
    }

    public void modelChanged(ModelEvent event) {}

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
        String  text = probThresholdTextField.getText();
        Integer i    = Integer.valueOf(text);

        return i.floatValue() / 100f;
    }

    /**
     * Loads the training models into this view in the correct color space
     * @param colorSpace color space to populate list with 
     * @param librarySelection name of the library to select from populated list
     */
    public void populateTrainingLibraryList(ColorSpace colorSpace, String librarySelection) {
        loadModel(null);

        ClassifierModel model   = (ClassifierModel) getModel();
        int             size    = model.getNumTrainingModels();
        ArrayListModel  newList = new ArrayListModel();

        for (int i = 0; i < size; i++) {
            TrainingModel m = model.getTrainingModel(i);

            // Only add those libraries in this color space
            if (m.getColorSpace().equals(colorSpace)) {
                newList.add(m);
            }
        }

        ListModel       listModel          = new ArrayListModel(newList);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);
 
        libraryNameComboBox.setModel(new ComboBoxAdapter(selectionInList));

        selectLibrary(librarySelection);
    }

    /**
     * Loads and displays the classes in the given  <code>trainingModel</code>
     *
     * @param trainingModel the training model
     */
    public void loadModel(TrainingModel trainingModel) {
        if (trainingModel != null) {
            int            size      = trainingModel.getNumClasses();
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
    void clearTrainingModelList() {
        ArrayListModel listModel = new ArrayListModel();

        classesInLibraryList.setModel(listModel);
    }
 
    /**
     *
     */
    void clearClassModelList() {
        ArrayListModel listModel = new ArrayListModel();

        classesInLibraryList.setModel(listModel);
    }

    /**
     * Selects the library by name.
     * @param name the name of the library 
     */
    void selectLibrary(String name) {
        int index = -1;
        int size  = libraryNameComboBox.getModel().getSize();

        if (size > 0) {
            index = 0;
        }

        // Look for the library by name in this color space
        for (int i = 0; i < size; i++) {
            TrainingModel m = (TrainingModel) libraryNameComboBox.getItemAt(i);

            // If the last user selection is found by name in this color space
            // keep the index to set it later
            if (m.getName().equals(name)) {
                index = i;
            }
        }

        if ((index >= 0) && (index < libraryNameComboBox.getItemCount())) {
            libraryNameComboBox.setSelectedIndex(index);
         } else {
            if (libraryNameComboBox.getItemCount() > 0)
                libraryNameComboBox.setSelectedIndex(0); 
        }
    }

    int getTrainingModelIndex() {
        return libraryNameComboBox.getSelectedIndex();
    }

    void setSelectedIndex(int i) {
        libraryNameComboBox.setSelectedIndex(i);
    }

    ColorSpace getColorSpace() {
        return (ColorSpace) colorSpaceComboBox.getSelectedItem();
    }

    void setColorSpace(ColorSpace colorSpace) {
        colorSpaceComboBox.setSelectedItem(colorSpace);
    }

    String getTrainingModelName() {
        TrainingModel m = (TrainingModel) libraryNameComboBox.getSelectedItem(); 
        return m.getName();
    }


}
