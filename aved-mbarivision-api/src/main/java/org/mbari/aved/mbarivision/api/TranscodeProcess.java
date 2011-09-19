/*
 * @(#)TranscodeProcess.java
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mbari.aved.mbarivision.api.utils.ExtendedVector;
import org.mbari.aved.mbarivision.api.utils.StreamGobbler;
import org.mbari.aved.mbarivision.api.utils.Utils;

/**
 * Requires transcode set on the machine
 * TranscodeProcess take a File (or whatever is supported by the transcode command)
 * and give a AvedVideo object as result. This object can then be processed by AVEDProcess
 */
public class TranscodeProcess extends Thread {

    public static String DEFAULT_OUTPUT_PATH = "./";
    public static String FILESTEM = "f";
    private AvedVideo outAvedVideo ;
    private File inVideoFile;
    private boolean isInitialized = false;
    private boolean isVideoFileValid = false;
    private OutputStream printStream = System.out;
    private boolean isRunning = false;
    private String transcodeCmd = null;
    private String transcodeOpts= "";
    private Process process ;
    /** Environmental parameters to use in Runtime calls*/
    private String envParams[] = null;
    private String[] validImageExtensions = {"ppm", "jpg", "gif", "png", "pnm"};

 
    /** Helper class to interrupt processes that take too long to run*/
    private class InterruptScheduler extends TimerTask {

        Thread target;

        public InterruptScheduler(Thread target) {
            this.target = target;
        }

        @Override
        public void run() {
            target.interrupt();
        }
    }

    /** Helper class to update the output of the transcode process 
    while it is running */
    private class UpdateOutputScheduler extends TimerTask {

        TranscodeProcess process = null;

        public UpdateOutputScheduler(TranscodeProcess process) {
            this.process = process;
        }
 
