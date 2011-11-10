/*
 * @(#)AVEDClassifierLibraryJNITestCopy.java
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



//~--- non-JDK imports --------------------------------------------------------

/** ************************************************************************ */
import junit.framework.*;

import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.TrainingModel;

public class AVEDClassifierLibraryJNITestCopy extends TestCase {
    public AVEDClassifierLibraryJNITestCopy(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAVEDClassifierLibraryJNI() throws Exception {
        String               dbRoot  = System.getProperty("user.home");
        String               logfile = System.getProperty("user.home") + "/matlablog.txt";
        ClassifierLibraryJNI app     = new ClassifierLibraryJNI(this, true);

        try {
            System.out.println("initialize library");

            String lcOSName = System.getProperty("os.name").toLowerCase();

            // If running from Mac
            if (lcOSName.startsWith("mac os x")) {
                app.initLib(logfile, 1);
            } else {
                app.initLib(logfile, 0);
            }

            System.out.println("Getting training classes");

            TrainingModel tmodels[] = app.get_training_classes(dbRoot);

            for (int i = 0; i < tmodels.length; i++) {
                System.out.println("Found training class: " + tmodels[i].toString());
                System.out.println("Description: " + tmodels[i].description());
            }

            if (tmodels.length > 0) {
                System.out.println("Copying first training class");

                TrainingModel tmodel = tmodels[0].copy();

                System.out.println("Found training class: " + tmodels.toString());
                System.out.println("Description: " + tmodel.description());
            }

            System.out.println("close library");
            app.closeLib();
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Exception" + e);
        }
    }
}
