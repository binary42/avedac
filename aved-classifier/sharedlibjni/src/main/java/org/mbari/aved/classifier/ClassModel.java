/*
 * @(#)ClassModel.java
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



package org.mbari.aved.classifier;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.commons.io.IOUtils;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>ClassModel</code> class represents
 * an AVED class which is a collection of images.
 *
 * <p>
 * @author dcline
 */
public class ClassModel {
    private String            className             = new String("-");
    private String            description           = new String("-");
    private String[]          imageExtensions       = {
        "ppm", "jpg", "gif", "png", "tif", "tiff", "bmp", "pnm"
    };
    private String            varsClassName         = new String("-");
    private File              squaredImageDirectory = new File("");
    private File              rawImageDirectory     = new File("");
    private ArrayList<String> fileList              = new ArrayList<String>();
    private File              dbrootDirectory       = new File("");

    /** Default to RGB */
    private ColorSpace color = ColorSpace.RGB;

    /**
     * Constructor
     */
    void ClassModel() {}
  
    /**
     * Returns a copy of the class model
     */
    public ClassModel copy() {
        ClassModel m = new ClassModel();
        m.className = this.getName();
        m.description = this.getDescription();
        m.varsClassName = this.getVarsClassName();
        m.squaredImageDirectory = this.getSquareImageDirectory();
        m.rawImageDirectory = this.getRawImageDirectory();
        m.fileList = (ArrayList<String>) this.fileList.clone();
        m.dbrootDirectory = this.getDatabaseRootdirectory();
        return m;
    }
    /**
     * Get the raw image directory. This is the directory the images
     * are stored in before conversion to square images for classification
     * @return the image directory
     */
    public File getRawImageDirectory() {
        return rawImageDirectory;
    }

    /**
     * Set the raw image directory. This is the directory the images
     * are stored in before conversion to square images for classification
     * @param imageDirectory image directory
     */
    public void setRawImageDirectory(File imageDirectory) throws Exception {
        rawImageDirectory = imageDirectory;

        if (rawImageDirectory.isDirectory() == false) {
            throw new Exception(rawImageDirectory + " is not a directory");
        }
    }

    /**
     * Get the image directory that holds raw images that have been converted
     * to square sizes as required for he AVED classifier
     * @return the image directory
     */
    public File getSquareImageDirectory() {
        return squaredImageDirectory;
    }

    /**
     * Set the image directory that holds raw images that have been converted
     * to square sizes as required for he AVED classifier
     * @param directory the image directory
     */
    public void setSquareImageDirectory(File directory) {
        squaredImageDirectory = directory;
    }

    /**
     *
     * @return the color space for this class
     */
    public ColorSpace getColorSpace() {
        return color;
    }

    /**
     *
     * @param color the color space for this class
     */
    public void setColorSpace(ColorSpace color) {
        this.color = color;
    }

    /**
     *
     * @return the database root directory this classsifier will store
     * trained data from this class in
     */
    public File getDatabaseRootdirectory() {
        return dbrootDirectory;
    }

    /**
     * Updates the valid file listing in the raw image directory
     */
    public void updateFileList() {
        if (fileList.isEmpty() == false) {
            fileList.clear();
        }

        if (rawImageDirectory.isDirectory()) {
            String[] files = rawImageDirectory.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    String extension = name.substring(name.lastIndexOf('.') + 1);

                    for (int i = 0; i < imageExtensions.length; i++) {
                        if (extension.equalsIgnoreCase(imageExtensions[i])) {
                            return true;
                        }
                    }

                    return false;
                }
            });

            // All files should have some event number that we need to sort on.
            Arrays.sort(files, new Comparator() {
                public int compare(Object o1, Object o2) {
                    String file1  = (String) o1;
                    String file2  = (String) o2;
                    int    index1 = file1.indexOf("evt");
                    int    index2 = file2.indexOf("evt");

                    if ((index1 == -1) || (index2 == -1)) {
                        System.err.println("ERROR: File does conform to naming specification, no evt##### found.");
                        System.err.println("Offending file: " + file1 + " or " + file2);

                        return file1.compareTo(file2);
                    }

                    String event1 = file1.substring(index1, index1 + 8);
                    String event2 = file2.substring(index2, index2 + 8);

                    return event1.compareTo(event2);
                }
            });

            if ((files != null) && (files.length > 0)) {
                fileList.addAll(Arrays.asList(files));
            }
        }
    }

    /**
     * Utility to copy src file to target file
     * @param src file to copy from
     * @param target file to copy to
     */
    public boolean copy(File src, File dest) throws Exception {
        try {

            // create/overwrite target
            dest.createNewFile();

            InputStream  in  = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            IOUtils.copy(in, out);
            in.close();
            out.flush();
            out.close();

            return true;
        } catch (Exception e) {

            // if can't find a file, delete empty file, display message,
            // return and don't transcode.
            dest.delete();

            String message = new String("Error copying " + ((dest != null)
                    ? dest.toString()
                    : "[null]") + "\nException: " + e.toString());

            throw new Exception(message);
        }
    }

    /**
     * Adds a file o the image training set directory for this model
     * @param file to add
     * @return true if the file was succesfully copied
     */
    public boolean addToTrainingSet(File file) {
        if ((rawImageDirectory != null) && rawImageDirectory.canWrite() && file.canRead()) {
            File moved = new File(rawImageDirectory, file.getName());

            try {
                if (!copy(file, moved)) {
                    return false;
                }
            } catch (Exception ex) {
                Logger.getLogger(ClassModel.class.getName()).log(Level.SEVERE, null, ex);
            }

            updateFileList();

            return true;
        }

        return false;
    }

    /**
     * Set the database root directory to store this model information to.
     * @param directory the name of the root directory
     */
    public void setDatabaseRoot(File directory) {
        dbrootDirectory = directory; 
    }

    /**
     * Returns the valid event image files found in an <code>ArrayList</code>
     * @return the sorted file listing
     */
    public ArrayList<String> getRawImageFileListing() {
        updateFileList();

        return fileList;
    }

    /**
     * Get the class name. This can be a name from the VARS knowledge base, or
     * a user-defined name
     * @return the class name
     */
    public String getName() {
        return className;
    }

    /**
     * Sets the class name. This can be from the VARS knowledge base or
     * a user-defined name
     * @param className
     */
    public void setName(String className) {
        this.className = className;
    }

    /**
     * Get the VARS class name. This is a name from the VARS knowledge base.
     */
    public String getVarsClassName() {
        return varsClassName;
    }

    /**
     * Set the associated vars name. This is a name from the VARS
     * knowledge base.
     */
    public void setVarsClassName(String varsClassName) {
        this.varsClassName = varsClassName;
    }

    /**
     * Get the description of the class. This is an optional,
     * user-defined description  of what this class represents.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the class. This is an optional,
     * user-defined description  of what this class represents.
     * @param desc the description
     */
    public void setDescription(String desc) {
        this.description = desc;
    }

    /**
     *
     * @return A formatted string ouput of this object
     */
    @Override
    public String toString() {
        return className;
    }

    /**
     * Returns a verbose formatted string description of the class
     * @return the description
     */
    public String description() {
        String s = "Class name: " + className + "\n" + "VARS class name: " + varsClassName + "\n" + "Description: "
                   + description + "\n" + "Color space: " + color + "\n" + "Database root directory: "
                   + dbrootDirectory + "\n" + "Raw image directory: " + rawImageDirectory + "\n"
                   + "Squared image directory: " + squaredImageDirectory + "\n";

        return s;
    }
}
