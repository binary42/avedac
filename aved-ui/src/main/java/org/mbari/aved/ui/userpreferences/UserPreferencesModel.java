/*
 * @(#)UserPreferencesModel.java
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



package org.mbari.aved.ui.userpreferences;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.appframework.AbstractModel;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.*;
import org.mbari.aved.classifier.ColorSpace;

/**
 * Singleton class that contains user preferences for the application
 *
 * @author  Danelle Cline
 */
public final class UserPreferencesModel extends AbstractModel {
    private static final String ASK_BEFORE_DELETE                   = "ASK_BEFORE_DELETE";
    public static int           ASK_BEFORE_DELETE_CHANGED           = 1;

    private static final String CLASS_ADD_ALL_TRAINING              = "CLASS_ADD_ALL_TRAINING";
    private static final String CLASS_COLOR_SPACE                   = "CLASS_COLOR_SPACE";
    private static final String CLASS_DATABASE_DIR_ROOT             = "CLASS_DATABASE_DIR_ROOT";
    private static final String CLASS_NAME_LIST                     = "CLASS_NAME_LIST";
    private static final String CLASS_TRAINING_DIR_ROOT             = "CLASS_TRAINING_DIR_ROOT";
    private static final String EVENT_IMAGE_DIR                     = "EVENT_IMAGE_DIR";
    private static final String EXCEL_EXPORT_DIR                    = "EXCEL_EXPORT_DIR";
    private static final String ID_LIST                             = "ID_LIST";
    private static final String IMAGE_DOCKING_DIRS                  = "IMAGE_DOCKING_DIRS";
    private static final String IMAGE_IMPORT_DIR                    = "IMAGE_IMPORT_DIR";
    private static final String LAST_CLASS_NAME                     = "LAST_CLASS_NAME";
    private static final String LAST_ID_NAME                        = "LAST_ID_NAME";
    private static final String LAST_IMPORTED_SRC_URL               = "LAST_IMPORTED_SRC_URL";
    private static final String LAST_IMPORTED_XML_LIST              = "LAST_IMPORTED_XML_LIST";
    private static final String LAST_MPEG_RESULTS_IMPORT_URL_PARENT = "LAST_MPEG_RESULTS_IMPORT_URL_PARENT";
    private static final String LAST_SPECIES_NAME                   = "LAST_CLASS_NAME";
    private static final String LAST_TAG_NAME                       = "LAST_TAG_NAME";
    private static final String LAST_TRAINING_SELECTION             = "LAST_TRAINING_SELECTION";
    private static final String LAST_VIDEO_IMPORT_DIR               = "LAST_VIDEO_IMPORT_DIR";
    private static final String ENABLE_FFMPEG                       = "ENABLE_FFMPEG";

    /** The maximum number of class names store */
    public static int MAX_NUM_CLASS_NAMES = 30;

    /** The maximum number of ids to store */
    public static int MAX_NUM_IDS = 30;

    /** The maximum number of tags to store */
    public static int           MAX_NUM_TAGS              = 30;
    private static final String PREDICTED_CLASS_NAME_LIST = "SPECIES_NAME_LIST";
    private static final String SCRATCH_DIR               = "SCRATCH_DIR";
    public static int           SCRATCH_DIR_CHANGED       = 2;
    private static final String TAG_LIST                  = "TAG_LIST";
    private static final String VIDEO_BATCH_INPUT_DIR     = "VIDEO_BATCH_INPUT_DIR";
    private static final String VIDEO_MASK_DIR            = "VIDEO_MASK_DIR";
    public static int           VIDEO_PLAYOUT_CHANGED     = 0;
    private static final String XML_EXPORT_DIR            = "XML_EXPORT_DIR";
    private static final String XML_IMPORT_DIR            = "XML_IMPORT_DIR";

    /** TODO: rename this to something meaningful number of docking directories */
    private int dockingDirsCnt = 0;

    /** The number of class names stored in the user preference history */
    private int numClasses = 0;

    /** The number of tags currently stored */
    private int numIds = 0;

    /** The number of class names stored in the user preference history */
    private int numSpecies = 0;

    /** The number of tags stored in the user preference history */
    private int numTags = 0;

    /** Variables to store preferences in */
    private Preferences preferences;

