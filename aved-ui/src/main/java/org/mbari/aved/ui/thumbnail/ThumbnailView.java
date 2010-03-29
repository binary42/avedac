/*
 * @(#)ThumbnailView.java
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

import com.jeta.forms.gui.form.FormAccessor;

import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.utils.ImageUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class ThumbnailView extends JFrameView {
    public static final String ID_PANEL  = "panel";     // javax.swing.JPanel
    public static final String ID_SLIDER = "slider";    // javax.swing.JSlider

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // Frequently accessed components
    private JSlider        mySlider       = null;
    private ThumbnailPanel thumbnailPanel = null;

    public ThumbnailView(ApplicationModel model, ThumbnailController controller) {

        // Constructor
        super("org/mbari/aved/ui/forms/Thumbnail.xml", model, controller);
        super.setFocusable(true);

        // Create a new custom panel and get a reference to the slider
        thumbnailPanel = new ThumbnailPanel(this);
        mySlider       = (JSlider) getForm().getComponentByName(ID_SLIDER);
        mySlider.setFocusable(true);

        // Replace the panel in the form with the custom panel
        FormAccessor a     = getForm().getFormAccessor();
        JPanel       panel = getForm().getPanel(ID_PANEL);

        a.replaceBean(panel, thumbnailPanel);
    }

    /**
     * @return slider reference
     */
    JSlider getSlider() {
        return mySlider;
    }

    /**
     * @return the panel reference
     */
    ThumbnailPanel getPanel() {
        return thumbnailPanel;
    }

    /**
     * Helper function to return typecast model
     * @return ApplicationModel used in this ThumbnailView
     */
    public ApplicationModel getModel() {
        return ((ApplicationModel) super.getModel());
    }

    /*
     *  Adjusts slider for a new list.
     * Call this after the model list has changed
     */
    public void setSlider() {
        int count = getModel().getEventListModel().getMaximum();

        if (count < 0) {
            mySlider.setPaintLabels(false);
            mySlider.setMaximum(10);
            mySlider.setEnabled(false);

            return;
        }

        // setting mySlider value automatically generates update
        // Do first so other adjustments (setMax) don't change.
        mySlider.setValue(0);
        mySlider.setMaximum(count);

        // Set tick spacing
        int minorTick = 1;
        int majorTick = 3;

        if ((count > getPicsPerPage()) && (count <= 100)) {
            minorTick = 1;
            majorTick = 5;
        } else if ((count > 100) && (count <= 300)) {
            minorTick = 5;
            majorTick = 10;
        } else if ((count > 300) && (count <= 1000)) {
            minorTick = 10;
            majorTick = 50;
        } else if (count > 1000) {
            minorTick = count / 100;    // int for truncation
            majorTick = count / 20;     // for truncation
        }

        // System.out.println("Count: " + count + "  minor: " + minorTick + " Major: " + majorTick);
        mySlider.setMinorTickSpacing(minorTick);
        mySlider.setMajorTickSpacing(majorTick);

        if ((minorTick == 1) || (majorTick == 1)) {
            mySlider.setSnapToTicks(true);
        } else {
            mySlider.setSnapToTicks(false);
        }

        // Crude hack to put a slider label from 1 - max instead of zero based
        Hashtable<Integer, JComponent> table = new Hashtable<Integer, JComponent>();

        for (int i = 0; i < (count - majorTick); i += majorTick) {
            String a = new String(Integer.toString(i + 1));

            table.put(new Integer(i), new JLabel(a));
        }

        table.put(new Integer(count), new JLabel(new String(Integer.toString(count + 1))));
        mySlider.setLabelTable(table);
        mySlider.setPaintLabels(true);

        // Set painting only after setting what the labels are!
        // AND after creating them!!!!
        // Especially when there were previously no labels.
        mySlider.setPaintTicks(true);
        mySlider.setPaintLabels(true);
        mySlider.setEnabled(true);
    }

    /**
     * @return the number of pictures per page displayed
     */
    public int getPicsPerPage() {
        return thumbnailPanel.getPicturesPerPage();
    }

    /**
     * TODO: add comment
     */
    public void modelChanged(ModelEvent event) {
        if (event instanceof EventListModel.EventListModelEvent) {
            EventListModel.EventListModelEvent e = (EventListModel.EventListModelEvent) event;

            switch (e.getID()) {
            case EventListModel.EventListModelEvent.LIST_CLEARED :
                thumbnailPanel.setWaitCursor(true);

                // Reset the pictures in the panel
                thumbnailPanel.reset();

                // Reset display
                showPageFromScroller(0);
                thumbnailPanel.invalidatePicPointer();
                thumbnailPanel.setWaitCursor(false);

                break;

            case EventListModel.EventListModelEvent.ONE_ENTRY_REMOVED :
                long id = e.getObjectId();

                thumbnailPanel.removePicture(id);

                // Reset display
                showPageFromScroller(0);

                break;

            case EventListModelEvent.MULTIPLE_ENTRIES_CHANGED :
                thumbnailPanel.setWaitCursor(true);

                ArrayList<Long> a    = e.getObjectIds();
                Iterator<Long>  iter = a.iterator();

                while (iter.hasNext()) {

                    // Reset display to show from the first index removed
                    thumbnailPanel.removePicture(iter.next());
                }

                showPageFromScroller(0);
                thumbnailPanel.setWaitCursor(false);

                break;

            case EventListModel.EventListModelEvent.NUM_LOADED_IMAGES_CHANGED :
                break;

            case EventListModel.EventListModelEvent.LIST_RELOADED :

                // TODO: test this and delete cursor in panel
                this.setCursor(ImageUtils.busyCursor);

                // thumbnailPanel.setWaitCursor(true);
                // Reset the pictures in the panel
                thumbnailPanel.reset();
                thumbnailPanel.invalidatePicPointer();

                // Create new pictures based on the current model
                thumbnailPanel.createThumbnailPictures(getModel());
                setSlider();

                break;
            }
        }
    }

    /*
     *  Updates the display to scroll down and show images from
     * the scroller forward
     * @param index of the scroller
     */
    public void showPageFromScroller(int index) {
        try {
            thumbnailPanel.showPageFromScroller(index, getModel());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Adds mouse listener to the thumbnail panel
     * This will add a mouse listener for each thumbnail picture
     */
    public void addMouseClickListener(MouseListener listener) {
        thumbnailPanel.addMouseClickListener(listener);
    }

    /*
     * Request focus on component
     * @see java.awt.Component#requestFocus(boolean)
     */
    public boolean requestFocus(boolean state) {
        if (state == true) {
            thumbnailPanel.requestFocus(state);
            showPageFromScroller(getSlider().getValue());

            return true;
        } else {
            return thumbnailPanel.requestFocus(state);
        }
    }
}
