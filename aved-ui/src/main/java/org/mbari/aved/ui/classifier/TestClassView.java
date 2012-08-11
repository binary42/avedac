/*
 * @(#)TestClassView.java
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

import com.jeta.forms.components.image.ImageComponent;

import com.jgoodies.binding.adapter.ComboBoxAdapter;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Image;

import java.io.File;

import java.net.URL;

import java.util.ArrayList;

import javax.swing.*;
import org.mbari.aved.ui.userpreferences.UserPreferences;

public class TestClassView extends JFrameView {
    private static final String ID_CLASS_COLORSPACE_COMBOBOX  = "classcolorspacecombo";    // "
    private static final String ID_CLASS_DESCRIPTION_TEXTAREA = "classdescription";        // javax.swing.JTextArea

    /*
     * Component names in the TestClassView form If any of the component name
     * are changed in the Abeille form designer, they should be modified here
     * too
     */
    private static final String ID_CLASS_NAME_COMBOBOX      = "classnamecombo";       // javax.swing.JComboBox
    private static final String ID_CLASS_NAME_LABEL         = "classname";            // javax.swing.JLabel
    private static final String ID_CLASS_NAME_PRED_LABEL    = "predictedclassname";
    private static final String ID_IMAGE_COMPONENT          = "classimage";
    private static final String ID_IMAGE_DIRECTORY_LABEL    = "imagedirectory";
    private static final String ID_LIBRARY_CLASS_JLIST      = "classesinlibrary";     // javax.swing.JList
    private static final String ID_LIBRARY_COLORSPACE_LABEL = "librarycolorspace";    // "
    private static final String ID_LIBRARY_NAME_COMBOBOX    = "libraryname";          // ""
    private static final String ID_NUM_IMAGES_LABEL         = "numclassimages";
    private static final String ID_RUN_BUTTON               = "run";                  // javax.swing.JButton
    private static final String ID_STOP_BUTTON              = "stop";                 // ""
    private final JTextArea     classDescriptionTextArea;
    private ImageComponent      classImageComponent;
    private final JComboBox     classNameComboBox, libraryNameComboBox, colorSpaceComboBox;
    private final JList         classesInLibraryList;
    private final JLabel        numImagesLabel, classNamePredLabel, classNameLabel, libraryColorSpaceLabel,
                                imageDirLabel;

    TestClassView(ClassifierModel model, TestClassController controller) {
        super("org/mbari/aved/ui/forms/ClassifierTestClass.xml", model, controller);

        // load frequently accessed components
        classNameComboBox        = getForm().getComboBox(ID_CLASS_NAME_COMBOBOX);
        libraryNameComboBox      = getForm().getComboBox(ID_LIBRARY_NAME_COMBOBOX);
        colorSpaceComboBox       = getForm().getComboBox(ID_CLASS_COLORSPACE_COMBOBOX);
        classesInLibraryList     = getForm().getList(ID_LIBRARY_CLASS_JLIST);
        classDescriptionTextArea = (JTextArea) getForm().getTextComponent(ID_CLASS_DESCRIPTION_TEXTAREA);
        classNameLabel           = getForm().getLabel(ID_CLASS_NAME_LABEL);
        libraryColorSpaceLabel   = getForm().getLabel(ID_LIBRARY_COLORSPACE_LABEL);
        imageDirLabel            = getForm().getLabel(ID_IMAGE_DIRECTORY_LABEL);
        numImagesLabel           = getForm().getLabel(ID_NUM_IMAGES_LABEL);
        classNamePredLabel       = getForm().getLabel(ID_CLASS_NAME_PRED_LABEL);
        classImageComponent      = (ImageComponent) getForm().getComponentByName(ID_IMAGE_COMPONENT);

        ActionHandler actionHandler = getActionHandler();

        getForm().getButton(ID_RUN_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_STOP_BUTTON).addActionListener(actionHandler);
        classNameComboBox.addActionListener(actionHandler);
        libraryNameComboBox.addActionListener(actionHandler);
        colorSpaceComboBox.addActionListener(actionHandler);

        // Populate the color space combo box
        ArrayListModel list = new ArrayListModel();

        for (ColorSpace color: ColorSpace.values()) {
            list.add(color);
        } 

        ListModel       listModel          = new ArrayListModel(list);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        colorSpaceComboBox.setModel(new ComboBoxAdapter(selectionInList)); 
        
        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Test Class");
        this.pack();

        // Insert a default icon
        URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

        initializeImageComponent(new File(url.getFile()), UserPreferences.getModel().getColorSpace());
    }

    /**
     * Get the @{link org.mbari.aved.classifier.ColorSpace}  color space for testing
     * @return the selected color space
     */
    ColorSpace getClassColorSpace() {
        return (ColorSpace) colorSpaceComboBox.getSelectedItem();
    }

    /**
     * Get the @{link org.mbari.aved.classifier.ClassModel} class model to test
     * @return the selected class model
     */
    ClassModel getClassModel() {
        return (ClassModel) classNameComboBox.getSelectedItem();
    }

    /**
     * Get the @{link org.mbari.aved.classifier.TrainingModel} class model to test
     * @return the selected training model
     */
    TrainingModel getTrainingModel() {
        return (TrainingModel) libraryNameComboBox.getSelectedItem();
    }

    /**
     * Helper class to return the model associated with this view
     * @return
     */
    @Override
    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    public void modelChanged(ModelEvent event) {}

    /**
     * Loads {@link org.mbari.aved.classifier.TrainingModel}
     * {@link org.mbari.aved.classifier.TrainingModel}
     *
     * @param trainingModel the training model
     */
    public void loadTrainingModel(TrainingModel trainingModel) {
        if (trainingModel != null) {
            int            size      = trainingModel.getNumClasses();
            ArrayListModel listModel = new ArrayListModel();

            for (int i = 0; i < size; i++) {
                listModel.add(trainingModel.getClassModel(i));
            }

            classesInLibraryList.setModel(listModel);

            // Set a custom renderer for displaying the thumbnail icons for classes
            // in the training model
            ClassModelListRenderer renderer = new ClassModelListRenderer(listModel);

            classesInLibraryList.setCellRenderer(renderer);
            libraryColorSpaceLabel.setText(trainingModel.getColorSpace().toString());
        } else {

            // Set an empty model
            ArrayListModel listModel = new ArrayListModel();

            classesInLibraryList.setModel(listModel);
            libraryColorSpaceLabel.setText("");
        }
    }

    /**
     * Loads the @{link org.mbari.aved.classifier.ClassModel} data
     * into the view
     *
     * @param model the to load
     */
    public void loadClassModel(ClassModel model) {
        if (model != null) {
            classNameLabel.setText(model.getName());
            classDescriptionTextArea.setText(model.getDescription());
            classNamePredLabel.setText(model.getPredictedName());

            File   f         = model.getRawImageDirectory();
            String parentDir = f.getParent();

            if (parentDir != null) {
                imageDirLabel.setText(f.getAbsolutePath());
                imageDirLabel.setToolTipText(f.toString());
            } else {
                imageDirLabel.setText(model.getRawImageDirectory().toString());
            }

            ArrayList<String> fileList  = model.getRawImageFileListing();
            Integer           numImages = Integer.valueOf(fileList.size());

            numImagesLabel.setText(numImages.toString());

            if (fileList.size() > 0) {
                File exampleImage = new File(model.getRawImageDirectory() + "/" + fileList.get(0));

                initializeImageComponent(exampleImage, model.getColorSpace());
            } else {

                // Insert a default icon
                URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

                initializeImageComponent(new File(url.getFile()), model.getColorSpace());
            }
        } else {
            classNameLabel.setText("");
            classDescriptionTextArea.setText("");
            classNamePredLabel.setText("");
            imageDirLabel.setText("");
            numImagesLabel.setText("");

            // Insert a default icon
            URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

            initializeImageComponent(new File(url.getFile()), UserPreferences.getModel().getColorSpace());
        }
    }

    /**
     * Initializes the image component in this view with the image
     * found in the <code>imageFile</code>. If no valid image is found
     * a default one will be used.
     * @param imageFile
     */
    private void initializeImageComponent(File imageFile, ColorSpace color) {
        ImageIcon icon = ClassModelListRenderer.createImageIcon(imageFile);

        if (icon == null) {

            // Insert a default icon here
            URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

            icon = new ImageIcon(url);
        }

        // Scale icons to a smaller size
        if (icon != null) {
            Image image = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

            icon = ClassModelListRenderer.convertImageIcon(image, color);
        }

        classImageComponent.setIcon(icon);
        getForm().repaint();
    }

    /**
     * Populates the classes available for testing ComboBox
     * 
     * @param colorSpace populate libraries only in this color space
     * @param classSelection name of the class to select from populated list
     */
    public void populateClassList(ColorSpace colorSpace, String classSelection) {
        loadClassModel(null);

        int            size = getModel().getNumClassModels();
        ArrayListModel list = new ArrayListModel();

        for (int i = 0; i < size; i++) {
            ClassModel aClassModel = getModel().getClassModel(i);

            if (aClassModel.getColorSpace().equals(colorSpace)) {
                list.add(aClassModel);
            }
        }

        ListModel       listModel          = new ArrayListModel(list);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        classNameComboBox.setModel(new ComboBoxAdapter(selectionInList));

        ClassModelListRenderer renderer = new ClassModelListRenderer(listModel);

        classNameComboBox.setRenderer(renderer);
        classNameComboBox.setMaximumRowCount(3);
        
        selectClass(classSelection);
    }

    /**
     * Set the color space
     * @return the @{link org.mbari.aved.classifier.ColorSpace}
     */
    void setColorSpace(ColorSpace color) {
        colorSpaceComboBox.setSelectedItem(color);
    }

    /**
     * Populates the available training libraries in the given
     * @{link org.mbari.aved.classifier.ColorSpace}.
     *
     * @param colorSpace populate libraries only in this color space
     * @param librarySelection name of the library to select from populated list
     */
    public void populateTrainingLibraryList(ColorSpace colorSpace, String librarySelection) {
        loadTrainingModel(null);

        ClassifierModel model = (ClassifierModel) getModel();

        // Get all available models in the same color space
        int            size                      = model.getNumTrainingModels();
        ArrayListModel trainingModelInColorSpace = new ArrayListModel();

        for (int i = 0; i < size; i++) {
            TrainingModel aTrainingModel = model.getTrainingModel(i);

            if (aTrainingModel.getColorSpace().equals(colorSpace)) {
                trainingModelInColorSpace.add(aTrainingModel);
            }
        }

        // Add training libraries to the combo box
        ListModel       listModel          = new ArrayListModel(trainingModelInColorSpace);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        libraryNameComboBox.setModel(new ComboBoxAdapter(selectionInList));
 
        selectLibrary(librarySelection); 
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
     * Selects the library by name.
     * @param name the name of the library
     */
    private void selectLibrary(String name ) {
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
            // if can't find selection, default to first one
            if (libraryNameComboBox.getItemCount() > 0)
                libraryNameComboBox.setSelectedIndex(0);
        }
    }

     /**
     * Selects the class by name.
     * @param name the name of the c;ass
     * @return false if the class is not found, otherwise true
     */
    private boolean selectClass(String name) {
        int index = -1;
        int size  = classNameComboBox.getModel().getSize();

        if (size > 0) {
            index = 0;
        }

        // Look for the library by name in this color space
        for (int i = 0; i < size; i++) {
            ClassModel m = (ClassModel) classNameComboBox.getItemAt(i);

            // If the last user selection is found by name in this color space
            // keep the index to set it later
            if (m.getName().equals(name)) {
                index = i;
            }
        }

        if ((index >= 0) && (index < classNameComboBox.getItemCount())) {
            classNameComboBox.setSelectedIndex(index);

            return true;
        } else {
            return false;
        }
    }

}
