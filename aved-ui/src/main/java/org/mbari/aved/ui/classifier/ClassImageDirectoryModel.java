/*
 * @(#)ClassImageDirectoryModel.java   10/03/17
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

import com.jgoodies.binding.beans.Model;

//~--- JDK imports ------------------------------------------------------------

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ClassImageDirectoryModel extends Model {
    private List     fileList        = new ArrayList();
    private String[] imageExtensions = { "ppm", "jpg", "jpeg" };
    private File     directory;
    private String   name;
    private String   status;

    public ClassImageDirectoryModel() {
        addPropertyChangeListener("directory", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateFileList();
            }
        });
    }

    public String getDirectory() {
        try {
            return directory.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();

            return "error accessing directory";
        }
    }

    public void setDirectory(File directory) {
        File oldDir = this.directory;

        this.directory = directory;
        firePropertyChange("directory", oldDir, directory);
    }

    public void updateFileList() {
        setStatus("Updating file list...");
        fileList.clear();

        if (directory.isDirectory() == false) {
            firePropertyChange("fileList", fileList, fileList, false);
            setStatus("Error: File is not a directory");

            return;
        }

        String[] files = directory.list(new FilenameFilter() {
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
        fileList.addAll(Arrays.asList(files));
        firePropertyChange("fileList", null, fileList, false);
        setStatus("Ready");
    }

    public List getFileList() {
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
            setName(directory.getName());
        }

        return name;
    }

    public void setName(String name) {
        String oldName = this.name;

        this.name = name;
        firePropertyChange("name", oldName, name);
    }
}
