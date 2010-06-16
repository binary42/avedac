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
import java.io.FilenameFilter;
import junit.framework.*;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.ColorSpace;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AVEDClassifierLibraryJNITestRunTest extends TestCase {

    private String[] imageExtensions = {"ppm", "jpg", "jpeg"};

    public AVEDClassifierLibraryJNITestRunTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public List getFileList(File directory) {
        String[] files = directory.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                String extension = name.substring(name.lastIndexOf('.') + 1);
                for (int i = 0; i < imageExtensions.length; i++) {
                    if (extension.equalsIgnoreCase(imageExtensions[i])) {
                        return true;
                    }
                }
                return false;
            }
        });

        //All files should have some event number that we need to sort on.
        Arrays.sort(files, new Comparator() {

            public int compare(Object o1, Object o2) {
                String file1 = (String) o1;
                String file2 = (String) o2;
                int index1 = file1.indexOf("evt");
                int index2 = file2.indexOf("evt");
                if (index1 == -1 || index2 == -1) {
                    System.err.println("ERROR: File does conform " +
                            "to naming specification, no evt##### found.");
                    System.err.println("Offending file: " + file1 + " or " + file2);
                    return file1.compareTo(file2);
                }
                String event1 = file1.substring(index1, index1 + 8);
                String event2 = file2.substring(index2, index2 + 8);
                return event1.compareTo(event2);
            }
        });
        return new ArrayList(Arrays.asList(files));
    }

    public void testAVEDClassifierLibraryJNI() throws Exception {
        String dbRoot = System.getProperty("user.home");
        String logfile = System.getProperty("user.home") + "/matlablog.txt";
	ClassifierLibraryJNI app = new ClassifierLibraryJNI();
        
        System.out.println("initialize library");
        app.initLib(logfile);

        try {
            
            URL squaredFlatImageUrl = getClass().getResource("2526_Training_Classes/flat");
	    if(squaredFlatImageUrl == null)
		System.out.println("Null");          
        System.out.println(squaredFlatImageUrl.toString());
            URL squaredRathImageUrl = getClass().getResource("2526_Training_Classes/rath");  
	    if(squaredRathImageUrl == null)
        System.out.println(squaredRathImageUrl.toString());
            
            String killFile = dbRoot + "runtest";
            
            // Now run image collection on this set - 
            // this should be run anytime images are added to the class Library
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

            String trainingClasses = "flat,rath";
            String trainingAlias = "BenthicTest";

            // Train classifier with these classes - this will 
            // build the training library if it isn't already built
            // this takes a while to run, so be patient
            app.train_classes(killFile,
                    trainingClasses, 
                    trainingAlias, 
                    dbRoot, ColorSpace.GRAY, 
                    "Test benthic training class");

            // Test  - this section tests images against the created  training library  
            URL testDir = getClass().getResource("2526_Test_Cases/2526_00_47_53_05-events");

            System.out.println("Running collect tests");

            // Run test image collection on this directory
            app.collect_tests(killFile,
                    testDir.getFile(), 
                    dbRoot, ColorSpace.GRAY);

            File f = new File(testDir.getFile());
            
            // The test class name is simply the  last name in the test 
            // directory pathname's name sequence. 
            String testClassName = f.getName();

            System.out.println("Collect tests finished");

            int numEvents = 4;
            int[] majoritywinnerindex = new int[numEvents];
            int[] probabilitywinnerindex = new int[numEvents];
            float[] probability = new float[numEvents];
            String[] eventids = new String[numEvents];
            float minprobthreshold = 0.8f;

            System.out.println("Running classifier");            
            System.out.println("Test class: " + testClassName + 
                                "\tTraining classes: " + trainingAlias);
            app.run_test(killFile,
                    eventids, 
                    majoritywinnerindex, 
                    probabilitywinnerindex,
                    probability, 
                    testClassName, 
                    trainingAlias, 
                    minprobthreshold, 
                    dbRoot, ColorSpace.GRAY);

            for (int i = 0; i < numEvents; i++) {
                System.out.println("event:" + eventids[i] 
                        + "\tmin prob:" + minprobthreshold 
                        + "\tmajoritywinnerclassindex:" 
                        + majoritywinnerindex[i] 
                        + "\tprobabilitywinnerindex:"
                        + probabilitywinnerindex[i] 
                        + "\tprobability in class:" 
                        + probability[i]);
            } 
             
        } catch (Exception e) {
            System.out.println("Exception" + e);
        }

        app.closeLib();
        System.exit(0);
    }
}
