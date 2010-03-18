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
import java.io.File;
import junit.framework.*;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import java.net.URL;
import org.mbari.aved.classifier.ColorSpace;

public class AVEDClassifierLibraryJNITestClass extends TestCase {

    public AVEDClassifierLibraryJNITestClass(String name) {
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
        app.initLib(logfile);

        try { 

            URL squaredFlatImageUrl = getClass().getResource("2526_Training_Classes/flat");
            URL squaredRathImageUrl = getClass().getResource("2526_Training_Classes/rath");
            String killFile = dbRoot + "testcollect";   
  
            System.out.println("Running collect class");
              
            // Now run image collection on this set - this should be run anytime 
            // images are added to the class Library
            app.collect_class(killFile,
                    squaredFlatImageUrl.getFile(), squaredFlatImageUrl.getFile(),
            "flat", dbRoot,
            "flatfish",
            "Test flatfish class",
            ColorSpace.GRAY);
            
            app.collect_class(killFile,
                    squaredRathImageUrl.getFile(), squaredRathImageUrl.getFile(),
            "rath", dbRoot,
            "Rathbunaster-Californicus",
            "Test Rathbunaster-Californicus class",
            ColorSpace.GRAY);

            System.out.println("Collect class finished");
            
            String trainingSet = "flat,rath";
            String trainingAlias = "BenthicTest"; 
            
            System.out.println("Running train class");
            // Train classifier with training class set - this will 
            // build the training library if it isn't already built
            // this takes a while to run the first time, so be patient
            app.train_classes(killFile, trainingSet, trainingAlias, dbRoot,
                    "Test benthic training class");

            System.out.println("Train class finished");

            File dir = new File(squaredFlatImageUrl.getFile());
            int numTestImages;

            // Test collection is 10% of the images
            if (dir.list().length > 10) {
                numTestImages = (int) (0.10 * (dir.list().length) + 0.5);
            } else {
                numTestImages = dir.list().length;
            }
            int[] classIndex = new int[numTestImages];
            float[] storeProbability = new float[numTestImages];
            String[] eventFilenames = new String[numTestImages];
            float minProbThreshold = 0.8f;
            String testClassName = "flat";

            System.out.println("Running test class");
            app.test_class(killFile,
                    eventFilenames, 
                    classIndex, 
                    storeProbability,
                    testClassName, 
                    trainingAlias, 
                    minProbThreshold, 
                    dbRoot);

            System.out.println("Testing against training classes:" + trainingSet +
                    "\t with  minimum probability:" + minProbThreshold);

            if (classIndex != null && storeProbability != null) {
                for (int i = 0; i < numTestImages; i++) {
                    System.out.println("filename:" + eventFilenames[i] +
                            "\tclassindex:" + classIndex[i] + "\tprobability in class:" + storeProbability[i]);
                }
            }

        } catch (Exception e) {
            System.out.println("Exception" + e);
        }

        app.closeLib();
        System.exit(0);
    }
}
