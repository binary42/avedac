/*
 * @(#)EventObjectContainer.java
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



package org.mbari.aved.ui.model;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.*;

import org.mbari.aved.mbarivision.api.AvedVideo;
import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.exceptions.FrameOutRangeException;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.Serializable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

public class EventObjectContainer implements Comparable, Serializable {
    static String                  DEFAULT_DESCRIPTION = new String("-");
    public static final Comparator LEXICAL_COMPARATOR  = new Comparator() {
        public int compare(Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
        }
    };
    private static final long                   serialVersionUID  = 1L;
    private int                                 bestEventFrame    = -1;
    private final HashMap<Integer, EventObject> eventHashMap      = new HashMap<Integer, EventObject>(51, 0.75f);
    private int                                 maxEventFrame     = -1;
    private long                                objectId          = 0;
    private LinkedList<Integer>                 sortedEventFrames = null;

    /**
     * Checks the loaded image for being all black/not.
     * This is used to deterine whether a bogus image from
     * e.g. a misfired strobe is found
     */
    private boolean             isBlackChecked = false;
    private EventImageCacheData eventImageCacheData;
    private ApplicationModel    mainModel;

    /**
     * Constructor
     * @param eventObject the initial event object contained in this
     */
    public EventObjectContainer(EventObject eventObject) {
        try {
            eventImageCacheData = new EventImageCacheData(this);

            if (eventObject != null) {
                objectId = eventObject.getObjectId();
                add(eventObject);
            }
        } catch (Exception ex) {
            Logger.getLogger(EventObjectContainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Constructor
     * @param eventObject the initial event object contained in this
     * @param mainModel the model
     */
    public EventObjectContainer(EventObject eventObject, ApplicationModel mainModel) {
        try {
            this.mainModel      = mainModel;
            eventImageCacheData = new EventImageCacheData(this);

            if (eventObject != null) {
                objectId       = eventObject.getObjectId();
                bestEventFrame = eventObject.getFrameEventSet().getFrameNumber();
                maxEventFrame  = bestEventFrame;
                add(eventObject);
            }
        } catch (Exception ex) {
            Logger.getLogger(EventObjectContainer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a cloned copy of the EventObject
     * @return clone of this EventObjectContainer
     */
    public EventObjectContainer clone() {
        try {

            // Get the first event and initialize the container with it
            Iterator<EventObject> it = this.eventHashMap.values().iterator();

            if (it.hasNext()) {
                EventObject          obj   = it.next().clone();
                EventObjectContainer clone = new EventObjectContainer(obj);

                // Set the clone id and source
                clone.objectId            = this.objectId;
                clone.mainModel           = this.mainModel;
                clone.eventImageCacheData = this.eventImageCacheData;

                // Iterate over the keys in the map
                while (it.hasNext()) {

                    // Get object and add to clone
                    EventObject value = it.next().clone();

                    clone.add(value);
                }

                // Sort the clone and return
                clone.sort();

                return clone;
            }
        } catch (Exception ex) {
            Logger.getLogger(EventObjectContainer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Cleans up the references to this object in its original frameSet event set
     * @param evtobj  The event object to remove
     */
    public void cleanup(EventObject evtobj) {
        synchronized (eventHashMap) {
            Iterator<EventObject> i = this.eventHashMap.values().iterator();

            // Go through hash list and delete the event object
            while (i.hasNext()) {
                EventObject value = i.next();

                if (value.equals(evtobj)) {

                    // Delete this eventobject from the FrameEventSet that contains it
                    FrameEventSet    frameSet     = value.getFrameEventSet();
                    Set<EventObject> eventObjects = frameSet.getEventObjects();

                    // Go through all events in the frameSet until you have found this event, and delete it.
                    for (EventObject event : eventObjects) {
                        if (event.getObjectId() == value.getObjectId()) {

                            // Remove the event object from the frameSet event set
                            eventObjects.remove(event);

                            break;
                        }
                    }

                    break;
                }
            }
        }
    }

    /**
     * Cleans up the references to this object in its original frameSet event set
     * This should be called before destroying this EventObjectContainer
     */
    public void cleanup() {
        synchronized (eventHashMap) {
            Iterator<EventObject> i = this.eventHashMap.values().iterator();

            // Go through hash list and delete each event object from its frameSet event set
            while (i.hasNext()) {
                EventObject value = i.next();

                // Delete this eventobject from the FrameEventSet that contains it
                FrameEventSet    frameSet     = value.getFrameEventSet();
                Set<EventObject> eventObjects = frameSet.getEventObjects();

                // Go through all events in the frameSet until you have found this event, and delete it.
                for (EventObject event : eventObjects) {
                    if (event.getObjectId() == value.getObjectId()) {
                        eventObjects.remove(event);

                        break;
                    }
                }
            }
        }
    }

    /**
     * Sets the image cache data for this container
     * @param data
     */
    public void setEventImageCacheData(EventImageCacheData data) {
        this.eventImageCacheData = data;
    }

    /**
     * Checks if the <code>EventObject<code> in this class is valid
     *
     * @return true if the <code>EventObject<code> contained in this class
     * is valid
     */
    public boolean isValid() {
        if (this.objectId != -1) {
            return true;
        }

        return false;
    }

    /**
     * Sets the best image associated with this event.
     * This will override the default setting of the "best" image representation
     * that is set to the largest instance found over the duration of the event.
     */
    public void setBestImageFrame(int frameNo) {
        if ((frameNo >= this.getStartFrame()) && (frameNo <= this.getEndFrame())) {
            bestEventFrame = frameNo;
        }
    }

    /**
     * Returns the total number of frames for this event
     * @return total number of frames for this event
     */
    public int getFrameDuration() {
        int start = getStartFrame();
        int end   = getEndFrame();

        return (end - start + 1);
    }

    /**
     * Sets the name of the class that best represents this event
     * @param value the name of the class
     */
    void setClassName(String value) {
        Iterator<Integer> iter = sortedEventFrames.iterator();

        while (iter.hasNext()) {
            EventObject o = eventHashMap.get(iter.next());

            o.setClassName(value);
        }
    }

    /**
     * Sets the name of the predicted class assigned to this event
     * @param value the name of the class
     * @param probability the probability it is within this class
     */
    void setPredictedClass(String value, float probability) {
        Iterator<Integer> iter = sortedEventFrames.iterator();

        while (iter.hasNext()) {
            EventObject o = eventHashMap.get(iter.next());

            o.setPredictedClass(value, probability);
        }
    }

    /**
     * Sets a id to associate with this
     *
     * @param id the name of the identifier
     */
    void setIdentityReference(String id) {
        Iterator<Integer> iter = sortedEventFrames.iterator();

        while (iter.hasNext()) {
            EventObject o = eventHashMap.get(iter.next());

            o.setIdentityReference(id);
        }
    }

    /**
     * Flags that the image associated with this event
     * was check for being all black
     */
    void setIsBlackChecked() {
        isBlackChecked = true;
    }

    /**
     * Returns true if  the image associated with this event
     * was check for being all black
     */
    boolean isBlackChecked() {
        return isBlackChecked;
    }

    /**
     * Sets a tag to associate with this class
     *
     * @param value the name of the class
     */
    void setTag(String tag) {
        Iterator<Integer> iter = sortedEventFrames.iterator();

        while (iter.hasNext()) {
            EventObject o = eventHashMap.get(iter.next());

            o.setTag(tag);
        }
    }

    /**
     * Returns the file for the given framenumber for this EventObjectContainer.
     * This is typically a file in a scratch directory prepended with
     * the video source stem followed by a 6 digit frameSet index number,
     * eventObject.g. /mnt/scratch/dcline/09272006_121528f000125.ppm
     * @return File associated with the frameNo or null if none found
     */
    public File getFrameSource(int frameNo) {

        // Format where the frames should be located accoring to the mainModel
        if ((mainModel != null) && (mainModel.getEventListModel() != null) && (mainModel.getSummaryModel() != null)) {
            SummaryModel model = this.mainModel.getSummaryModel();
            AvedVideo    v     = model.getAvedVideo();

            if (v != null) {
                return v.getFrameName(frameNo);
            }

            return null;
        } else {
            return null;
        }
    }

    public boolean equals(Object object) {
        return compareTo(object) == 0;
    }

    public String getShortDescription() {
        String description = null;

        if (maxEventFrame != -1) {
            description = "Object ID: " + this.objectId;
        } else {
            description = DEFAULT_DESCRIPTION;
        }

        return description;
    }

    @Override
    public int hashCode() {
        return (int) objectId;
    }

    public void add(EventObject eventObject) {
        synchronized (eventHashMap) {
            int maxSize = 0;

            eventHashMap.put(eventObject.getFrameEventSet().getFrameNumber(), eventObject);

            // Reset the Event that represents the maximum sized event if found bigger one
            if (maxEventFrame != -1) {
                EventObject object = eventHashMap.get(maxEventFrame);

                maxSize = object.getCurrSize();
            }

            if (eventObject.getCurrSize() > maxSize) {
                maxEventFrame  = eventObject.getFrameEventSet().getFrameNumber();
                bestEventFrame = maxEventFrame;

                /*
                 * System.out.println("#########Max size for event: "
                 * + eventObject.getObjectId() + " size: " + eventObject.getCurrSize()
                 * + " frameSet#:" + eventObject.getFrameEventSet().getFrameNumber() );
                 */
            }
        }

        sort();
    }

    public void sort() {
        synchronized (eventHashMap) {
            sortedEventFrames = new LinkedList<Integer>(eventHashMap.keySet());    // Get the keys as a list
            Collections.sort(sortedEventFrames);    // Sort the events by increasing framenumber
        }
    }

    /**
     * Gets the best image associated with this event
     */
    public ImageIcon getBestImage() {
        if (eventImageCacheData != null) {
            return eventImageCacheData.getImage();
        }

        return null;
    }

    /**
     * Gets the event object in this container that corresponds to a frameSet number
     * @param frameNo  The frameSet number.
     * @return EventObject
     * @throws FrameOutRangeException if the frameNo is out of range
     */
    public EventObject getEventObject(int framenum) throws FrameOutRangeException {
        synchronized (eventHashMap) {
            if ((framenum >= getStartFrame()) && (framenum <= getEndFrame())) {
                try {
                    return eventHashMap.get(framenum);
                } catch (java.lang.IndexOutOfBoundsException e) {
                    throw new FrameOutRangeException(this, framenum);
                }
            }

            throw new FrameOutRangeException(this, framenum);
        }
    }

    @Override
    public String toString() {
        String s;

        s = "Object ID: " + objectId + "\r";

        Iterator iter = sortedEventFrames.iterator();

        while (iter.hasNext()) {
            Integer     i = (Integer) iter.next();
            EventObject o = eventHashMap.get(i);

            s += "frame: " + i + o.toStringNoId() + "\n";
        }

        s += "\n";

        return s;
    }

    public int compareTo(Object o) {
        if (o.equals(objectId)) {
            return 1;
        }

        return 0;
    }

    public long getObjectId() {
        return objectId;
    }

    /**
     *
     * @return the class name or an empty string if none exists
     */
    public String getClassName() {
        synchronized (eventHashMap) {
            Iterator<Integer> iter = sortedEventFrames.iterator();

            if (iter.hasNext()) {
                EventObject o = eventHashMap.get(iter.next());

                return o.getClassName();
            }
        }

        return new String("");
    }

    /**
     *
     * @return the predicted class probability
     */
    public Float getPredictedClassProbability() {
        synchronized (eventHashMap) {
            Iterator<Integer> iter = sortedEventFrames.iterator();

            if (iter.hasNext()) {
                EventObject o = eventHashMap.get(iter.next());

                return o.getPredictedClassProbability();
            }
        }

        return new Float(0.f);
    }

    /**
     *
     * @return the predicted class name or an empty string if none exists
     */
    public String getPredictedClassName() {
        synchronized (eventHashMap) {
            Iterator<Integer> iter = sortedEventFrames.iterator();

            if (iter.hasNext()) {
                EventObject o = eventHashMap.get(iter.next());

                return o.getPredictedClassName();
            }
        }

        return new String("");
    }

    /**
     *
     * @return the tag or an empty string if none exists
     */
    public String getTag() {
        synchronized (eventHashMap) {
            Iterator<Integer> iter = sortedEventFrames.iterator();

            if (iter.hasNext()) {
                EventObject o = eventHashMap.get(iter.next());

                return o.getTag();
            }
        }

        return new String("");
    }

    /**
     *
     * @return the identity reference or an empty string if none exists
     */
    public String getIdentityReference() {
        synchronized (eventHashMap) {
            Iterator<Integer> iter = sortedEventFrames.iterator();

            if (iter.hasNext()) {
                EventObject o = eventHashMap.get(iter.next());

                return o.getIdentityReference();
            }
        }

        return new String("");
    }

    public int getMaxSize() {
        synchronized (eventHashMap) {
            if (maxEventFrame != -1) {
                EventObject object = eventHashMap.get(maxEventFrame);

                return object.getCurrSize();
            }
        }

        return 0;
    }

    public String getStartTimecode() {
        synchronized (eventHashMap) {
            return eventHashMap.get(sortedEventFrames.getFirst()).getFrameEventSet().getTimecode();
        }
    }

    public String getEndTimecode() {
        synchronized (eventHashMap) {
            return eventHashMap.get(sortedEventFrames.getLast()).getFrameEventSet().getTimecode();
        }
    }

    public int getStartFrame() {
        synchronized (eventHashMap) {
            return eventHashMap.get(sortedEventFrames.getFirst()).getFrameEventSet().getFrameNumber();
        }
    }

    public int getEndFrame() {
        synchronized (eventHashMap) {
            return eventHashMap.get(sortedEventFrames.getLast()).getFrameEventSet().getFrameNumber();
        }
    }

    /**
     * Finds the next larger instance to the one
     * found at the given frame
     *
     * @return returns the next larger frame, or -1 if none found
     * or he given frame is out of range
     */
    int findNextBestFrame() {
        Iterator<Integer> iter        = sortedEventFrames.iterator();
        int               maxSize     = eventHashMap.get(bestEventFrame).getCurrSize();
        int               nextMaxSize = -1;

        if (bestEventFrame != -1) {
            int maxFrame = bestEventFrame;

            while (iter.hasNext()) {
                EventObject eventObject = eventHashMap.get(iter.next());
                int         size        = eventObject.getCurrSize();

                // Find the next largest size
                if ((size > nextMaxSize) && (size != maxSize)) {
                    maxFrame    = eventObject.getFrameEventSet().getFrameNumber();
                    nextMaxSize = size;
                }
            }

            return maxFrame;
        }

        return -1;
    }

    /**
     * Returns event frameSet that represents the "best" representation of this AVED event.
     * This is typically the largest instance found over the duration of the event.
     * @return best event frameSet, or -1 if none found
     */
    public int getBestEventFrame() {
        return bestEventFrame;
    }

    /**
     * Returns timecode for the the "best" representation of this AVED event.
     * @return timecode string, or empty string if none found
     */
    public String getBestEventTimecode() {
        synchronized (eventHashMap) {
            if (bestEventFrame != -1) {
                EventObject object = eventHashMap.get(bestEventFrame);

                return object.getFrameEventSet().getTimecode();
            }
        }

        return new String("");
    }
}