    /** The video playout mode */
    private VideoPlayoutMode videoPlayoutMode;
  
    public UserPreferencesModel() {

        // Initialize variable to store preferences in under the package node
        preferences = Preferences.userRoot().node("/org/mbari/aved/editor");

        // FOR DEBUGGING ONLY
        // dump(preferences);
        VideoPlayoutMode.initialize();

        ArrayList<String> list = getNodeValues(TAG_LIST);

        numTags    = list.size();
        list       = getNodeValues(ID_LIST);
        numIds     = list.size();
        list       = getNodeValues(CLASS_NAME_LIST);
        numClasses = list.size();
    }
 
    public enum VideoPlayoutMode {

        // These are ordered in the manner that makes sense to display them
        DEFAULT(0, "Use default player", ""),

        // arbitrarily set to Quicktime - should probably set to something
        // bundled in Mac/Linux distros
        OTHER(1, "Use user defined player", "Quicktime");

        private static final String EXTERNAL_VIDEO_PLAYER = "EXTERNAL_VIDEO_PLAYER";
        private static final String VIDEO_PLAYOUT_MODE    = "VIDEO_PLAYOUT_MODE";
        public String               command;
        public final String         description;
        public final int            index;
        public UserPreferencesModel model;

        private VideoPlayoutMode(int index, String description, String command) {
            this.index       = index;
            this.description = description;
            this.command     = command;
        }

        private static void initialize() {
            URL    url       = Application.class.getResource("/org/mbari/aved/ui/html/");
            String urlString = "file://";

            // If html code found assume using vlc html launcher,
            // otherwise default to use URL string "file://" only
            if (url != null) {
                urlString = "file://" + url.getFile() + "/launchvlc.html?f=file://";
            }

            DEFAULT.command = urlString;

            // TODO: Change default OTHER to be something reasonable based on the OS name
            String lcOSName = System.getProperty("os.name").toLowerCase();

            if (lcOSName.startsWith("mac os x")) {

                // OTHER.command =
            }
        }
    }

    /**
     * Be sure one cannot create a new copy of this object be overriding
     * clone() method
     * @return
     * @throws java.lang.CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Flush by default flushes the preferences content to disk
     *
     */
    private void flush() {
        close();
    }

