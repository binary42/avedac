/*
 * @(#)MatlabWorker.java   10/03/17
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

import org.jdesktop.swingworker.SwingWorker;

import org.mbari.aved.classifier.ClassifierLibraryJNI;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dcline
 */
public class MatlabWorker extends SwingWorker {
    private final String matlabCancel;

    public MatlabWorker(String name) throws Exception {
        this.matlabCancel = name;
    }

    public String getCancel() {
        return this.matlabCancel;
    }

    /**
     * Interrupts the worker and matlab thread
     * @param mayInterruptIfRunning
     * @return
     */
    public boolean cancelWorker(boolean mayInterruptIfRunning) {
        try {
            ClassifierLibraryJNI app = Classifier.getLibrary();

            app.set_kill(matlabCancel, 1);

            return super.cancel(mayInterruptIfRunning);
        } catch (Exception ex) {
            Logger.getLogger(MatlabWorker.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    @Override
    protected Object doInBackground() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
