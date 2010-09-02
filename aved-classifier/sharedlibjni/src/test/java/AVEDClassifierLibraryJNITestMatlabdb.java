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
import org.mbari.aved.classifier.ClassModel;
import org.mbari.aved.classifier.TrainingModel;

public class AVEDClassifierLibraryJNITestMatlabdb extends TestCase {

    public AVEDClassifierLibraryJNITestMatlabdb(String name) {
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

        try {
            System.out.println("Getting collected classes in " + dbRoot);
            ClassModel[] collectedClasses = app.get_collected_classes(dbRoot);

            System.out.println("Getting training classes in " + dbRoot);
            TrainingModel[] trainingClasses = app.get_training_classes(dbRoot);

            if (trainingClasses != null) {
                for (int i = 0; i < trainingClasses.length; i++) {
                    System.out.println("Training classes: \n" + trainingClasses[i].toString());
                }
            } else {
                System.out.println("No training classes found in " + dbRoot);
            }
            if (collectedClasses != null) {
                for (int i = 0; i < collectedClasses.length; i++) {
                    System.out.println("Collected class \n" + i + ": \n"
                            + collectedClasses[i].toString());
                }
            } else {
                System.out.println("No collected classes found in " + dbRoot);
            }


        } catch (Exception e) {
            System.out.println("Exception" + e);
        }
        app.closeLib();
        System.exit(0);
    }
}
