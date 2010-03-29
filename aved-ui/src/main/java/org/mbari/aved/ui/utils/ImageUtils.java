/*
 * @(#)ImageUtils.java
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

//~--- JDK imports ------------------------------------------------------------

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ImageUtils {

    /** Busy and wait cursor */
    public final static Cursor busyCursor    = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    public final static Cursor defaultCursor = Cursor.getDefaultCursor();
    public final static String dpx           = "dpx";
    public final static String jpg           = "jpg";
    public final static String mov           = "mov";
    public final static String mpeg          = "mpeg";
    public final static String png           = "png";
    public final static String pnm           = "pnm";
    public final static String ppm           = "ppm";

    /*
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s   = f.getName();
        int    i   = s.lastIndexOf('.');

        if ((i > 0) && (i < s.length() - 1)) {
            ext = s.substring(i + 1).toLowerCase();
        }

        return ext;
    }

    /**
     * Converts an jpeg image into a squared version of the image.
     * This will create a new image with whatever dimension is larger -
     * height or width
     * @param imgInFile the file  of the jpeg image to convert
     * @param imgOutFile the file path to store the resulting image to
     * @throws java.lang.Exception if the image is not found, or is not
     * a jpeg images
     */
    public static void squareJpegThumbnail(String imgInFilePath, String imgOutFilePath) throws Exception {
        Image        image        = Toolkit.getDefaultToolkit().getImage(imgInFilePath);
        MediaTracker mediaTracker = new MediaTracker(new Container());

        mediaTracker.addImage(image, 0);
        mediaTracker.waitForID(0);

        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        int maxLength   = 0;

        // square the image with the largest dimension
        // if the image is already square then copy it
        if (imageWidth > imageHeight) {
            maxLength = imageWidth;
        } else {
            maxLength = imageHeight;
        }

        BufferedImage thumbImage = new BufferedImage(maxLength, maxLength, BufferedImage.TYPE_INT_RGB);
        Graphics2D    graphics2D = thumbImage.createGraphics();

        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, maxLength, maxLength, null);

        BufferedOutputStream out     = new BufferedOutputStream(new FileOutputStream(imgOutFilePath));
        JPEGImageEncoder     encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam      param   = encoder.getDefaultJPEGEncodeParam(thumbImage);
        int                  quality = 100;

        param.setQuality((float) quality / 100.0f, false);
        encoder.setJPEGEncodeParam(param);
        encoder.encode(thumbImage);
        out.flush();
        out.close();
    }
}
