/*
 * @(#)PlayerManager.java
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

import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.model.EventObjectContainer;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton class that manages creating EventPlayer. This class
 * only allows one EventEditorView to be created for each event in the
 * EventListModel. This class limits the number of Players to reduce
 * memory requirements.
 *
 * @author dcline
 *
 */
public class PlayerManager implements ModelListener {
    private static final PlayerManager INSTANCE = new PlayerManager();

    /** Maximum number of view that can be open */
    private final static int MAX_VIEWS = 3;

    /** The list of event editors maintained by this manager. */
    private LinkedList<Player> players = new LinkedList<Player>();

    /** The upper left point the last player was opened */
    private Point lastViewerUpperLeftPt = new Point(0, 0);

    private PlayerManager() {}

    /** Returns the singleton instance of this class */
    public static PlayerManager getInstance() {
        return INSTANCE;
    }

    /**
     * Simple logic to cascade players across the display
     */
    private void cascadeView(Player e) {
        Toolkit   kit        = e.getView().getToolkit();
        Dimension screenSize = kit.getScreenSize();

        if (lastViewerUpperLeftPt.x + e.getView().getWidth() > screenSize.width) {
            lastViewerUpperLeftPt.x = 0;
            lastViewerUpperLeftPt.y += (e.getView().getHeight()) / 4;
        }

        if (lastViewerUpperLeftPt.y + e.getView().getHeight() > screenSize.height) {
            lastViewerUpperLeftPt.y = 0;
        }

        e.getView().setLocation(lastViewerUpperLeftPt.x, lastViewerUpperLeftPt.y);
        e.getView().setVisible(true);
        e.getView().requestFocus();
        lastViewerUpperLeftPt.x += (e.getView().getWidth()) / 4;
    }

    /**
     * Opens a view for the event
     *
     * @param event The event to open in the view
     * @param model The data model associated with the event
     * @param location upper left location to place the view
     * to the last opened event view
     */
    void openView(EventObjectContainer event, ApplicationModel model, Point location) {
        try {
            if ((event != null) && (model != null)) {
                Player e = null;

                // Search for an player that contains this event
                Iterator iter = players.iterator();

                while (iter.hasNext()) {
                    e = (Player) iter.next();

                    if (e.isEditing(event)) {
                        e.getView().setVisible(false);
                        e.getView().setLocation(location.x, location.y);
                        e.getView().setVisible(true);
                        e.getView().requestFocus();

                        return;
                    }
                }

                // Remove the first (oldest) view in this list
                // before adding new players to the list
                if (players.size() >= MAX_VIEWS) {
                    remove(players.getFirst().getView());
                }

                e = new Player(event, model);
                players.add(e);
                e.getView().setLocation(location.x, location.y);
                e.getView().setVisible(true);
                e.getView().requestFocus();
            }
        } catch (Exception ex) {
            Logger.getLogger(PlayerManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Opens a view for the event
     *
     * @param event The event to open in the view
     * @param model The data model associated with the event
     * @param cascade True if the event should cascade next
     * to the last opened event view
     */
    public void openView(EventObjectContainer event, ApplicationModel model) {
        try {
            if ((event != null) && (model != null)) {
                Player e = null;

                // Search for an player that contains this event
                Iterator iter = players.iterator();

                while (iter.hasNext()) {
                    e = (Player) iter.next();

                    if (e.isEditing(event)) {
                        e.getView().setVisible(true);
                        e.getView().requestFocus();

                        return;
                    }
                }

                // Remove the first (oldest) view in this list
                // before adding new players to the list
                if (players.size() >= MAX_VIEWS) {
                    remove(players.getFirst().getView());
                }

                e = new Player(event, model);
                players.add(e);
                cascadeView(e); 
            }
        } catch (Exception ex) {
            Logger.getLogger(PlayerManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Clears the private EventEditorView list and closes all open Players
     */
    public void clear() {
        Iterator<Player> iter = players.iterator();

        while (iter.hasNext()) {
            Player t = (Player) iter.next();

            t.getView().dispose();
        }

        players.clear();

        // Run the garbage collector
        Runtime.getRuntime().gc();
    }

    /**
     * Removes the view from the manager list
     * @param view
     */
    void remove(PlayerView view) {

        // Search for an editor that contains this event
        Iterator iter = players.iterator();

        while (iter.hasNext()) {
            Player p = (Player) iter.next();

            if ((view != null) && view.equals(p.getView())) {
                p.getView().dispose();
                players.remove(p);
                p = null;

                // Run the garbage collector
                Runtime.getRuntime().gc();

                return;
            }
        }
    }

    public void modelChanged(ModelEvent event) {
        if (event instanceof EventListModelEvent) {
            EventListModelEvent e = (EventListModelEvent) event;

            switch (event.getID()) {
            case EventListModel.EventListModelEvent.ONE_ENTRY_REMOVED :
                Long objectIds = e.getObjectId();

                // Search for an editor that contains this event
                Iterator v = players.iterator();

                while (v.hasNext()) {
                    Player p = (Player) v.next();

                    if (p.isEditing(objectIds)) {
                        players.remove(p);
                        p.getView().dispose();

                        break;
                    }
                }

                break;

            case EventListModel.EventListModelEvent.LIST_CLEARED :
            case EventListModel.EventListModelEvent.LIST_RELOADED :
                clear();

                break;

            default :
                break;
            }
        }
    }
}
