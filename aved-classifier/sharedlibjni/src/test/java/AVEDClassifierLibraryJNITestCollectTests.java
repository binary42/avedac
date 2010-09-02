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

public class AVEDClassifierLibraryJNITestCollectTests extends TestCase {

    public AVEDClassifierLibraryJNITestCollectTests(String name) {
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

        System.out.println("initialize library");
        String lcOSName = System.getProperty("os.name").toLowerCase();
        
        // If running from Mac
        if (lcOSName.startsWith("mac os x")) {
            app.initLib(logfile, 1);
        } else {
            app.initLib(logfile, 0);
        }

        URL testDir = getClass().getResource("2526_Test_Cases/2526_00_47_53_05-events");

        try {
            System.out.println("Running collect");
            String killFile = dbRoot + "collecttest";

            /* Run test image collection on this directory only */
            app.collect_tests(killFile, testDir.getFile(), dbRoot, ColorSpace.GRAY);

            System.out.println("Collect finished");

        } catch (Exception e) {
            System.out.println("Exception" + e);
        }

        app.closeLib();
        System.exit(0);
    }
}
