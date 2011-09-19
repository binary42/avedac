/*
 * @(#)TestAVED.java
 * 
 * Copyright 2011 MBARI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
