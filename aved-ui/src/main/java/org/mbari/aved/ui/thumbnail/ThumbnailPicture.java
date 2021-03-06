/*
 * @(#)ThumbnailPicture.java
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

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.model.EventObjectContainer;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import org.mbari.aved.ui.utils.ImageUtils;

/**
 *
 * The <code>ThumbnailPicture</code> class creates a
 * picture that best represents an individual event
 * contained in an {@link org.mbari.aved.ui.model.EventObjectContainer}.
 * Images may be scaled down, but not up, depending on their size.
 *
 * The size of this <code>ThumbnailPicture</code>
 * is defined by the <code>ThumbnailPanel</code>.
 *
 * <p>The main role of <code>ThumbnailPicture</code> is to manage painting
 * a {@link java.awt.image.BufferedImage} in the best possible way to preserve
 * the aspect ratio of the original image.  If no image is found or there is
 * a problem transforming the image, a default image will be displayed
 * indicating what the error generally is.
 *
 * @see ThumbnailPanel
 */
public class ThumbnailPicture extends JComponent {
    public static String CLASS_DESCRIPTION     = "Class:";
    public static String INVALID_OBJECT_ID     = "-";
    public static String OBJECT_ID_DESCRIPTION = "Object ID:";
    public static int    PADDING               = 5;
    public static int    PADDINGx2             = PADDING * 2;
    public static String TAG_DESCRIPTION       = "Tag:";

    /** Default image to display if an image is missing */
    private static ImageIcon missingImageIcon;

    /**
     * Default image to display upon transform errors.
     * This can occur if there are problems scaling the image
     * and still maintaining the aspect ratio
     */
    private static ImageIcon transformErrorImageIcon;

    /** The container for the event represented by this picture */
    private EventObjectContainer eventObjectContainer;

    /** Scroller index used for controller the scrolling display */
    private int                iScroller;
    private ListSelectionModel listSelectionModel;
    private JPanel             panel;

    /**
     * ThumbnailPicture. <bold>This will exit the system if default images for transform and
     * missinframes are not found.</bold>
     * @param EventObjectContainer event represented in this <code>ThumbnailPicture</code>
     * @param panel Parent panel this resides in
     * @param list ListselectionModel this is chosen from
     */
    public ThumbnailPicture(EventObjectContainer container, ThumbnailPanel panel, ListSelectionModel list) {
        listSelectionModel = list;
        this.panel         = panel;
        super.setOpaque(false);

        if (transformErrorImageIcon == null) {

            // Get a static copy of default image to display upon transformation errors
            URL url = Application.class.getResource("/org/mbari/aved/ui/images/transformexception.jpg");

            if (url == null) {
                System.err.println("Cannot find transformexception.jpg");
                System.exit(1);
            } else {
                transformErrorImageIcon = new ImageIcon(url);
            }
        }

        if (missingImageIcon == null) {

            // Get a static copy of default image to display upon transformation errors
            URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

            if (url == null) {
                System.err.println("Cannot find missingframeexception.jpg");
                System.exit(1);
            } else {
                missingImageIcon = new ImageIcon(url);
            }
        }

        reset(container);
    }

    /**
     * Resets the EventObjectContainer that this thumbnail picture represents
     *
     * @param container
     */
    public void reset(EventObjectContainer container) {
        eventObjectContainer = container;
    }

    /**
     * Set the scroller index for this picture
     * @param i The scroller index
     */
    void setScrollerIndex(int i) {
        iScroller = i;
    }

    /**
     * Helper function to return EventObjectContainer represented by this
     * ThumbnailPicture
     * @return the contained EventObjectContiner object
     */
    public EventObjectContainer getEventObjectContainer() {
        return eventObjectContainer;
    }

    /**
     * Helper function to get the scroller index for this ThumbnailPicture
     * @return scroller index
     */
    int getScrollerIndex() {
        return iScroller;
    }

