/*
 * @(#)ClassifierController.java
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
import aved.model.EventObject;
import java.io.FileNotFoundException;

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.model.EventImageCache;
import org.mbari.aved.ui.model.EventImageCacheData;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mbari.aved.classifier.ClassifierLibraryJNI;

/**
 *
 * @author dcline
 */
public class ClassifierController extends AbstractController implements ModelListener {

    private final CreateClass createClass;
    private final CreateTrainingLibrary createTrainingLib;
    private final EventListModel eventListModel;
    private final RunClassifier runClassifier;
    private final TestClass testClass;
    private ClassifierLibraryJNITaskWorker jniQueue;

    public ClassifierController(EventListModel list, SummaryModel summaryModel) throws Exception {
        eventListModel = list;

        // Create common model for all controllers/views to use
        ClassifierModel model = new ClassifierModel();
        ClassifierView view = new ClassifierView(model, this);

        // Need to create the model and view before creating the controllers
        setView(view);
        setModel(model);

        // Register as listener to the summary and list model
        model.addModelListener(this);
        list.addModelListener(this);

        createTrainingLib = new CreateTrainingLibrary(model);
        createClass = new CreateClass(model, list);
        testClass = new TestClass(model);
        runClassifier = new RunClassifier(model, list, summaryModel);

        // Replace the views
        view.setTrainingPanel(createTrainingLib.getView().getForm());
        view.setCreateClassPanel(createClass.getView().getForm());
        view.setTestClassPanel(testClass.getView().getForm());
        view.setRunPanel(runClassifier.getView().getForm());
        view.pack();

        try {
            jniQueue = new ClassifierLibraryJNITaskWorker();
            jniQueue.execute();
        } catch (Exception ex) {
            Logger.getLogger(ClassifierController.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception(ex.getMessage());
        }

        // Initialize the database directory from the user-defined preferences
        File dbDir = UserPreferences.getModel().getClassDatabaseDirectory();
        model.setDatabaseRoot(dbDir);

        // Initialize the training image directory
        File trainingDir = UserPreferences.getModel().getClassImageDirectory();
        model.setClassTrainingImageDirectory(trainingDir);
    }

    /**
     * Default constructor
     */
    ClassifierController() {
        createTrainingLib = null;
        createClass = null;
        testClass = null;
        runClassifier = null;
        eventListModel = null;
        jniQueue = null;
    }

    @Override
    public ClassifierModel getModel() {
        return (ClassifierModel) super.getModel();
    }

    @Override
    public ClassifierView getView() {
        return (ClassifierView) super.getView();
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Model listener. Reacts to changes in the
     * {@link org.mbari.aved.ui.classifier.model}
     * and  {@link org.mbari.aved.ui.model.EventListModel}
     */
    public void modelChanged(ModelEvent event) {

        /**
         * When the classifier training image directory changes, repopulate
         * the persistent list of classes. This requires storing the class information
         * in a directory for now, until this information is actually put
         * in a real database
         */
        if (event instanceof ClassifierModel.ClassifierModelEvent) {
            switch (event.getID()) {
                case ClassifierModel.ClassifierModelEvent.CLASSIFIER_DBROOT_MODEL_CHANGED:
                    try {
                        LoadModelWorker task = new LoadModelWorker(this.getModel());
                        this.addQueue(task);
                    } catch (Exception ex) {
                        Logger.getLogger(ClassifierController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
            }
        } /**
         * when entries are changed, check if this belongs to a class and whether
         * the user is creating a training classes, and if so add it to the
         * appropriate training class
         */
        else if (event instanceof EventListModel.EventListModelEvent) {
            EventListModel.EventListModelEvent e = (EventListModel.EventListModelEvent) event;

            switch (e.getID()) {
                case EventListModelEvent.MULTIPLE_ENTRIES_CHANGED:
                    // Get the list of model indexes that have changes
                    ArrayList<Integer> a = e.getModelIndexes();
                    Iterator<Integer> iter = a.iterator();

                    // Go through each, creating directories if needed and adding
                    // the images to the model
                    while (iter.hasNext()) {
                        File f = UserPreferences.getModel().getClassImageDirectory();
                        EventObjectContainer eoc = eventListModel.getElementAt(iter.next());

                        if ((eoc != null) && (eoc.getClassName().length() > 0)) {
                            try {
                                String className = eoc.getClassName();
                                File dir = new File(f + "/" + className + "//");

                                if (!dir.exists()) {
                                    if (f.canWrite()) {
                                        dir.mkdir();
                                    } else {

                                        // TODO: display error message to user here
                                        return;
                                    }
                                }

                                // create a new model with some reasonable defaults
                                ClassModel newModel = new ClassModel();

                                newModel.setRawImageDirectory(dir);
                                newModel.setName(className);
                                newModel.setVarsClassName(eoc.getClassName());
                                newModel.setDescription(eoc.getClassName());

                                AddClassImageWorker thread = new AddClassImageWorker(newModel, eoc);

                                thread.execute();

                            } catch (Exception ex) {
                                Logger.getLogger(ClassifierController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                default:
                    break;
            }
        }
    }

    SwingWorker getWorker() {
        if (this.jniQueue != null) {
            return jniQueue;
        }
        return null;
    }

    public void kill(ClassifierLibraryJNITask task) {
        jniQueue.cancelTask(task);
    }

    /**
     * Adds a task to the jni queue for later execution
     * @param task
     */
    public void addQueue(ClassifierLibraryJNITask task) {
        if (jniQueue != null) {
            jniQueue.add(task);
        }
    }

    /**
     * Utility method to pass through a reader to bridge byte streams
     * between the matlab log file to a graphical display
     * @return the input stream reader associated with the matlab log file
     */
    public BufferedReader getBufferedReader() {
        if (jniQueue != null) {
            return jniQueue.getBufferedReader();
        }
        return null;
    }

    /**
     * Kills the jni worker queue and closes the JNI matlab library
     */
    public void shutdown() {
        if (jniQueue != null) {
            jniQueue.cancel();
        }
    }

    /**
     * Worker to manager adding cropped-event images to 
     * class training library.  This occurs in a SwingWorker
     * in the backbgroun because this can take a while for
     * very long events. 
     */
    public class AddClassImageWorker extends SwingWorker {

        private final ClassModel classModel;
        private final EventObjectContainer event;
        //private ProgressDisplay progressDisplay;

        public AddClassImageWorker(ClassModel classModel, EventObjectContainer event) {
            this.event = event;
            this.classModel = classModel;
            //TODO: add another type of progress display to the main view
            //to indicate this is working in the background
            //progressDisplay = new ProgressDisplay(this, "Adding class image" + classModel.getName());
        }

        @Override
        protected Object doInBackground() throws Exception {
            //progressDisplay.display("Adding class " + classModel.getName() + "...");

            boolean addImages = UserPreferences.getModel().getAddTrainingImages();

            if (addImages) {
                File dir = UserPreferences.getModel().getClassImageDirectory();
                File classDir = new File(dir + "/" + classModel.getName());

                // Add this class to the docking image directories
                UserPreferences.getModel().setDockingImageDirectory(classDir);

                for (int frameNo = event.getStartFrame(); frameNo <= event.getEndFrame(); frameNo++) {

                    // If found a valid frame number
                    if (frameNo >= 0) {
                        try {
                            int bestFrameNo = frameNo;
                            EventImageCacheData data = new EventImageCacheData(event);

                            data.initialize(bestFrameNo);

                            EventObject object = event.getEventObject(bestFrameNo);

                            if ((object != null) && EventImageCache.createCroppedImageOfEvent(data, object)) {
                                classModel.addToTrainingSet(data.getImageSource());
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(AddClassImageWorker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }

            //progressDisplay.display("Done !");
            //progressDisplay.getView().dispose();

            // Add the model to the classes
            if (!getModel().checkClassExists(classModel)) {
                getModel().addClassModel(classModel);
            }

            return this;
        }
    }

    /**
     * A task thread designed to execute JNI functions from a command queue
     * @author dcline
     */
    private class ClassifierLibraryJNITaskWorker extends SwingWorker {

        private boolean exit = false;
        private boolean isInitialized = false;
        private final Queue<ClassifierLibraryJNITask> queue = new LinkedList<ClassifierLibraryJNITask>();
        private final ClassifierLibraryJNI jniLibrary = new ClassifierLibraryJNI(this);
        private final File logFile = new File(UserPreferences.getModel().getDefaultScratchDirectory().getAbsolutePath() + "/matlablog.txt");

        ClassifierLibraryJNITaskWorker() throws Exception {
        }

        @Override
        protected Object doInBackground() throws Exception {
            getLibrary();

            while (exit == false) {
                if (queue.isEmpty() == false) {
                    ClassifierLibraryJNITask task = queue.element();
                    try {
                        task.run(jniLibrary);
                    } catch (Exception ex) {
                        Logger.getLogger(ClassifierLibraryJNITaskWorker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    queue.remove();
                    getModel().setJniTaskComplete(task.getId());
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
            closeLibrary();
            return null;
        }

        /**
         * Add a task to the queue for later execution
         * @param task the task to add
         */
        public synchronized void add(ClassifierLibraryJNITask task) {
            queue.add(task);
        }

        /**
         * Cancels a running Matlab method through the JNI layer
         * This sets a kill flag in a file that is read
         * by the compiled matlab code.  
         * @param task
         */
        private synchronized void cancelTask(ClassifierLibraryJNITask task) {
            Iterator<ClassifierLibraryJNITask> it = queue.iterator();
            while (it.hasNext()) {
                if (it.next().equals(task)) {
                    task.setCancelled();
                    jniLibrary.set_kill(task.getCancel(), 1);
                    break;
                }
            }
        }

        /**
         * Cancels this worker and closes the jni library
         */
        public synchronized void cancel() {
            exit = true;
            super.cancel(true);
            closeLibrary();
        }

        /**
         * Creates a {@link java.io.BufferedReader} associated with the Matlab log file
         * This is intended for use in redirecting the Matlab text log ouput
         * to a display.
         *
         * @return the BufferedReader
         */
        public synchronized BufferedReader getBufferedReader() {
            BufferedReader br = null;
            if (logFile.exists() && logFile.canRead()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(logFile);
                    InputStreamReader isr = new InputStreamReader(fis);
                    br = new BufferedReader(isr);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(ClassifierLibraryJNITaskWorker.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return br;
        }

        /**
         * Returns the singleton <code>ClassifierLibraryJNI</code>
         * <p> An exception may be thrown if the Matlab log file does not exist,
         * which indicates there is something  wrong with the Matlab library
         * initialization. This is likely caused by an invalid matlab log file
         * directory 
         *
         * @return a {@link org.mbari.aved.classifier.ClassifierLibraryJNI} singleton
         */
        private synchronized ClassifierLibraryJNI getLibrary() throws Exception {
            if (isInitialized == false) {

                try {
                    String lcOSName = System.getProperty("os.name").toLowerCase();

                    // If running from Mac
                    if (lcOSName.startsWith("mac os x")) {
                        jniLibrary.initLib(logFile.getAbsolutePath(), 1);
                    } else {
                        jniLibrary.initLib(logFile.getAbsolutePath(), 0);
                    }

                    isInitialized = true;
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Logger.getLogger(ClassifierLibraryJNITaskWorker.class.getName()).log(Level.SEVERE, null, e);
                }
            }

            if (isInitialized == true) {
                return jniLibrary;
            }

            return null;
        }

        /**
         * Closes the library. If this is called, the library will be reopened
         * during the first getLibrary() call
         *
         * @see     getLibrary()
         */
        private synchronized void closeLibrary() {
            try {
                if (isInitialized == true) {
                    jniLibrary.closeLib();
                    isInitialized = false;
                }

            } catch (Exception ex) {
                Logger.getLogger(ClassifierLibraryJNITaskWorker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
