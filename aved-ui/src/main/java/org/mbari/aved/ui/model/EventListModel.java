/*
 * @(#)EventListModel.java
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



package org.mbari.aved.ui.model;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.EventObject;

import org.mbari.aved.ui.appframework.AbstractModel;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.exceptions.FrameOutRangeException;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoundedRangeModel;
import javax.swing.ListModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;

public class EventListModel extends AbstractModel implements ListModel, BoundedRangeModel {

    /** Cache of buffered images that represents this model data */
    private EventImageCache imageCache = null;

    /** Jump to index value */
    private int jumpToIndex = 0;

    /** Contains EventObjects to edit */
    private LinkedList<EventObjectContainer> list = new LinkedList<EventObjectContainer>();

    /** Used in BoundedRangeModel logic. This is used by the scroller. */
    private boolean isBoundedRangeModelValueIsAdjusting = false;

    /** Listeners */
    private ArrayList<ChangeListener> boundedRangeModelDataListeners = new ArrayList<ChangeListener>(5);

    /** Sync for this list */
    private final String syncList = "syncList";

    /**
     * Default constructor
     */
    public void EventListModel() {}

    /**
     * @return a reference to the image cache associated with this list
     */
    public EventImageCache getImageCache() {
        return imageCache;
    }

    /**
     * Sends a Model event ot notify listeners of the total of new changes
     * to the list. This is a helper function to refresh the GUI during
     * loading of the list
     */
    void notifyImagesChanged(int totalChanged) {

        // Send model event
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.NUM_LOADED_IMAGES_CHANGED, totalChanged));
    }

    /**
     * Clears out the list and notifies model event listeners
     *
     *  Call within block synced by: <code>syncList</code>
     */
    public void reset() {
        if (imageCache != null) {
            imageCache.clear();
        }

        imageCache = null;

        synchronized (syncList) {
            list.clear();
        }

        // Run the garbage collector
        Runtime.getRuntime().gc();
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.LIST_CLEARED, 0));
    }

    /**
     * Deletes the element from this list model
     * @param myEvent The object to delete.
     * This object will no longer be valid when this is deleted,
     * so use this with caution !!
     *
     *  Call within block synced by: <code>syncList</code>
     */
    public void deleteElement(EventObjectContainer myEvent) {
        long id    = -1;
        int  index = -1;

        synchronized (syncList) {
            if (list.contains(myEvent)) {

                // Get the id and index before deleting
                id    = myEvent.getObjectId();
                index = list.indexOf(myEvent);
                list.remove(myEvent);
                myEvent.cleanup();

                if ((imageCache != null) && (index != -1)) {
                    imageCache.remove(index);
                }
            }
        }

        // Run the garbage collector
        Runtime.getRuntime().gc();

        if ((id != -1) && (index != -1)) {
            notifyChanged(new EventListModelEvent(this, EventListModelEvent.ONE_ENTRY_REMOVED, id, index));
        }
    }

    /**
     * Returns the index in this list of the first occurrence of the
     * specified element, or -1 if the List does not contain this
     * element.  More formally, returns the lowest index i such that
     * <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>, or -1 if
     * there is no such index.
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param event element to search for.
     * @return the index in this list of the first occurrence of the
     *         specified element, or -1 if the list does not contain this
     *         element.
     */
    public int getIndexOf(EventObjectContainer event) {
        synchronized (syncList) {
            return list.indexOf(event);
        }
    }

    /**
     * Get the element next in the list to the given event
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param event element to search for
     * @return
     */
    public EventObjectContainer getElementNextTo(EventObjectContainer event) {
        synchronized (syncList) {
            int index = list.indexOf(event);

            if ((index >= 0) && (++index < list.size())) {
                return list.get(index);
            }
        }

        return null;
    }

    /**
     * Get the element previous in the list to the given event
     *
     * Call within block synced by: <code>syncList</code>
     *
     * @param event
     * @return
     */
    public EventObjectContainer getElementPrevTo(EventObjectContainer event) {
        synchronized (syncList) {
            int index = list.indexOf(event);

            if (--index >= 0) {
                return list.get(index);
            }
        }

        return null;
    }

    /**
     * Deletes the element from this list model
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param index the zero based index to delete
     */
    public void delete(int index) {
        long id = -1;

        synchronized (syncList) {
            if (index >= list.size()) {
                return;
            }

            // Adjust scroller value by one
            if (jumpToIndex > 0) {
                jumpToIndex--;
            }

            id = list.get(index).getObjectId();
            System.out.println("Deleting Object ID: " + id);

            // First delete the references to this object in the original FrameEventSet
            list.get(index).cleanup();

            // Remove the container from the linked list
            list.remove(index);

            // Remove the image cache at index
            if (imageCache != null) {
                imageCache.remove(index);
            }
        }

        // Run the garbage collector
        Runtime.getRuntime().gc();

        // Send model event
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.ONE_ENTRY_REMOVED, id, index));
    }

    /**
     * Combines the items in the list.  This sorts the
     * items in the list by their objectId's, combines all the events
     * into one with the lowest objectId. Using the lowest objectId is
     * completely arbitrary and has no significance.
     *
     * Called within block synced by: <code>syncList</code>
     *
     * @param containers the list of EventObjectContainers to delete
     * @return the combined EventObjectContainer, or null if none created
     */
    public EventObjectContainer combine(ArrayList<EventObjectContainer> containers) {
        Iterator<EventObjectContainer> i       = containers.iterator();
        ArrayList<Integer>             indexes = new ArrayList<Integer>();

        while (i.hasNext()) {
            int k = list.indexOf(i.next());

            if (k >= 0) {
                indexes.add(new Integer(k));
            }
        }

        return combineByIndex(indexes);
    }

    /**
     * Combines the items in the list at the specified indexes. This sorts the
     * associated EventObjectContainers found at the <code>indexes</code>, by
     * their objectId's, and combines all the events into one with the lowest
     * objectId. Using the lowest objectId is completely aarbitrary and has no
     * significance.  If two events exist in the same frame, the conflict is
     * resolved by simply choosing the one with the larger square pixesl area.
     *
     * Called within block synced by: <code>syncList</code>
     * @param indexes the list of indexes delete
     *
     * @return the combined EventObjectContainer, or null if none created
     */
    public EventObjectContainer combineByIndex(ArrayList<Integer> indexes) {

        // Sort the indexes in case they are random
        Collections.sort(indexes);

        Iterator<Integer> i = indexes.iterator();

        /**
         * Get the first event and add all the other events to it
         */
        if (i.hasNext()) {
            int                  k                = (int) i.next();
            EventObjectContainer primaryContainer = (EventObjectContainer) list.get(k);

            // Get the next eventID
            while (i.hasNext()) {
                k = (int) i.next();

                // Get its container
                EventObjectContainer toMergeContainer = (EventObjectContainer) list.get(k);

                /**
                 * Go through all the frames and add the event objects
                 * to the primary container
                 */
                EventObject primary    = null;
                EventObject toMerge    = null;
                int         startframe = toMergeContainer.getStartFrame();
                int         endframe   = toMergeContainer.getEndFrame();

                for (int j = startframe; j <= endframe; j++) {

                    /**
                     * Get the event objects from their containers
                     */
                    try {
                        primary = primaryContainer.getEventObject(j);
                    } catch (FrameOutRangeException ex) {

                        // An event object wasn't found at this frame number
                        primary = null;
                    }

                    try {
                        toMerge = toMergeContainer.getEventObject(j);
                    } catch (FrameOutRangeException ex) {

                        // An event object wasn't found at this frame number
                        toMerge = null;
                    }

                    /**
                     * If both container have event object in this frame,
                     * resolve the conflict by simply choosing the
                     * larger of the two. There is probably a better
                     * way of resolving this, e.g. by asking the
                     * user to resolve this in the GUI, but for
                     * now this method is a starting point
                     */
                    if ((primary != null) && (toMerge != null)) {
                        if (primary.getCurrSize() >= toMerge.getCurrSize()) {

                            /**
                             * remove the eventobject from the container and leave
                             * the remainder intact
                             */
                            toMergeContainer.cleanup(toMerge);
                        } else {

                            /**
                             * remove the event object from the primary and
                             * add the larger to the container
                             */
                            primaryContainer.cleanup(primary);
                            toMerge.setStartFrameNumber(primaryContainer.getStartFrame());
                            toMerge.setObjectId(primaryContainer.getObjectId());
                            primaryContainer.add(toMerge);
                        }
                    }

                    /**
                     * If there isn't an event object in this frame
                     * no conflicts need to be resolved, just go
                     * ahead and change the object ID,
                     * then add it to the primary
                     */
                    if ((primary == null) && (toMerge != null)) {
                        toMerge.setStartFrameNumber(primaryContainer.getStartFrame());
                        toMerge.setObjectId(primaryContainer.getObjectId());
                        primaryContainer.add(toMerge);
                    }
                }
            }

            // Skip past the first eventID and create a list of indexes to remove
            indexes.remove(0);

            // Remove all from the list
            deleteByIndex(indexes);

            return primaryContainer;
        }

        return null;
    }

    /**
     * Adds identifiers to elements in this list model
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param containers the EventObjectContainers to add identifiers to
     * @param id the name of the identifier to assign
     */
    public void setIdAll(ArrayList<EventObjectContainer> containers, String id) {
        Iterator<EventObjectContainer> iIter     = containers.iterator();
        ArrayList<Long>                objectIds = new ArrayList<Long>();
        ArrayList<Integer>             indexes   = new ArrayList<Integer>();

        // Add the id to the user preferences
        UserPreferences.getModel().addIdList(id);

        synchronized (syncList) {
            while (iIter.hasNext()) {
                try {
                    EventObjectContainer c = iIter.next();
                    int                  j = list.indexOf(c);

                    if (j != -1) {

                        // Add the event id to the ID list
                        objectIds.add(new Long(c.getObjectId()));
                        indexes.add(new Integer(j));
                        System.out.println("Identity reference for Object ID: " + c.getObjectId() + " is " + id);
                        c.setIdentityReference(id);
                    }
                } catch (IndexOutOfBoundsException e) {}
            }
        }

        // Send model event
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.MULTIPLE_ENTRIES_CHANGED, objectIds, indexes));
    }

    /**
     * Adds tags to elements in this list model
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param containers the EventObjectContainers to add tags to
     * @param tag the name of the tag to assign
     */
    public void setTagAll(ArrayList<EventObjectContainer> containers, String tag) {
        Iterator<EventObjectContainer> iIter     = containers.iterator();
        ArrayList<Long>                objectIds = new ArrayList<Long>();
        ArrayList<Integer>             indexes   = new ArrayList<Integer>();

        // Add the tag to the user preferences
        UserPreferences.getModel().addTagList(tag);

        synchronized (syncList) {
            while (iIter.hasNext()) {
                try {
                    EventObjectContainer c = iIter.next();
                    int                  j = list.indexOf(c);

                    if (j != -1) {

                        // Add the event id to the ID list
                        objectIds.add(new Long(c.getObjectId()));
                        indexes.add(new Integer(j));
                        System.out.println("Tagging Object ID: " + c.getObjectId() + " as " + tag);
                        c.setTag(tag);
                    }
                } catch (IndexOutOfBoundsException e) {}
            }
        }

        // Send model event
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.MULTIPLE_ENTRIES_CHANGED, objectIds, indexes));
    }

    /**
     * Assigns classes to the elements from this list model
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param indexes the zero based index ArrayList of Integers to classify
     * @param className The name of the class to assign
     */
    public void setClassAll(ArrayList<EventObjectContainer> containers, String className) {
        Iterator<EventObjectContainer> iIter     = containers.iterator();
        ArrayList<Long>                objectIds = new ArrayList<Long>();
        ArrayList<Integer>             indexes   = new ArrayList<Integer>();

        // Add the tag to the user preferences
        UserPreferences.getModel().addClassNameList(className);

        synchronized (syncList) {
            while (iIter.hasNext()) {
                try {
                    EventObjectContainer c = iIter.next();
                    int                  j = list.indexOf(c);

                    if (j != -1) {

                        // Add the event id to the ID list
                        objectIds.add(new Long(c.getObjectId()));
                        indexes.add(new Integer(j));
                        System.out.println("Assigning class for Object ID: " + c.getObjectId() + " as " + className);
                        c.setClassName(className);
                    }
                } catch (IndexOutOfBoundsException e) {}
            }
        }

        // Send model event
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.MULTIPLE_ENTRIES_CHANGED, objectIds, indexes));
    }

    /**
     * Assigns species to the elements from this list model
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param indexes the zero based index ArrayList of Integers to classify
     * @param speciesName the name of the class to assign
     * @param probability the probability of the class assignment
     */
    public void setPredictedClass(ArrayList<EventObjectContainer> containers, String className, float probability) {
        Iterator<EventObjectContainer> iIter     = containers.iterator();
        ArrayList<Long>                objectIds = new ArrayList<Long>();
        ArrayList<Integer>             indexes   = new ArrayList<Integer>();

        // Add the tag to the user preferences
        UserPreferences.getModel().addClassNameList(className);

        synchronized (syncList) {
            while (iIter.hasNext()) {
                try {
                    EventObjectContainer c = iIter.next();
                    int                  j = list.indexOf(c);

                    if (j != -1) {

                        // Add the event id to the ID list
                        objectIds.add(new Long(c.getObjectId()));
                        indexes.add(new Integer(j));
                        System.out.println("Assigning predicted class for Object ID: " + c.getObjectId() + " as "
                                           + className);
                        c.setPredictedClass(className, probability);
                    }
                } catch (IndexOutOfBoundsException e) {}
            }
        }

        // Send model event
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.MULTIPLE_ENTRIES_CHANGED, objectIds, indexes));
    }

    /**
     * Deletes the elements from this list model
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param indexes the zero based index ArrayList of Integers to delete
     */
    public void deleteByIndex(ArrayList<Integer> indexes) {
        ArrayList<EventObjectContainer> collection = new ArrayList<EventObjectContainer>(indexes.size());
        Iterator<Integer>               i          = indexes.iterator();
        ArrayList<Long>                 objectIds  = new ArrayList<Long>();

        // Sort the indexes in case they are random
        Collections.sort(indexes);

        synchronized (syncList) {
            while (i.hasNext()) {
                try {
                    int                  j = (int) i.next();
                    EventObjectContainer c = list.get(j);

                    System.out.println("Deleting Event ID: " + c.getObjectId());

                    // Add the event id to the ID list
                    objectIds.add(new Long(c.getObjectId()));

                    // Add this EventObjectContainer to the collection
                    collection.add(c);

                    // Delete the references to this object in the original FrameEventSet
                    c.cleanup();

                    // Adjust scroller value by one
                    if (jumpToIndex > 0) {
                        jumpToIndex--;
                    }
                } catch (IndexOutOfBoundsException e) {}
            }

            Collections.sort(objectIds);

            // Remove all from the list
            list.removeAll(collection);
            System.out.println("EventListModel removed " + indexes.size() + " new size:" + list.size());

            // / Remove all from the cache
            if (imageCache != null) {
                imageCache.removeIndexes(indexes);
            }
        }

        // Run the garbage collector
        Runtime.getRuntime().gc();

        // Send model event
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.MULTIPLE_ENTRIES_CHANGED, objectIds, indexes));
    }

    /**
     * Deletes the elements from this list model
     *
     *  Call within block synced by: <code>syncList</code>
     *
     * @param indexes The list of EventObjectContainers to delete
     */
    public void delete(ArrayList<EventObjectContainer> collection) {
        Iterator<EventObjectContainer> i         = collection.iterator();
        ArrayList<Long>                objectIds = new ArrayList<Long>();
        ArrayList<Integer>             indexes   = new ArrayList<Integer>();
        Boolean                        modified  = false;

        synchronized (syncList) {
            while (i.hasNext()) {
                try {
                    EventObjectContainer c = i.next();

                    // If a valid index is found for this event object
                    if (list.indexOf(c) != -1) {
                        System.out.println("Deleting Event ID: " + c.getObjectId() + c.toString());

                        // Add the event id to the ID list
                        objectIds.add(new Long(c.getObjectId()));
                        indexes.add(new Integer(list.indexOf(c)));

                        // Delete the references to this object in the original FrameEventSet
                        c.cleanup();

                        // Adjust scroller value by one
                        if (jumpToIndex > 0) {
                            jumpToIndex--;
                        }
                    }
                } catch (IndexOutOfBoundsException e) {}
            }

            // Remove all from the list
            modified = list.removeAll(collection);

            if (modified == true) {
                System.out.println("EventListModel removed " + indexes.size() + " new size:" + list.size());

                // / Remove all from the cache
                if (imageCache != null) {
                    imageCache.removeIndexes(indexes);
                }
            }
        }

        if (modified == true) {

            // Run the garbage collector
            Runtime.getRuntime().gc();

            if (objectIds.size() > 1) {

                // Send model event
                notifyChanged(new EventListModelEvent(this, EventListModelEvent.MULTIPLE_ENTRIES_CHANGED, objectIds,
                        indexes));
            } else if (objectIds.size() == 1) {
                notifyChanged(new EventListModelEvent(this, EventListModelEvent.ONE_ENTRY_REMOVED, objectIds.get(0),
                        indexes.get(0)));
            }
        }
    }

    /**
     * Add a  set of EventObjectContainer entries to this listmodel.
     * Call within block synced by: <code>syncList</code>
     * @param entries the list of entries to add
     */
    public void add(ArrayList<EventObjectContainer> entries) {
        ArrayList<Long>    objectIds = new ArrayList<Long>();
        ArrayList<Integer> indexes   = new ArrayList<Integer>();

        synchronized (syncList) {
            Iterator<EventObjectContainer> i = entries.iterator();

            if (list != null) {
                while (i.hasNext()) {
                    EventObjectContainer c = i.next();

                    // If the object isn't already in the list
                    if (list.indexOf(c) == -1) {
                        ListIterator<EventObjectContainer> j  = list.listIterator();
                        int                                k  = 0;
                        Long                               id = c.getObjectId();

                        // Add the object in increasing objectId order
                        // Go through the list until the next higher id is
                        // found and insert before this.
                        while (j.hasNext()) {
                            Long                 idNext = id;
                            EventObjectContainer event  = null;

                            // If any containers left
                            if (j.hasNext()) {
                                event  = j.next();
                                idNext = event.getObjectId();
                            }

                            // If found a larger id
                            if (idNext > id) {
                                if (event != null) {
                                    k = list.indexOf(event);
                                }

                                break;
                            }

                            k++;
                        }

                        jumpToIndex++;
                        list.add(k, c);
                        indexes.add(new Integer(k));
                        objectIds.add(new Long(id));
                        System.out.println("Adding Event ID: " + id + " to index " + k + c.toString());

                        // / Add element to the image cache
                        if (imageCache != null) {
                            EventImageCacheData data;

                            try {
                                data = new EventImageCacheData(c);
                                imageCache.add(k, data);
                            } catch (Exception ex) {
                                Logger.getLogger(EventListModel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }

                System.out.println("EventListModel added " + indexes.size() + " new size:" + list.size());
            }
        }

        // Send a notifier to all the ModelListeners that entries have changed
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.MULTIPLE_ENTRIES_CHANGED, objectIds, indexes));
    }

    /**
     * Add a set of EventObjectContainer entries to this listmodel.
     * Call within block synced by: <code>syncList</code>
     * @param entries the list of entries to add
     */
    public void add(LinkedList<EventObjectContainer> entries) {
        synchronized (syncList) {
            jumpToIndex = 0;

            if (list != null) {
                list.clear();
            }

            // Initialize the list
            list = new LinkedList<EventObjectContainer>(entries);
        }

        // Send a notifier to all the ModelListeners
        notifyChanged(new EventListModelEvent(this, EventListModelEvent.LIST_RELOADED, 0));
    }

    // ========================================================================
    // BoundedRangeModel Implementation
    // =========================================================================
    public void addChangeListener(ChangeListener x) {
        synchronized (boundedRangeModelDataListeners) {
            boundedRangeModelDataListeners.add(x);
        }
    }

    public void removeChangeListener(ChangeListener x) {
        synchronized (boundedRangeModelDataListeners) {
            boundedRangeModelDataListeners.remove(x);
        }
    }

    /**
     * Min and max are zero based which works well for the JSlider.
     *
     * @return -1 if no items
     */
    public int getMaximum() {
        synchronized (syncList) {
            return list.size() - 1;
        }
    }

    public int getExtent() {
        return 0;
    }

    public int getMinimum() {
        return 0;
    }

    public boolean getValueIsAdjusting() {
        return isBoundedRangeModelValueIsAdjusting;
    }

    public void setValueIsAdjusting(boolean b) {
        isBoundedRangeModelValueIsAdjusting = b;

        if (false == b) {
            notifyChanged(new EventListModelEvent(this, EventListModelEvent.CURRENT_PAGE_CHANGED, 0));
        }
    }

    // We just ignore this since the internal list controls this.
    public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
        setValueIsAdjusting(adjusting);
    }

//  We just ignore this since the internal list controls this.
    public void setExtent(int newExtent) {}

    // We just ignore this since the internal list controls this.
    public void setMaximum(int newMaximum) {}

    // We just ignore this since the internal list controls this.
    public void setMinimum(int newMinimum) {}

    /**
     *
     * @param newValue
     *           Invalid values are silently ignored.
     */
    public void setValue(int newValue) {
        Boolean notify = false;

        synchronized (syncList) {
            if ((newValue >= 0) && (newValue < list.size())) {
                notify      = true;
                jumpToIndex = newValue;
            }
        }

        if (notify) {
            notifyChanged(new EventListModelEvent(this, EventListModelEvent.CURRENT_PAGE_CHANGED, jumpToIndex));
        }
    }

    public int getSize() {
        if (list != null) {
            return list.size();
        }

        return 0;
    }

    public void addListDataListener(ListDataListener l) {

        // TODO Auto-generated method stub
    }

    public void removeListDataListener(ListDataListener l) {

        // TODO Auto-generated method stub
    }

    public boolean isLastEvent(EventObjectContainer event) {
        if ((event != null) && (event == list.getLast())) {
            return true;
        }

        return false;
    }

    public boolean isFirstEvent(EventObjectContainer event) {
        if ((event != null) && (event == list.getFirst())) {
            return true;
        }

        return false;
    }

    /**
     * Sets the image data in the list element at index i
     * @param index list index
     * @param evt element to set at the index
     */
    public void setElementAt(int index, EventObjectContainer evt) {
        synchronized (syncList) {
            if ((list != null) && (index < list.size())) {
                EventObjectContainer e = (EventObjectContainer) list.get(index);

                e = evt.clone();
            }
        }
    }

    /**
     * Starts the image cache model image loader
     * This will populate the image cache model with
     * buffered images. This should be called every time
     * the video source is changed and subsequent
     * transcoded output frames change
     */
    public void loadImageCacheDataByEvent() throws Exception {
        imageCache = new EventImageCache();
        imageCache.loadImageCache(this, false);
    }
    /**
     * Starts the image cache model image loader
     * This will populate the image cache model with
     * buffered images by loading event images by frame.
     * This should only be used in cases for still frames
     * where we assume events are only 1 frame long. In this case  
     * it is more efficient to load all events in a given
     * frame, rather than event-wise as in video where the 
     * best frame can be 
     */
    public void loadImageCacheDataByFrame() throws Exception{
        imageCache = new EventImageCache();
        imageCache.loadImageCache(this, true);
    }

    /**
     * Return an element in the list model at a given index
     * @param index of the element
     * @return the EventObjectContainer element
     */
    public EventObjectContainer getElementAt(int index) {
        synchronized (syncList) {
            if ((list != null) && (index < list.size())) {
                EventObjectContainer e = (EventObjectContainer) list.get(index);

                return e;
            }
        }

        return null;
    }

    /**
     * Returns the jumpto index value for this list model. This is
     * the last selected value by the user
     * @return the jumpto index value for this list model.
     */
    public int getValue() {
        return this.jumpToIndex;
    }


    public class EventListModelEvent extends ModelEvent {

        /**
         * Indicates that the current image has changes and that the listening
         * software should redisplay the image
         */
        public static final int CURRENT_PAGE_CHANGED = 5;

        /**
         * Indicates that the event list has been removed
         */
        public static final int LIST_CLEARED = 8;

        /**
         * Indicates that the entire event list has been reloaded
         */
        public static final int LIST_RELOADED = 7;

        /**
         * Identifies more than one change in the lists contents.
         */
        public static final int MULTIPLE_ENTRIES_CHANGED = 9;

        /**
         * Indicates the number of total loaded images associated
         * with the list has changed
         */
        public static final int NUM_LOADED_IMAGES_CHANGED = 4;

        /**
         * Indicates that the list has changed by removing a single entry.
         */
        public static final int ONE_ENTRY_REMOVED = 6;

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /**
         * Stores array of Event IDs from the last model event
         */
        ArrayList<Long> objectIds = new ArrayList<Long>();

        /**
         * Stores array of List model indexes from the last model event
         */
        ArrayList<Integer> modelIndexes = new ArrayList<Integer>();

        /**
         * Constructor for this custom ModelEvent. Basically just like ModelEvent.
         * This is the default constructor for events that don't need to set the
         * any contained variables.
         * @param obj  the object that originated the event
         * @param type an integer that identifies the ModelEvent type
         * @param index the index of the event; its meaning depends on the
         * type specified and is retreived through the super.getFlag() method
         */
        public EventListModelEvent(Object obj, int type, int index) {
            super(obj, type, "EventListModelEvent:" + type, index);
        }

        /**
         * Constructor for this custom ModelEvent. Basically just like ModelEvent.
         * This is the default constructor for events that don't need to set the
         * any contained variables.
         * @param obj  the object that originated the event
         * @param type    an integer that identifies the ModelEvent type
         * @param message a message to add to the event description
         */
        public EventListModelEvent(Object obj, int type, String message) {
            super(obj, type, "EventListModelEvent:" + type + " " + message);
        }

        /**
         * Constructor for this custom ModelEvent.
         * @param obj  the object that originated the event
         * @param objectIds   the event ID of the events changed by this ModelEvent
         * @param indexes the original event indexes changed by this ModelEvent
         */
        public EventListModelEvent(Object obj, int type, ArrayList<Long> eventIDS, ArrayList<Integer> indexes) {
            super(obj, type, "EventListModelEvent:" + type + "EventID Size: " + eventIDS.size());
            objectIds.clear();
            objectIds = eventIDS;
            modelIndexes.clear();
            modelIndexes = indexes;
        }

        /**
         * Constructor for this custom ModelEvent.
         * @param obj  the object that originated the event
         * @param eventID   the event ID of the event changed by this ModelEvent
         * @param index the original event index changed by this ModelEvent
         */
        public EventListModelEvent(Object obj, int type, long eventID, int index) {
            super(obj, type, "EventListModelEvent:" + type + "EventID: " + eventID);
            objectIds.clear();
            objectIds.add(new Long(eventID));
            modelIndexes.clear();
            modelIndexes.add(new Integer(index));
        }

        /** Returns the event ID array associated with this ModelEvent */
        public ArrayList<Long> getObjectIds() {
            return objectIds;
        }

        /** Returns the image cache associated with this list */
        public EventImageCache getImageCache() {
            return imageCache;
        }

        /** Returns the List model index array associated with this ModelEvent */
        public ArrayList<Integer> getModelIndexes() {
            return modelIndexes;
        }

        /**
         * Returns the single object ID associated with this ModelEvent
         *  Returns -1 if no event ID was found
         */
        public long getObjectId() {

            /*
             *  This assumes for single list ModelEvent actions, the first element
             * in the array stores the event ID
             */
            if (objectIds.size() > 0) {
                return objectIds.get(0);
            }

            return -1;
        }

        /**
         * Returns the single event list index associated with this ModelEvent
         *      Returns -1 if no event ID was found
         */
        public int getEventIndex() {

            /*
             *  This assumes for single list ModelEvent actions, the first element
             * in the array stores the event ID
             */
            if (modelIndexes.size() > 0) {
                return modelIndexes.get(0);
            }

            return -1;
        }
    }
}
