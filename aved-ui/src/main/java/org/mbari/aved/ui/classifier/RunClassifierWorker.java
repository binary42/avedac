/*
 * @(#)RunClassifierWorker.java
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

import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.TrainingModel;
import org.mbari.aved.ui.classifier.table.TableModel;
import org.mbari.aved.ui.model.EventImageCache;
import org.mbari.aved.ui.model.EventImageCacheData;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dcline
 */
public class RunClassifierWorker extends ClassifierLibraryJNITask {
    private float               minProbThreshold = .09f;
    EventListModel              eventListModel;
    private final VotingMethod  method;
    private TableModel          tableModel;
    private final File          testDir;
    private final TrainingModel trainingModel;

    public RunClassifierWorker(TrainingModel model, float minProbThreshold, File testDir,
                               EventListModel eventListModel, VotingMethod method)
            throws Exception {
        super(model.getName());
        this.trainingModel    = model.copy();
        this.minProbThreshold = minProbThreshold;
        this.eventListModel   = eventListModel;
        this.testDir          = testDir;
        this.method           = method;
    }

    TableModel getTableModel() {
        return tableModel;
    }

    @Override
    protected void run(ClassifierLibraryJNI library) throws Exception {
        try {
            int size         = eventListModel.getSize();
            int numEvtImages = 0;

            for (int i = 0; i < size; i++) {
                EventObjectContainer event = eventListModel.getElementAt(i);

                numEvtImages += event.getFrameDuration();
            }

            if (!testDir.exists()) {
                testDir.mkdirs();
            }

            String[] children = testDir.list();

            // if event images are not created already create them
            // this is a simple test of file number, which may not be accurate
            // for some special cases.
            // TODO: match event files names; remove everything else, or some
            // variant of that to speed-up
            if (numEvtImages != children.length) {
                deleteDir(testDir);
                testDir.mkdirs();

                for (int i = 0; i < size; i++) {
                    EventObjectContainer event = eventListModel.getElementAt(i);

                    for (int frameNo = event.getStartFrame(); frameNo <= event.getEndFrame(); ++frameNo) {
                        if (isCancelled()) {
                            return;
                        }

                        // If found a valid frame number
                        if (frameNo >= 0) {
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
                        }
                    }
                }
            }

            String dbRoot = trainingModel.getDatabaseRootdirectory().toString();

            // Run test image collection on the data
            library.collect_tests(this.getCancel(), testDir.getAbsolutePath(), dbRoot, trainingModel.getColorSpace());

            int      numEvents                 = eventListModel.getSize();
            int[]    majoritywinnerindex       = new int[numEvents];
            int[]    probabilitywinnerindex    = new int[numEvents];
            int[]    maxprobabilitywinnerindex = new int[numEvents];
            float[]  probability               = new float[numEvents];
            String[] eventids                  = new String[numEvents];

            if (isCancelled()) {
                return;
            }

            // Run the classifier
            library.run_test(this.getCancel(), eventids, majoritywinnerindex, probabilitywinnerindex,
                             maxprobabilitywinnerindex, probability, testDir.getName(), trainingModel.getName(),
                             minProbThreshold, dbRoot, trainingModel.getColorSpace());

            // Add one column for the Unknown class
            int      columns     = trainingModel.getNumClasses() + 1;
            int      rows        = columns;
            String[] columnNames = new String[columns];
            int[][]  statistics  = new int[rows][columns];

            // Create hash map for look-up of the class index by name and
            // vice-versa
            HashMap<String, Integer> map = new HashMap<String, Integer>(columns, 0.75f);

            map.put(TrainingModel.UNKNOWN_CLASS_LABEL, new Integer(0));

            for (int j = 1; j < columns; j++) {
                map.put(trainingModel.getClassModel(j - 1).getName(), new Integer(j));
            }

            HashMap<Integer, String> mapbyid = new HashMap<Integer, String>(columns, 0.75f);

            mapbyid.put(new Integer(0), TrainingModel.UNKNOWN_CLASS_LABEL);

            for (int j = 1; j < columns; j++) {
                mapbyid.put(new Integer(j), trainingModel.getClassModel(j - 1).getName());
            }

            for (int i = 0; i < size; i++) {
                EventObjectContainer event = eventListModel.getElementAt(i);

                if (isCancelled()) {
                    return;
                }

                String np;
                Float  pp;
                Float  p = event.getPredictedClassProbability();

                if (p == null) {
                    p = new Float(0.0f);
                }

                switch (method) {
                case MAJORITY :
                    np = mapbyid.get(new Integer(majoritywinnerindex[i]));
                    pp = new Float(probability[i]);
                    event.setPredictedClass(np, pp);

                    break;

                case PROBABILITY :
                    np = mapbyid.get(new Integer(probabilitywinnerindex[i] - 1));
                    pp = new Float(probability[i]);
                    event.setPredictedClass(np, pp);

                    break;

                case MAXPROBABLITY :
                    np = mapbyid.get(new Integer(maxprobabilitywinnerindex[i] - 1));
                    pp = new Float(probability[i]);
                    event.setPredictedClass(np, pp);

                    break;
                }
            }

            int sum[] = new int[trainingModel.getNumClasses() + 1];

            // Format the statistics array for display in a JTable
            for (int j = 0; j < columns; j++) {
                if (j > 0) {
                    columnNames[j] = trainingModel.getClassModel(j - 1).getName();
                } else {
                    columnNames[0] = TrainingModel.UNKNOWN_CLASS_LABEL;
                }
            }

            for (int k = 0; k < numEvents; k++) {
                EventObjectContainer evt             = eventListModel.getElementAt(k);
                int                  frame           = evt.getBestEventFrame();
                int                  j               = 0;
                String               actualClassName = evt.getEventObject(frame).getClassName();

                if (map.containsKey(actualClassName)) {
                    j = map.get(actualClassName);
                }

                int i = 0;

                switch (method) {
                case MAJORITY :
                    i = majoritywinnerindex[k] - 1;

                    break;

                case PROBABILITY :
                    i = probabilitywinnerindex[k] - 1;

                    break;

                case MAXPROBABLITY :
                    i = maxprobabilitywinnerindex[k] - 1;

                    break;
                }

                sum[i]++;
                statistics[i][j]++;

                // Put the statistics and column names in a TableModel
                tableModel = new TableModel(columnNames, statistics, sum);
            }

            setFini();
        } catch (RuntimeException ex) {
            Logger.getLogger(RunClassifierController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {

            // Only log if this was an exception caused by a non-user cancel
            if (!this.isCancelled()) {
                Logger.getLogger(RunClassifierController.class.getName()).log(Level.SEVERE, null, ex);
                setFini();
            }
        }
    }

    /**
     * Deletes all files and subdirectories under dir.
     * @param dir
     * @return <code>true</code> if all deletions were successful.
     */
    public boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();

            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));

                if (isCancelled()) {
                    return false;
                }

                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}
