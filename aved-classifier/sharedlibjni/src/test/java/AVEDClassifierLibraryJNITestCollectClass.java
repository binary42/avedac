/*
 * Copyright 2009 MBARI
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

import junit.framework.*;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import java.net.URL;
import org.mbari.aved.classifier.ColorSpace;

public class AVEDClassifierLibraryJNITestCollectClass extends TestCase {

    public AVEDClassifierLibraryJNITestCollectClass(String name) {
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
        ClassifierLibraryJNI app = new ClassifierLibraryJNI();

        System.out.println("initialize library");
        String lcOSName = System.getProperty("os.name").toLowerCase();
        
        // If running from Mac
        if (lcOSName.startsWith("mac os x")) {
            app.initLib(logfile, 1);
        } else {
            app.initLib(logfile, 0);
        }

        URL squaredFlatImageUrl = getClass().getResource("2526_Training_Classes/flat");
        URL squaredRathImageUrl = getClass().getResource("2526_Training_Classes/rath");

        try {
            System.out.println("Running collect class");
            String killFile = dbRoot + "testcollect";

            app.collect_class(killFile, squaredFlatImageUrl.getFile(),
                    squaredFlatImageUrl.getFile(),
                    "flat", dbRoot,
                    "flatfish",
                    "Test flatfish class",
                    ColorSpace.GRAY);

            app.collect_class(killFile, squaredRathImageUrl.getFile(),
                    squaredRathImageUrl.getFile(),
                    "rath", dbRoot,
                    "Rathbunaster-Californicus",
                    "Test Rathbunaster-Californicus class",
                    ColorSpace.GRAY);

            System.out.println("Collect class finished");

        } catch (Exception e) {
            System.out.println("Exception" + e);
        }

        app.closeLib();
        System.exit(0);
    }
}
