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

public class AVEDClassifierLibraryJNITestTrain extends TestCase {

    public AVEDClassifierLibraryJNITestTrain(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAVEDClassifierLibraryJNI() throws Exception {
        int i;
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

        URL squaredFlatImageUrl = getClass().getResource("2526_Training_Classes/flat");
        URL squaredRathImageUrl = getClass().getResource("2526_Training_Classes/rath");

        /*URL flatUrl = getClass().getResource("2526_Training_Classes/flat");

        File flatDir = new File(flatUrl.getFile());
        File[] flatImages = flatDir.listFiles();
        LibraryImage[] flatClassImages = new LibraryImage[flatImages.length];
        for (i = 0; i < flatImages.length; i++) {
        flatClassImages[i] = new LibraryImage(flatImages[i].getAbsolutePath());
        }
        URL rathUrl = getClass().getResource("2526_Training_Classes/rath");
        File rathDir = new File(rathUrl.getFile());
        File[] rathImages = rathDir.listFiles();
        LibraryImage[] rathClassImages = new LibraryImage[rathImages.length];
        for (i = 0; i < rathImages.length; i++) {
        rathClassImages[i] = new LibraryImage(rathImages[i].getAbsolutePath());
        }*/
        try {
            String killFile = dbRoot + "testtrain";

            // Now run image collection on this set - this should be run anytime images are added to the class Library
            app.collect_class(killFile,
                    squaredFlatImageUrl.getFile(),
                    squaredFlatImageUrl.getFile(),
                    "flat", dbRoot,
                    "flatfish",
                    "Test flatfish class",
                    ColorSpace.GRAY);

            app.collect_class(killFile,
                    squaredRathImageUrl.getFile(),
                    squaredRathImageUrl.getFile(),
                    "rath", dbRoot,
                    "Rathbunaster-Californicus",
                    "Test Rathbunaster-Californicus class",
                    ColorSpace.GRAY);

            String trainingset = "flat,rath";
            String trainingalias = "BenthicTest";

            // Train classifier with training class set - this will 
            // build the training set if it isn't already built
            // this takes a while to run the first time, so be patient
            app.train_classes(killFile,
                    trainingset,
                    trainingalias,
                    dbRoot, ColorSpace.GRAY, "Test benthic training class");
        } catch (Exception e) {
            System.out.println("Exception" + e);
        }

        app.closeLib();
        System.exit(0);
    }
}
