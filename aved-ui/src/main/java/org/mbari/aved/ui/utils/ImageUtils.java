/*
 * @(#)ImageUtils.java
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



package org.mbari.aved.ui.utils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import java.io.File;

import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.stream.*;

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

        Iterator        iter   = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter     writer = (ImageWriter) iter.next();
        ImageWriteParam iwp    = writer.getDefaultWriteParam();

        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionQuality(1);    // an integer between 0 and 1

        // 1 specifies minimum compression and maximum quality

        File                  file   = new File(imgOutFilePath);
        FileImageOutputStream output = new FileImageOutputStream(file);

        writer.setOutput(output);

        IIOImage iiimage = new IIOImage(thumbImage, null, null);

        writer.write(null, iiimage, iwp);
        writer.dispose();
    }
    
    /**
     * http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Tech/Chapter06/plotDemo.html#PlotPanel
     * Return height
     */
    public static Dimension setFont(String name, int style, Graphics g, String msg, int box_width) {
        int         type_size     = 12;
        int         type_size_min = 4;
        int         msg_width;
        FontMetrics fm;

        do {
            Font f = new Font(name, style, type_size);    // $NON-NLS-1$

            // Create the font and pass it to the Graphics context
            // g.setFont(new Font("Monospaced", Font.PLAIN, type_size));
            g.setFont(f);

            // Get measures needed to center the message
            fm = g.getFontMetrics();

            // How many pixels wide is the string
            msg_width = fm.stringWidth(msg);

            // See if the text will fit
            if (msg_width < box_width) {

                // { x = x_box + (box_width / 2) - (msg_width / 2);}
                break;
            }

            // Try smaller type
            type_size -= 2;
        } while (type_size >= type_size_min);

        // Don't display the numbers if they did not fit
        if (type_size < type_size_min) {
            return new Dimension(0, 0);
        }

        return new Dimension(msg_width, fm.getHeight());
    }
}