        @Override
        public void run() throws AvedRuntimeException {
            String ext = Utils.getExtension(process.getInVideoFile()); 
                File directory = process.getOutTemporaryStorage();
                if (directory.isDirectory()) {
                    String[] files = directory.list(new FilenameFilter() {

                        public boolean accept(File dir, String name) {
                            String extension = name.substring(name.lastIndexOf('.') + 1);
                            for (int i = 0; i < validImageExtensions.length; i++) {
                                if (extension.equalsIgnoreCase(validImageExtensions[i])) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });

                    if (files.length > 0) {
                        // Get the first file in the list and set the file extension 
                        File f = new File(files[0]);
                        outAvedVideo.setFileExt(Utils.getExtension(f));

                        /** Find the file stem, this should be a alpha character
                        or combination of dashes and dots. TODO:  if the
                        frames are formatted with a number stem, 
                        e.g. 4912_000001.ppm, 4912_0000002.ppm this fails */
                        Pattern pattern = Pattern.compile("[a-zA-Z.%+-]*");
                        Matcher matcher = pattern.matcher(f.toString());

                        if (matcher.find()) {
                            /*String out = String.format("I found the text \"%s\" starting at " +
                            "index %d and ending at index %d.%n",
                            matcher.group(), matcher.start(), matcher.end());
                            System.out.println(out);*/

                            outAvedVideo.setFileStem(matcher.group());
                        } else {
                            throw new AvedRuntimeException("Error finding the file stem for " + f.toString());
                        }

                        // Find the number of digits the frame is formatted to, e.g.
                        // f000001.ppm is 6 digits.  
                        Pattern pattern2 = Pattern.compile("([0-9]+)");
                        Matcher matcher2 = pattern2.matcher(f.toString());

                        if (matcher2.find()) {
                            /*String out2 = String.format("I found the text \"%s\" starting at " +
                            "index %d and ending at index %d.%n",
                            matcher2.group(), matcher2.start(), matcher2.end());
                            System.out.println(out2);*/
                            int l = matcher2.group().length();
                            outAvedVideo.setNbDigits(l);
                        } else {
                            throw new AvedRuntimeException("Error finding the number of digits " 
                                    + f.toString() + " is formatted to");
                        }
                         
                        isInitialized = true;
                    }
                } 
 
            return;
        }
    } 

    /**
     * Constructor of the TranscodeProcess
     * @param movie the file to transcode
     * @throws IOException if the specified file doesn't exist
     */
    public TranscodeProcess(File movie) throws IOException { 
        inVideoFile = movie;
        outAvedVideo = null;
        if (!this.getInVideoFile().isFile()) {
            this.isVideoFileValid = false;
            throw new IOException("Video clip not found at " + this.getInVideoFile().toString());
        } else {
            String name = Utils.getNameWithoutExtension(getInVideoFile());
            int nbFrame = 0;
            File outputStorage = new File(TranscodeProcess.DEFAULT_OUTPUT_PATH + name + File.separator);
            outAvedVideo = new AvedVideo(movie.toString(), nbFrame, outputStorage);
            isVideoFileValid = true;
        }

        // Get the system environment variables
        Map<String, String> variables = System.getenv();

        envParams = new String[variables.size()];
        int i = 0;     // index into the next empty array element        


        for (Map.Entry entry : variables.entrySet()) {
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();

            // Add a few paths because the default transcode
            // installation on a mac doesn't change the system-wide PATH
            // environment variable by default and gets installed in /opt/local/bin
            // add in /usr/bin in and /usr/local in case it is missing
            if (name.equals("PATH")) {
                envParams[i++] = name + "=" + value + ":" + "/opt/local/bin:/usr/bin:/usr/local/bin:";
            } else {

                envParams[i++] = name + "=" + value;
            }
        }
    }

    /** 
     * Removes output video generated from the transcode process
     */
    public void clean() throws AvedRuntimeException {
        if (this.isVideoFileValid() && this.getOutAVEDVideo() != null && this.getInVideoFile() != null) {

            try {
                if (isRunning) {
                    kill();
                }
                Utils.deleteDir(getOutAVEDVideo().getOutputDirectory());

            } catch (ThreadDeath d) {
                throw new AvedRuntimeException("The Transcode clean thread has died");
            }
        }
    }

    /**
     * Sets the path where the AvedVideo will be stored
     * @param p the path where the AvedVideo will be stored
     */
    public void setOutTemporaryStorage(String p) {
        if (this.isVideoFileValid()) {
            getOutAVEDVideo().setOutputDirectory(new File(p));
        }
    }

    /**
     * Returns the path where AvedVideo is stored
     */
    public File getOutTemporaryStorage() {
        if (this.isVideoFileValid() && getOutAVEDVideo() != null) {
            return getOutAVEDVideo().getOutputDirectory();
        }
        return null;
    }

    /** Sets the print stream where the output of this process will be displayed */
    public void setPrintStream(OutputStream ps) {
        if (ps == null) {
            ps = System.out;
        }
        printStream = ps;
    }

    /**
     * Gets if the video file is valid. Valid files must exist and be a file, not a
     * directory.
     * @return if the video is valid
     */
    public boolean isVideoFileValid() {
        return (this.isVideoFileValid == true);
    }

    /**
     * Gets the number of frame transcoded
     * @return the number of frame transcoded
     */
    private int getNbFramesTranscoded() {

        if (this.isVideoFileValid()) {
            ExtensionFilter filter = new ExtensionFilter("." + outAvedVideo.getFileExt());
            File dir = getOutAVEDVideo().getOutputDirectory();
            String[] list = dir.list(filter);
            return list.length;

        } else {
            return 0;
        }
    }

    /**
     * 
     * @return true if the process is initialized
     */
    public boolean isInitialized() {
        return this.isInitialized;
    }
    /**
     * 
     * @return true if the process is running
     */
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * Sets the command to runTranscode the transcoder
     */
    public void setTranscodeCommand(String command) {
        this.transcodeCmd = command;
    }


    /**
     * Sets optional transcode arguments to pass to the transcoder
     */
    public void setTranscodeOpts(String command) {
        this.transcodeOpts = command;
    }

    /**
     * Helper function to return the defined transcode command
     * @return the transcoded command used
     */
    public String getTranscodeCmd() {
        return this.transcodeCmd;
    }

    /**
     * Runs the transcode process
     */
    @Override
    public void run() throws AvedRuntimeException {

        if (!isRunning && this.isVideoFileValid() && this.getOutAVEDVideo() != null && this.getInVideoFile() != null) {

            // Make the output directory in case it doesn't exist
            getOutAVEDVideo().getOutputDirectory().mkdirs();

            // Get optional transcode command (rarely needed)
            String cmd = getTranscodeCmd();
            
            if (cmd == null) {
                try {
                    isRunning = true;

                    //TODO pass extra args here as 3rd argument
                    runTranscode(this.getInVideoFile(), this.getOutAVEDVideo().getOutputDirectory(), transcodeOpts);

                } catch (IOException e) {
                    isRunning = false;
                    throw new AvedRuntimeException("The Transcode process has been interupted" + e.toString());
                } catch (ThreadDeath d) {
                    isRunning = false;
                    throw new AvedRuntimeException("The Transcode process thread has died");
                } catch (Exception ex) {
                    isRunning = false;
                    throw new AvedRuntimeException(ex.toString());
                }
            } else {
                try {
                    isRunning = true;

                    System.out.println("Executing " + cmd);
                    // execute the command 
                    Process proc = Runtime.getRuntime().exec(cmd, envParams);

                    ExtendedVector line = new ExtendedVector();
                    StreamGobbler errGobbler = new StreamGobbler(proc.getErrorStream(), printStream);
                    // any output?
                    StreamGobbler outGobbler = new StreamGobbler(proc.getInputStream(), printStream, "OUTPUT");
                    outGobbler.setLineVector(line);
                    outGobbler.start();
                    errGobbler.start();
                    proc.waitFor();

                    isRunning = false;

                    if (proc.exitValue() != 0) {
                        throw new AvedRuntimeException("Error when attempting to convert the" +
                                " clip - check the script output for errors");
                    }
                    // Update the number of frames transcoded 
                    this.getOutAVEDVideo().setNbFrame(getNbFramesTranscoded());

                } catch (IOException e) {
                    throw new AvedRuntimeException("The transcode process has has an IO exception" + e.toString());
                } catch (InterruptedException e) {
                    throw new AvedRuntimeException("The transcode process has been interupted" + e.toString());
                } catch (ThreadDeath d) {
                    throw new AvedRuntimeException("The transcode process thread has died");
                } catch (Exception e) {
                    throw new AvedRuntimeException("The trancode process has had a general exception" + e.toString());
                }
            }
        }
    }

    /**
     * Kills the transcode process if it is running
     */
    public void kill() {
        if (process != null) {
            try {
                process.destroy();
                isRunning = false;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method to return transcoded output
     * @return AvedVideo object where output video is contained
     */
    public AvedVideo getOutAVEDVideo() {
        return outAvedVideo;
    }

    /**
     * Helper method to get input video file
     * @return input video file location
     */
    public File getInVideoFile() {
        return inVideoFile;
    }

    /**
     * Inner class to Filter ppms
     */
    class ExtensionFilter implements FilenameFilter {

        private String extension;

        public ExtensionFilter(String extension) {
            this.extension = extension;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(extension));
        }
    }

    /**
     * Cleans up after the transcode process
     */
    void clean(File dir) {
        Utils.deleteDir(dir);
    }

    /**
     * Returns the location of the command with the fully qualified
     * path
     * @param cmd to search for
     * @return String location of cmd or empty string if none found
     */
    String getCmdLoc(String cmd) throws Exception {

        // Get the path environment and convert to string array
        String getpath = System.getenv("PATH");

        // Append /opt/local/bin and /usr/bin as this doesn't always get included
        // in the system path on Mac OS X  
        String finalpath = getpath + ":/opt/local/bin:/usr/bin";
        String p = finalpath.substring(4);

        // Convert colon delimited string to an array
        String[] paths = stringToArray(p);
        long timeout = (long) 10000;

        String path = null;
        int count = 0;
        int exitval = 0;
        for (count = 0; count < paths.length; count++) {
            path = paths[count];
            if (path == null) {
                throw new Exception("Could not find cmd path " + cmd);
            } else {
                try {
                    System.out.println("looking for " + cmd + " in " + path);
                    String fullcmd = path + "/" + cmd;
                    Process proc = Runtime.getRuntime().exec(fullcmd, this.envParams);

                    // Set a timer to interrupt the process if it does not return within the timeout period
                    Timer timer = new Timer();
                    timer.schedule(new InterruptScheduler(Thread.currentThread()), timeout);
                    try {
                        exitval = proc.waitFor();

                        if (exitval != 0) {
                            return path + "/" + cmd;
                        }
                    } catch (InterruptedException e) {
                        // Stop the process from running
                        proc.destroy();
                        if (exitval != 0) {
                            return path + "/" + cmd;
                        } else {
                            throw new Exception(fullcmd + " did not return after " + timeout + " milliseconds");
                        }
                    } finally {
                        // Stop the timer
                        timer.cancel();
                    }


                } catch (IOException ex) {                    // command failed, continue to next command
                } catch (InterruptedException ex) {
                    Logger.getLogger(TranscodeProcess.class.getName()).log(Level.SEVERE, null, ex);
                    throw new Exception(ex.getMessage());
                }
            }
        }
        throw new Exception("Could not find cmd " + cmd);
    }

    /** Put all "words" in a string into an array.
     * 
     * @param wordString
     * @return
     */
    String[] stringToArray(String wordString) {
        String[] result;
        int i = 0;     // index into the next empty array element

        //--- Declare and create a StringTokenizer
        StringTokenizer st = new StringTokenizer(wordString, ":", false);

        //--- Create an array which will hold all the tokens.
        result = new String[st.countTokens()];

        //--- Loop, getting each of the tokens
        while (st.hasMoreTokens()) {
            result[i++] = st.nextToken();
        }

        return result;
    }

    /**
     * Runs the external transcode binary convering the file into individual ppms
     * @param file to transcode 
     * @param transcodeopts extra transcode compatible arguments to pass to the transcoder
     * @return true if transcoded runTranscode without exception or a negative return code 
     * @throws java.lang.Exception 
     */
    public boolean runTranscode(File file, File outputdir, String transcodeopts) throws Exception {

        String extraargs = "";
        String ext = Utils.getExtension(file);
        String filename = file.toString();
        String filestem = Utils.getNameWithoutExtension(file);
        String timecode = null;
        boolean hasISOtimecode = false;
        String outputfileseed = outputdir.toString() + "/f";
        String lcOSName = System.getProperty("os.name").toLowerCase();
        String transcodecmd = null;
        String avidumpcmd = null;
        String cmd = null;
        // Set a timer to update information about the transcoded output after timeout period
        Timer timer = new Timer();

        long timeout = (long) 500; 
        timer.schedule(new UpdateOutputScheduler(this), timeout);
        
        try {

            /**If this is not a tar archive, then assume it's video and we need 
             * to find the transcode executable
             */
            if (!ext.equals("tar") && !ext.equals("gz") && !ext.equals("tgz")) {
                // Get transcode path 
                transcodecmd = getCmdLoc("transcode");

                // only show progress every 100 frames
                if (lcOSName.startsWith("mac")) {
                   transcodecmd = transcodecmd + " --progress_rate 100 ";
                } else { 
                   transcodecmd = transcodecmd + " -q 1 --print_status 100";
                } 
            }
            /** Check if this file has a ISO8601 timecode timestamp and extract
            timestamp from the name. This is a crude test so far that only
            checks for a set of numbers appended with a T*/
            String timecodestr;
            int x = filestem.lastIndexOf("T");
            if (x > 0) {
                if (filestem.contains("Z")) {
                    int j = filestem.lastIndexOf("Z");
                    timecodestr = filestem.substring(x + 1, j);
                } else {
                    timecodestr = filestem.substring(x + 1, x + 7);
                }
                if (timecodestr.length() > 0 && (Integer.parseInt(timecodestr)) > 0) {
                    timecode = timecodestr;
                    hasISOtimecode = true;
                }
            }
            ext.toLowerCase();

            if (ext.equals("avi")) {
                /*tcprobecmd = getCmdLoc("tcprobe");
                if (tcprobecmd.length() != 0) {
                    String tcprobe = new String(tcprobecmd + " -i " + filename);
                    System.out.println("Executing " + tcprobe);
                    // execute the command  
                    Process proc = Runtime.getRuntime().exec(tcprobe, envParams);

                    ExtendedVector line = new ExtendedVector();
                    StreamGobbler errGobbler = new StreamGobbler(proc.getErrorStream(), printStream);
                    // any output?
                    StreamGobbler outGobbler = new StreamGobbler(proc.getInputStream(), printStream, "OUTPUT");
                    outGobbler.setLineVector(line);
                    outGobbler.start();
                    errGobbler.start();
                    proc.waitFor();
                    String s = line.toString();

                    // if have a ISO timecode formatted file, need to calculate the correct framenumber
                    // this only works for the patched version of transcode on our
                    // Linux server
                    if (hasISOtimecode && lcOSName.startsWith("linux")) {
                        //find the floating number between the -f <rate> command sequence,
                        //by finding the indexes between the two delimeters, and 
                        //parsing the resulting substring into a float
                        //e.g. frame rate: -f 29.970 [25.0]
                        int i = s.lastIndexOf("-f");
                        int j = s.indexOf("[", i);
                        if (i > 0 && j > 0) {
                            String r = s.substring(i + 3, j - 2);
                            float rate = Float.parseFloat(r);

                            int framenumber = Utils.timecode2counter(rate, timecode);
                            extraargs = new String("-f " + rate + " " + "-start_timecode " + framenumber);
                        }
                    }
                }*/
                String codec = "Unknown";
                // If have the avidump command, use it to find the codec to better
                // control transcoding
                try {
                    avidumpcmd = getCmdLoc("avidump");
                    String avicmd = avidumpcmd + " " + filename;
                    System.out.println("Executing " + avicmd);
                    // execute the command 
                    Process proc = Runtime.getRuntime().exec(avicmd, envParams);

                    ExtendedVector line = new ExtendedVector();
                    StreamGobbler errGobbler = new StreamGobbler(proc.getErrorStream(), printStream);
                    // any output?
                    StreamGobbler outGobbler = new StreamGobbler(proc.getInputStream(), printStream, "OUTPUT");
                    outGobbler.setLineVector(line);

                    outGobbler.start();
                    errGobbler.start();
                    proc.waitFor();

                    // find the codec
                    String search = "compressor:";
                    for (int i = 0; i < line.size(); i++) {
                        if (line.get(i).toString().contains(search)) {
                            String tmp = line.get(i).toString();
                            int j = tmp.lastIndexOf(search);
                            if (j > 0) {
                                codec = tmp.substring(j + search.length() + 1);
                            }
                        }
                    }

                } catch (Exception ex) {
                }

                if (codec.equals("DX50")) {
                    cmd = transcodecmd + " -i " + filename + " -o " + outputfileseed + " -x ffmpeg,null -y " + outAvedVideo.getFileExt() + ",null " + extraargs + " " + transcodeopts;
                } else if (codec.equals("mpg2")) {
                    cmd = transcodecmd + " -i " + filename + " -o " + outputfileseed + " -x mpeg2,null -y " + outAvedVideo.getFileExt() + ",null " + extraargs + " " + transcodeopts;
                } else {
                    cmd = transcodecmd + " -i " + filename + " -o " + outputfileseed + " -x ffmpeg,null -y " + outAvedVideo.getFileExt() + ",null " + extraargs + " " + transcodeopts;
                }
                outputdir.mkdir();     
                process = Runtime.getRuntime().exec(cmd, envParams);

            } else if (ext.equals("mpeg") || ext.equals("mpg")) {
                cmd = transcodecmd + " -i " + filename + " -x mpeg2,null -y " + outAvedVideo.getFileExt() + ",null -o " + outputfileseed + " " + extraargs + " " + transcodeopts;
                outputdir.mkdir();     
                process = Runtime.getRuntime().exec(cmd, envParams);
            } //Run transcode or scripts needed to untar and convert to ppms
            else if (ext.endsWith("gz") || ext.endsWith("tgz")) {

                // In a shell, format the command to unzip and untar all in one step
                String uncompresscmd = "gunzip -c " + filename + " | " + "tar -x -v -C " + outputdir.toString() + " -f - ";
                String[] cmdarray = {
                    "/bin/sh",
                    "-c",
                    uncompresscmd,
                };              
                outputdir.mkdir();     
                process = Runtime.getRuntime().exec(cmdarray, envParams);
            } else if (ext.endsWith("tar")) {
                cmd = "tar " + " -C " + outputdir.toString() + " -x -v -f " + filename;
                process = Runtime.getRuntime().exec(cmd, envParams);
            } else {
                cmd = transcodecmd + "-i " + filename + " -o " + outputfileseed + " -y " + outAvedVideo.getFileExt() + ",null " + extraargs + " " + transcodeopts;
                outputdir.mkdir();            
                process = Runtime.getRuntime().exec(cmd, envParams);
            }
            // any error messages ?       
            ExtendedVector line = new ExtendedVector();
            StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream(), printStream);
            // any output?
            StreamGobbler outGobbler = new StreamGobbler(process.getInputStream(), printStream, "OUTPUT");
            outGobbler.setLineVector(line);

            outGobbler.start();
            errGobbler.start();

            int exitVal = process.waitFor();
            if (exitVal != 0) {
                timer.cancel();
                throw new AvedRuntimeException("Error running command " + cmd);
            } 
                
            int numRetries = 3;
            for (int i=0; i< numRetries; i++) {            
            if (!isInitialized)
                System.out.println("Sleeping");
                Thread.sleep(timeout + 500);
            } 
            
        } catch (NumberFormatException ex) {
            throw new AvedRuntimeException(ex.toString());
        } catch (Exception ex) {
            throw new AvedRuntimeException(ex.toString());
        } finally {
            timer.cancel();
        }

        return true;
    }
}
