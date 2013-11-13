/*
 * @(#)ImageChangeUtil.java
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



package org.mbari.aved.ui.thumbnail;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

/**
 *
 * The <code>ImageChangeUtil</code> is a static class with a collection
 * of methods to perform basic operations like scaling, rotating, and
 * fitting a  {@link java.awt.Image BufferedImage}.
 *
 * <p>
 * This class is used primarily by the {@link ThumbnailPicture} class to
 * scale thumbnails for viewing.
 *
 *  @see ThumbnailPicture
 */
public class ImageChangeUtil {
    private ImageChangeUtil() {}

    /**
     * Scales a <code>BufferedImage</code> using a theta that
     * defines the scaling in both width and height.
     *
     *
     * @param image BufferedImage
     * @param theta scaling factor. Must be between 0-1.0
     *
     * @return a scaled version of the original {@code BufferedImage}.
     * Throws exception if {@link java.awt.image.AffineTransformOp AffineTransformOp} fails to scale.
     */
    public static BufferedImage scale(BufferedImage image, float theta) throws Exception {

        // //System.out.println("theta: " + t);
        try {
            BufferedImage     newimage = image;
            AffineTransform   at       = AffineTransform.getScaleInstance((double) theta, (double) theta);
            AffineTransformOp op       = new AffineTransformOp(at, null);

            return op.filter(newimage, null);
        } catch (Exception ex) {
            System.err.println(ex.toString());
            ex.printStackTrace();

            // Fail safe because this randomly blew up once in Linux.
            // Might not be a real problem, but what the heck!
            return image;
        }
    }

    /**
     * Adjust image to fit in the given size maintaining the aspect ratio.
     * @param image BufferedImage
     * @param scaleWidth int
     * @param scaleHeight int
     *
     * @return BufferedImage that fits into the specified width and height
     * Throws exception if {@link java.awt.image.AffineTransformOp AffineTransformOp} fails to scale.
     */
    public static BufferedImage fitAspect(BufferedImage image, int scaleWidth, int scaleHeight) throws Exception {
        if (image == null) {
            return null;
        }

        int imgWidth  = image.getWidth();
        int imgHeight = image.getHeight();

        if ((scaleWidth > 0) && (scaleHeight > 0)) {
            float t = ImageChangeUtil.calcThetaDown(imgWidth, imgHeight, scaleWidth, scaleHeight);

            // System.out.println("Theta: " + t);
            if (t > 0.f) {
                return ImageChangeUtil.scale(image, t);
            }
        }

        return image;
    }

    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.G.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint,
            boolean higherQuality) {
        int           type = (img.getTransparency() == java.awt.Transparency.OPAQUE)
                             ? BufferedImage.TYPE_INT_RGB
                             : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret  = (BufferedImage) img;
        int           w, h;

        if (higherQuality) {

            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {

            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && (w > targetWidth)) {
                w /= 2;

                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && (h > targetHeight)) {
                h /= 2;

                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D    g2  = tmp.createGraphics();

            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
        } while ((w != targetWidth) || (h != targetHeight));

        return ret;
    }

    /**
     * Reszes image down to specified width and height
     *
     * @param toWidth width to scale to
     * @param toHeight height to scale to
     *
     * @return ResizedImage Returns null if the image did not need to be resized.
     */
    public static BufferedImage resizeImageDown(BufferedImage image, double toWidth, double toHeight) throws Exception {
        if (toWidth < 10) {
            return null;
        }

        if (toHeight < 10) {
            return null;
        }

        float t = ImageChangeUtil.calcThetaDown(image.getWidth(), image.getHeight(), toWidth, toHeight);

        if (t > 0.f) {

            // System.out.println("Resizing Needed!");
            return scale(image, t);
        }

        return null;
    }

    /**
     * Calculates how to size the picture up or down to fit in the area given.
     *
     * @param toWidth width to scale to
     * @param toHeight height to scale to
     *
     * @return the floating theta value to scale both width and height dimensions
     * to fit into the given <code>toWidth</code> and <code>toHeight</code>
     */
    public static float calcTheta(int fromWidth, int fromHeight, double toWidth, double toHeight) {
        float t = 1.0f;

        if ((int) toWidth == fromWidth && (int) toHeight == fromHeight) {
            return t;
        }

        // If either dimension is smaller, shrink.
        if ((toWidth < fromWidth) || (toHeight < fromHeight)) {
            return calcThetaDown(fromWidth, fromHeight, toWidth, toHeight);
        } else if ((toWidth > fromWidth) || (toHeight > fromHeight)) {
            return calcThetaUp(fromWidth, fromHeight, toWidth, toHeight);
        } else {
            return t;
        }
    }

    /**
     * Calculates how to size the picture up or down to fit in the area given.
     *
     * @param fromWidth original image width to scale from
     * @param fromHeight original image height to scale from
     *
     * @param toWidth width to scale down to
     * @param toHeight height to scale down to
     *
     * @return the floating theta value to scale both width and height dimensions
     * to fit into the given <code>toWidth</code> and <code>toHeight</code>
     */
    public static float calcThetaDown(int fromWidth, int fromHeight, double toWidth, double toHeight) {

        // System.out.println("Converting From: w: " + fromWidth + " h: " + fromHeight);
        // System.out.println("Converting To: w: " + toWidth + " h: " + toHeight);
        float wideBy = 0;

        if (fromWidth > toWidth) {
            wideBy = (float) toWidth / fromWidth;
        }

        float highBy = 0;

        if (fromHeight > toHeight) {
            highBy = (float) toHeight / fromHeight;
        }

        // Take the smaller fraction to result in the highest size reduction.
        float theta = 0;

        // If both the same, take one of them.
        if (wideBy == highBy) {
            theta = highBy;
        }    // Otherwise take the smallest number greater than zero.
                else {
            if ((wideBy > 0) && (highBy > 0)) {
                if (wideBy < highBy) {
                    theta = wideBy;
                } else {
                    theta = highBy;
                }
            } else if (wideBy > 0) {
                theta = wideBy;
            } else if (highBy > 0) {
                theta = highBy;
            }

            // System.out.println("DOWN wide by: " + wideBy + "  high by: " + highBy);
            // System.out.println("DOWN Theta: " + theta);
        }

        return theta;
    }

    /**
     * Calculates how to size the picture up to fit in the area given.
     *
     * @param fromWidth original image width to scale from
     * @param fromHeight original image height to scale from
     *
     * @param toWidth width to scale up to
     * @param toHeight height to scale up to
     *
     * @return the floating theta value to scale both width and height dimensions
     * to fit into the given <code>toWidth</code> and <code>toHeight</code>
     */
    public static float calcThetaUp(int fromWidth, int fromHeight, double toWidth, double toHeight) {

        // System.out.println("Converting From: w: " + fromWidth + " h: " + fromHeight);
        // System.out.println("Converting To: w: " + toWidth + " h: " + toHeight);
        float wideBy = 0;

        if (fromWidth < toWidth) {
            wideBy = (float) toWidth / fromWidth;
        }

        float highBy = 0;

        if (fromHeight < toHeight) {
            highBy = (float) toHeight / fromHeight;
        }

        // Take the smaller fraction to result in the smallest size increase.
        float theta = 0;

        // If both the same, take one of them.
        if (wideBy == highBy) {
            theta = highBy;
        }    // Otherwise take the smallest number greater than zero
                else {
            if (wideBy < highBy) {
                theta = wideBy;
            } else {
                theta = highBy;
            }
        }

        // System.out.println("UP Wide by: " + wideBy + "  igh By: " + highBy);
        // System.out.println("UP Theta: " + theta);
        return theta;
    }
}
