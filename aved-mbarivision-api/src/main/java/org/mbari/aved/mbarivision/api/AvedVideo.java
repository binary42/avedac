/*
 * @(#)AvedVideo.java
 * 
 * Copyright 2013 MBARI
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


/**
 * AvedVideo is the AVED internal video class, it contains the name of the video,
 * the path to the pnm folder and the number of frame in the video
 */
public class AvedVideo {
	
	private String name;
	private int numFrames;
	private File imageDir;
        private int numDigits = 6; 
        
        //default number of digits in the frame number
        // this is determined by the transcode binary
        private String fileStem = "f";
        private String fileExt = "ppm";
	
	/**
	 * The constructor
	 * @param name the name of the video
	 * @param numFrames the number of frame contained into the video
	 * @param imageDir the directory where the images are stored
	 */
	public AvedVideo (String name, int numFrames, File imageDir) {
		this.name = name;
		this.numFrames = numFrames;
		this.imageDir = imageDir;
	}
         /**
	 * Sets the name of the file extension of the frames
         * within the video
	 * @param n the name of the file extension (without the ".")
	 */
	public void setFileExt (String n) { this.fileExt = n; }
	 /**
	 * Sets the name of the file extension of the frames
         * within the video
	 * @param n the name of the file extension (without the ".")
	 */
	public void setFileStem (String s) { this.fileStem = s; }
	/**
	 * Sets the name of the video
	 * @param n the name of the video
	 */
	public void setName (String n) { this.name = n; }
        /**
	 * Sets the number of digits each frame is indexed with
         * If not set, this defaults to 6 which is the transcode
         * default
	 * @param n number of digits
	 */
	public void setNbDigits (int n) { this.numDigits = n; }     
	/**
	 * Sets the number of frame contained into the video
	 * @param n number of frame in the video
	 */
	public void setNbFrame (int n) { this.numFrames = n; }        
	/**
	 * Returns the name of the video output
	 * @param n number of the frame in the video
	 */
	public File getFrameName (int n) { 
            String framename = String.format("%s/%s" + "%0" + numDigits + "d.%s",
                    imageDir,
                    fileStem, n, fileExt);                     
                   return new File(framename);
        }
	/**
	 * Sets the folder where the imageDir are stored
	 * @param f the imageDir folder, AVED read only imageDir folder
	 */
	public void setOutputDirectory (File f) { this.imageDir = f; }
	/**
	 * Gets the name of the video
	 * @return the name of the video
	 */
	public String getName () { return(name); }
	/**
	 * Gets the number of frame contained into the video
	 * @return the number of frame contained into the video
	 */
	public int getNbFrame () { return(numFrames); }
	/**
	 * Gets the Folder where imageDir are stored
	 * @return the Folder where the imageDir are stored
	 */
	public File getOutputDirectory () { return(imageDir); }
         /**
	 * @return n the name of the file extension of the frames (without the ".")
	 */
	public String getFileExt () { return this.fileExt; }
	 /** Returns the name of the file extension of the frames
         * this is typically f, but can be custom for still
         * frame, time-lapse video sequences that can be 
         * packaged in a tar in a custom format
         * @return file stem  
         */
        public String getFileStem() { return this.fileStem; }
	
}
