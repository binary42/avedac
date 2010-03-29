/*
 * @(#)ImageFilenameFilter.java
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
import java.io.FilenameFilter;

public class ImageFilenameFilter implements FilenameFilter {

    // Accept all directories and all mpeg, ppm, mov, and dpx files
    public boolean accept(File f, String name) {
        if ((f.isDirectory() && name.endsWith(ImageUtils.mpeg)) || name.endsWith(ImageUtils.ppm)
                || name.endsWith(ImageUtils.mov) || name.endsWith(ImageUtils.dpx)) {
            return true;
        } else {
            return false;
        }
    }

    // The description of this filter
    public String getDescription() {
        return "Support Images and Clips";
    }
}
