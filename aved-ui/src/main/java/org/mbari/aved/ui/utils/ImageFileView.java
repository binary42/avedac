/*
 * @(#)ImageFileView.java   10/03/17
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

import javax.swing.*;
import javax.swing.filechooser.*;

/* ImageFileView.java is used by FileChooserDemo2.java. */
public class ImageFileView extends FileView {
    ImageIcon movIcon = new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/quicktime.gif"));
    ImageIcon pngIcon = new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/png.gif"));
    ImageIcon mpgIcon = new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/mpg.gif"));
    ImageIcon dpxIcon = new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/dpx.gif"));

    public String getName(File f) {
        return null;    // let the L&F FileView figure this out
    }

    public String getDescription(File f) {
        return null;    // let the L&F FileView figure this out
    }

    public Boolean isTraversable(File f) {
        return null;    // let the L&F FileView figure this out
    }

    public String getTypeDescription(File f) {
        String extension = ImageUtils.getExtension(f);
        String type      = null;

        if (extension != null) {
            if (extension.equals(ImageUtils.mpeg)) {
                type = "MPEG Clip";
            } else if (extension.equals(ImageUtils.mov)) {
                type = "Quicktime Clip";
            } else if (extension.equals(ImageUtils.pnm) || (extension.equals(ImageUtils.pnm))) {
                type = "PNM Image";
            } else if (extension.equals(ImageUtils.ppm) || (extension.equals(ImageUtils.ppm))) {
                type = "PPM Image";
            } else if (extension.equals(ImageUtils.jpg) || (extension.equals(ImageUtils.jpg))) {
                type = "JPG Image";
            } else if (extension.equals(ImageUtils.dpx)) {
                type = "DPX Image";
            }
        }

        return type;
    }

    public Icon getIcon(File f) {
        String extension = ImageUtils.getExtension(f);
        Icon   icon      = null;

        if (extension != null) {
            if (extension.equals(ImageUtils.mpeg)) {
                icon = mpgIcon;
            } else if (extension.equals(ImageUtils.mov)) {
                icon = movIcon;
            } else if (extension.equals(ImageUtils.png) || (extension.equals(ImageUtils.ppm))) {
                icon = pngIcon;
            } else if (extension.equals(ImageUtils.dpx)) {
                icon = dpxIcon;
            }
        }

        return icon;
    }
}
