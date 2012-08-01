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

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.jdesktop.swingworker.SwingWorker;
import org.mbari.aved.classifier.ClassifierLibraryJNI;
import org.mbari.aved.classifier.ColorSpace;

public class AVEDClassifierLibraryTestKill extends TestCase {

    public AVEDClassifierLibraryTestKill(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testAVEDClassifierLibraryJNI() throws Exception {
        final String dbRoot = System.getProperty("user.home");
        final String logfile = System.getProperty("user.home") + "/matlablog.txt";

        final String killFile = dbRoot + "/testkill";

        ClassifierLibraryJNI app = new ClassifierLibraryJNI();

        System.out.println("initialize library");
        String lcOSName = System.getProperty("os.name").toLowerCase();
        
        // If running from Mac
        if (lcOSName.startsWith("mac os x")) {
            app.initLib(logfile, 1);
        } else {
            app.initLib(logfile, 0);
        }

        SimulateTask test = new SimulateTask(killFile, dbRoot, app);
        test.execute();

        Thread.sleep(1000);

        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(AVEDClassifierLibraryTestKill.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.out.println("Killing " + killFile);
        app.set_kill(killFile, 1);

        app.closeLib();
        System.exit(0);
    }

    class SimulateTask extends SwingWorker<Void, Void> {

        private final ClassifierLibraryJNI library;
        private final String killFile;
        private final String dbRoot;

        public SimulateTask(String killFile, String dbRoot, ClassifierLibraryJNI library) throws Exception {
            this.library = library;
            this.killFile = killFile;
            this.dbRoot = dbRoot;
        }
        /*
         * Main task. Executed in background thread.
         */

        @Override
        public Void doInBackground() {
            URL squaredFlatImageUrl = getClass().getResource("2526_Training_Classes/flat");

            try {
                System.out.println("Running collect class");

                library.collect_class(killFile, squaredFlatImageUrl.getFile(),
                        squaredFlatImageUrl.getFile(),
                        "flat", dbRoot,
                        "flatfish",
                        "Test flatfish class",
                        ColorSpace.GRAY);

                System.out.println("Collect class finished");

            } catch (Exception e) {
                System.out.println("Exception" + e);
            }

            return null;
        }

        /*
         * Executed in event dispatch thread
         */
        public void done() {
            System.out.println("Done!\n");
            System.exit(0);
        }
    }
}
