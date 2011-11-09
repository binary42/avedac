/*
 * @(#)ThumbnailPanel.java
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



package org.mbari.aved.ui.thumbnail;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.TableSorter;
import org.mbari.aved.ui.utils.ImageUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

/**
 * ThumbnailPanel.
 * Lots of images gives:
 * Exception in thread "AWT-EventQueue-0" java.lang.OutOfMemoryError: Java heap space
 * -Joption
 * Pass option to the Java runtime environment, where option is one of the options
 * described on the reference page for the java application launcher.
 * For example, -J-Xmx48M sets the maximum memory to 48 megabytes.
 * NOTE: PASSING TO JAR FILE JUST ADJUSTS HOW JAR FILE IS COMPILED BY JAVA.
 * DOES NOT PASS INTO THE JAR FILE!!!!
 * It is a common convention for -J to pass options to the underlying runtime environment.
 * -Xms Minimum memory.  The default value is 2MB.
 * -Xmx Maximum memory.  The default value is 64MB.
 * -Xms500M -Xmx1000M
 * -J-Xms500M -J-Xmx1000M
 */
public class ThumbnailPanel extends JPanel {
    int                            nColumns    = 4;
    int                            nRows       = 4;
    JFrame                         parentFrame = null;
    boolean                        isVisible   = false;
    private List<ThumbnailPicture> pictures    = new ArrayList<ThumbnailPicture>();
    int                            iScroller;
    int                            nPicturesPerPage;

    // ==========================================================================
    // Constructor
    // ==========================================================================
    public ThumbnailPanel(JFrame mf) {

        // Set up double buffered
        super(true);
        super.setFocusable(true);
        parentFrame = mf;
        reset();
    }

    /**
     * Sets the grid layout for the designated rows and columns
     * and sets the selected picture cell
     * @param rows The number of rows to assign
     * @param cols The number of columns to assign
     */
    public void setRowCol(int rows, int cols) {
        nRows    = rows;
        nColumns = cols;

        // Out with the old.
        if (!pictures.isEmpty()) {
            super.removeAll();
        }

        // Figure out and set the layout.
        int hgap = 2;
        int vgap = 2;

        if (nRows < 1) {
            nRows = 1;
        }

        if (nColumns < 1) {
            nColumns = 1;
        }

        nPicturesPerPage = nRows * nColumns;
        super.setLayout(new GridLayout(rows, nColumns, hgap, vgap));
    }

    /**
     *
     * @return number of pictures per page displayed
     */
    public int getPicturesPerPage() {
        return nPicturesPerPage;
    }

    /**
     * @return showing scroller index. Returns -1 if not initialized
     */
    int getScrollerIndex() {
        return iScroller;
    }

    /**
     * Cancel current showing stuff to force re-evaluation and loading of all images.
     */
    void invalidatePicPointer() {
        iScroller = -1;
    }

    /**
     * Returns the scroller index for the {@link ThumbnailPicture} that corresponds
     * to the given id
     *
     * @param id index of the <code>ThumbnailPicture</code> to reset
     */
    public int getScrollerIndex(long id) throws IndexOutOfBoundsException {
        Iterator<ThumbnailPicture> iter = pictures.iterator();

        while (iter.hasNext()) {
            ThumbnailPicture     picture = iter.next();
            EventObjectContainer c       = picture.getEventObjectContainer();

            if ((c != null) && (c.getObjectId() == id)) {
                return picture.getScrollerIndex();
            }
        }

        return 0;
    }

    /**
     * Removes the {@link ThumbnailPicture} at the given index from this panel
     * @param i index in the panel to reset
     *
     * @param id index of the <code>ThumbnailPicture</code> to reset
     */
    public void removePicture(long id) throws IndexOutOfBoundsException {
        Iterator<ThumbnailPicture> iter = pictures.iterator();

        while (iter.hasNext()) {
            ThumbnailPicture     picture = iter.next();
            EventObjectContainer c       = picture.getEventObjectContainer();

            if ((c != null) && (c.getObjectId() == id)) {
                pictures.remove(iter);

                break;
            }
        }
    }

