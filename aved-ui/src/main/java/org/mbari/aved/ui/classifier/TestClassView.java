/*
 * @(#)TestClassView.java
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
    private static final String ID_CLASS_NAME_VARS_LABEL    = "classnamevars";
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
    private final JComboBox     classNameComboBox, libraryNameComboBox, classColorComboBox;
    private final JList         classesInLibraryList;
    private final JLabel        numImagesLabel, classNameVarsLabel, classNameLabel, libraryColorSpaceLabel,
                                imageDirLabel;

    TestClassView(ClassifierModel model, TestClassController controller) {
        super("org/mbari/aved/ui/forms/ClassifierTestClass.xml", model, controller);

        // load frequently accessed components
        classNameComboBox        = getForm().getComboBox(ID_CLASS_NAME_COMBOBOX);
        libraryNameComboBox      = getForm().getComboBox(ID_LIBRARY_NAME_COMBOBOX);
        classColorComboBox       = getForm().getComboBox(ID_CLASS_COLORSPACE_COMBOBOX);
        classesInLibraryList     = getForm().getList(ID_LIBRARY_CLASS_JLIST);
        classDescriptionTextArea = (JTextArea) getForm().getTextComponent(ID_CLASS_DESCRIPTION_TEXTAREA);
        classNameLabel           = getForm().getLabel(ID_CLASS_NAME_LABEL);
        libraryColorSpaceLabel   = getForm().getLabel(ID_LIBRARY_COLORSPACE_LABEL);
        imageDirLabel            = getForm().getLabel(ID_IMAGE_DIRECTORY_LABEL);
        numImagesLabel           = getForm().getLabel(ID_NUM_IMAGES_LABEL);
        classNameVarsLabel       = getForm().getLabel(ID_CLASS_NAME_VARS_LABEL);
        classImageComponent      = (ImageComponent) getForm().getComponentByName(ID_IMAGE_COMPONENT);

        ActionHandler actionHandler = getActionHandler();

        getForm().getButton(ID_RUN_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_STOP_BUTTON).addActionListener(actionHandler);
        classNameComboBox.addActionListener(actionHandler);
        libraryNameComboBox.addActionListener(actionHandler);
        classColorComboBox.addActionListener(actionHandler);

        // Populate the color space combo box
        ArrayListModel list = new ArrayListModel();

        list.add(ColorSpace.GRAY);
        list.add(ColorSpace.RGB);
        list.add(ColorSpace.YCBCR);

        ListModel       listModel          = new ArrayListModel(list);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        classColorComboBox.setModel(new ComboBoxAdapter(selectionInList));

        // default to RGB color space
        classColorComboBox.setSelectedIndex(1);

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Test Class");
        this.pack();

        // Insert a default icon
        URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

        initializeImageComponent(new File(url.getFile()), ColorSpace.RGB);
    }

    /**
     * Get the @{link org.mbari.aved.classifier.ColorSpace}  color space for testing
     * @return the selected color space
     */
    ColorSpace getClassColorSpace() {
        return (ColorSpace) classColorComboBox.getSelectedItem();
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
            classNameVarsLabel.setText(model.getVarsClassName());

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
            classNameVarsLabel.setText("");
            imageDirLabel.setText("");
            numImagesLabel.setText("");    
            
            // Insert a default icon
            URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

            initializeImageComponent(new File(url.getFile()), ColorSpace.RGB);
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
     */
    public void populateClassList(ColorSpace colorSpace) {
        loadClassModel(null);
        ArrayListModel classModelList = getModel().getClassModels();
        ArrayListModel list           = new ArrayListModel();

        for (int i = 0; i < classModelList.getSize(); i++) {
            ClassModel aClassModel = (ClassModel) classModelList.get(i);

            if (aClassModel.getColorSpace().equals(colorSpace)) {
                list.add(aClassModel);
            }
        }

        ListModel       listModel          = new ArrayListModel(list);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        classNameComboBox.setModel(new ComboBoxAdapter(selectionInList));

        if (!selectionInList.isEmpty()) {
            classNameComboBox.setSelectedIndex(0);
        }

        ClassModelListRenderer renderer = new ClassModelListRenderer(listModel);

        classNameComboBox.setRenderer(renderer);
        classNameComboBox.setMaximumRowCount(3);
    }

    /**
     * Populates the available training libraries in the given
     * @{link org.mbari.aved.classifier.ColorSpace}.
     *
     * @param colorSpace
     */
    public void populateTrainingLibraryList(ColorSpace colorSpace) {
        loadTrainingModel(null);
        
        ClassifierModel model = (ClassifierModel) getModel();
 
        // Get all available models in the same color space
        ArrayListModel trainingModel             = model.getTrainingModels();
        ArrayListModel trainingModelInColorSpace = new ArrayListModel();

        for (int i = 0; i < trainingModel.getSize(); i++) {
            TrainingModel aTrainingModel = (TrainingModel) trainingModel.get(i);

            if (aTrainingModel.getColorSpace().equals(colorSpace)) {
                trainingModelInColorSpace.add(aTrainingModel);
            }
        }

        // Add training libraries to the combo box
        ListModel       listModel          = new ArrayListModel(trainingModelInColorSpace);
        ValueModel      selectedItemHolder = new ValueHolder();
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        libraryNameComboBox.setModel(new ComboBoxAdapter(selectionInList));

        // Select the first one
        if (!selectionInList.isEmpty()) {
            libraryNameComboBox.setSelectedIndex(0);
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
}
