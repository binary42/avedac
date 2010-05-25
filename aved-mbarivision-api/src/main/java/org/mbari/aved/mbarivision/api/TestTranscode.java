package org.mbari.aved.mbarivision.api;

import java.io.File;
import java.io.IOException; 
import java.util.logging.Level;
import java.util.logging.Logger;

/**t
 * This class demonstrates the TranscodeProcess class
 * It will transcode the file into individual frames
 */
public class TestTranscode {

    public static void main(String[] argv) {

        // execute the command 
        //File movie = new File("test-video/09282006_140048.mov");
        //File movie = new File ("test-video/2344_00_32_40_25.avi");
        //File movie = new File("/Users/dcline/aved/ui/video/processedresults/20090121T221225Z.avi");        
        //File movie = new File("/Users/dcline/aved/ui/video/processedresults/2344_00_32_40_25.mov");
        //File movie = new File("/Users/dcline/aved/ui/video/processedresults/20060808T000000_4912_complete.tar");
        File movie = new File("/Users/dcline/aved/ui/video/processedresults/20060808T000000_4912_32_103.tar.gz");
        TestTranscode t = new TestTranscode();

        try {
            TranscodeProcess transcoder = new TranscodeProcess(movie);
            transcoder.setPrintStream(System.out);
            transcoder.setOutTemporaryStorage("/Users/dcline/Desktop/test");
            transcoder.run(); 
            try {

                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestTranscode.class.getName()).log(Level.SEVERE, null, ex);
            }
            transcoder.clean();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (AvedRuntimeException e) {
            e.printStackTrace();
        }
    }
}

