/*
 * @(#)AVEDClassifierLibraryJNITestLib.java
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

//~--- non-JDK imports --------------------------------------------------------
/** ************************************************************************ */
import junit.framework.*;

import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.TrainingModel;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

public class AVEDClassifierLibraryJNITestLib extends TestCase {

    public AVEDClassifierLibraryJNITestLib(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAVEDClassifierLibraryJNI() throws Exception {
        String dbRoot = System.getProperty("user.home");
        String logfile = System.getProperty("user.home") + "/matlablog.txt";
        ClassifierLibraryJNI app = new ClassifierLibraryJNI(this);

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

            System.out.println("Getting collected classes");

            ClassModel cmodels[] = app.get_collected_classes(dbRoot);

            for (int i = 0; i < cmodels.length; i++) {
                System.out.println("Found class: " + cmodels[i].toString());
                System.out.println("Description: " + cmodels[i].description());
            }

            System.out.println("close library");
            app.closeLib();
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Exception" + e);
        }
    }
}
