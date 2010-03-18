/*
 * @(#)CollectTestImageWorker.java   10/03/17
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
