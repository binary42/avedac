/*
 * @(#)DisplayThumbnail.java
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



package org.mbari.aved.ui.player;

//~--- JDK imports ------------------------------------------------------------

import com.sun.media.jai.widget.DisplayJAI;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

/**
 *
 * @author dcline
 *
 * This class displays an image and puts a bounding box around
 * in a specified location.
 */
public class DisplayThumbnail extends DisplayJAI {

    // The dimensions of the original image.
    private int imageWidth, imageHeight;

    // dragging the viewport.
    private int lastX, lastY;

    // The scale (< 1.0) for the thumbnail creation.
    private float scale;

    // The scaled bounding box
    private Rectangle2D scaledBoundingBox;

    // The dimensions of the visible region.
    private int visibleRegionWidth, visibleRegionHeight;

    /**
     * The constructor for the class, which creates a thumbnail version of
     * the image and sets the user interface. This can be used for more
     * complex operations like grabbing an image in the the region
     * under the viewport, changing the viewport color, etc. but none
     * of these operations are currently used. However, these operations
     * were left in in anticipation of doing more complex operations
     * in the AVED UI like correcting/adding the bounding boxes, e.g.
     *
     * @param image the image to be used for the thumbnail creation.
     * @param scale the scale to be used for the thumbnail creation.
     * @param width the width of the desired viewport (pixels).
     * @param height the width of the desired viewport (pixels).
     * @param upperleft the position to place the viewport at relative to the PlanarImage @param.
     * @param model the model to use with the EventPopupMenu
     */
    public DisplayThumbnail(PlanarImage image, float scale, int width, int height, Point upperleft) {
        this.scale          = scale;
        visibleRegionWidth  = width;
        visibleRegionHeight = height;
        set(image);

        // We'd like to listen to mouse movements within this rather
        // then delegate to a controller because it's simpler
        addMouseMotionListener(this);
        addMouseListener(this);

        // Initially the scaled viewport will be positioned at upperleft.x,y
        scaledBoundingBox = new Rectangle2D.Float(upperleft.x * scale, upperleft.y * scale, width * scale,
                height * scale);
    }

    /**
     * Sets the image for this display
     * @param image the image to set
     */
    public void set(PlanarImage image) {

        // Get some stuff about the image.
        imageWidth  = image.getWidth();
        imageHeight = image.getHeight();

        // Must create a thumbnail image using that scale.
        ParameterBlock pb = new ParameterBlock();

        pb.addSource(image);
        pb.add(scale);
        pb.add(scale);
        pb.add(0.0F);
        pb.add(0.0F);
        pb.add(new InterpolationNearest());

        PlanarImage thumbnail = JAI.create("scale", pb, null);

        // Use this thumbnail as the image for the DisplayJAI component.
        super.set(thumbnail);
    }

    /**
     * This method will repaint the component. It will draw the thumbnail image,
     * then draw some yellow lines over the tiles' boundary (if the image is
     * tiled) then draw the viewport.
     */
    public synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Paint the bouding box
        g2d.setColor(Color.YELLOW);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        Stroke stroke = new BasicStroke(1f);

        g2d.setStroke(stroke);
        g2d.draw(scaledBoundingBox);
    }

    /**
     * Return the real world (original image) bounds based on the input mouse position.
     * @return the original image's bounds.
     */
    public Rectangle getImageBounds(int x, int y) {

        // Ignore events outside the border.
        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        if (y > (Math.round(imageHeight * scale))) {
            y = Math.round(imageHeight * scale);
        }

        if (x > (Math.round(imageWidth * scale))) {
            x = Math.round(imageWidth * scale);
        }

        int fromX  = (int) Math.round((x) / scale);
        int fromY  = (int) Math.round((y) / scale);
        int width  = (int) Math.round(imageWidth / scale);
        int height = (int) Math.round(imageHeight / scale);

        return new Rectangle(fromX, fromY, width, height);
    }

    /**
     * Return the real world (original image) bounds based on the scaledBoundingBox
     * @return the original image's bounds.
     */
    public Rectangle getCroppedImageBounds() {

        // Get the boundaries in the original image coordinates.
        int fromX  = (int) Math.round((scaledBoundingBox.getX()) / scale);
        int fromY  = (int) Math.round((scaledBoundingBox.getY()) / scale);
        int width  = (int) Math.round(scaledBoundingBox.getWidth() / scale);
        int height = (int) Math.round(scaledBoundingBox.getHeight() / scale);

        // Fix rounding errors to avoid exceptions on the crop.
        fromX = Math.min(fromX, (imageWidth - visibleRegionWidth));
        fromY = Math.min(fromY, (imageHeight - visibleRegionHeight));

        return new Rectangle(fromX, fromY, width, height);
    }

    /**
     * Repositions the scaled viewport bounds
     */
    public void repositionViewportBounds(int width, int height, Point upperleft) {
        visibleRegionWidth  = width;
        visibleRegionHeight = height;

        Rectangle initBounds = getBoundingBoxBounds();

        // The scaled viewport is positioned at upperleft.x,y
        scaledBoundingBox = new Rectangle2D.Float(upperleft.x * scale, upperleft.y * scale, width * scale,
                height * scale);

        // Store the approximate region where the viewport is after the change.
        Rectangle finalBounds = getBoundingBoxBounds();

        // Repaint only the needed section.
        repaint(finalBounds.union(initBounds));
    }

    /**
     * Return the scaled bounding box bounds.
     * @return the bounding box bounds.
     */
    public Rectangle getBoundingBoxBounds() {
        Rectangle temp = scaledBoundingBox.getBounds();

        temp.setBounds((int) temp.getX(), (int) temp.getY(), (int) temp.getWidth(), (int) temp.getHeight());

        return temp;
    }
}
