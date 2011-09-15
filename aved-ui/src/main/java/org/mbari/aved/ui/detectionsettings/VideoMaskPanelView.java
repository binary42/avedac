/*
 * @(#)VideoMaskPanelView.java
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



package org.mbari.aved.ui.detectionsettings;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.player.DisplayThumbnail;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class VideoMaskPanelView extends JFrameView {
    public static final String ID_BROWSE_BUTTON          = "browse";       // javax.swing.JButton
    public static final String ID_CLOSE_BUTTON           = "close";        // javax.swing.JButton
    public static final String ID_CURSOR_LABEL           = "cursor";       // JLabel
    public static final String ID_FILE_COMBO             = "file";         // javax.swing.JComboBox
    public static final String ID_RESET_DEFAULTS_BUTTON  = "reset";        // javax.swing.JButton
    public static final String ID_VIDEO_MASK_IMAGE_PANEL = "videomask";    // ImageComponent
    private JLabel             cursorLabel;
    private DisplayThumbnail   maskThumbnail;

    public VideoMaskPanelView(DetectionSettingsModel model, VideoMaskPanelController controller) {
        super("org/mbari/aved/ui/forms/DetectionSettingsVideoMask.xml", model, controller);

        ActionHandler actionHandler = getActionHandler();

        // Add action handler to panel button and combo boxes
        getForm().getComboBox(ID_FILE_COMBO).addActionListener(actionHandler);
        getForm().getButton(ID_BROWSE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_CLOSE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_RESET_DEFAULTS_BUTTON).addActionListener(actionHandler);

        // Initialize frequently accessed components
        cursorLabel = getForm().getLabel(ID_CURSOR_LABEL);
        disableCursor();
        loadModel(model);
    }

    public JComboBox getFileComboBox() {
        return getForm().getComboBox(ID_FILE_COMBO);
    }

    public DisplayThumbnail getMaskThumbnail() {
        return maskThumbnail;
    }

    public void disableCursor() {
        cursorLabel.setVisible(false);
    }

    public void displayCursorLabel(String text) {
        if (!cursorLabel.isVisible()) {
            cursorLabel.setVisible(true);
        }

        cursorLabel.setText(text);
    }

    public void loadModel(DetectionSettingsModel model) {
        try {
            displayMask(model.getVideoMaskFile());
        } catch (Exception e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void repaint() {

        // Force repaint of the panel component. For some reason this
        // doesn't automatically repaint itself. Maybe JAI bug ?
        getForm().getPanel(ID_VIDEO_MASK_IMAGE_PANEL).repaint();
    }

    public void modelChanged(ModelEvent event) {}

    public void displayMask(File f) throws Exception {
        PlanarImage image = null;

        // Remove existing image if one exists
        if (maskThumbnail != null) {
            getForm().getPanel(ID_VIDEO_MASK_IMAGE_PANEL).remove(maskThumbnail);
            maskThumbnail.removeMouseMotionListener((VideoMaskPanelController) getController());
            maskThumbnail = null;
            repaint();
        }

        if (f != null) {

            // Now get the image mask and add it to the panel
            try {

                /*
                 * Point upperleft = new Point(0,0);
                 * JPanel panel = getForm().getPanel(ID_VIDEO_MASK_IMAGE_PANEL);
                 * image = JAI.create("fileload", f.toString());
                 * int viewportwidth = panel.getWidth();
                 * int viewportheight = panel.getHeight();
                 * // Set the default scale to 1.0
                 * float scale = 1.0f;
                 * // If the image width or height is greater than the viewport can fit, find the best scale
                 * if(image.getWidth() > viewportwidth || image.getHeight() > viewportheight) {
                 * float scaleheight = (float)viewportheight/(float)image.getHeight();
                 * float scalewidth = (float)viewportwidth/(float)image.getWidth();
                 * // take the smaller of the two scales
                 * scale = (scalewidth < scaleheight )? scalewidth:scaleheight;
                 * }
                 *
                 * maskThumbnail = new DisplayThumbnail(image,scale,
                 * viewportwidth, viewportheight, upperleft,
                 * getModel());
                 * panel.add(maskThumbnail);
                 *
                 * // We must register mouse motion listeners to it !
                 * maskThumbnail.addMouseMotionListener((VideoMaskPanelController)getController());
                 * maskThumbnail.useViewport(false);
                 */
            } catch (Exception e) {
                e.printStackTrace();

                throw new Exception("Error opening: " + f.toString());

                // TODO: display error message here
            }
        }
    }
}
