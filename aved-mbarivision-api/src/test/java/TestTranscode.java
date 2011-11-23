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
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.mbari.aved.mbarivision.api.AvedRuntimeException;
import org.mbari.aved.mbarivision.api.TranscodeProcess;

public class TestTranscode extends TestCase {

    public TestTranscode(String name) {
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

    public final void testTranscode() throws Exception { 

        final URL clipname = getClass().getResource("/transcodetest.avi");

        final File movie = new File(clipname.getFile());

        try {
            TranscodeProcess transcoder = new TranscodeProcess(movie);
            transcoder.setPrintStream(System.out);
            transcoder.setOutTemporaryStorage("/tmp");
	    transcoder.setTranscodeOpts("-c 0-10 ");
            transcoder.run();
            try {

                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestTranscode.class.getName()).log(Level.SEVERE, null, ex);
            }
            transcoder.clean();

        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception(e);
        } catch (AvedRuntimeException e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }
}
