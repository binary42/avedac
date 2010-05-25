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
import org.mbari.aved.ui.userpreferences.UserPreferencesModel;
import org.mbari.aved.ui.utils.ImageUtils;
import org.mbari.aved.ui.utils.ParseUtils;
 
//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;

import java.security.AccessControlException;
import java.security.AccessController;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

class CreateClassController extends AbstractController implements ModelListener, MouseListener, WindowListener {
    private ClassModel           classModel;
    private final EventListModel eventListModel;
    private CreateClassWorker    worker;

    CreateClassController(ClassifierModel model, EventListModel eventListModel) {
        setModel(model);
        setView(new CreateClassView(model, this));
        this.eventListModel = eventListModel;
        classModel          = new ClassModel();

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
        String               actionCommand = e.getActionCommand();
        UserPreferencesModel prefs         = UserPreferences.getModel();

        if (actionCommand.equals("Browse")) {

            // Set the filechooser with the last imported directory
            JFileChooser chooser    = new JFileChooser();
            File         defaultDir = prefs.getLastOpenedClassTrainingDirectory();

            chooser.setCurrentDirectory(defaultDir);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Choose directory to import");

            if (chooser.showOpenDialog(getView()) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();

                prefs.setLastOpenedClassTrainingDirectory(f);
                getView().addImageDirectory(f);
                getView().selectImageDirectory(f);
            }
        } else if (actionCommand.equals("imageDirComboBoxChanged")) {
            try {
                JComboBox box = (JComboBox) e.getSource();
                Object    dir = box.getSelectedItem();
                File      directory;

                if (dir.getClass().equals(File.class)) {
                    directory = (File) dir;
                } else {
                    directory = new File(dir.toString());
                }

                classModel.setDescription(directory.getName());
                classModel.setName(directory.getName());
                classModel.setVarsClassName(directory.getName());
                classModel.setRawImageDirectory(directory);
                getView().loadModel(classModel);
            } catch (Exception ex) {
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
            JComboBox  box        = ((JComboBox) e.getSource());
            ColorSpace colorSpace = (ColorSpace) box.getSelectedItem();

            if (classModel != null) {
                classModel.setColorSpace(colorSpace);
                getView().loadModel(classModel);
            }
        } else if (actionCommand.equals("Stop")) {
            if (worker != null) {
                worker.cancelWorker(false);
            }
        } else if (actionCommand.equals("Run")) {
            try {
                String className      = classModel.getName();
                File   imageDirectory = classModel.getRawImageDirectory();
                String varsClassName  = classModel.getVarsClassName();

                if (className.length() == 0) {
                    String                message = new String("Class name is empty. Please enter a new class name.");
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (varsClassName.length() == 0) {
                    String                message = new String("Vars class name is empty. Please select a new name.");
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (imageDirectory.length() == 0) {
                    String                message =
                        new String("Image directory is empty. Please enter a valid image directory");
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

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

                ClassModel newModel = new ClassModel();

                newModel.setVarsClassName(classModel.getVarsClassName());
                newModel.setName(classModel.getName()); 
                newModel.setDescription(classModel.getDescription());
                newModel.setRawImageDirectory(classModel.getRawImageDirectory());
                newModel.setColorSpace(classModel.getColorSpace()); 
                newModel.setDatabaseRoot(UserPreferences.getModel().getClassDatabaseDirectory());
                
                worker = new CreateClassWorker(newModel);
                worker.execute();
                getView().setRunButton(false);
                getView().setStopButton(true);
            } catch (Exception ex) {
                NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), ex.getMessage());

                dialog.setVisible(true);
                dialog.answer();
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
                getView().setRunButton(true);
                getView().setStopButton(false);
            }
        }
    }

    /**
     * Model listener. Reacts to changes in the
     * {@link org.mbari.aved.ui.classifier.model}
     * and  {@link org.mbari.aved.ui.model.EventListModel}
     */
    public void modelChanged(ModelEvent event) {
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {

            // When the *-training class directory changes, update the available
            // classes
            case ClassifierModel.ClassifierModelEvent.TRAINING_CLASS_DIR_UPDATED :
                File dir       = UserPreferences.getModel().getLastOpenedClassTrainingDirectory();
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
                                if (subdirs[i].equals(dir)) {
                                    getView().selectImageDirectory(dir);

                                    break;
                                }
                            }
                        }    // Otherwise, select the first subdirectory
                                else {
                            if (subdirs.length > 0) {
                                getView().selectImageDirectory(subdirs[0]);
                            }
                        }
                    }
                }

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

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void windowClosing(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowActivated(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}

    private class CreateClassWorker extends MatlabWorker {
        private ClassModel            newClassModel;
        private final ProgressDisplay progressDisplay;

        public CreateClassWorker(ClassModel newClassModel) throws Exception {
            super(newClassModel.getName());
            this.newClassModel   = newClassModel;
            this.progressDisplay = new ProgressDisplay(this, "Creating class " + newClassModel.getName());
        }

        /**
         * Resets the start/stop buttons and disables the progress display.
         */
        private void reset() {
            getView().setRunButton(true);
            getView().setStopButton(false);
            progressDisplay.getView().setVisible(false);
        }

        @Override
        protected Object doInBackground() throws Exception {
            progressDisplay.display("Creating class ...");

            ArrayList<String> filePaths = newClassModel.getRawImageFileListing();
            LibraryImage[]    imageset  = new LibraryImage[filePaths.size()];
            File              rootPath  = newClassModel.getRawImageDirectory();
            File              path      = new File(rootPath.toString() + "/square/");

            newClassModel.setSquareImageDirectory(path);

            // Check if you have permission to write to the target
            // parent directory
            File parent = new File(path.getParent());

            if (!parent.canWrite()) {
                NonModalMessageDialog dialog;

                dialog = new NonModalMessageDialog(getView(),
                                                   "You do not have write permission to " + parent.toString()
                                                   + ". Please correct this.");
                dialog.setVisible(true);

                if (dialog.answer()) {
                    reset();

                    return this;
                }
            }

            // Create the new directory for storing squared images
            if (!path.exists()) {
                try {
                    path.mkdir();
                } catch (SecurityException ex) {

                    // Should never get here with the above checkWrite
                    Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);

                    NonModalMessageDialog dialog;

                    dialog = new NonModalMessageDialog(getView(), ex.getMessage());
                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        reset();

                        return this;
                    }
                }
            }  

            // Convert images to square images as required for the classifier
            for (int i = 0; i < filePaths.size(); i++) {
                
                // If user cancelled, return
                if (isCancelled()) {
                    reset();

                    return this;
                }

                String f            = rootPath + "/" + filePaths.get(i);
                String imageFileOut = path + "/" + ParseUtils.removeFileExtension(filePaths.get(i)) + ".jpg";
                File   image        = new File(imageFileOut);

                // Only create the square image if it doesn't exist to save
                // some time
                if (!image.exists()) {
                    progressDisplay.display("Creating square image " + image.getName() + imageFileOut.toString());

                    try {
                        ImageUtils.squareJpegThumbnail(f, imageFileOut);
                    } catch (Exception ex) {
                        NonModalMessageDialog dialog;

                        dialog = new NonModalMessageDialog(getView(), ex.getMessage());
                        dialog.setVisible(true);

                        return this;
                    }
                }

                imageset[i] = new LibraryImage(imageFileOut);
            }

            ProgressDisplayStream progressDisplayStream = null;

            // Get a input stream on the matlab log file to display in
            // the progress display window
            try {
                ClassifierLibraryJNI app = Classifier.getLibrary();
                InputStreamReader    isr = Classifier.getInputStreamReader();

                progressDisplayStream = new ProgressDisplayStream(progressDisplay, isr);
                progressDisplayStream.execute();

                // Run the collection - this creates a new class
                app.collect_class(this.getCancel(), newClassModel.getRawImageDirectory().toString(),
                                  newClassModel.getSquareImageDirectory().toString(), newClassModel.getName(),
                                  newClassModel.getDatabaseRootdirectory().toString(),
                                  newClassModel.getVarsClassName(), newClassModel.getDescription(),
                                  newClassModel.getColorSpace());

                // Add the model only after successfully created
                getModel().addClassModel(newClassModel);
                progressDisplayStream.isDone = true;
                progressDisplay.getView().dispose();

                NonModalMessageDialog dialog;

                dialog = new NonModalMessageDialog(getView(), newClassModel.getName() + " class finished");
                dialog.setVisible(true);
            } catch (Exception ex) {

                // if (this.getCancel() == 0) {
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);

                NonModalMessageDialog dialog;

                dialog = new NonModalMessageDialog(getView(), ex.getMessage());
                dialog.setVisible(true);

                if (dialog.answer()) {
                    reset();

                    return null;
                }

                // }
            }

            setProgress(0);
            progressDisplayStream.isDone = true;
            reset();

            return this;
        }
    }
}
