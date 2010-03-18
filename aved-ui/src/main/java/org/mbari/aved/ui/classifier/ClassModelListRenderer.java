/*
 * @(#)ClassModelListRenderer.java   10/03/17
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ColorSpace;
import org.mbari.aved.ui.Application;

//~--- JDK imports ------------------------------------------------------------

import java.awt.*;
import java.awt.image.*;

import java.io.File;

import java.net.*;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

/**
 * Configure to display the specified class image and class name
 * when the component's <code>paint</code> method is then called to
 * "render" the cell.  This is intended to be used in both JComboBox
 * and JList components.
 * @author dcline
 */
public class ClassModelListRenderer extends JLabel implements ListCellRenderer {
    private static final int PREFERRED_HEIGHT = 60;

    /** Preferred width and height for the display of the class thumbnails */
    private static final int PREFERRED_WIDTH = 200;
    private ImageIcon        icon;
    private final ListModel  listModel;
    private Font             missingImageFont;

    public ClassModelListRenderer(ListModel listModel) {
        this.listModel = listModel;
        setOpaque(true);
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(CENTER);
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
    }

    /**
     * Converts and image into gray scale if the color space is grayscale
     * TODO: convert to whatever color space is required here, not just gray
     * @param image
     * @param colorSpace
     * @return
     */
    public static ImageIcon convertImageIcon(Image image, ColorSpace colorSpace) {

        // Convert to gray if in that color space. Images
        // are actually stored in color but converted to gray
        // in the classifier. Display them as GRAY to avoid
        // confusion as the classifier can only work in one
        // color space at a time, e.g. classes in different
        // color spaces cannot be mixed
        if (colorSpace.equals(ColorSpace.GRAY)) {

            // This isn't necessarily how the image is rendered in the Matlab
            // code, but it makes it easier to see the thumbnails
            ImageFilter   filter = new GrayFilter(true, 50);
            ImageProducer prod;

            prod = new FilteredImageSource(image.getSource(), filter);

            Image grayScale = Toolkit.getDefaultToolkit().createImage(prod);

            return new ImageIcon(grayScale);
        } else {
            return new ImageIcon(image);
        }
    }

    /*
     * This method finds the image and text corresponding
     * to the selected value and returns the label, which is
     * and image and name that represents a given ClassModel .
     * If no image can be found for a ClassModel, a
     * "(no image available) text is displayed instead.
     */
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        // Set the icon and text.  If icon was null, say so.
        if ((value != null) && value.getClass().equals(ClassModel.class)) {
            ClassModel model = (ClassModel) value;

            if (model != null) {
                ArrayList<String> listing = model.getRawImageFileListing();

                if (listing.size() > 0) {
                    File f = new File(model.getRawImageDirectory().toString() + "/" + listing.get(0));

                    icon = createImageIcon(f);
                } else {

                    // Insert a default icon here
                    URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

                    icon = new ImageIcon(url);
                }

                // scale icons to a normalized size here
                if (icon != null) {
                    Image image = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

                    icon = convertImageIcon(image, model.getColorSpace());
                }

                String name = model.getName();

                setIcon(icon);

                if (icon != null) {
                    setText(name);
                    setFont(list.getFont());
                } else {
                    setDefaultText(name + " (no image available)", list.getFont());
                }
            }
        }

        return this;
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    public static ImageIcon createImageIcon(File file) {

        // Create an image icon out of the first file found
        // This is completely arbitrary and there may be a better
        // imgae to represent any given class than the first image
        try {
            if (file != null) {
                java.net.URL url = file.toURL();

                return new ImageIcon(url);
            } else {
                System.err.println("Couldn't find file: " + file.getName());
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(ClassModelListRenderer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ClassModelListRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Set the default font and text
     */
    protected void setDefaultText(String missingText, Font normalFont) {
        if (missingImageFont == null) {
            missingImageFont = normalFont.deriveFont(Font.BOLD);
        }

        setFont(missingImageFont);
        setText(missingText);
    }
}
