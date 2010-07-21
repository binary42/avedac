/*
 * @(#)CreateClassView.java
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
import com.jeta.forms.gui.form.FormAccessor;

import com.jgoodies.binding.adapter.ComboBoxAdapter;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import java.awt.Component;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

import java.awt.Image;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;

import java.io.File;

import java.net.URL;

import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class CreateClassView extends JFrameView {

    private static final String ID_CLASS_DESCRIPTION_TEXTAREA = "classdescription";    // ""
    private static final String ID_CLASS_NAME_TEXTFIELD = "classname";           // javax.swing.JTextArea
    private static final String ID_CLASS_NAME_VARS_TEXTFIELD = "classsnamevars";      // ""
    private static final String ID_COLORSPACE_COMBOBOX = "colorspace";          // ""
    private static final String ID_IMAGE_COMPONENT = "classimage";

    /*
     * Component names in the CreateClassView form If any of the component name
     * are changed in the Abeille form designer, they should be modified here
     * too
     */
    private static final String ID_IMAGE_DIRECTORY_COMBOBOX = "imagedirectory";    // javax.swing.JCombobBox
    private static final String ID_KNOWLEDGE_BASE_PANEL = "knowledgebase";
    private static final String ID_NUM_CLASS_IMAGES_LABEL = "numclassimages";    // javax.swing.JLabel
    private static final String ID_RUN_BUTTON = "run";               // javax.swing.JButton
    private static final String ID_STOP_BUTTON = "stop";              // ""
    private final JTextArea classDescriptionTextArea;
    private ImageComponent classImageComponent;
    private final JTextField classNameTextField;
    private final JTextField classNameVarsTextField;
    private ConceptTreePanel conceptTreePanel;
    private final JComboBox dirComboBox, colorSpaceComboBox;
    private final JPanel knowledgeBasePanel;
    private final JLabel numImagesLabel;

    CreateClassView(ClassifierModel model, CreateClassController controller) {
        super("org/mbari/aved/ui/forms/ClassifierCreateClass.xml", model, controller);

        // loadModel frequently accessed components
        numImagesLabel = getForm().getLabel(ID_NUM_CLASS_IMAGES_LABEL);
        dirComboBox = getForm().getComboBox(ID_IMAGE_DIRECTORY_COMBOBOX);
        colorSpaceComboBox = getForm().getComboBox(ID_COLORSPACE_COMBOBOX);
        classNameTextField = getForm().getTextField(ID_CLASS_NAME_TEXTFIELD);
        classNameVarsTextField = getForm().getTextField(ID_CLASS_NAME_VARS_TEXTFIELD);
        classDescriptionTextArea = (JTextArea) getForm().getTextComponent(ID_CLASS_DESCRIPTION_TEXTAREA);
        knowledgeBasePanel = getForm().getPanel(ID_KNOWLEDGE_BASE_PANEL);
        classImageComponent = (ImageComponent) getForm().getComponentByName(ID_IMAGE_COMPONENT);

        dirComboBox.setRenderer(new ComboBoxRenderer());
        
        // / most of the tool tips are handled by the Abeille designer, but this
        // is a dynamic compoment so set it here
        knowledgeBasePanel.setToolTipText("VARS knowledge base. Double-click "
                + "on any element to copy into the class name field.");

        ActionHandler actionHandler = getActionHandler();

        getForm().getButton(ID_RUN_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_STOP_BUTTON).addActionListener(actionHandler);
        dirComboBox.addActionListener(actionHandler);
        colorSpaceComboBox.addActionListener(actionHandler);

        // Populate the color space combo box
        ArrayListModel list = new ArrayListModel();

        list.add(ColorSpace.GRAY);
        list.add(ColorSpace.RGB);
        list.add(ColorSpace.YCBCR);

        ListModel listModel = new ArrayListModel(list);
        ValueModel selectedItemHolder = new ValueHolder("Select a color space");
        SelectionInList selectionInList = new SelectionInList(listModel, selectedItemHolder);
         
        colorSpaceComboBox.setModel(new ComboBoxAdapter(selectionInList));

        // default to RGB color space
        colorSpaceComboBox.setSelectedIndex(1);

        // Insert a default icon
        URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

        initializeImageComponent(new File(url.getFile()));

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Create Class");
        this.addWindowListener((WindowListener) this.getController());
    }

    /**
     * Custom renderer for displaying full file paths as a tooltip
     * when selecting the image directory combo box
     */
    class ComboBoxRenderer extends BasicComboBoxRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                if (-1 < index) { 
                    if (value.getClass().equals(File.class)) {
                        String text = ((File) value).getAbsolutePath();
                        list.setToolTipText(text);
                    }
                }
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setFont(list.getFont());
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    public void replaceKnowledgeBasePanel(JPanel panel) {
        FormAccessor formAccessor = getForm().getFormAccessor("class.form");

        if ((panel != null) && (knowledgeBasePanel != null)) {
            formAccessor.replaceBean(knowledgeBasePanel, panel);
        }
    }

    /**
     * Creates the VARS Knowledge Base concept tree and registers a mouse
     * listener to the tree
     * @param listener the listener to attach to the concept tree
     */
    public void createConceptTree(MouseListener listener) {
 
        conceptTreePanel = new ConceptTreePanel(listener);
        conceptTreePanel.setOpaque(true);
        conceptTreePanel.buildPanel();
        replaceKnowledgeBasePanel(conceptTreePanel);
        pack();
    }

    /**
     * Get the class name
     * @return the class name either user-defined or from VARS knowledge base
     */
    String getClassName() {
        return classNameTextField.getText();
    }

    /**
     * Get the color sapce
     * @return the @{link org.mbari.aved.classifier.ColorSpace}
     */
    ColorSpace getColorSpace() {
        return (ColorSpace) colorSpaceComboBox.getSelectedItem();
    }

    /**
     * Gets the concept tree panel
     * @return the concept tree panel
     */
    ConceptTreePanel getConceptTreePanel() {
        return conceptTreePanel;
    }

    /**
     * Get the description of this class. This is a free-form description
     * that describes what this class is in more detail than the class name
     * @return the description
     */
    String getDescription() {
        return classDescriptionTextArea.getText();
    }

    /**
     * Get the image directory that contains images that represent this class
     * @return the image directory
     */
    File getImageDirectory() {
        Object dir = dirComboBox.getSelectedItem();

        if (dir.getClass().equals(File.class)) {
            return (File) dir;
        } else {
            return new File(dir.toString());
        }
    }

    /**
     * Get the vars concept name
     * @return
     */
    String getClassNameVars() {
        return classNameVarsTextField.getText();
    }

    /**
     * Enables/disables the Run JButton in this view
     * @param b  true to enable the button, otherwise false
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
     * Helper class to return the model associated with this view
     * @return
     */
    @Override
    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    public void modelChanged(ModelEvent event) {
    }

    /**
     * Sets the data model that the image directory <code>JComboBox</code>
     * uses to obtain the list of items.
     *
     * @param directories the list of directories to populte the box with
     *
     */
    void initializeImageDirectories(File[] directories) {
        dirComboBox.setModel(new DefaultComboBoxModel(directories));
    }

    /**
     * Loads the model data into the view
     * @param model the model to load
     */
    void loadModel(ClassModel model) {
        classNameTextField.setText(model.getName());
        classNameVarsTextField.setText(model.getVarsClassName());
        classDescriptionTextArea.setText(model.getDescription()); 
        try {
            ArrayList<String> fileList = model.getRawImageFileListing();
            Thread.sleep(500);
            Integer numImages = Integer.valueOf(fileList.size());

            numImagesLabel.setText(numImages.toString());

            if (fileList.size() > 0) {
                File exampleImage = new File(model.getRawImageDirectory() + "/" + fileList.get(0)); 
                initializeImageComponent(exampleImage);
            } else {

                // Insert a default icon
                URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");
                initializeImageComponent(new File(url.getFile()));
            }
        } catch (Exception ex) {
            Logger.getLogger(CreateClassView.class.getName()).log(Level.SEVERE, null, ex);

        }

        // TODO: if knowledgeBasePanel is loaded, scroll down
        // to the class designation
    }

    /**
     * Initializes the image component in this view with the image
     * found in the <code>imageFile</code>. If no valid image is found
     * a default one will be used.
     * @param imageFile
     */
    private void initializeImageComponent(File imageFile) {
        ImageIcon icon = ClassModelListRenderer.createImageIcon(imageFile);

        if (icon == null) {

            // Insert a default icon here
            URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

            icon = new ImageIcon(url);
        }

        // Scale icons to a smaller size
        if (icon != null) {
            Image image = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

            icon = ClassModelListRenderer.convertImageIcon(image, getColorSpace());
        }

        classImageComponent.setIcon(icon);
        getForm().repaint();
    }

    /**
     * Selects the image directory in the JComboBox
     * @param f
     */
    void selectImageDirectory(File f) {
        dirComboBox.setSelectedItem(f);
    }

    /**
     * Adds the image directory to the JComboBox if it doesn't already
     * exist
     * @param f
     */
    void addImageDirectory(File f) {
        dirComboBox.addItem(f);
    }

    /**
     * Sets the VARS concept name in the class name label
     * @param name the VARS concept name.
     */
    void setVarsName(String name) {
        classNameVarsTextField.setText(name);
    }
}
