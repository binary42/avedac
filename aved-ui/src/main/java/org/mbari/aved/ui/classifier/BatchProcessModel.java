/*
 * @(#)BatchProcessModel.java
 *
 * Copyright 2013 MBARI
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

import org.mbari.aved.ui.ApplicationModel;

//~--- JDK imports ------------------------------------------------------------

import java.io.File; 
import java.util.HashMap;

import java.util.logging.Level;
import java.util.logging.Logger; 

/**
 *
 * @author dcline
 */
public class BatchProcessModel {
    private ApplicationModel model;
    private String     status;
    private HashMap<Integer, Object> classTotal = new HashMap<Integer, Object>();

    public BatchProcessModel(File events) { 
        this.status = "";

        try {
            model = new ApplicationModel();
            model.getSummaryModel().setXmlFile(events); 
        } catch (Exception ex) {
            Logger.getLogger(BatchProcessModel.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    File getFile() {
        return model.getSummaryModel().getXmlFile();
    }

    ApplicationModel getModel() {
        return this.model;
    }

    void setStatus(String string) {
        this.status = string;
    }

    String getStatus() {
        return this.status;
    }

    Object getClassTotal(int classIndex) {
        return classTotal.get(classIndex);
    }

    void setClassTotal(int classIndex, Object ttl) {
        classTotal.put(classIndex, ttl);
    }

 
}
