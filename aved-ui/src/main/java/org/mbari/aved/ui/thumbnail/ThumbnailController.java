/*
 * @(#)ThumbnailController.java
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

import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.EventPopupMenu;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.player.PlayerManager;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * The <code>ThumbnailController</code> class is responsible for
 * managing control of <code>ThumbnailView</code>, and
 * <code>ThumbnailModel</code>.
 * <p>
 *
 *  @see ThumbnailView
 *  @see ApplicationModel
 */
public class ThumbnailController extends AbstractController
        implements ModelListener, KeyListener, ChangeListener, ListSelectionListener {

    /** True when a popup window is displayed */
    private Boolean                          hasPopup = false;
    private MouseClickThumbnailActionHandler mouseClickListener;

    /**
     * Links the view and model to this <code>ThumbnailController</code>
     * <p>
     * Implements listener interfaces for receiving keyboard events (keystrokes)
     * and mouse slider changes, and changes in the {@link ApplicationModel}
     *
     * @param model ApplicationModel linked to both this <code>ThumbnailController</code>
     * and <code>ThumbnailView</code>
     *
     * @see ThumbnailView
     * @see ApplicationModel
     */
    public ThumbnailController(ApplicationModel model) {
        setModel(model);
        setView(new ThumbnailView(model, this));
        mouseClickListener = new MouseClickThumbnailActionHandler();

        // Listen to changes in the model
        model.addModelListener(this);

        // Listen to changes in the list selection model
        model.getListSelectionModel().addListSelectionListener(this);

        // Add the key listener
        getView().getSlider().addKeyListener(this);
        getView().getPanel().addKeyListener(this);

        // Add the change listener
        getView().getSlider().addChangeListener(this);
    }

    /**
     * @return type cast ThumbnailView created by this <code>ThumbnailController</code>
     */
    public ThumbnailView getView() {
        return ((ThumbnailView) super.getView());
    }

    /**
     * Helper function to typecast model from
     * {@link org.mbari.aved.ui.appframework.AbstractModel} to
     * {@link org.mbari.aved.ui.ApplicationModel ApplicationModel}
     *
     * @return type cast {@link org.mbari.aved.ui.ApplicationModel} used in this
     * <code>ThumbnailController</code>
     */
    public ApplicationModel getModel() {
        return ((ApplicationModel) super.getModel());
    }

    /**
     * Operation handler for handling actions needed by the View
     * currently not used - is just put here as a placeholder in
     * case it is needed in the future.
     *
     * @param actionCommand A semantic event which indicates that a
     * component-defined action occurred.
     */
    public void actionPerformed(ActionEvent e) {}

    /**
     * Manages mouse clicks on a {@link ThumbnailPicture}
     * <p>
     * On a single mouse-click shows a pop-up menu of an {@link org.mbari.aved.ui.EventPopupMenu}
     * <p>
     * On a double mouse-click opens up a {@link org.mbari.aved.ui.player.Player}
     */
    void actionClickThumbnailPicture(MouseEvent e) {
        ThumbnailPicture thumbnail = (ThumbnailPicture) e.getSource();

        if (e.getID() == MouseEvent.MOUSE_CLICKED) {

            // On double click, but not while a popup is showing
            if (!hasPopup) {
                if (e.getClickCount() == 2) {

                    // Get the object container for this event id and open player
                    EventObjectContainer c = thumbnail.getEventObjectContainer();

                    PlayerManager.getInstance().openView(c, getModel(), true);
                } else {

                    // On a single click toggle selection highlight
                    thumbnail.flipSelection();
                }
            }

            hasPopup = false;
        } else if (((e.getID() == MouseEvent.MOUSE_PRESSED) || (e.getID() == MouseEvent.MOUSE_RELEASED))
                   && e.isPopupTrigger()) {

            // Only show popup if this is really a popup trigger
            Point          pt    = e.getPoint();
            EventPopupMenu popup = new EventPopupMenu(getModel());

            popup.show((Component) e.getSource(), pt.x, pt.y);
            hasPopup = true;
        }
    }

    /**
     * Model listener. Reacts to changes in the {@link org.mbari.aved.ui.ApplicationModel}
     */
    public void modelChanged(ModelEvent event) {
        EventListModel events = getModel().getEventListModel();

        if (event instanceof EventListModel.EventListModelEvent) {
            EventListModel.EventListModelEvent e = (EventListModel.EventListModelEvent) event;

            switch (e.getID()) {
            case EventListModel.EventListModelEvent.NUM_LOADED_IMAGES_CHANGED :                
                 getView().getSlider().setValue(e.getFlag());
                 if (!events.getValueIsAdjusting()) {
                   getView().showPageFromScroller(e.getFlag());
                 }                 
                break;

            case EventListModel.EventListModelEvent.LIST_CLEARED :

                // Initialize the mouse action handler for both the form and the
                // thumbnail panel
                getView().removeMouseListener(mouseClickListener);

                break;

            case EventListModel.EventListModelEvent.LIST_RELOADED :

                // Initialize the mouse action handler for both the form and the
                // thumbnail panel
                getView().addMouseClickListener(mouseClickListener);

                break;

            case EventListModelEvent.CURRENT_PAGE_CHANGED :
            case EventListModelEvent.MULTIPLE_ENTRIES_CHANGED :
            case EventListModelEvent.ONE_ENTRY_REMOVED :
                if (!events.getValueIsAdjusting()) {
                    int value = events.getValue();

                    getView().showPageFromScroller(value);
                }

                break;

            default :
                break;
            }
        }
    }

    /**
     * Invoked when key is typed in the slider.
     * Currently does nothing.
     *
     * @param event KeyEvent
     */
    public void keyTyped(KeyEvent event) {}

    /**
     *
     * Invoked when key is typed in the slider
     * Currently coes nothing.
     *
     * @param event KeyEvent
     */
    public void keyReleased(KeyEvent event) {}

    /**
     * Invoked when key is pressed in the slider.
     * Controls responses to e.g. page-up and page-down
     * key mappings
     *
     * @param event KeyEvent
     */
    public void keyPressed(KeyEvent event) {
        EventListModel model = getModel().getEventListModel();

        switch (event.getKeyCode()) {
        case KeyEvent.VK_PAGE_DOWN : {
            int currentlyAt = model.getValue();
            int jumpTo      = currentlyAt + getView().getPicsPerPage();

            if (jumpTo > model.getMaximum()) {
                jumpTo = model.getMaximum();
            }

            getView().getSlider().setValue(jumpTo);
            event.consume();

            break;
        }

        case KeyEvent.VK_PAGE_UP :
            int currentlyAt = model.getValue();
            int jumpTo      = currentlyAt - getView().getPicsPerPage();

            if (jumpTo < 0) {
                jumpTo = 0;
            }

            getView().getSlider().setValue(jumpTo);
            event.consume();

            break;
        }
    }

    /**
     *  Invoked when the slider has changed its state.
     *  <p>This sets the current value of the <code>BoundedRangeModel</code>
     *  in the {@link org.mbari.aved.ui.model.EventListModel}.
     *  This is used primarily to synchronize the scrollbar and scrollers in the ThumnbailView and
     *  TableViews
     *
     * @param e ChangeEvent
     */
    public void stateChanged(ChangeEvent e) {
        Object source = e.getSource();

        if (source instanceof JSlider) {
            JSlider theJSlider = (JSlider) source;

            if (!theJSlider.getValueIsAdjusting()) {
                EventListModel model = getModel().getEventListModel();

                model.setValue(theJSlider.getValue());
            }
        } else {
            System.out.println("#############Something unknown changed in ThumbnailController: " + source);
        }
    }

    /**
     * Invoked then the <code>ListSelectionModel</code> changes in the {@link org.mbari.aved.ui.ApplicationModel}.
     * <p>
     * @param e the selection event
     */
    public void valueChanged(ListSelectionEvent e) {
        boolean isAdjusting = e.getValueIsAdjusting();

        // When the list stops adjusting, trigger an update of the view
        if (!isAdjusting) {
            EventListModel model       = getModel().getEventListModel();
            int            currentlyAt = model.getValue();

            getView().showPageFromScroller(currentlyAt);
        }
    }

    /**
     * Subclass to handles mouse clicks in {@link ThumbnailPicture}
     */
    class MouseClickThumbnailActionHandler implements MouseListener {
        public void mouseClicked(MouseEvent e) {
            actionClickThumbnailPicture(e);
        }

        public void mouseEntered(MouseEvent e) {
            actionClickThumbnailPicture(e);
        }

        public void mouseExited(MouseEvent e) {
            actionClickThumbnailPicture(e);
        }

        public void mousePressed(MouseEvent e) {
            actionClickThumbnailPicture(e);
        }

        public void mouseReleased(MouseEvent e) {
            actionClickThumbnailPicture(e);
        }
    }
}
