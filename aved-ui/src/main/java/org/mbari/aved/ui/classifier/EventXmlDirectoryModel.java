/*
 * @(#)EventXmlDirectoryModel.java
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

import com.jgoodies.binding.beans.Model;

//~--- JDK imports ------------------------------------------------------------

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.FileNotFoundException; 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventXmlDirectoryModel extends Model {
    private List<File> fileList = new ArrayList<File>();
    private String     name;
    private File       startingDirectory;
    private String     status;

    public EventXmlDirectoryModel() {
        addPropertyChangeListener("directory", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateFileList();
            }
        });
    }

    public File getDirectory() {
        return startingDirectory;
    }

    public void setDirectory(File directory) {
        File oldDir = this.startingDirectory;

        this.startingDirectory = directory;
        firePropertyChange("directory", oldDir, directory);
    }

    /**
     * Directory is valid if it exists, does not represent a file, and can be read.
     */
    static private void validateDirectory(File aDirectory) throws FileNotFoundException, IllegalArgumentException {
        if (aDirectory == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }

        if (!aDirectory.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + aDirectory);
        }

        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " + aDirectory);
        }

        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
        }
    }

    /**
     * Recursively walk a directory tree and return a List of all
     * Files found; the List is sorted using File.compareTo().
     *
     * @param aStartingDir is a valid directory, which can be read.
     */
    static public List<File> getFileListing(File aStartingDir) throws FileNotFoundException,IllegalArgumentException {

        validateDirectory(aStartingDir);

        List<File> result = getFileListingNoSort(aStartingDir);

        Collections.sort(result);

        return result;
    }

    static boolean isEventFile(File file) {
        if (file.isFile()) {
            String name      = file.getName();
            String extension = name.substring(name.indexOf('.') + 1);

            if (extension.equalsIgnoreCase("events.xml")) {
                return true;
            }
        }

        return false;
    }

    static private List<File> getFileListingNoSort(File aStartingDir) throws FileNotFoundException {
        List<File> result       = new ArrayList<File>();
        File[]     filesAndDirs = aStartingDir.listFiles();
        List<File> filesDirs    = Arrays.asList(filesAndDirs);

        for (File file : filesDirs) {
            if (file.isFile()) {
                if (isEventFile(file)) {
                    result.add(file);
                }
            } else {
                List<File> list = getFileListingNoSort(file);

                for (File f : list) {
                    result.add(f);
                }
            }
        }

        return result;
    }

    public void updateFileList() {
        try {
            setStatus("Updating event file list...");
            fileList.clear();
            fileList = getFileListing(startingDirectory);
            firePropertyChange("fileList", null, fileList, false);
            setStatus("Ready");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EventXmlDirectoryModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(EventXmlDirectoryModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<File> getFileList() {
        return fileList;
    }

    public void setStatus(String status) {
        String oldStatus = this.status;

        this.status = status;
        firePropertyChange("status", oldStatus, status);
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        if (name == null) {
            setName(startingDirectory.getName());
        }

        return name;
    }

    public void setName(String name) {
        String oldName = this.name;

        this.name = name;
        firePropertyChange("name", oldName, name);
    }
}