    // ==========================================================================
    // Overridden Methods
    // ==========================================================================
    protected void paintComponent(Graphics graphics) {
        Graphics g = graphics.create();

        if (panel != null) {
            if (listSelectionModel.isSelectedIndex(iScroller) && (eventObjectContainer != null)) {
                setBackground(Application.lookAndFeelSettings.getSelectedColor());
            } else {
                setBackground(Color.gray);
            }

            // Draw in our entire space, even if isOpaque is false.
            g.setColor(this.getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());

            // Draw a rectangle around the component
            g.setColor(Color.white);

            BasicStroke stroke = new BasicStroke(4.0f);
            Graphics2D  g2     = (Graphics2D) g;

            g2.setStroke(stroke);
            g2.drawRect(1, 1, super.getWidth(), super.getHeight());
        }

        if (eventObjectContainer != null) {
            ImageIcon imageicon = eventObjectContainer.getBestImage();

            // If the event object does not have an image, set it to a default image
            if (imageicon == null) {
                imageicon = missingImageIcon;
            }

            Graphics2D g2 = (Graphics2D) g;

            g2.setPaint(Color.BLACK);

            Dimension textSize = null;
            Dimension d1       = null;
            Dimension d2       = null;

            // If is valid object identifier, then name with object id
            if (eventObjectContainer.isValid()) {
                d1 = ImageUtils.setFont("Lucida Grande", Font.PLAIN, g, OBJECT_ID_DESCRIPTION, super.getWidth());
                d2 = ImageUtils.setFont("Sanserif", Font.ITALIC, g, Long.toString(eventObjectContainer.getObjectId()),
                             super.getWidth());
                textSize = new Dimension(d1.height + d2.height, d1.width + d2.width);
            } else {
                d1 = ImageUtils.setFont("Lucida Grande", Font.PLAIN, g, OBJECT_ID_DESCRIPTION, super.getWidth());
                d2 = ImageUtils.setFont("Sanserif", Font.ITALIC, g, INVALID_OBJECT_ID, super.getWidth());
            }

            // A total of three rows, including tag and class name to the textSize height
            textSize = new Dimension(3 * (d1.height + d2.height), d1.width + d2.width);

            // Adjust text height to leave room for text.
            int           imgHeight = super.getHeight() - (int) textSize.getHeight() - PADDINGx2;
            int           imgWidth  = super.getWidth() - PADDINGx2;
            BufferedImage im        = null;
            int           topOfText = 0;

            if ((imgWidth > 0) && (imgHeight > 0)) {
                try {

                    // Create an ImageIcon from the image data
                    int width  = imageicon.getIconWidth();
                    int height = imageicon.getIconHeight();

                    // Create a new empty image buffer to "draw" the resized image into
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                    // Create a Graphics object to do the "drawing"
                    Graphics2D g2d = image.createGraphics();

                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                    // Draw the resized image
                    g2d.drawImage(imageicon.getImage(), 0, 0, width, height, null);
                    g2d.dispose();

                    // Fit image into aspect ratio and draw it in the center
                    // of the block
                    im = ImageChangeUtil.fitAspect(image, imgWidth, imgHeight);

                    int centerX = (int) ((super.getWidth() - im.getWidth()) / 2);
                    int centerY = (int) ((super.getHeight() - im.getHeight()) / 2);

                    g2.drawImage(im, centerX, centerY, null);
                    topOfText = centerY + im.getHeight();

                    // If cannnot scale, then log and display transform error image
                } catch (Exception ex) {
                    System.err.println("Scaling error" + ex.toString());

                    if (transformErrorImageIcon != null) {
                        try {

                            // Create an ImageIcon from the image data
                            int width  = transformErrorImageIcon.getIconWidth();
                            int height = transformErrorImageIcon.getIconHeight();

                            // Create a new empty image buffer to "draw" the resized image into
                            BufferedImage transformErrImage = new BufferedImage(width, height,
                                                                  BufferedImage.TYPE_INT_RGB);

                            // Create a Graphics object to do the "drawing"
                            Graphics2D g2d = transformErrImage.createGraphics();

                            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                                 RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                            // Draw the resized image
                            g2d.drawImage(transformErrorImageIcon.getImage(), 0, 0, width, height, null);
                            g2d.dispose();

                            // Fit image into aspect ratio
                            im = ImageChangeUtil.fitAspect(transformErrImage, imgWidth, imgHeight);

                            int centerX = (int) ((super.getWidth() - im.getWidth()) / 2);
                            int centerY = (int) ((super.getHeight() - im.getHeight()) / 2);

                            topOfText = centerY + im.getHeight();
                            g2.drawImage(im, centerX, centerY, null);
                        } catch (Exception ex1) {
                            Logger.getLogger(ThumbnailPicture.class.getName()).log(Level.SEVERE, null, ex1);
                        }
                    }
                }
            }    // If image height and width > 0

            // Draw the text just under the image now that we know the
            // final size of the image.
            if (textSize.getWidth() > 0) {

                // Widthwize, center the text.
                int textX = (int) ((super.getWidth() - textSize.getWidth()) / 2);

                g.setColor(Color.BLACK);

                // Draw the object ID descriptor in black italic
                ImageUtils.setFont("Sanserif", Font.ITALIC, g, OBJECT_ID_DESCRIPTION, super.getWidth());

                FontMetrics fm = g.getFontMetrics();

                // Text is draw up from the Ascent and down from the Descent.
                int textY = (int) (topOfText + fm.getMaxAscent());

                g.drawString(OBJECT_ID_DESCRIPTION, textX, textY);

                // Get a valid object identifier
                String objectId = (eventObjectContainer.isValid()
                                   ? Long.toString(eventObjectContainer.getObjectId())
                                   : INVALID_OBJECT_ID);

                // Draw the object ID in bold
                ImageUtils.setFont("Sanserif", Font.BOLD, g, objectId, super.getWidth());
                g.drawString(objectId, textX + d1.width, textY);

                if (eventObjectContainer.getTag().length() > 0) {
                    textY += fm.getMaxAscent();

                    // Draw the tag descriptor in italic
                    Dimension d = ImageUtils.setFont("Sanserif", Font.ITALIC, g, TAG_DESCRIPTION, super.getWidth());

                    g.drawString(TAG_DESCRIPTION, textX, textY);

                    // Draw the tag in bold
                    ImageUtils.setFont("Sanserif", Font.BOLD, g, eventObjectContainer.getTag(), super.getWidth());
                    g.drawString(eventObjectContainer.getTag(), textX + d.width, textY);
                }

                if (eventObjectContainer.getClassName().length() > 0) {
                    textY += fm.getMaxAscent();

                    // Draw the tag descriptor in italic
                    Dimension d = ImageUtils.setFont("Sanserif", Font.ITALIC, g, CLASS_DESCRIPTION, super.getWidth());

                    g.drawString(CLASS_DESCRIPTION, textX, textY);

                    // Draw the class name in bold
                    ImageUtils.setFont("Sanserif", Font.BOLD, g, eventObjectContainer.getClassName(), super.getWidth());
                    g.drawString(eventObjectContainer.getClassName(), textX + d.width, textY);
                }
            }
        }    // If eventObjectContainer != null

        g.dispose();
    }


    boolean flipSelection() {
        if (listSelectionModel.isSelectedIndex(iScroller)) {
            listSelectionModel.removeSelectionInterval(iScroller, iScroller);
            this.repaint();
            return false;
        } else {
            listSelectionModel.addSelectionInterval(iScroller, iScroller);
            this.repaint();
            return true;
        }

    }
}
