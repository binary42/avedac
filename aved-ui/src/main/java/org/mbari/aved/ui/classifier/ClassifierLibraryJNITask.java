/*
 * @(#)ClassifierLibraryJNITask.java
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

import java.util.Random;
import org.mbari.aved.classifier.ClassifierLibraryJNI;

/**
 *
 * @author dcline
 */
public class ClassifierLibraryJNITask {
    private String  matlabCancel = "";
    private boolean isFinished   = false;
    private boolean isCancelled  = false;

    public ClassifierLibraryJNITask(String name) throws Exception {
        Random random   = new Random();
        this.matlabCancel = name + Integer.toString(random.nextInt(10));
    }

    /*
     * return true if the task was successfully finished
     */
    public final synchronized boolean isFini() {
        return isFinished;
    }

    /**
     * Sets the task completion flag.
     * This should only be called after successful
     * task completion
     */
    public final synchronized void setFini() {
        isFinished = true;
    }

    /**
     * Sets the user-cancelled flag
     */
    public final synchronized void setCancelled() {
        isCancelled = true;
    }

    /**
     * @return  true if the matlab call was cancelled
     */
    public final synchronized boolean isCancelled() {
        return isCancelled;
    }

    /**
     * @return the cancel signal name
     */
    public final synchronized String getCancel() {
        return this.matlabCancel;
    }
 
    protected void run(ClassifierLibraryJNI library) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @return the task id - this is used for checking the status
     */
    public final synchronized int getId() {
        return this.hashCode();
    }
}
