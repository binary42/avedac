/*
 * @(#)CollectTestImageWorker.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.EventObject;

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.ui.model.EventImageCache;
import org.mbari.aved.ui.model.EventImageCacheData;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.progress.ProgressDisplay;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dcline
 */
public class CollectTestImageWorker extends SwingWorker {
    private final EventListModel  listModel;
    private final ProgressDisplay progressDisplay;
    private final SummaryModel    summaryModel;

    public CollectTestImageWorker(EventListModel list, SummaryModel summary) {
        this.listModel       = list;
        this.summaryModel    = summary;
        this.progressDisplay = new ProgressDisplay(this, "Collecting test images from " + summary.getXmlFile());
    }

    @Override
    protected Object doInBackground() throws Exception {
        progressDisplay.display("Collecting test images ...");

        File testDir = summaryModel.getTestImageDirectory();

        if (!testDir.exists()) {
            testDir.mkdirs();
        }

        int size = listModel.getSize();

        for (int i = 0; i < size; i++) {
            EventObjectContainer event = listModel.getElementAt(i);

            for (int frameNo = event.getStartFrame(); frameNo <= event.getEndFrame(); frameNo++) {

                // If found a valid frame number
                if (frameNo >= 0) {
                    try {
                        int                 bestFrameNo = frameNo;
                        EventImageCacheData data        = new EventImageCacheData(event);

                        // If the event has a class, then rename
                        // the event with an appended name - replacing
                        // all the white spaces with dashes
                        if (event.getClassName().length() > 0) {
                            data.initialize(testDir, event.getClassName(), bestFrameNo);
                        } else {
                            data.initialize(testDir, "", bestFrameNo);
                        }

                        EventObject object = event.getEventObject(bestFrameNo);

                        if (object != null) {
                            EventImageCache.createSquaredImageOfEvent(data, object);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(CollectTestImageWorker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        progressDisplay.display("Done !");

        return this;
    }
}
