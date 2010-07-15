/*
 * @(#)ClassifierLibraryJNITask.java
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


import org.mbari.aved.classifier.ClassifierLibraryJNI;

//~--- JDK imports ------------------------------------------------------------


/**
 *
 * @author dcline
 */
public class ClassifierLibraryJNITask {
    private String matlabCancel = new String("");
    private boolean isCancelled = false;
    private boolean isFinished = false;

    public ClassifierLibraryJNITask(String name) throws Exception {
        this.matlabCancel = name;
    }

    /*
     * return true is the task was successfully finished
     */
    public synchronized boolean isFini() {
        return isFinished;
    }
    /**
     * Sets the task completion flag.
     * This should only be called after successful
     * task completion
     */
    public synchronized void setFini() {
        isFinished = true;
    }

    /**
     * Sets the user-cancelled flag
     */
    public synchronized void setCancelled () {
        isCancelled = true;
    }
    /** 
     * @return  true if the matlab call was cancelled
     */
    public synchronized boolean isCancelled() {
        return isCancelled;
    }

    /**
     * @return the cancel signal name
     */
    public synchronized String getCancel() {
        return this.matlabCancel;
    }
 
    protected synchronized void run(ClassifierLibraryJNI library) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