    /**
     * Closes the preferences and writes them to disk
     */
    public void close() {
        try {
            preferences.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(UserPreferencesModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void dump(Preferences p) {
        System.out.println("node's absolute path: " + p.absolutePath());

        try {
            System.out.print("Node's children: ");

            for (String s : p.childrenNames()) {
                System.out.print(s + " ");
            }

            System.out.println("");
            System.out.print("Node's keys: ");

            for (String s : p.keys()) {
                System.out.print(s + " ");
            }

            System.out.println("");
            System.out.println("Node's name: " + p.name());
            System.out.println("Node's parent: " + p.parent());
            System.out.println("NODE: " + p);
            System.out.println("userNodeForPackage: " + p.userNodeForPackage(UserPreferencesModel.class));
            System.out.println("All information on node");

            for (String s : p.keys()) {
                System.out.println(" " + s + " = " + p.get(s, ""));
            }
        } catch (BackingStoreException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Gets values only from a node of the parent node
     * @param node The name of the node to read from
     * @returns the list of strings (without keys) for the given node
     */
    private ArrayList<String> getNodeValues(String node) {
        ArrayList<String> list = new ArrayList<String>();
        Preferences       p    = preferences.node(node);

        try {
            for (String s : p.keys()) {

                // System.out.print(s + " ");
                list.add(p.get(s, ""));
            }
        } catch (BackingStoreException ex) {
            Logger.getLogger(UserPreferencesModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        return list;
    }

    /**
     * Writes an element to a node from the parent node
     * @param node The name of the node to write to
     * @param key The key
     * @param velue The value of the key
     */
    private void putNode(String node, String key, String value) {
        Preferences p = preferences.node(node);

        p.put(key, value);
    }

    /**
     * Writes a key/value string to the preferences file
     * @param key the key to write to
     * @param value the  value to write
     */
    private void put(String key, String value) {
        preferences.put(key, value);
        flush();
    }

    /**
     * Reads a key/value string fromo the preferences file
     * @param key the key to read
     * @param value the value to read
     */
    private String get(String key, String value) {
        String s = preferences.get(key, value);

        return s;
    }

    /**
     * Gets the delete without warning preference
     */
    public boolean getAskBeforeDelete() {
        String delete = get(ASK_BEFORE_DELETE, "false");

        if (delete.equals("false")) {
            return false;
        }

        return true;
    }

    /**
     * Gets the list of user-defined tags
     * @return A String list of tags
     */
    public ArrayList<String> getTagList() {
        return getNodeValues(TAG_LIST);
    }

    /**
     * Gets the list of class names
     * @return A String list of class names
     */
    public ArrayList<String> getClassNameList() {
        return getNodeValues(CLASS_NAME_LIST);
    }

    /**
     * Gets the list of predicted class names
     * @return A String list of specs names
     */
    public ArrayList<String> getPredictedClassList() {
        return getNodeValues(PREDICTED_CLASS_NAME_LIST);
    }

    /**
     * Gets the root class training image directory.
     * This defaults to the HOME environment variable, or
     * the USERPROFILE environment variable
     * @return a directory
     */
    public File getClassImageDirectory() {
        return new File(preferences.get(CLASS_TRAINING_DIR_ROOT, getDefaultClassDirectoryString()));
    }

    /**
     * Gets the root class database directory.
     * This defaults to the HOME environment variable, or
     * the USERPROFILE environment variable
     * @return a directory
     */
    public File getClassDatabaseDirectory() {
        return new File(preferences.get(CLASS_DATABASE_DIR_ROOT, getDefaultClassDirectoryString()));
    }

    /**
     * Gets the list of user-defined ids
     * @return A String list of ids
     */
    public ArrayList<String> getIdList() {
        return getNodeValues(ID_LIST);
    }

    /** 
     * @param b true if using ffmpeg for transcoding instead of transcode (default)
     */
    public boolean getEnableFfmpeg() {
        return Boolean.valueOf(get(ENABLE_FFMPEG, Boolean.toString(false)));
    }
    
    /**
     *  Clears the list of user-defined ids
     */
    public void clearIdList() {
        Preferences p = preferences.node(ID_LIST);

        try {
            p.removeNode();
        } catch (BackingStoreException ex) {
            Logger.getLogger(UserPreferencesModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *  Clears the list of user-defined classes
     */
    public void clearClassList() {
        Preferences p = preferences.node(CLASS_NAME_LIST);

        try {
            p.removeNode();
        } catch (BackingStoreException ex) {
            Logger.getLogger(UserPreferencesModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *  Clears the list of user-defined tags
     */
    public void clearTagList() {
        Preferences p = preferences.node(TAG_LIST);

        try {
            p.removeNode();
        } catch (BackingStoreException ex) {
            Logger.getLogger(UserPreferencesModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Gets the user preference for what color space the classifier
     * is defaulted to run in
     * 
     * @return ColorSpace
     */
    public ColorSpace getColorSpace() {        
        String value = get(CLASS_COLOR_SPACE,  ColorSpace.RGB.toString());
        return ColorSpace.valueOf(value);        
    }

    /**
     * Gets the user preference to add all images tagged
     * with a class name to the training library. This is
     * turned on by default
     *
     * @return true if set to add all images to the training library
     */
    public boolean getAddTrainingImages() {
        String value = get(CLASS_ADD_ALL_TRAINING, "true");

        if (value.equals("false")) {
            return false;
        }

        return true;
    }

    /**
     * Adds an a id to the list of persistently stored user-defined tags.
     * If the id already exists or is empty, this does nothing.
     * @param tag The value of the id
     */
    public void addIdList(String id) {
        if (id.length() == 0) {
            return;
        }

        setId(id);

        ArrayList<String> l = getNodeValues(ID_LIST);
        Iterator<String>  i = l.iterator();

        while (i.hasNext()) {
            if (i.next().equals(id)) {
                return;
            }
        }

        // Didn't find the id, so go ahead and add it
        numIds = ((numIds >= MAX_NUM_IDS)
                  ? 0
                  : numIds);

        String key = String.format("%s" + "%0" + 2 + "d", "ID", numIds);

        putNode(ID_LIST, key, id);
        numIds++;
    }

    /**
     * Adds an a tag to the list of persistently stored user-defined tags.
     * If the tag already exists or is empty, this does nothing.
     * @param tag The value of the tag
     */
    public void addTagList(String tag) {
        if (tag.length() == 0) {
            return;
        }

        this.setTag(tag);

        ArrayList<String> l = getNodeValues(TAG_LIST);
        Iterator<String>  i = l.iterator();

        while (i.hasNext()) {
            if (i.next().equals(tag)) {
                return;
            }
        }

        // Didn't find the tag, so go ahead and add it
        numTags = ((numTags >= MAX_NUM_TAGS)
                   ? 0
                   : numTags);

        String key = String.format("%s" + "%0" + 2 + "d", "TAG", numTags);

        putNode(TAG_LIST, key, tag);
        numTags++;
    }

    /**
     * Adds an a class name to the list of persistently stored class names.
     * If the name already exists or is empty, this does nothing.
     * @param className The value of the class
     */
    public void addClassNameList(String className) {
        if (className.length() == 0) {
            return;
        }

        setClassName(className);

        ArrayList<String> l = getNodeValues(CLASS_NAME_LIST);
        Iterator<String>  i = l.iterator();

        while (i.hasNext()) {
            if (i.next().equals(className)) {
                return;
            }
        }

        // Didn't find the className, so go ahead and add it
        numClasses = ((numClasses >= MAX_NUM_CLASS_NAMES)
                      ? 0
                      : numClasses);

        String key = String.format("%s" + "%0" + 2 + "d", "CLASSNAME", numClasses);

        putNode(CLASS_NAME_LIST, key, className);
        numClasses++;
    }

    /**
     * Adds a predicted class name to the list of persistently stored class names.
     * If the name already exists or is empty, this does nothing.
     * @param name The value of the class
     */
    public void addPredictedClassNameList(String className) {
        if (className.length() == 0) {
            return;
        }

        setSpeciesName(className);

        ArrayList<String> l = getNodeValues(PREDICTED_CLASS_NAME_LIST);
        Iterator<String>  i = l.iterator();

        while (i.hasNext()) {
            if (i.next().equals(className)) {
                return;
            }
        }

        // Didn't find the className, so go ahead and add it
        numSpecies = ((numSpecies >= MAX_NUM_CLASS_NAMES)
                      ? 0
                      : numSpecies);

        String key = String.format("%s" + "%0" + 2 + "d", "CLASSNAME", numSpecies);

        putNode(PREDICTED_CLASS_NAME_LIST, key, className);
        numSpecies++;
    }

    /**
     * Sets the root class training image directory
     * @param f the root directory to set
     */
    public void setClassTrainingImageDirectory(File f) {
        put(CLASS_TRAINING_DIR_ROOT, f.getAbsolutePath());
    }

    /**
     * Sets the root class database directory
     * @param f the root directory to set
     */
    public void setClassDatabaseDirectory(File f) {
        put(CLASS_DATABASE_DIR_ROOT, f.getAbsolutePath());
    }

    /**
     * Sets the user preference to add all images labeled
     * with a class name to the training library.
     *
     * @param state set to true to add all images to the training library
     */
    public void setAddTrainingImages(boolean state) {
        if (!state) {
            put(CLASS_ADD_ALL_TRAINING, "false");
        } else {
            put(CLASS_ADD_ALL_TRAINING, "true");
        }
    }

    /**
     * Sets the user default color space to run the
     * classifier with.
     *
     * @param colorspace 
     */
    public void setColorSpace(ColorSpace c) {
        put(CLASS_COLOR_SPACE, c.toString());
    }

    /**
     * Sets the last directory training images were successfully
     * imported from
     * @param f the directory to set
     */
    public void setClassImportDirectory(File f) {
        put(IMAGE_IMPORT_DIR, f.getAbsolutePath());
    }

    /**
     * Sets the delete without warning preference
     */
    public void setAskBeforeDelete(boolean state) {
        if (!state) {
            put(ASK_BEFORE_DELETE, "false");

            ModelEvent e = new ModelEvent(this, ASK_BEFORE_DELETE_CHANGED, "false");

            notifyChanged(e);
        } else {
            put(ASK_BEFORE_DELETE, "true");

            ModelEvent e = new ModelEvent(this, ASK_BEFORE_DELETE_CHANGED, "true");

            notifyChanged(e);
        }
    }

    /** Sets the last docking container directories */
    public void setDockingImageDirectory(File f) {
        ArrayList<String> l = getNodeValues(IMAGE_DOCKING_DIRS);
        Iterator<String>  i = l.iterator();

        while (i.hasNext()) {
            if (i.next().equals(f.toString())) {
                return;
            }
        }

        // Didn't find the directory, so go ahead and add it
        dockingDirsCnt = ((dockingDirsCnt >= MAX_NUM_CLASS_NAMES)
                          ? 0
                          : dockingDirsCnt);

        String key = String.format("%s" + "%0" + 2 + "d", "DIR", dockingDirsCnt);

        putNode(IMAGE_DOCKING_DIRS, key, f.getAbsolutePath());
        dockingDirsCnt++;
    }

    /**
     * Clears all the image docking directories
     */
    public void clearDockingImagesDirectories() {
        try {
            if (preferences.nodeExists(IMAGE_DOCKING_DIRS)) {
                Preferences p = preferences.node(IMAGE_DOCKING_DIRS);

                p.removeNode();
            }
        } catch (BackingStoreException ex) {
            Logger.getLogger(UserPreferencesModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setEnableFfmpeg(boolean b) {
        put(ENABLE_FFMPEG, Boolean.toString(b));
    }
    
    public void setEventImageDirectory(File f) {
        put(EVENT_IMAGE_DIR, f.getAbsolutePath());
    }

    public File getEventImageDirectory() {
        return new File(get(EVENT_IMAGE_DIR, getDefaultDirectoryString()));
    }

    public void setVideoBatchInputDirectory(File f) {
        put(VIDEO_BATCH_INPUT_DIR, f.getAbsolutePath());
    }

    public void setVideoMaskDirectory(File f) {
        put(VIDEO_MASK_DIR, f.getAbsolutePath());
    }

    /**
     * Returns the video mask directory, and if not available set to PWD environment variable
     */
    public File getVideoMaskDirectory() {
        return new File(get(VIDEO_MASK_DIR, getDefaultDirectoryString()));
    }

    /**
     *
     * @return returns in the following search order:
     * if exists, the PWD
     * if exists, the HOME environment variable
     * if exists, the USERPROFILE environment variable
     * or "file://" if all else fails
     */
    private String getDefaultURLString() {
        if (System.getenv("PWD") != null) {
            return "file://" + System.getenv("PWD").toString();
        } else if (System.getenv("HOME") != null) {
            return "file://" + System.getenv("HOME").toString();
        } else if (System.getenv("USERPROFILE") != null) {
            return "file://" + System.getenv("USERPROFILE").toString();
        } else {
            return "file://";
        }
    }

    /**
     *
     * @return returns in the following search order:
     * if exists and user has write permission, the SCRATCH_DIR
     * if exists and user has write permission, the /var/tmp directory
     * if exists and user has write permission, the HOME environment variable
     * or "./" if all else fails
     */
    public File getDefaultScratchDirectory() {
        File tmp = new File("/var/tmp");

        if (System.getenv("SCRATCH_DIR") != null) {
            return new File(System.getenv("SCRATCH_DIR"));
        } else if (tmp.exists() && tmp.canWrite()) {
            return tmp;
        } else if (System.getenv("USERPROFILE") != null) {
            return new File(System.getenv("USERPROFILE"));
        } else {
            return new File("./");
        }
    }

    /**
     *  Returns the default scratch directory
     */
    public File getScratchDirectory() {
        try {
            return new File(preferences.get(SCRATCH_DIR, getDefaultScratchDirectory().getCanonicalPath()));
        } catch (IOException ex) {
            Logger.getLogger(UserPreferencesModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new File(preferences.get(SCRATCH_DIR, getDefaultScratchDirectory().getPath()));
    }

    /**
     * Returns the directory the last training images where imported from
     */
    public File getLastClassImageImportDirectory() {
        return new File(get(IMAGE_IMPORT_DIR, getDefaultClassDirectoryString()));
    }

    /** Returns a list of the  docking container directories */
    public ArrayList<File> getLastTrainingImageDockingDirectories() {
        ArrayList<File> list = new ArrayList<File>();

        try {
            Preferences p = preferences.node(IMAGE_DOCKING_DIRS);

            for (String s : p.keys()) {
                list.add(new File(p.get(s, "")));
            }
        } catch (BackingStoreException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return list;
    }

    public void removeClassImageDockingDirectory(File f) {
        try {
            Preferences node      = preferences.node(IMAGE_DOCKING_DIRS);
            String      childname = "IMAGE_EXPORT_DIRS" + dockingDirsCnt;

            node.put(childname, f.getAbsolutePath());
            dockingDirsCnt++;

            Preferences p     = preferences.node(IMAGE_DOCKING_DIRS);
            String      match = f.getAbsolutePath();

            for (String s : p.keys()) {
                if (p.get(s, "").equals(match)) {
                    p.remove(s);
                    flush();
                }
            }
        } catch (BackingStoreException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     *
     * @return  returns in the following search order:
     * if exists, the HOME environment variable, or
     * if exists the USERPROFILE environment variable
     * or "./" if all else fails
     */
    private String getDefaultDirectoryString() {
        if (System.getenv("HOME") != null) {
            return System.getenv("HOME").toString();
        } else if (System.getenv("USERPROFILE") != null) {
            return System.getenv("USERPROFILE").toString();
        } else {
            return "./";
        }
    }

    /**
     * Returns the default class image training directory.
     * This is platform-dependent and corresponds the the
     * installer mac-assembly or linux-assembly file
     * installation of the examples classes
     */
    private String getDefaultClassDirectoryString() {
        String lcOSName = System.getProperty("os.name").toLowerCase();

        if (lcOSName.startsWith("mac os x")) {
            File examples = new File("/Applications/AVEDac/examples/trainingclasses");

            if (!examples.exists()) {
                examples.mkdirs();
            }

            return examples.toString();
        } else {
            if (System.getenv("HOME") != null) {
                return System.getenv("HOME").toString();
            } else {
                return "./";
            }
        }
    }

    /**
     *    Returns the directory XML results were successfully imported from,
     *    and if not available returns HOME or USERPROFILE environment variable
     */
    public File getXmlImportDirectory() {
        return new File(preferences.get(XML_IMPORT_DIR, getDefaultDirectoryString()));
    }

    /**
     * Returns the directory XML results were successfully exported to,
     * and if not available returns HOME or USERPROFILE environment variable
     */
    public File getXmlExportDirectory() {
        return new File(preferences.get(XML_EXPORT_DIR, getDefaultDirectoryString()));
    }

    /** Sets the directory Excel data was successfully exported to */
    public void setScratchDirectory(File f) {
        put(SCRATCH_DIR, f.getAbsolutePath());

        ModelEvent e = new ModelEvent(this, SCRATCH_DIR_CHANGED, f.getAbsolutePath());

        notifyChanged(e);
    }

    /** Sets the directory results were successfully imported from */
    public void setExportXmlDirectory(File f) {
        put(XML_EXPORT_DIR, f.getAbsolutePath());
    }

    /**
     * Returns the directory Excel data was successfully exported to,
     * and if not available returns HOME or environment variable
     */
    public File getExportedExcelDirectory() {
        File f = new File(preferences.get(EXCEL_EXPORT_DIR, getDefaultDirectoryString()));
        if (f.isDirectory()) return f;
        return new File(f.getParent());
    }

    /** Sets the last training library selection by name */
    public void setTrainingLibrarySelection(String className) {
        put(LAST_TRAINING_SELECTION, className);
    }

    /**
     * Gets the last training library selection by name. If
     * a noe selection exists, return an empty string
     */
    public String getTrainingLibrarySelection() {
        return get(LAST_TRAINING_SELECTION, "");
    }

    /** Sets the directory Excel data was successfully exported to */
    public void setExcelExportDirectory(File f) {
        put(EXCEL_EXPORT_DIR, f.getAbsolutePath());
    }

    /**
     * Returns the directory XML results were successfully imported from,
     * and if not available returns HOME or environment variable
     */
    public File setXmlImportDirectory() {
        return new File(get(XML_IMPORT_DIR, getDefaultDirectoryString()));
    }

    public URL getImportSourceUrl() throws MalformedURLException {
        return new URL(get(LAST_IMPORTED_SRC_URL, getDefaultURLString()));
    }

    /**
     *  Sets the directory results were successfully imported from
     * @param f The directory last successfully imported
     */
    public void setImportXmlDirectory(File f) {
        put(XML_IMPORT_DIR, f.getAbsolutePath());
    }

    /**
     * Returns the parent directory clips were successfully imported from,
     * and if not available returns HOME or USERPROFILE environment variable
     */
    public String getImportVideoDir() {
        return get(LAST_VIDEO_IMPORT_DIR, getDefaultDirectoryString());
    }

    /**
     * Returns the last used class name and if not available
     * returns an empty string
     */
    public String getClassName() {
        return get(LAST_CLASS_NAME, "");
    }

    /**
     * Returns the last used species name and if not available
     * returns an empty string
     */
    public String getSpeciesName() {
        return get(LAST_SPECIES_NAME, "");
    }

    /**
     * Returns the last used id and if not available
     * returns an empty string
     */
    public String getId() {
        return get(LAST_ID_NAME, "");
    }

    /**
     * Returns the last used tag nd if not available
     * returns an empty string
     */
    public String getTag() {
        return get(LAST_TAG_NAME, "");
    }

    /**
     * Sets the URL path input source video clips were successfully imported from
     * @param dir the directory
     */
    public void setImportVideoDir(String dir) {
        put(LAST_VIDEO_IMPORT_DIR, dir);
    }

    /**
     *  Sets the name of the last used class name
     * @param className the class name
     */
    public void setClassName(String className) {
        put(LAST_CLASS_NAME, className);
    }

    /**
     *  Sets the name of the last used species name
     * @param className the species name
     */
    private void setSpeciesName(String className) {
        put(LAST_SPECIES_NAME, className);
    }

    /**
     *  Sets the name of the last used tag name
     * @param tagName the class name
     */
    private void setTag(String tag) {
        put(LAST_TAG_NAME, tag);
    }

    /**
     *  Sets the name of the last used tag name
     * @param tagName the class name
     */
    private void setId(String id) {
        put(LAST_ID_NAME, id);
    }

    /** Sets the last imports video source URL */
    public void setImportSourceUrl(URL url) {
        put(LAST_IMPORTED_SRC_URL, url.toString());
    }

    /**
     * Sets the URL results were successfully imported from
     * if not available returns PWD environment variable
     */
    public void setImportMpegDirectory(URL u) {
        put(LAST_MPEG_RESULTS_IMPORT_URL_PARENT, u.toString());
    }

    /** Returns the video batch input directory, and if not available set to PWD environment variable */
    public File getVideoBatchInputDirectory() {
        return new File(get(VIDEO_BATCH_INPUT_DIR, getDefaultDirectoryString()));
    }

    /** Returns the video playout mode preference */
    public VideoPlayoutMode getPlayoutMode() {
        String s = get(videoPlayoutMode.VIDEO_PLAYOUT_MODE, "0");

        // set to default if can't find preference
        if (String.valueOf(videoPlayoutMode.DEFAULT.index).equals(s)) {
            return videoPlayoutMode.DEFAULT;
        } else {
            String player = get(videoPlayoutMode.EXTERNAL_VIDEO_PLAYER, videoPlayoutMode.OTHER.command);

            videoPlayoutMode.OTHER.command = player;

            return VideoPlayoutMode.OTHER;
        }
    }

    /** Sets the video playout mode preference */
    public void setPlayoutMode(VideoPlayoutMode mode) {
        if (mode == VideoPlayoutMode.DEFAULT) {
            put(VideoPlayoutMode.VIDEO_PLAYOUT_MODE, String.valueOf(VideoPlayoutMode.DEFAULT.index));
        } else {
            put(VideoPlayoutMode.VIDEO_PLAYOUT_MODE, String.valueOf(VideoPlayoutMode.OTHER.index));
            put(VideoPlayoutMode.EXTERNAL_VIDEO_PLAYER, mode.command);
        }

        ModelEvent e = new ModelEvent(this, VIDEO_PLAYOUT_CHANGED, mode.description);

        notifyChanged(e);
    }
}
