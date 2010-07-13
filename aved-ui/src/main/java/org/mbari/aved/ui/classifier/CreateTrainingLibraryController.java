/*
 * @(#)CreateTrainingLibraryController.java
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
import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.progress.ProgressDisplayStream;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.ImageFileFilter;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.io.InputStreamReader;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import org.jdesktop.swingworker.SwingWorker;

public class CreateTrainingLibraryController extends AbstractController implements ModelListener {

    /** True when a popup window is displayed */
    private Boolean hasPopup = false;
    private final MouseClickJListActionHandler mouseClickJListActionHandler;
    private TrainingModel trainingModel;
    private CreateTrainingLibraryTask task;

    /**
     * Constructor
     * @param model
     */
    public CreateTrainingLibraryController(ClassifierModel model) {
        setModel(model);
        setView(new CreateTrainingLibraryView(model, this));

        // Register as listener to the model
        getModel().addModelListener(this);
        mouseClickJListActionHandler = new MouseClickJListActionHandler();
        getView().addMouseClickListener(mouseClickJListActionHandler);

        try {

            // Create a default library
            File dbroot = UserPreferences.getModel().getClassDatabaseDirectory();

            trainingModel = new TrainingModel();
            trainingModel.setDatabaseRoot(dbroot);
        } catch (Exception ex) {
            Logger.getLogger(CreateTrainingLibraryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* Just some helper functions to access model and view */
    @Override
    public ClassifierModel getModel() {
        return ((ClassifierModel) super.getModel());
    }

    @Override
    public CreateTrainingLibraryView getView() {
        return ((CreateTrainingLibraryView) super.getView());
    }

    /**
     * Creates a {@link JFileChooser} to search for a new root directory
     * for the classifier training images
     */
    private void browse() {
        JFileChooser chooser = new JFileChooser();

        // Add a custom file filter and disable the default
        chooser.addChoosableFileFilter(new ImageFileFilter());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Initialize the chooser with the class directory
        File dir = UserPreferences.getModel().getClassTrainingImageDirectory();

        chooser.setCurrentDirectory(dir);
        chooser.setDialogTitle("Choose root training directory");

        if (chooser.showOpenDialog(getView()) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            getModel().setClassTrainingImageDirectory(file);
        } else {

            // TODO print error message box
            System.out.println("No Selection ");
        }
    }

    /**
     * Copies an item from avaiable to the selected list
     */
    private void copyItemToSelected() {
        if (!getView().getAvailableList().isSelectionEmpty()) {

            // get user selected items and indices
            Object item[] = getView().getAvailableList().getSelectedValues();

            // iterate through each item, adding to the selected list
            for (int i = 0; i < item.length; i++) {
                if (item[i].getClass().equals(ClassModel.class)) {
                    trainingModel.addClassModel((ClassModel) item[i]);
                }
            }

            getView().populateSelectedList(trainingModel);
        }
    }

    /**
     * Remove selected items from selected class JList
     */
    private void removeItemFromSelected() {
        if (!getView().getIncludeList().isSelectionEmpty()) {

            // get user selected items and indices
            Object item[] = (getView()).getIncludeList().getSelectedValues();

            for (int i = 0; i < item.length; i++) {

                // remove from selected list - the user has
                // decided not to select this
                trainingModel.removeClassModel((ClassModel) item[i]);
            }

            getView().populateSelectedList(trainingModel);
        }
    }

    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("Browse")) {
            browse();
        } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
            JComboBox box = ((JComboBox) e.getSource());
            ColorSpace newColorSpace = (ColorSpace) box.getSelectedItem();
            ColorSpace oldColorSpace = trainingModel.getColorSpace();

            trainingModel.setColorSpace(newColorSpace);
            getView().populateAvailableClassList(newColorSpace);

            // If a different color space, clear the selected list
            if (!newColorSpace.equals(oldColorSpace)) {
                getView().selectAll();
                removeItemFromSelected();
            }
        } else if (actionCommand.equals("Stop")) {
            if (task != null) {
                getView().setRunButton(true);
                getView().setStopButton(false);
                Classifier.getController().kill(task);
            }
        } else if (actionCommand.equals("Run")) {
            String trainingClassName = getView().getTrainingClassName();

            // Do some checking for valid name and number of classes
            if (trainingClassName.length() == 0) {
                String message = new String("Please enter a new library name.");
                NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                dialog.setVisible(true);

                if (dialog.answer()) {
                    return;
                }
            }

            trainingModel.setName(trainingClassName);

            if (trainingModel.getNumClasses() == 0) {
                String message = new String("Training class must have at least one class. "
                        + "Please select at least one class.");
                NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                dialog.setVisible(true);

                if (dialog.answer()) {
                    return;
                }
            }

            try {

                // If at least one class then create
                final SwingWorker worker = Classifier.getController().getWorker();
                task = new CreateTrainingLibraryTask(trainingModel);
                Classifier.getController().addQueue(task);
                getView().setRunButton(false);
                getView().setStopButton(true);

                /// Create a progress display thread for monitoring this task
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        InputStreamReader isr = Classifier.getController().getInputStreamReader();
                        ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                                "Creating training model " + trainingModel.getName());
                        progressDisplay.getView().setVisible(true);

                        ProgressDisplayStream progressDisplayStream = new ProgressDisplayStream(progressDisplay, isr);
                        progressDisplayStream.execute();
                        while (!task.isCancelled() && !task.isFini()) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ex) {
                            }
                        }
                        getView().setRunButton(true);
                        getView().setStopButton(false);
                        progressDisplay.getView().dispose();

                        if (task.isFini()) { 
                            // Add to training model when successfully run
                            getModel().addTrainingModel(trainingModel);

                            NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), trainingModel.getName() + " training library finished");
                            dialog.setVisible(true);
                        } else {
                            if (task.isCancelled()) {
                                NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), trainingModel.getName() + " training library stopped");
                                dialog.setVisible(true);
                            }
                        }
                    }
                };

                thread.start();

            } catch (Exception ex) {
                Logger.getLogger(CreateTrainingLibraryController.class.getName()).log(Level.SEVERE, null, ex);

                return;
            }
        } else if (actionCommand.equals("<<")) {
            removeItemFromSelected();
        } else if (actionCommand.equals(">>")) {
            copyItemToSelected();
        } else if (actionCommand.equals("availLibraryNameComboBoxChanged")) {
            JComboBox box = ((JComboBox) e.getSource());

            trainingModel = (TrainingModel) box.getSelectedItem();
            getView().selectColorSpace(trainingModel.getColorSpace());
            getView().populateSelectedList(trainingModel);
        }
    }

    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {

                // When the database root directory change or the models are updated
                // reset the color space
                case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED:
                case ClassifierModel.ClassifierModelEvent.CLASS_MODELS_UPDATED:

                    if (trainingModel != null) {
                        getView().selectColorSpace(trainingModel.getColorSpace());
                    }

                    break;
            }
        }
    }

    /**
     * Manages mouse clicks on the available and include class
     * JLists in the
     * {@link  org.mbari.aved.ui.classifier.CreateTrainingLibraryView}
     * <p>
     * On a single mouse-click shows a pop-up menu of an
     * {@link org.mbari.aved.ui.classifier.TrainingLibraryPopupMenu}
     * <p>
     * A double mouse-click either adds or removes a selection,
     * depending on what JList it originates from
     * <p>
     */
    void actionClickList(MouseEvent e) {
        JList list = (JList) e.getSource();

        if (e.getID() == MouseEvent.MOUSE_CLICKED) {

            // On double click, but not while a popup is showing
            if (!hasPopup) {
                if (e.getClickCount() == 2) {

                    // On a double click, add the class
                    if (list.equals(getView().getAvailableList())) {
                        copyItemToSelected();
                    }

                    if (list.equals(getView().getIncludeList())) {
                        removeItemFromSelected();
                    }
                } else {
                    // On a single click toggle selection highlight
                    // list.flipSelection();
                }
            }

            hasPopup = false;
        } else if (((e.getID() == MouseEvent.MOUSE_PRESSED) || (e.getID() == MouseEvent.MOUSE_RELEASED))
                && e.isPopupTrigger()) {

            // Only show popup if this is really a popup trigger
            Point pt = e.getPoint();
            TrainingLibraryPopupMenu popup = new TrainingLibraryPopupMenu(getView());

            popup.show(list, pt.x, pt.y);
            hasPopup = true;
        }
    }

    private class CreateTrainingLibraryTask extends ClassifierLibraryJNITask {

        TrainingModel model;

        public CreateTrainingLibraryTask(TrainingModel model) throws Exception {
            super(model.getName());
            this.model = model;
        }

        @Override
        protected void run(ClassifierLibraryJNI library) throws Exception {

            try {

                // Format a comma delimited list of class names for the classifier
                String trainingClasses = new String("");
                int numClassModels = model.getNumClasses();

                for (int i = 0; i < numClassModels; i++) {
                    ClassModel m = model.getClassModel(i);

                    if (i != 0) {
                        trainingClasses = trainingClasses + "," + m.getName();
                    } else {
                        trainingClasses = m.getName();
                    }
                }

                library.train_classes(this.getCancel(), trainingClasses, model.getName(),
                        model.getDatabaseRootdirectory().toString(),
                        model.getColorSpace(), model.getDescription());

                setFini();

            } catch (Exception ex) {
                if (!isCancelled()) {
                    Logger.getLogger(CreateTrainingLibraryController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return;
        }
    }

    /**
     * Subclass to handles mouse clicks in
     * {@link org.mbari.aved.ui.classifier.CreateTrainingLibraryView}
     */
    class MouseClickJListActionHandler implements MouseListener {

        public void mouseClicked(MouseEvent e) {
            actionClickList(e);
        }

        public void mouseEntered(MouseEvent e) {
            actionClickList(e);
        }

        public void mouseExited(MouseEvent e) {
            actionClickList(e);
        }

        public void mousePressed(MouseEvent e) {
            actionClickList(e);
        }

        public void mouseReleased(MouseEvent e) {
            actionClickList(e);
        }
    }
}
