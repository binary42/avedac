package org.mbari.aved.mbarivision.api;

import java.io.File;
import java.io.IOException;

public class TestAVED {
	
	public static void main (String[] argv) {

                TestAVED t = new TestAVED();
                
		File movie = new File ("/mnt/tempbox/Jerome/benoit/Neptune-2006-03-30_1730.mp2");
		try {

			TranscodeProcess transcoder = new TranscodeProcess(movie);
			transcoder.setOutTemporaryStorage("/RAID/jerome");//"/home/jerome/aved/movie2process");
			transcoder.run();
			/* Get the video from the TranscodProcess */
			AvedVideo my_video = transcoder.getOutAVEDVideo();
			/* Creation of the AVEDProcess */
			AvedProcess aved = new AvedProcess(my_video);
			/* Set some parameters */
			aved.getMbarivisionOptions().cacheSize = 20;
			/* Start the process */
			aved.run();

		} catch (IOException e) {
			e.printStackTrace();
		}  catch (AvedRuntimeException e) {
			System.out.println(e.getMessage());
		}

	}
	
}
