/*
 * @(#)ThumbnailPicture.java   10/03/17
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
class ThumbnailPicture extends JComponent {
    private static String CLASS_DESCRIPTION     = "Class:";
    private static String INVALID_OBJECT_ID     = "-";
    private static String OBJECT_ID_DESCRIPTION = "Object ID:";
    private static int    PADDING               = 5;
    private static int    PADDINGx2             = PADDING * 2;
    private static String TAG_DESCRIPTION       = "Tag:";

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
                d1 = setFont("Lucida Grande", Font.PLAIN, g, OBJECT_ID_DESCRIPTION, super.getWidth());
                d2 = setFont("Sanserif", Font.ITALIC, g, Long.toString(eventObjectContainer.getObjectId()),
                             super.getWidth());
                textSize = new Dimension(d1.height + d2.height, d1.width + d2.width);
            } else {
                d1 = setFont("Lucida Grande", Font.PLAIN, g, OBJECT_ID_DESCRIPTION, super.getWidth());
                d2 = setFont("Sanserif", Font.ITALIC, g, INVALID_OBJECT_ID, super.getWidth());
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
                setFont("Sanserif", Font.ITALIC, g, OBJECT_ID_DESCRIPTION, super.getWidth());

                FontMetrics fm = g.getFontMetrics();

                // Text is draw up from the Ascent and down from the Descent.
                int textY = (int) (topOfText + fm.getMaxAscent());

                g.drawString(OBJECT_ID_DESCRIPTION, textX, textY);

                // Get a valid object identifier
                String objectId = (eventObjectContainer.isValid()
                                   ? Long.toString(eventObjectContainer.getObjectId())
                                   : INVALID_OBJECT_ID);

                // Draw the object ID in bold
                setFont("Sanserif", Font.BOLD, g, objectId, super.getWidth());
                g.drawString(objectId, textX + d1.width, textY);

                if (eventObjectContainer.getTag().length() > 0) {
                    textY += fm.getMaxAscent();

                    // Draw the tag descriptor in italic
                    Dimension d = setFont("Sanserif", Font.ITALIC, g, TAG_DESCRIPTION, super.getWidth());

                    g.drawString(TAG_DESCRIPTION, textX, textY);

                    // Draw the tag in bold
                    setFont("Sanserif", Font.BOLD, g, eventObjectContainer.getTag(), super.getWidth());
                    g.drawString(eventObjectContainer.getTag(), textX + d.width, textY);
                }

                if (eventObjectContainer.getClassName().length() > 0) {
                    textY += fm.getMaxAscent();

                    // Draw the tag descriptor in italic
                    Dimension d = setFont("Sanserif", Font.ITALIC, g, CLASS_DESCRIPTION, super.getWidth());

                    g.drawString(CLASS_DESCRIPTION, textX, textY);

                    // Draw the class name in bold
                    setFont("Sanserif", Font.BOLD, g, eventObjectContainer.getClassName(), super.getWidth());
                    g.drawString(eventObjectContainer.getClassName(), textX + d.width, textY);
                }
            }
        }    // If eventObjectContainer != null

        g.dispose();
    }

    /**
     * http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Tech/Chapter06/plotDemo.html#PlotPanel
     * Return height
     */
    private static Dimension setFont(String name, int style, Graphics g, String msg, int box_width) {
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

    void flipSelection() {
        if (listSelectionModel.isSelectedIndex(iScroller)) {
            listSelectionModel.removeSelectionInterval(iScroller, iScroller);
        } else {
            listSelectionModel.addSelectionInterval(iScroller, iScroller);
        }

        this.repaint();
    }
}
