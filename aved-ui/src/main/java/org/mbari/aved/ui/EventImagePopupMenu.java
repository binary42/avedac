/*
 * @(#)EventImagePopupMenu.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.BoundingBox;
import aved.model.EventObject;

import org.mbari.aved.ui.exceptions.FrameOutRangeException;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.player.DisplayThumbnail;
import org.mbari.aved.ui.thumbnail.ImageChangeUtil;

//~--- JDK imports ------------------------------------------------------------

import java.awt.*;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import java.awt.image.BufferedImage;
import java.io.File;

import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * Creates and displays popupMenu with a single image of the "best"
 * representation of an Event
 *
 * @author D.Cline
 */
public class EventImagePopupMenu {

    /** Default image to display upon missing frame exceptions */
    private static PlanarImage missingImage;
    private int                imageHeight = 0;

    /** Current viewport width/height for jai */
    private int                  imageWidth = 0;
    private EventObjectContainer event;
    private JPanel               imagePanel;

    /**
     * The thumbnail to display in a popup window
     * We use the DisplayThumbnail class which is a
     * subclass of DisplayJAI. This class allows
     * us to scale potentially large images down
     * into our view, or it can be used with no
     * scaling
     */
    private DisplayThumbnail jai;
    private JPopupMenu       popupMenu;

    public EventImagePopupMenu(EventObjectContainer event) {
        this.event      = event;
        this.imagePanel = new JPanel(); 
                
        // Get a static copy of default image to display upon transformation errors
        URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

        if (url == null) {
            System.err.println("Cannot find missingframeexception.jpg");
            System.exit(1);
        }
        else {

            // Create an ImageIcon from the image data
            ImageIcon imageIcon = new ImageIcon(url);
            int width = imageIcon.getIconWidth();
            int height = imageIcon.getIconHeight();

            // Create a new empty image buffer to "draw" the resized image into
            BufferedImage bufferedResizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // Create a Graphics object to do the "drawing"
            Graphics2D g2d = bufferedResizedImage.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // Draw the resized image
            g2d.drawImage(imageIcon.getImage(), 0, 0, width, height, null);
            missingImage = PlanarImage.wrapRenderedImage(bufferedResizedImage);
        }
    }

    private void actionClickTable(MouseEvent e) {
        this.popupMenu.setVisible(false);
    }

    /**
     * Shows the popupMenu in the same position as the invoker (e.g. mouse click position)
     * @param invoker
     * @param x
     * @param y
     */
    public void show(Component invoker, int x, int y) {

        // Create the popupMenu menu.
        if (event != null) {
            popupMenu = new JPopupMenu(ApplicationInfo.getName() + " Event: " + Long.toString(event.getObjectId()));
            popupMenu.setToolTipText(event.getShortDescription());
            popupMenu.add(imagePanel);
            displayImage();
            popupMenu.show(invoker, x, y);
        }
    }

    /**
     * Controls telling the view what image to display from the
     * offset from the beginning of the event
     * @param offset
     */
    private void displayImage() {

        // Get the image sequence, and display the image
        try {
            File        src      = null;
            EventObject eventObj = null;
            int         num      = event.getBestEventFrame();

            eventObj = event.getEventObject(num);

            // Get the frame source and catch exception
            // in case it is missing
            try {
                src = event.getFrameSource(num);
            } catch (Exception e) {
                src = null;
                Logger.getLogger(EventImagePopupMenu.class.getName()).log(Level.SEVERE, null, e);

                return;
            }

            // TODO: push this logic down into the PlayerView
            // it knows how to best display the timecode
            displayEventImage(eventObj, src);

            // TODO: add this in displayTimecodeFrameString(eventObj.getFrameEventSet().getTimecode(),
            // eventObj.getFrameEventSet().getFrameNumber());
        } catch (FrameOutRangeException e) {

            // If image is missing display message
            // Create the yes/no dialog
            String message = "Error: " + e.getMessage();

            Logger.getLogger(EventImagePopupMenu.class.getName()).log(Level.SEVERE, null, e);
        } catch (Exception e) {
            Logger.getLogger(EventImagePopupMenu.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Display the event image for this event object.
     * This will load the image frame this EventObject
     * is in and overlay the bounding box representing it
     * on the image.
     */
    public void displayEventImage(EventObject eventObj, File frame) {
        PlanarImage image = null;

        if ((eventObj != null) && (frame != null)) {
            try {

                // Load up the frame, or use missing image it is undefined
                image = ((frame != null)
                         ? JAI.create("fileload", frame.toString())
                         : missingImage);
            } catch (IllegalArgumentException ex) {
                image = missingImage;
            }

            // If file load failed, use missingImage
            if (image == null) {
                image = missingImage;
            }

            // Calculate the cropping coordinates from the bounding box
            BoundingBox b       = eventObj.getBoundingBox();
            int         xorigin = b.getLowerLeftX();
            int         yorigin = b.getUpperRightY();
            int         width   = b.getUpperRightX() - b.getLowerLeftX();
            int         height  = b.getLowerLeftY() - b.getUpperRightY();

            // If the bounding box is beyond the image image size, adjust
            // this. This can occur if there is a discrepancy  between the
            // image used in p/mbarivision processing and the
            // image used in the graphical interface
            if (xorigin + width > image.getWidth()) {
                width = image.getWidth() - xorigin;
            }

            if (yorigin + height > image.getHeight()) {
                height = image.getHeight() - yorigin;
            }

            // Get the upperleft origin
            Point upperleftorigin = new Point(xorigin, yorigin);

            // Display the image and reposition the viewport
            // Note that all the image sizes and coordinates
            // are given in the actual image scale - any scaling that
            // needs to be done is done in jai.
            if ((jai != null) && (imageWidth >= image.getWidth()) && (imageHeight >= image.getHeight())) {
                jai.set(image);
                jai.repositionViewportBounds(width, height, upperleftorigin);
            } else {

                // Scale JAI if the image is larger than 60% of the main display
                // TODO: this is somewhat arbitrary and really should be scaled by
                // the total available size, and not just the JAI component
                Dimension d      = Application.getView().getSize();
                float     scale  = 1.0f;
                float     factor = 0.60f;

                if ((image.getWidth() >= factor * d.width) || (image.getHeight() >= factor * d.height)) {
                    scale = ImageChangeUtil.calcTheta(image.getWidth(), image.getHeight(), factor * d.width,
                                                      factor * d.height);
                }

                imageWidth  = image.getWidth();
                imageHeight = image.getHeight();
                jai         = new DisplayThumbnail(event, image, scale, width, height, upperleftorigin);
                imagePanel.add(jai);
            }

            image = null;
        }
    }
}
