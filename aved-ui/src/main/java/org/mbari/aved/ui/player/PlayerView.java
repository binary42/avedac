/*
 * @(#)PlayerView.java
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



package org.mbari.aved.ui.player;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.BoundingBox;
import aved.model.EventObject;

import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.MainMenu;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.thumbnail.ImageChangeUtil;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import java.io.File;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

/**
 * This class is based on the DisplayThumbnailApp.java by Rafael Santos.
 *
 * SWING components have been factored out into an Abeille form, and additional
 * AVED project Event display components added.
 *
 * This creates an instance of DisplayThumbnail and an instance of DisplayJAI
 * that are controlled by the EventEditorController.
 */
public class PlayerView extends JFrameView {
    private static final String ID_CLASS_LABEL              = "class";
    private static final String ID_CLOSE_BUTTON             = "close";
    private static final String ID_DELETE_BUTTON            = "delete";
    private static final String ID_EVENT_BEST_FRAME_LABEL   = "eventbestframe";
    private static final String ID_EVENT_DISPLAY_PANEL      = "eventdisplay";
    private static final String ID_EVENT_DURATION_LABEL     = "eventduration";
    private static final String ID_EVENT_ENDFRAME_LABEL     = "eventendframe";
    private static final String ID_EVENT_ENDTIMECODE_LABEL  = "eventendtimecode";
    private static final String ID_EVENT_ID_LABEL           = "eventid";
    private static final String ID_EVENT_MAX_SIZE_LABEL     = "eventmaxsize";
    private static final String ID_EVENT_STARTFRAME_LABEL   = "eventstartframe";
    private static final String ID_EVENT_STARTTIMECODELABEL = "eventstarttimecode";
    private static final String ID_FRAME_LABEL              = "framelabel";
    private static final String ID_FWD_END_BUTTON           = "forwardtoend";

    /*
     * Component names in the EventEditor form If any of the component name
     * are changed in the Abeille form designer, they should be modified here
     * too
     */
    private static final String ID_HEADER_LABEL               = "header";
    private static final String ID_IDENTIFIER_LABEL           = "id";
    private static final String ID_NEXT_BUTTON                = "next";
    private static final String ID_PLAYSTOP_BUTTON            = "playstop";
    private static final String ID_PLAY_FWD_STEP_BUTTON       = "playforwardstep";
    private static final String ID_PLAY_RWD_STEP_BUTTON       = "playreversestep";
    private static final String ID_PREDICTED_CLASS_LABEL      = "predictedclass";
    private static final String ID_PREDICTED_CLASS_PROB_LABEL = "predictedclassprob";
    private static final String ID_PREV_BUTTON                = "prev";
    private static final String ID_RWD_END_BUTTON             = "rewindtoend";
    private static final String ID_TAG_LABEL                  = "tag";
    private static final String ID_TIMECODE_LABEL             = "timecodelabel";
    private static JMenuBar     menuBar;

    /** Default image to display upon mssing frame exceptions */
    private static PlanarImage missingImage;
    private int                imageHeight = 0;

    /** Current viewport width/height for jai */
    private int                  imageWidth = 0;
    private EventObjectContainer event;
    private JLabel               frameLabel;
    private JPanel               imagePanel;

    /**
     * The thumbnail to display in the player window
     * We use the DisplayThumbnail class which is a
     * subclass of DisplayJAI. This class allows
     * us to scale potentially large images down
     * into our view, or it can be used with no
     * scaling
     */
    private DisplayThumbnail jai;
    private JButton          nextButton;
    private JButton          playStopButton;
    private JButton          prevButton;
    private JLabel           timecodeLabel;

