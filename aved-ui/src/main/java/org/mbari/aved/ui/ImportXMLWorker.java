/*
 * @(#)ImportXMLWorker.java   10/03/17
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.EventDataStream;
import aved.model.EventObject;
import aved.model.FrameEventSet;
import aved.model.SourceMetadata;
import aved.model.xml.Mapper;

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.utils.URLUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Cursor;

import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

/**
 *
 * @author dcline
 *
 * Helper class to import results and update the ApplicationModel. This can be
 * slow for even small XML files, so put this in worker thread to not bog
 * down the UI
 */
class ImportXMLWorker extends SwingWorker {

    /** Frequently accessed busy and wait cursors */
    public final static Cursor busyCursor    = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    public final static Cursor defaultCursor = Cursor.getDefaultCursor();

    /**
     * Helper member to send message to editor controller
     *
     */
    private ApplicationController controller;

    /**
     * Handle to data stream mapped to the current editor. This is reset
     * every time a new XML schema is loaded
     */
    private EventDataStream eventDataStream;

    /* Simple display to show import progress */
    ProgressDisplay progressDisplay;

    /** XML file to import and controller */
    File xmlFile;

    /**
     * Import the results in the XML file and put in hash map
     *
     * @param file
     *            XML file to import/edit
     */
    public ImportXMLWorker(File file, ApplicationController controller) {
        try {

            // TODO: need null checking here
            this.controller = controller;
            xmlFile         = file;
            progressDisplay = new ProgressDisplay((SwingWorker) this, "Importing " + xmlFile.getName() + "...");
        } catch (Exception e) {

            // TODO Add error message box here
            e.printStackTrace();
            Application.getView().setDefaultCursor();
        }
    }

    /**
     * Executed after the {@code doInBackground} method is finished.
     *
     * @see #doInBackground
     * @see #isCancelled()
     * @see #get
     */
    protected void done() {
        progressDisplay = null;
    }

    /*
     * Application task. Executed in background thread. This execute the importXML
     * function, which can be slow for large XML files
     */
    @Override
    protected Object doInBackground() throws Exception {
        progressDisplay.display("Importing " + xmlFile.getName() + " now...");

        // Initialize progress property.
        setProgress(0);
        Application.getView().setCursor(busyCursor);

        // Create event map with 201 objects and 75% loading factor
        // This should be enough to store events collected over a few
        // minutes and will grow when needed
        HashMap<Long, EventObjectContainer> map    = new HashMap<Long, EventObjectContainer>(201, 0.75f);
        Object                              object = null;

        progressDisplay.display("Parsing XML file now");

        // Import the XML file using Brian S. AVED XML parser for AVED DB
        // files
        try {
            progressDisplay.display("Unmarshalling the XML file...");

            URL         url         = xmlFile.toURL();
            InputStream inputStream = url.openStream();

            object = Mapper.unmarshall(inputStream);
            inputStream.close();
            progressDisplay.display("Unmarshalling done");
        } catch (Exception e) {
            Application.getView().setCursor(defaultCursor);
            Logger.getLogger(ImportXMLWorker.class.getName()).log(Level.SEVERE, null, e);

            String message = new String("Error - cannot parse xml file: " + xmlFile.getName() + "\nmessage:"
                                        + e.getMessage());
            NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) controller.getView(), message);

            dialog.setVisible(true);

            return null;
        }

        // Get handle to data stream
        eventDataStream = (EventDataStream) object;

        SummaryModel model = controller.getModel().getSummaryModel();

        // Update the XML file. This must be done before setting the source
        model.setXmlFile(xmlFile);

        // Update the data stream in the model
        model.setEventDataStream(eventDataStream);

        // Update the source metadata if there is one
        SourceMetadata source = null;

        source = eventDataStream.getSourceMetadata();
        progressDisplay.display("Checking for a video source identifier");

        // If a video source defined check if it contains
        // a file or http protocol string before setting it
        if (source != null) {
            String id = source.getSourceIdentifier();

            // If this is a true url reference and not a local file
            // just set it
            if (URLUtils.isURL(id)) {
                model.setInputSourceURL(new URL(id));
            }    // otherwise check if a file and convert it to a file URL reference
                    else if (URLUtils.isFile(id)) {

                // Convert to to a file reference
                File video = new File(id);

                // If there is no root path in the source identifier
                // assume it is in the same path as the XML,
                // and set its root to the same path as the XML
                if (video.getParent() == null) {
                    String v = new String("file:" + xmlFile.getParent() + "/" + video.getName());

                    model.setInputSourceURL(new URL(v));
                } else {
                    model.setInputSourceURL(new URL(new String("file:" + video.toString())));
                }
            }
        } else {
            model.setInputSourceURL(null);
        }

        long                 key   = 0;
        EventObjectContainer value = null;

        progressDisplay.display("Extracting event objects...");

        // Walk through all FrameEventSets and extract event objects
        SortedSet<FrameEventSet> frames = eventDataStream.getFrameEventSets();
        int                      max    = ((frames.size() > 0)
                                           ? frames.size()
                                           : 1);    // avoid divide

        // by zero
        // exception
        int count = 0;

        for (FrameEventSet f : frames) {

            // Set the progress bar to something between 0 - 99 %
            setProgress((99 * count++) / max);

            // Go through all events in the frame
            Set<EventObject> eventObjs = f.getEventObjects();

            for (EventObject event : eventObjs) {

                // Key is simply the objectid which is of type long
                key = event.getObjectId();
                event.setFrameEventSet(f);
                event.setId(key);

                // If key is not stored in this map, add a new object of
                // type Event to the map
                if (!map.containsKey(key)) {
                    value = new EventObjectContainer(event, controller.getModel());
                    map.put(key, value);
                } else {    // Otherwise, add this EventObject to the

                    // EventObjectContainer
                    value = (EventObjectContainer) map.get(key);
                    value.add(event);
                }
            }
        }

        // Sort the map by key using the event ID
        LinkedList<Long> keys = new LinkedList<Long>(map.keySet());    // Get the keys from

        // the map as a list
        progressDisplay.display("Sorting events by increasing ID order");
        Collections.sort(keys);

        // Create a collection of AVEDEvents based on the sorted event keys
        progressDisplay.display("Creating a collection based on event keys");

        // Sleep for a second so the user can see this last message since
        // the sorting is very fast
        Thread.sleep(1000);

        LinkedList<EventObjectContainer> entries = new LinkedList<EventObjectContainer>();
        Iterator<Long>                   i       = keys.iterator();

        max   = keys.size();
        count = 0;

        while (i.hasNext()) {
            EventObjectContainer e = (EventObjectContainer) map.get(i.next());

            // Sort the events in this container now that we are done adding to it
            e.sort();

            // Add the container to the list of containers
            entries.add(e);
        }

        ApplicationModel mainModel = controller.getModel();

        // Set the progress bar to 100% and reset cursor
        setProgress(100);

        // Add the sorted collection to the list model
        mainModel.add(entries);
        Application.getView().setCursor(defaultCursor);

        return null;
    }
}
