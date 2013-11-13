/*
 * @(#)ImageFileView.java
 * 
 * Copyright 2013 MBARI
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



package org.mbari.aved.ui.utils;

//~--- non-JDK imports --------------------------------------------------------
 
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