    public PlayerView(EventObjectContainer event, ApplicationModel model, PlayerController controller) {
        super("org/mbari/aved/ui/forms/EventPlayer.xml", model, controller);

        // Set the Menu bar to the same as the main application view
        if (menuBar == null) {
            MainMenu mainMenu = new MainMenu(model);

            menuBar = mainMenu.buildJJMenuBar();
        }

        this.setJMenuBar(menuBar);
        this.event = event;

        // Initialize frequently accessed components for getting/setting
        imagePanel     = getForm().getPanel(ID_EVENT_DISPLAY_PANEL);
        timecodeLabel  = getForm().getLabel(ID_TIMECODE_LABEL);
        frameLabel     = getForm().getLabel(ID_FRAME_LABEL);
        playStopButton = (JButton) getForm().getButton(ID_PLAYSTOP_BUTTON);
        nextButton     = (JButton) getForm().getButton(ID_NEXT_BUTTON);
        prevButton     = (JButton) getForm().getButton(ID_PREV_BUTTON);

        ActionHandler actionHandler = getActionHandler();

        imagePanel.addKeyListener(controller);
        nextButton.addKeyListener(controller);
        prevButton.addKeyListener(controller);
        playStopButton.addKeyListener(controller);
        getForm().addKeyListener(controller);
        this.addKeyListener(controller);
        nextButton.addActionListener(actionHandler);
        prevButton.addActionListener(actionHandler);
        playStopButton.addActionListener(actionHandler);
        getForm().getButton(ID_DELETE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_DELETE_BUTTON).addKeyListener(controller);
        getForm().getButton(ID_CLOSE_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_CLOSE_BUTTON).addKeyListener(controller);
        getForm().getButton(ID_PLAY_RWD_STEP_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_PLAY_RWD_STEP_BUTTON).addKeyListener(controller);
        getForm().getButton(ID_PLAY_FWD_STEP_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_PLAY_FWD_STEP_BUTTON).addKeyListener(controller);
        getForm().getButton(ID_FWD_END_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_FWD_END_BUTTON).addKeyListener(controller);
        getForm().getButton(ID_RWD_END_BUTTON).addActionListener(actionHandler);
        getForm().getButton(ID_RWD_END_BUTTON).addKeyListener(controller);

        // Don't let the user resize the window.
        setResizable(false);
        setTitle(ApplicationInfo.getName() + "-" + "Event Player");

        if (missingImage == null) {

            // Get a static copy of default image to display upon errors
            URL url = Application.class.getResource("/org/mbari/aved/ui/images/missingframeexception.jpg");

            if (url == null) {
                System.err.println("Cannot find missingframeexception.jpg");
                System.exit(1);
            } else {

                // Create an ImageIcon from the image data
                ImageIcon imageIcon = new ImageIcon(url);
                int       width     = imageIcon.getIconWidth();
                int       height    = imageIcon.getIconHeight();

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

        try {
            initialize();
            this.pack();
            this.setFocusable(true);

            // Set the focus on the next button. This is arbitrarily set
            getForm().getButton(ID_NEXT_BUTTON).requestFocusInWindow();
            this.setVisible(true);
        } catch (Exception ex) {
            Logger.getLogger(PlayerView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     * Display the timecode string. Generally this is called everyframe to
     * display the timecode and/or frame number coorresponding to a particular
     * file
     */
    public void displayTimecodeFrameString(String string, int framenum) {
        if (string != null) {
            timecodeLabel.setText(string);
            frameLabel.setText(Integer.toString(framenum));
        }
    }

    /**
     * Display the event image for this event object.
     * This will load the image frame this EventObject
     * is in and overlay the bounding box representing it
     * on the image.
     */
    public void displayEventImage(EventObjectContainer eventObj, BoundingBox b,  File frame) {
        PlanarImage image = null;

        if (eventObj != null) {
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

                // If the new image is larger than the existing and
                // a JAI is already initialied, remove the JAI before
                // creating a new one
                if ((imagePanel != null) && (jai != null)) {
                    imagePanel.remove(jai);
                    jai.removeKeyListener((KeyListener) this.getController());
                    jai = null;
                    this.validate();
                }

                // Scale JAI if the image is larger than 70% of the main display
                // TODO: this is somewhat arbitrary and really should be scaled by
                // the total size of the PlayerView, and not just the JAI component
                // of the PlayerView.
                Dimension d      = Application.getView().getSize();
                float     scale  = 1.0f;
                float     factor = 0.70f;

                if ((image.getWidth() >= factor * d.width) || (image.getHeight() >= factor * d.height)) {
                    scale = ImageChangeUtil.calcTheta(image.getWidth(), image.getHeight(), factor * d.width,
                                                      factor * d.height);
                }

                imageWidth  = image.getWidth();
                imageHeight = image.getHeight();
                jai         = new DisplayThumbnail(event, image, scale, width, height, upperleftorigin);
                imagePanel.add(jai);
                jai.addKeyListener((KeyListener) this.getController());
                this.validate();
            }

            image = null;
        }
    }

    /* Sets the icon on the play stop button */
    public void setPlayStopButtonIcon(ImageIcon icon) {
        if (icon != null) {
            playStopButton.setIcon(icon);
        }
    }

    /**
     * Updates the labels
     */
    public void updateLabels() {

        // Populate all the labels
        String header = " Object ID: " + event.getObjectId();

        getForm().getLabel(ID_HEADER_LABEL).setText(header);
        getForm().getLabel(ID_TAG_LABEL).setText(event.getTag());
        getForm().getLabel(ID_IDENTIFIER_LABEL).setText(event.getIdentityReference());
        getForm().getLabel(ID_CLASS_LABEL).setText(event.getClassName());
        getForm().getLabel(ID_PREDICTED_CLASS_LABEL).setText(event.getPredictedClassName());

        if (event.getPredictedClassProbability() != null) {
            getForm().getLabel(ID_PREDICTED_CLASS_PROB_LABEL).setText(event.getPredictedClassProbability().toString());
        }

        getForm().getLabel(ID_EVENT_ID_LABEL).setText(String.valueOf(event.getObjectId()));
        getForm().getLabel(ID_EVENT_MAX_SIZE_LABEL).setText(String.valueOf(event.getMaxSize()));
        getForm().getLabel(ID_EVENT_BEST_FRAME_LABEL).setText(String.valueOf(event.getBestEventFrame()));
        getForm().getLabel(ID_EVENT_DURATION_LABEL).setText(String.valueOf(event.getTtlFrames()));
        getForm().getLabel(ID_EVENT_STARTFRAME_LABEL).setText(String.valueOf(event.getStartFrame()));
        getForm().getLabel(ID_EVENT_ENDFRAME_LABEL).setText(String.valueOf(event.getEndFrame()));
        getForm().getLabel(ID_EVENT_STARTTIMECODELABEL).setText(event.getStartTimecode());
        getForm().getLabel(ID_EVENT_ENDTIMECODE_LABEL).setText(event.getEndTimecode());
    }

    /**
     * Initialize the player view. Populates
     * all the fields and images with the
     * event information.
     * @throws java.lang.Exception
     */
    public void initialize() throws Exception {
        updateLabels();

        // Display the timecode and frame number for the best representation
        // of this event
        displayTimecodeFrameString(event.getBestEventTimecode(), event.getBestEventFrame());

        // Display the best representation of the event
        int         frame    = event.getBestEventFrame();
        EventObject eventObj = event.getEventObject(frame);
        File        f        = event.getFrameSource(frame);

        displayEventImage(event, eventObj.getBoundingBox(), f);
    }

    /**
     * This method is here just to keep the MouseMotionListener interface happy.
     */
    public void mouseMoved(MouseEvent e) {}

    /**
     * Helper function to return the contained next button
     *
     * @return the next button instance
     */
    JButton getNextButton() {
        return this.nextButton;
    }

    public void modelChanged(ModelEvent event) {
        if (event instanceof EventListModelEvent) {
            switch (event.getID()) {
            case EventListModelEvent.ONE_ENTRY_REMOVED :

                // Uncomment this if you want a display, message window to
                // popup  in separate thread when an item is deleted

                /*
                 * if (myEvent.getObjectId() == ((EventListModelEvent) event).getEventID()) {
                 * MessageDisplayThread display = new MessageDisplayThread(
                 * this, "Event " + myEvent.getObjectId() + " has been deleted");
                 * display.start();
                 * }
                 */
                break;

            case EventListModelEvent.MULTIPLE_ENTRIES_CHANGED :
                updateLabels();
            default :
                break;
            }
        }
    }

    public void disableNextButton() {
        nextButton.setEnabled(false);
    }

    public void enableNextButton() {
        nextButton.setEnabled(true);
    }

    public void disablePrevButton() {
        prevButton.setEnabled(false);
    }

    public void enablePrevButton() {
        prevButton.setEnabled(true);
    }

    public class MessageDisplayThread extends Thread {
        private JFrame frame   = null;
        private String message = null;

        public MessageDisplayThread(JFrame frame, String message) {
            this.frame   = frame;
            this.message = message;
        }

        public void run() {
            if ((frame != null) && (message != null)) {
                try {
                    NonModalMessageDialog dialog = new NonModalMessageDialog(frame, message);

                    if (dialog.answer()) {
                        setVisible(false);

                        // close this, Need to send message
                        // to EditorController to tell it to remove the reference, or
                    }
                } catch (Exception ex) {
                    Logger.getLogger(PlayerView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
