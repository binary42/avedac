/*
 * @(#)CreateClassController.java
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
import org.mbari.aved.classifier.LibraryImage;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.progress.ProgressDisplayStream;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.utils.ImageUtils;
import org.mbari.aved.ui.utils.ParseUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileFilter;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import org.jdesktop.swingworker.SwingWorker;

class CreateClassController extends AbstractController implements ModelListener, MouseListener, WindowListener {

    private static final ClassModel classModel = new ClassModel();
    private static CreateClassTask task;

    CreateClassController(ClassifierModel model, EventListModel eventListModel) {
        setModel(model);
        setView(new CreateClassView(model, this));

        // Register as listener to the models
        getModel().addModelListener(this);
        eventListModel.addModelListener(this);

        // Create the concept tree
        getView().createConceptTree(this);
    }

    @Override
    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    @Override
    public CreateClassView getView() {
        return (CreateClassView) super.getView();
    }

    /**
     * Operation handler for handling actions initiated in the view
     *
     * @param actionCommand A semantic event which indicates that a
     * component-defined action occurred.
     */
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("imageDirComboBoxChanged")) {
            try {
                JComboBox box = (JComboBox) e.getSource();
                Object dir = box.getSelectedItem();
                File directory;

                if (dir.getClass().equals(File.class)) {
                    directory = (File) dir;
                } else {
                    directory = new File(dir.toString());
                }

                if (directory != null) {
                    File d = UserPreferences.getModel().getClassDatabaseDirectory();
                    classModel.setDatabaseRoot(d);
                    classModel.setRawImageDirectory(directory);
                    classModel.setDescription(directory.getName());
                    classModel.setName(directory.getName());
                    classModel.setVarsClassName(directory.getName());
                    getView().loadModel(classModel);
                }
            } catch (Exception ex) {
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
            JComboBox box = ((JComboBox) e.getSource());
            ColorSpace colorSpace = (ColorSpace) box.getSelectedItem();

            if (classModel != null) {
                classModel.setColorSpace(colorSpace);
                getView().loadModel(classModel);
            }
        } else if (actionCommand.equals("Stop")) {
            if (task != null) {
                getView().setRunButton(true);
                getView().setStopButton(false);
                Classifier.getController().kill(task);
            }
        } else if (actionCommand.equals("Run")) {
            try {
                String className = classModel.getName();
                File imageDirectory = classModel.getRawImageDirectory();
                String varsClassName = classModel.getVarsClassName();

                if (className.length() == 0) {
                    String message = new String("Class name is empty. Please enter a new class name.");
                    NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (varsClassName.length() == 0) {
                    String message = new String("VARS class name is empty. Please select a new name.");
                    NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (imageDirectory.length() == 0) {
                    String message =
                            new String("Image directory is empty. Please enter a valid image directory");
                    NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (!imageDirectory.exists()) {
                    String message = new String(imageDirectory
                            + " does not exist. Please enter a valid image directory");
                    NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                //ClassModel newModel = classModel.copy();
                final ClassModel newModel = new ClassModel();
                newModel.setVarsClassName(classModel.getVarsClassName());
                newModel.setName(classModel.getName());
                newModel.setDescription(classModel.getDescription());
                newModel.setRawImageDirectory(classModel.getRawImageDirectory());
                newModel.setColorSpace(classModel.getColorSpace());
                File d = UserPreferences.getModel().getClassDatabaseDirectory();
                newModel.setDatabaseRoot(d);

                // Check if you have permission to write to the target
                // parent directory
                ArrayList<String> filePaths = newModel.getRawImageFileListing(); 
                File rootPath = newModel.getRawImageDirectory();
                File path = new File(rootPath.toString() + "/square/");
                File parent = new File(path.getParent());

                if (!parent.canWrite()) {
                    String message = new String(
                            "You do not have write permission to " + parent.toString()
                            + ". Please correct this.");
                    NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);
                    dialog.setVisible(true);
                    dialog.answer();
                    return;
                }

                // Create the command for class creation and add it to the queue
                final SwingWorker worker = Classifier.getController().getWorker();
                task = new CreateClassTask(worker, newModel);
                Classifier.getController().addQueue(task);
                getView().setRunButton(false);
                getView().setStopButton(true);

                /// Create a progress display thread for monitoring this task
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        BufferedReader br = Classifier.getController().getBufferedReader();
                        ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                                "Creating class " + newModel.getName());
                        progressDisplay.getView().setVisible(true);

                        ProgressDisplayStream progressDisplayStream = new ProgressDisplayStream(progressDisplay, br);
                        progressDisplayStream.execute();
                        while (!task.isCancelled() && !task.isFini()) {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ex) {
                            }
                        }
                        getView().setRunButton(true);
                        getView().setStopButton(false); 
                        progressDisplay.getView().dispose(); 

                        // Add the model only after successfully created
                        if (task.isFini() && getModel() != null) {
                            try {
                                getModel().addClassModel(newModel);
                                NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), newModel.getName() + " class creation finished");
                                dialog.setVisible(true);
                            } catch (Exception ex) {
                                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        else {
                            if (task.isCancelled()) {
                                NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), newModel.getName() + " classcreation stopped");
                                dialog.setVisible(true);
                            }
                        }
                    }
                };

                thread.start();

            } catch (Exception ex) {
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
                NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), ex.getMessage());
                dialog.setVisible(true);
                dialog.answer();

                getView().setRunButton(true);
                getView().setStopButton(false);
            }
        }
    }

    /**
     * Update the training classes directories in the view
     */
    private void updateTrainingClasses() {
        File dir = UserPreferences.getModel().getLastOpenedClassTrainingDirectory();
        File parentDir = getModel().getClassTrainingImageDirectory();

        if (parentDir != null) {

            // This filter only returns directories
            FileFilter fileFilter = new FileFilter() {

                public boolean accept(File file) {
                    return file.isDirectory();
                }
            };
            File[] subdirs = parentDir.listFiles(fileFilter);

            // Populate the view with subdirectories
            if ((subdirs != null) && (subdirs.length > 0)) {
                getView().initializeImageDirectories(subdirs);
            }

            // Select the last selected directory if it is valid
            // and in  the list of subdirectories
            if (subdirs != null) {
                if ((dir != null) && dir.isDirectory()) {
                    for (int i = 0; i < subdirs.length; i++) {
                        if (subdirs[i].getName().equals(dir.getName())) {
                            getView().selectImageDirectory(dir);

                            break;
                        }
                        // Otherwise, select the first subdirectory
                        getView().selectImageDirectory(subdirs[0]);
                    }
                } // Otherwise, select the first subdirectory
                else {
                    if (subdirs.length > 0) {
                        getView().selectImageDirectory(subdirs[0]);
                    }
                }
            }
        }
    }

    /**
     * Model listener. Reacts to changes in the
     * {@link org.mbari.aved.ui.classifier.model} 
     */
    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {

                // When the *-training class directory changes, update the available
                // classes
                case ClassifierModel.ClassifierModelEvent.TRAINING_CLASS_DIR_UPDATED:
                    updateTrainingClasses();
                    break;
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        ConceptTreePanel conceptTreePanel = getView().getConceptTreePanel();

        if ((conceptTreePanel != null) && (e.getClickCount() == 2)) {
            String name = conceptTreePanel.getSelectedConceptName();

            getView().setVarsName(name);
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void windowClosing(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    private class CreateClassTask extends ClassifierLibraryJNITask {

        private ClassModel newClassModel;

        public CreateClassTask(SwingWorker parentWorker, ClassModel newClassModel) throws Exception {
            super(newClassModel.getName());
            this.newClassModel = newClassModel;
        }

        protected void run(ClassifierLibraryJNI library) throws Exception {

            ArrayList<String> filePaths = newClassModel.getRawImageFileListing();
            LibraryImage[] imageset = new LibraryImage[filePaths.size()];
            File rootPath = newClassModel.getRawImageDirectory();
            File path = new File(rootPath.toString() + "/square/");

            newClassModel.setSquareImageDirectory(path);

            // Check if you have permission to write to the target
            // parent directory
            File parent = new File(path.getParent());

            if (!parent.canWrite()) {
                String message = new String(
                        "Incorrect write permission to " + parent.toString());
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, message);
                return;
            }

            // Create the new directory for storing squared images
            if (!path.exists()) {
                try {
                    path.mkdir();
                } catch (SecurityException ex) {

                    // Should never get here with the above checkWrite
                    Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);

                }
            }

            // Convert images to square images as required for the classifier
            for (int i = 0; i < filePaths.size(); i++) {

                // If user cancelled, return
                if (isCancelled()) {
                    return;
                }

                String f = rootPath + "/" + filePaths.get(i);
                String imageFileOut = path + "/" + ParseUtils.removeFileExtension(filePaths.get(i)) + ".jpg";
                File image = new File(imageFileOut);

                // Only create the square image if it doesn't exist to save
                // some time
                if (!image.exists()) {

                    try {
                        ImageUtils.squareJpegThumbnail(f, imageFileOut);
                    } catch (Exception ex) {
                        NonModalMessageDialog dialog;

                        dialog = new NonModalMessageDialog(getView(), ex.getMessage());
                        dialog.setVisible(true);
                        return;
                    }
                }

                imageset[i] = new LibraryImage(imageFileOut);
            }

            // Get a input stream on the matlab log file to display in
            // the progress display window
            try {

                // Run the collection - this creates a new class
                library.collect_class(this.getCancel(), newClassModel.getRawImageDirectory().toString(),
                        newClassModel.getSquareImageDirectory().toString(), newClassModel.getName(),
                        newClassModel.getDatabaseRootdirectory().toString(),
                        newClassModel.getVarsClassName(), newClassModel.getDescription(),
                        newClassModel.getColorSpace());

                this.setFini();

            } catch (Exception ex) {
                // Only log if this was an exception caused by a non-user cancel
                if (!this.isCancelled()) {
                    Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
