/*
 * @(#)ProcessedResultsFileFilter.java
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



package org.mbari.aved.ui.utils;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.utils.ImageUtils;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ProcessedResultsFileFilter extends FileFilter {

    // Accept all directories with an xml file
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = ImageUtils.getExtension(f);

        if (extension != null) {
            if (extension.equals("XML") || extension.equals("xml")) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    // The description of this filter
    public String getDescription() {
        return "All XML files";
    }
}