    /**
     * Clears out all the thumbnail pictures and resets
     * the <code>GridLayout</code> used in this panel
     */
    public void reset() {

        // Remove all pictures from this panel
        Iterator<ThumbnailPicture> iter = pictures.iterator();

        while (iter.hasNext()) {
            ThumbnailPicture picture = iter.next();

            if (picture != null) {
                super.remove(picture);
            }
        }

        pictures.clear();

        // Set the pictures to view
        setRowCol(nRows, nColumns);

        // Make sure we can get focus back here.
        super.setFocusable(true);

        // TODO: set this to the lookandfeel background
        super.setBackground(Color.GRAY);
        repaint();
    }

    /**
     * Adds a mouse listener to handle thumbnail clicks
     * @param listener
     */
    public void addMouseClickListener(MouseListener listener) {
        if (pictures != null) {
            Iterator<ThumbnailPicture> iter = pictures.iterator();

            while (iter.hasNext()) {
                iter.next().addMouseListener(listener);
            }
        }
    }

    /**
     * Creates enough {@link ThumbnailPicture}s to populate a single page in the panel
     * @param model ApplicationModel that contains a valid {@link org.mbari.aved.ui.model.EventListModel}
     * and a valid {@link javax.swing.ListSelectionModel}.
     */
    public void createThumbnailPictures(ApplicationModel model) {
        EventListModel     events = model.getEventListModel();
        ListSelectionModel list   = model.getListSelectionModel();

        // Remove any pictures in case already initialized
        pictures.clear();
        super.removeAll();

        // Add the picture components for the new layout
        for (int i = 0; i < nPicturesPerPage; i++) {
            EventObjectContainer data = events.getElementAt(i);

            if (data != null) {
                ThumbnailPicture p = new ThumbnailPicture(data, this, list);

                pictures.add(p);
                super.add(p);
            }
        }
    }

    /**
     * Shows images from the scroller value plus one page.
     * @param value scroller value to show at the top of the page
     * @param model ApplicationModel that contains a valid {@link org.mbari.aved.ui.model.EventListModel}
     * and {@link javax.swing.ListSelectionModel} to reset the panel (if needed)
     * to show this scroller value.
     *
     * @throws Exception
     */
    public void showPageFromScroller(int value, ApplicationModel model) throws Exception {
        invalidatePicPointer();

        if ((iScroller == value) ||!isVisible) {

            // Don't need to repaint if have not scrolled or am not showing
            iScroller = value;

            return;
        }

        iScroller = value;

        int offset = value;

        // System.out.println("iScroller: " + value);
        // System.out.println("offset: " + offset + " pictures per page:" + nPicturesPerPage);
        // Translated the row index into the real eventlistmodel index
        // through the sorter, since the table may be sorted. This
        // keeps the table/thumbnail displays in sync with each other
        TableSorter sorter = model.getSorter();

        if (sorter != null) {
            EventListModel eventlistmodel = model.getEventListModel();

            // Reset all of the pictures for this page
            for (int i = 0; i < pictures.size(); i++) {
                int scrollIndex = i + offset;

                // If beyond the model size, return otherwise this throws an
                // ArrayIndexOutOfBoundsException. But be sure to repaint
                // before returning to display any changes
                if (scrollIndex >= eventlistmodel.getSize()) {
                    if (pictures.get(i) != null) {
                        pictures.get(i).reset(null);
                    }
                } else {
                    EventObjectContainer data = eventlistmodel.getElementAt(sorter.modelIndex(scrollIndex));

                    if (pictures.get(i) != null) {
                        pictures.get(i).reset(data);
                        pictures.get(i).setScrollerIndex(scrollIndex);
                    }
                }

                this.repaint();
            }
        }
    }

    /**
     * Request focus on this panel. Sets internal state variable used in repaint logic
     * @param state the focus state
     * @return the focus state
     */
    public boolean requestFocus(boolean state) {
        isVisible = state;

        return state;
    }

    /**
     * Make sure all components show wait at same time.
     * TODO: fix this - it currently only sets a cursor
     * for the panel - not sure if this is correct
     * @param wait
     */
    public void setWaitCursor(boolean wait) {

        // TODO: test this wait cursor
        if (wait) {
            this.setCursor(ImageUtils.busyCursor);
        } else {
            this.setCursor(ImageUtils.defaultCursor);
        }
    }
}
