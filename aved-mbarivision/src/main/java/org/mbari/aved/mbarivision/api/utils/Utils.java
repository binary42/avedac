package org.mbari.aved.mbarivision.api.utils;

import java.io.File;

/**
 * Utils is a class where usefull function are implemented
 */
public class Utils {

    /**
     * Gets the name without the extension of a File
     * @param f the file
     * @return the name of the file without its extension
     */
    public static String getNameWithoutExtension(File f) {

        String filename = f.getName();
        String choppedFilename;
        // Where the last dot is. There may be more than one.
        int dotPlace = filename.lastIndexOf('.');
        if (dotPlace >= 0) {
            choppedFilename = filename.substring(0, dotPlace);
        } else {
            choppedFilename = filename;
        }
        return choppedFilename;
    }

    /**
     * Gets the extension of a File
     * @param f the file
     * @return return the extension of the file
     */
    public static String getExtension(File f) {

        String filename = f.getName();
        String ext;
        // Where the last dot is. There may be more than one.
        int dotPlace = filename.lastIndexOf('.');
        if (dotPlace >= 0) {
            ext = filename.substring(dotPlace + 1);
        } else {
            ext = "";
        }
        return ext;
    }
    
    /**
     * Frame number from 29.97 frame/s drop frame timecode, assume 0 frames
     * Usage: calculate2997dropframes(hours,minutes,seconds)
     */
    public static int calculate2997dropframes(int hours, int minutes, int seconds) {
        int frame = 0; //is null as a placeholder in case need to add in frame count later
        int total_minutes = 60 * hours + minutes;
        int b = (total_minutes - total_minutes / 10) * 2;
        return (108000 * hours + 1800 * minutes + 30 * seconds + frame) - b;
    }

    /**
     * Frame number from timecode, assume 0 frames
     * Usage: calculateframes(hours,minutes,seconds)
     */
    public static int calculateframes(float rate, int hours, int minutes, int seconds) {
        int frame = 0; //is null as a placeholder in case need to add in frame count later
        return (int) ((3600 * hours + 60 * minutes + seconds) * rate + frame);
    }

    /**
     * Determines what method to use to convert timecode to a counter based on frame rate
     */
    public static int timecode2counter(float rate, String tc) throws Exception {

        int dropframes = 0;
        try {
            String h = tc.substring(1, 2);
            int hours = Integer.parseInt(h);
            String m = tc.substring(3, 4);
            int minutes = Integer.parseInt(m);
            String s = tc.substring(5, 6);
            int seconds = Integer.parseInt(s);
            if (rate == 29.97) {
                dropframes = calculate2997dropframes(hours, minutes, seconds);
            } else {
                dropframes = calculateframes(rate, hours, minutes, seconds);
            }
        } catch (NumberFormatException ex) {
            throw new Exception(ex.toString());
        }
        return dropframes;

    }
 
    /**
     * Deletes all files and subdirectories under dir.
     * @param dir
     * @return <code>true</code> if all deletions were successful.
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}
