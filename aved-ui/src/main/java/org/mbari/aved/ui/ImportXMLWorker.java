/*
 * @(#)ImportXMLWorker.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.EventDataStream;
import aved.model.EventObject;
import aved.model.FrameEventSet;
import aved.model.SourceMetadata;
import aved.model.xml.Mapper;

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.progress.ProgressDisplay;
import org.mbari.aved.ui.utils.URLUtils;

//~--- JDK imports ------------------------------------------------------------


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
public class ImportXMLWorker extends SwingWorker {


    /** Maximum frame any event found in. Used for bounding transcoding range */
    private Integer maxEventFrame = 0;

    /** Flag to indicate whether to display progress or not */
    private boolean displayProgress = true;

    /**
     * Helper member to send message to editor controller
     *
     */
    private AbstractController controller;

    /**
     * Handle to data stream mapped to the current editor. This is reset
     * every time a new XML schema is loaded
     */
    private EventDataStream  eventDataStream;
    private ApplicationModel model;

    /* Simple display to show import progress */
    ProgressDisplay progressDisplay;

    /** XML file to import and controller */
    File xmlFile;

    /**
     * Import the results in the XML file and put in hash map
     *
     * @param xmlFile
     *            XML file to import/edit
     */
    public ImportXMLWorker(File xmlFile, ApplicationModel model, AbstractController controller,
                           boolean displayProgress) {
        try {
            this.model           = model;
            this.controller      = controller;
            this.xmlFile         = xmlFile;
            this.displayProgress = displayProgress;

            if (displayProgress) {
                progressDisplay = new ProgressDisplay((SwingWorker) this, "Importing " + xmlFile.getName() + "...");
            }
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

    /**
     * Maximum frame any event found in. Used for bounding transcoding range
     * @return
     */
    public int getMaxEventFrame() {
        return this.maxEventFrame;
    }

    /*
     * Application task. Executed in background thread. This execute the importXML
     * function, which can be slow for large XML files
     */
    @Override
    protected Object doInBackground() throws Exception {
        if (displayProgress) {
            progressDisplay.display("Importing " + xmlFile.getName() + " now...");
        }

        // Initialize progress property.
        setProgress(0);
        Application.getView().setBusyCursor();

        // Create event map with 201 objects and 75% loading factor
        // This should be enough to store events collected over a few
        // minutes and will grow when needed
        HashMap<Long, EventObjectContainer> map    = new HashMap<Long, EventObjectContainer>(201, 0.75f);
        Object                              object = null;

        if (displayProgress) {
            progressDisplay.display("Parsing XML file now");
        }

        // Import the XML file using Brian S. AVED XML parser for AVED DB
        // files
        try {
            if (displayProgress) {
                progressDisplay.display("Unmarshalling the XML file...");
            }

            URL         url         = xmlFile.toURL();
            InputStream inputStream = url.openStream();

            object = Mapper.unmarshall(inputStream);
            inputStream.close();

            if (displayProgress) {
                progressDisplay.display("Unmarshalling done");
            }
        } catch (Exception e) {
            Application.getView().setDefaultCursor();
            Logger.getLogger(ImportXMLWorker.class.getName()).log(Level.SEVERE, null, e);

            String                message = "Error - cannot parse xml file: " + xmlFile.getName() + "\nmessage:"
                                            + e.getMessage();
            NonModalMessageDialog dialog  = new NonModalMessageDialog((JFrame) controller.getView(), message);

            dialog.setVisible(true);

            return null;
        }

        // Get handle to data stream
        eventDataStream = (EventDataStream) object;

        // Update the XML file. This must be done before setting the source
        model.getSummaryModel().setXmlFile(xmlFile);

        // Update the data stream in the model
        model.getSummaryModel().setEventDataStream(eventDataStream);

        // Update the source metadata if there is one
        SourceMetadata source = null;

        source = eventDataStream.getSourceMetadata();

        if (displayProgress) {
            progressDisplay.display("Checking for a video source identifier");
        }

        // If a video source defined check if it contains
        // a file or http protocol string before setting it
        if (source != null) {
            String id = source.getSourceIdentifier();

            // If this is a true url reference and not a local file
            // just set it
            if (URLUtils.isURL(id)) {
                model.getSummaryModel().setInputSourceURL(new URL(id));
            } else if (URLUtils.isFile(id)) {

                // otherwise check if a file and convert it to a file URL reference
                // Convert to to a file reference
                File video = new File(id);

                // If there is no root path in the source identifier
                // assume it is in the same path as the XML,
                // and set its root to the same path as the XML
                if (video.getParent() == null) {
                   String v = "file:" + xmlFile.getParent() + "/" + video.getName();

                    model.getSummaryModel().setInputSourceURL(new URL(v));
                } else {
                    model.getSummaryModel().setInputSourceURL(new URL("file:" + video.toString()));
                }
            }
        } else {
            // if no video source identify, set it to a default AVI file
            // this is completely arbitrary
            String defaultSource = "file:" + xmlFile.getParent() + "/" + xmlFile.getName() + ".avi";
            model.getSummaryModel().setInputSourceURL(new URL(defaultSource));
        }

        long                 key   = 0;
        EventObjectContainer value = null;

        if (displayProgress) {
            progressDisplay.display("Extracting event objects...");
        }

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
                    value = new EventObjectContainer(event, model);
                    map.put(key, value);
                } else {    // Otherwise, add this EventObject to the

                    // EventObjectContainer
                    value = (EventObjectContainer) map.get(key);
                    value.add(event);
                }
            }

            if (!eventObjs.isEmpty()) {
                maxEventFrame = f.getFrameNumber();
            }
        }

        // Sort the map by key using the event ID
        LinkedList<Long> keys = new LinkedList<Long>(map.keySet());    // Get the keys from

        // the map as a list
        if (displayProgress) {
            progressDisplay.display("Sorting events by increasing ID order");
        }

        Collections.sort(keys);

        // Create a collection of AVEDEvents based on the sorted event keys
        if (displayProgress) {
            progressDisplay.display("Creating a collection based on event keys");
        }

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

        // Set the progress bar to 100% and reset cursor
        setProgress(100);

        // Add the sorted collection to the list model
        model.add(entries);
        Application.getView().setDefaultCursor();

        return null;
    }
}
