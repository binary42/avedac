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
package org.mbari.aved.ui.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import org.apache.commons.io.IOUtils;
import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationController;
import org.mbari.aved.ui.userpreferences.UserPreferences;

/**
 *
 * @author dcline
 */
public class VideoUtils {

    /**
     * Utility to download files from a URL. This assumes no user or password is
     * required to copy from the URL
     *
     * @param url URL to download from
     * @param target file to copy to
     */
    public static void download(URL url, File target) throws Exception {
        try {
            Application.getView().setBusyCursor();

            // create/overwrite target
            target.createNewFile();

            // and download
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            OutputStream out = new FileOutputStream(target);

            IOUtils.copy(in, out);
            in.close();
            out.flush();
            out.close();
            Application.getView().setDefaultCursor();
        } catch (Exception e) {

            // if can't find a file, delete empty file, display message,
            // return and don't transcode.
            target.delete();

            String message = "Error downloading " + ((url != null)
                    ? url.toString()
                    : "[null]") + "\nException: " + e.toString();

            throw new Exception(message);
        }
    }

    /**
     * Helper function to import still image archive/video files to associate with a XML file
     *
     * @param source The image source URL associated with the xmlfile
     * @param xmlfile The xmlfile 
     *
     * @returns the URL to the video source file it found or null if none found
     */
    public static URL searchImageSource(File xmlfile, URL source) {

        // Convert input source associated with the XML to frames
        // and use these frames to start the image loader to
        // extract images of the events

        if (source != null) {
            try {
                boolean isValidUrl = URLUtils.isValidURL(source);
                if (isValidUrl) {

                    // If the source is a valid URL
                    return source;
                } else if (!isValidUrl) {

                    // If the video source is an invalid URL, alert user and search for
                    // a source in the same directory as the xmlfile
                    // File f = new File(UserPreferences.getModel().getImportVideoDir().toString());
                    File f = new File(xmlfile.toString()); 
                    File file;
                    
                    if ((file = searchForClip(xmlfile, ".avi")) != null
                            || (file = searchForClip(xmlfile, ".avi", f)) != null
                            || (file = searchForClip(xmlfile, ".mov")) != null
                            || (file = searchForClip(xmlfile, ".mov", f)) != null
                            || (file = searchForClip(xmlfile, ".tar")) != null
                            || (file = searchForClip(xmlfile, ".tar", f)) != null
                            || (file = searchForClip(xmlfile, ".tar.gz")) != null
                            || (file = searchForClip(xmlfile, ".tar.gz", f)) != null) {
                        if (file != null) {
                            Logger.getLogger(ApplicationController.class.getName()).log(Level.INFO, null, "Found alternative video source " + file.toString());
                            return file.toURL();
                        }

                    }
                } else if (URLUtils.isFileUrl(source.toString())) {

                    // if not a URL, simply use the original file
                    return source;
                }
            } catch (Exception ex) {
                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);

                return null;
            }
        }

        return null;
    }

    /**
     * Searches for clip to associate with XML results file name, appended with
     * ext in the same directory as the XML results
     *
     * @return file with clip if it's found
     */
    public static File searchForClip(File xmlFile, String ext, File dir) {
        if ((xmlFile != null) && (ext != null) && (dir != null)) {
            File clip = null;

            try {

                // Parse XML file and create events mpeg based on same name,
                // e.g. parse root out of "1234.events.XML" to create
                // 1234.results.mpeg
                clip = new File(dir.toString() + "/" + ParseUtils.removeFileExtension(xmlFile.getName()) + ext);

                // System.out.println("Searching for clip: " + clip.toString());
                // If file exists in the same path as the XML file, assumes this
                // is correct,
                // otherwise, give dialog box indicating results not found
                if (clip.exists()) {
                    return clip;
                } else {
                    return null;
                }
            } catch (Exception ex) {
                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);

                return clip;
            }
        }

        return null;
    }

    /**
     * Searches for clip to associate with XML results root file name in the
     * same directory as the XML results
     *
     * @return file name or null if not found
     */
    public static File searchForClip(File xmlFile, String ext) {
        if ((xmlFile != null) && (ext != null)) {
            File clip = null;

            try {

                // Parse XML file and create events mpeg based on same name,
                // e.g. parse root out of "1234.events.XML" to create
                // 1234.results.mpeg
                clip = new File(ParseUtils.removeFileExtension(xmlFile.getAbsolutePath()) + ext);

                // System.out.println("Searching for clip: " + clip.toString());
                // If file exists in the same path as the XML file, assumes this
                // is correct,
                // otherwise, give dialog box indicating results not found
                if (clip.exists()) {
                    return clip;
                } else {
                    return null;
                }
            } catch (Exception ex) {
                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);

                return clip;
            }
        }

        return null;
    }

    /**
     * Browse for video clip, or still image archive starting in last imported directory
     */
    public static File browseForImageSource(File setCurrentDirectory) throws Exception {
        File f = null;

        // Add a custom file filter and disable the default
        JFileChooser chooser = new JFileChooser();

        chooser.addChoosableFileFilter(new ImageFileFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setDialogTitle("Choose Still Image/Video Source File");
        chooser.setCurrentDirectory(setCurrentDirectory);

        if (chooser.showOpenDialog(Application.getView()) == JFileChooser.APPROVE_OPTION) {
            f = chooser.getSelectedFile();
            UserPreferences.getModel().setImportVideoDir(f.getParent());
        } else {

            // TODO: print dialog message box with something meaningful here
            System.out.println("No Selection ");

            return null;
        }

        return f;
    }
}
