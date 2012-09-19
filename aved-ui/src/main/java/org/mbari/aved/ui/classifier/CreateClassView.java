/*
 * @(#)CreateClassView.java
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

import com.google.inject.Injector;

import com.jeta.forms.components.image.ImageComponent;
import com.jeta.forms.gui.form.FormAccessor;

import com.jgoodies.binding.adapter.ComboBoxAdapter;
import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import org.jdesktop.swingx.JXTree;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.classifier.knowledgebase.KnowledgeBaseUtil;

import vars.ToolBelt;

import vars.knowledgebase.ui.Lookup;

import vars.shared.ui.tree.ConceptTreeCellRenderer;
import vars.shared.ui.tree.ConceptTreeModel;
import vars.shared.ui.tree.ConceptTreePanel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Image;
import java.awt.event.WindowListener;

import java.io.File;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class CreateClassView extends JFrameView {
    private static final String ID_CLASS_DESCRIPTION_TEXTAREA = "classdescription";    // ""
    private static final String ID_CLASS_NAME_TEXTFIELD       = "classname";           // javax.swing.JTextArea
    private static final String ID_CLASS_NAME_PRED_TEXTFIELD  = "predictedclassname"; // ""
    private static final String ID_COLORSPACE_COMBOBOX        = "colorspace";          // ""
    private static final String ID_DELETE_BUTTON              = "delete";              // javax.swing.JButton
    private static final String ID_IMAGE_COMPONENT            = "classimage";

    /*
     * Component names in the CreateClassView form If any of the component name
     * are changed in the Abeille form designer, they should be modified here
     * too
     */
    private static final String  ID_IMAGE_DIRECTORY_COMBOBOX = "imagedirectory";    // javax.swing.JCombobBox
    private static final String  ID_KNOWLEDGE_BASE_PANEL     = "knowledgebase";
    private static final String  ID_NUM_CLASS_IMAGES_LABEL   = "numclassimages";    // javax.swing.JLabel
    private static final String  ID_RUN_BUTTON               = "run";               // javax.swing.JButton
    private static final String  ID_STOP_BUTTON              = "stop";              // ""
    private final JTextArea      classDescriptionTextArea;
    private ImageComponent       classImageComponent;
    private final JTextField     classNamePredictedClassName;
    private ConceptTreePanel     conceptTreePanel;
    private final AbstractButton deleteBtn;
    private final JComboBox      dirComboBox, colorSpaceComboBox;
    private final JPanel         knowledgeBasePanel;
    private final JLabel         numImagesLabel;
    private final JTextField     classNameTextField;

    CreateClassView(ClassifierModel model, CreateClassController controller) {
        super("org/mbari/aved/ui/forms/ClassifierCreateClass.xml", model, controller);

        // loadModel frequently accessed components
        numImagesLabel              = getForm().getLabel(ID_NUM_CLASS_IMAGES_LABEL);
        dirComboBox                 = getForm().getComboBox(ID_IMAGE_DIRECTORY_COMBOBOX);
        colorSpaceComboBox          = getForm().getComboBox(ID_COLORSPACE_COMBOBOX);
        classNameTextField          = getForm().getTextField(ID_CLASS_NAME_TEXTFIELD);
        classNamePredictedClassName = getForm().getTextField(ID_CLASS_NAME_PRED_TEXTFIELD);
        classDescriptionTextArea    = (JTextArea) getForm().getTextComponent(ID_CLASS_DESCRIPTION_TEXTAREA);
        knowledgeBasePanel          = getForm().getPanel(ID_KNOWLEDGE_BASE_PANEL);
        classImageComponent         = (ImageComponent) getForm().getComponentByName(ID_IMAGE_COMPONENT);
        dirComboBox.setRenderer(new ComboBoxRenderer());
        deleteBtn = getForm().getButton(ID_DELETE_BUTTON);

        ActionHandler actionHandler = getActionHandler();

        getForm().getButton(ID_RUN_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_STOP_BUTTON).addActionListener(actionHandler);
        deleteBtn.addActionListener(actionHandler);
        dirComboBox.addActionListener(actionHandler);
        colorSpaceComboBox.addActionListener(actionHandler);

        // Populate the color space combo box
        ArrayListModel list = new ArrayListModel();

        list.addAll(Arrays.asList(ColorSpace.values()));

        ListModel       listModel          = new ArrayListModel(list);
        ValueModel      selectedItemHolder = new ValueHolder("Select a color space");
        SelectionInList selectionInList    = new SelectionInList(listModel, selectedItemHolder);

        colorSpaceComboBox.setModel(new ComboBoxAdapter(selectionInList));

        // Insert a default icon
        URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

        initializeImageComponent(new File(url.getFile()));

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "Create Class");
        this.getRootPane().setDefaultButton((JButton) getForm().getButton(ID_RUN_BUTTON));
        this.addWindowListener((WindowListener) this.getController());
    }

    public void replaceKnowledgeBasePanel(JPanel panel) {
        FormAccessor formAccessor = getForm().getFormAccessor("class.form");

        if ((panel != null) && (knowledgeBasePanel != null)) {
            formAccessor.replaceBean(knowledgeBasePanel, panel);

            // most of the tool tips are handled by the Abeille designer, but this
            // is a dynamic compoment so set it here
            panel.setToolTipText("VARS knowledge base. Double-click "
                                 + "on any element to copy into the class name field.");
        }
    }

    /**
     * Creates the VARS Knowledge Base concept tree and registers a mouse
     * listener to the tree
     * @param listener the listen to attach to the concept tree
     */
    public void createConceptTree(TreeSelectionListener listener) {
        if (KnowledgeBaseUtil.isKnowledgebaseAvailable()) {
            Injector injector = (Injector) Lookup.getGuiceInjectorDispatcher().getValueObject();
            ToolBelt toolBelt = injector.getInstance(ToolBelt.class);

            conceptTreePanel = new ConceptTreePanel(toolBelt.getKnowledgebaseDAOFactory());

            final ConceptTreeModel treeModel = new ConceptTreeModel(toolBelt.getKnowledgebaseDAOFactory());
            JXTree                 tree      = new JXTree(treeModel);

            tree.addTreeSelectionListener(listener);
            tree.setCellRenderer(new ConceptTreeCellRenderer());
            conceptTreePanel.setJTree(tree);
            conceptTreePanel.setOpaque(true);
            replaceKnowledgeBasePanel(conceptTreePanel);
        }
    }

    /**
     * Get the class name
     * @return the class name either user-defined or from VARS knowledge base
     */
    String getClassName() {
        return classNameTextField.getText();
    }

    /**
     * Get the color space
     * @return the @{link org.mbari.aved.classifier.ColorSpace}
     */
    ColorSpace getColorSpace() {
        return (ColorSpace) colorSpaceComboBox.getSelectedItem();
    }
    
    /**
     * Set the color space
     * @return the @{link org.mbari.aved.classifier.ColorSpace}
     */
    void setColorSpace(ColorSpace color) {
        colorSpaceComboBox.setSelectedItem(color);
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
     * Get the predicted class name
     * @return
     */
    String getClassNamePredicted() {
        return classNamePredictedClassName.getText();
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

    @Override
    public void modelChanged(ModelEvent event) {}

    /**
     * Sets the data model that the image directory <code>JComboBox</code>
     * uses to obtain the list of items.
     *
     * @param directories the list of directories to populate the box with
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
        classNamePredictedClassName.setText(model.getPredictedName());
        classDescriptionTextArea.setText(model.getDescription());

        try {
            ArrayList<String> fileList = model.getRawImageFileListing();
 
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
     * Gets the image directory selected in the JComboBox
     * @param f
     */
    File getSelectImageDirectory() {
        return (File) dirComboBox.getSelectedItem();
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
     * Sets the predicted name in the correct JLabel
     * @param name the predicted name.
     */
    void setPredictedName(String name) {
        classNamePredictedClassName.setText(name);
    }

    /**
     * Enables/disables the delete button
     * @param state
     */
    void setEnabledDeleteButton(boolean state) {
        deleteBtn.setEnabled(state);
    }

    /**
     * Custom renderer for displaying full file paths as a tooltip
     * when selecting the image directory combo box
     */
    class ComboBoxRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
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
            setText((value == null)
                    ? ""
                    : value.toString());

            return this;
        }
    }
}
