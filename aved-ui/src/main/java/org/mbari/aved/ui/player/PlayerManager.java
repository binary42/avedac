/*
 * @(#)PlayerManager.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

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
     * @param cascade True if the event should cascade next
     * to the last opened event view
     */
    public void openView(EventObjectContainer event, ApplicationModel model, Boolean cascade) {
        try {
            if ((event != null) && (model != null)) {
                Player e = null;

                // Search for an editor that contains this event
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

                if (cascade) {
                    cascadeView(e);
                } else {
                    e.getView().setVisible(true);
                    e.getView().requestFocus();
                }
            }
        } catch (Exception e1) {

            // TODO Auto-generated catch block
            e1.printStackTrace();
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
