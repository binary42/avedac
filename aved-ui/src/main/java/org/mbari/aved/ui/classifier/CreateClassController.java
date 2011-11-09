/*
 * @(#)CreateClassController.java
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

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.classifier.LibraryImage;
import org.mbari.aved.mbarivision.api.utils.Utils;
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

import vars.knowledgebase.Concept;

import vars.shared.ui.tree.ConceptTreeNode;
import vars.shared.ui.tree.ConceptTreePanel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
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
import javax.swing.JTree; 
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

class CreateClassController extends AbstractController implements ModelListener, WindowListener, TreeSelectionListener {
    private final static String     SQUARE_PATH = "/square";
    private ClassModel classModel  = new ClassModel() {};
    private static CreateClassTask  task;

    CreateClassController(ClassifierModel model, EventListModel eventListModel) {
        setModel(model);
        setView(new CreateClassView(model, this));

        // Register as listener to the models
        getModel().addModelListener(this);
        eventListModel.addModelListener(this);

        // Create the concept tree
        getView().createConceptTree(this);

        // Disable delete
        getView().setEnabledDeleteButton(false);
        
        // Default to RGB
        getView().setColorSpace(ColorSpace.RGB);
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
                Object    dir = box.getSelectedItem();
                File      directory;

                if (dir.getClass().equals(File.class)) {
                    directory = (File) dir;
                } else {
                    directory = new File(dir.toString());
                }

                if (directory != null) {
                    if (directory.getName().equals("training") || directory.getName().equals("features")
                            || directory.getName().equals("")) {
                        getView().setEnabledDeleteButton(false);

                        return;
                    }

                    getView().setEnabledDeleteButton(true);
                     
                    ClassModel m = getModel().getClassModel(directory.getName());
                    
                      if (m != null) {
                        classModel = m.copy();
                        classModel.setColorSpace(getView().getColorSpace());
                        classModel.updateFileList();
                    } else {
                        File d = UserPreferences.getModel().getClassDatabaseDirectory();
                        classModel.setDatabaseRoot(d);
                        classModel.setRawImageDirectory(directory);
                        classModel.setDescription(directory.getName());
                        classModel.setName(directory.getName());
                        classModel.setVarsClassName(directory.getName());
                        classModel.setColorSpace(getView().getColorSpace());
                        classModel.updateFileList();
                    }
                   
                    getView().loadModel(classModel);
                } else {
                    getView().setEnabledDeleteButton(false);
                }
            } catch (Exception ex) {
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);

                NonModalMessageDialog dialog = new NonModalMessageDialog(getView(), ex.getMessage());

                dialog.setVisible(true);
            }
        } else if (actionCommand.equals("colorSpaceComboBoxChanged")) {
            JComboBox  box        = ((JComboBox) e.getSource());
            ColorSpace colorSpace = (ColorSpace) box.getSelectedItem();

            classModel.setColorSpace(colorSpace);
            getView().loadModel(classModel);
        } else if (actionCommand.equals("Delete")) {
            try {
                File imageDirectory = classModel.getRawImageDirectory();

                // delete all of the images
                if (!Utils.deleteDir(imageDirectory)) {
                    NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(),
                                                       "Couldn't delete " + imageDirectory.getPath());

                    Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null,
                                     "Couldn't delete " + imageDirectory.getPath());
                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                // add the delete class task to the queue
                DeleteClassTask deleteTask = new DeleteClassTask(classModel);

                Classifier.getController().addQueue(deleteTask);

                // remove the class model from the list
                getModel().removeClassModel(classModel);
                updateClasses();
            } catch (Exception ex) {
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (actionCommand.equals("Stop")) {
            if (task != null) {
                getView().setRunButton(true);
                getView().setStopButton(false);
                Classifier.getController().kill(task);
            }
        } else if (actionCommand.equals("Run")) {
            try {
                String className      = classModel.getName();
                File   imageDirectory = classModel.getRawImageDirectory();
                String varsClassName  = classModel.getVarsClassName();

                if (className.length() == 0) {
                    String                message = "Class name is empty. Please enter a new class name.";
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (varsClassName.length() == 0) {
                    String                message = "VARS class name is empty. Please select a new name.";
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (imageDirectory.length() == 0) {
                    String                message = "Image directory is empty. Please enter a valid image directory";
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                if (!imageDirectory.exists()) {
                    String                message = imageDirectory
                                                    + " does not exist. Please enter a valid image directory";
                    NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);

                    if (dialog.answer()) {
                        return;
                    }
                }

                final ClassModel newModel = classModel.copy();
                File             d        = UserPreferences.getModel().getClassDatabaseDirectory();

                newModel.setDatabaseRoot(d);

                // Check if you have permission to write to the target
                // parent directory
                File rootPath = newModel.getRawImageDirectory();
                File path     = new File(rootPath.toString() + SQUARE_PATH);
                File parent   = new File(path.getParent());

                if (!parent.canWrite()) {
                    String message = "You do not have write permission to " + parent.toString()
                                     + ". Please correct this.";
                    NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                    dialog.setVisible(true);
                    dialog.answer();

                    return;
                }

                // Create the command for class creation and add it to the queue
                final SwingWorker worker = Classifier.getController().getWorker();

                // / Create a progress display thread for monitoring this task
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            BufferedReader  br              = Classifier.getController().getBufferedReader();
                            ProgressDisplay progressDisplay = new ProgressDisplay(worker,
                                                                  "Creating class " + newModel.getName());

                            progressDisplay.getView().setVisible(true);

                            ProgressDisplayStream progressDisplayStream = new ProgressDisplayStream(progressDisplay,
                                                                              br);

                            progressDisplayStream.execute();
                            task = new CreateClassTask(worker, newModel);
                            Classifier.getController().addQueue(task);
                            getView().setRunButton(false);
                            getView().setStopButton(true);

                            while (!task.isCancelled() &&!task.isFini()) {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException ex) {}
                            }

                            getView().setRunButton(true);
                            getView().setStopButton(false);
                            progressDisplay.getView().dispose();

                            // Add the model only after successfully created
                            if (task.isFini() && (getModel() != null)) {
                                try {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                       newModel.getName() + " class creation finished");

                                    dialog.setVisible(true);
                                } catch (Exception ex) {
                                    Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                if (task.isCancelled()) {
                                    NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                       newModel.getName() + " class creation stopped");

                                    dialog.setVisible(true);
                                }
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                };

                thread.start();
            } catch (Exception ex) {
                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);

                NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), ex.getMessage());

                dialog.setVisible(true);
                getView().setRunButton(true);
                getView().setStopButton(false);
            }
        }
    }

    /**
     * Update the classes directories in the view
     */
    private void updateClasses() {
        File dir       = UserPreferences.getModel().getLastClassImageImportDirectory();
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
                }    // Otherwise, select the first subdirectory
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

            // When the class directory changes or class models are updated
            // update the available classes
            case ClassifierModel.ClassifierModelEvent.TRAINING_DIR_UPDATED :   
            case ClassifierModel.ClassifierModelEvent.CLASS_MODELS_UPDATED : 
                updateClasses();
                break;  
            }
            
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

    public void valueChanged(TreeSelectionEvent e) {
        ConceptTreePanel conceptTreePanel = getView().getConceptTreePanel();
        JTree            tree             = conceptTreePanel.getJTree();
        TreePath         selectionPath    = tree.getSelectionPath();

        if (selectionPath != null) {
            ConceptTreeNode node    = (ConceptTreeNode) selectionPath.getLastPathComponent();
            Concept         concept = (Concept) node.getUserObject();

            getView().setVarsName(concept.getPrimaryConceptName().getName());
        }
    }

    private class CreateClassTask extends ClassifierLibraryJNITask {
        private ClassModel newClassModel;

        public CreateClassTask(SwingWorker parentWorker, ClassModel newClassModel) throws Exception {
            super(newClassModel.getName());
            this.newClassModel = newClassModel;
        }
 
        protected void run(ClassifierLibraryJNI library)  {
            ArrayList<String> filePaths = newClassModel.getRawImageFileListing();
            LibraryImage[]    imageset  = new LibraryImage[filePaths.size()];
            File              rootPath  = newClassModel.getRawImageDirectory();
            File              path      = new File(rootPath.toString() + SQUARE_PATH);

            newClassModel.setSquareImageDirectory(path);

            // Check if you have permission to write to the target
            // parent directory
            File parent = new File(path.getParent());

            if (!parent.canWrite()) {
                String message = "Incorrect write permission to " + parent.toString();

                Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, message);
                return;
            }

            // Clean, the  directory for storing squared images
            if (path.exists()) {
                String[] info = path.list();

                for (int i = 0; i < info.length; i++) {
                    File n = new File(path.getAbsolutePath() + path.separator + info[i]);

                    if (!n.isFile()) {    // skip ., .., other directories too
                        continue;
                    }

                    if (!n.delete()) {
                        Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null,
                                         "Couldn't remove " + n.getPath());
                    }
                }
            } else {
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

                String f            = rootPath + "/" + filePaths.get(i);
                String imageFileOut = path + "/" + ParseUtils.removeFileExtension(filePaths.get(i)) + ".jpg";

                try {
                    ImageUtils.squareJpegThumbnail(f, imageFileOut);
                } catch (Exception ex) {
                    NonModalMessageDialog dialog;

                    dialog = new NonModalMessageDialog(getView(), ex.getMessage());
                    dialog.setVisible(true);

                    return;
                }

                imageset[i] = new LibraryImage(imageFileOut) {};
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
                newClassModel.updateFileList();
                getModel().addClassModel(newClassModel);
                this.setFini();
            } catch (Exception ex) {

                // Only log if this was an exception caused by a non-user cancel
                if (!this.isCancelled()) {
                    Logger.getLogger(CreateClassController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }


    private class DeleteClassTask extends ClassifierLibraryJNITask {
        private ClassModel classModel;

        public DeleteClassTask(ClassModel classModel) throws Exception {
            super(classModel.getName());
            this.classModel = classModel;
        }

        protected void run(ClassifierLibraryJNI library) {
            try {

                // Delete the class
                library.delete_class(classModel.getName(), classModel.getDatabaseRootdirectory().getAbsolutePath(),
                                     classModel.getColorSpace());
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
