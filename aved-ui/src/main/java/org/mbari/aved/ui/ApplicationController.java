/*
 * @(#)ApplicationController.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

import org.apache.commons.io.IOUtils;

import org.mbari.aved.mbarivision.api.utils.Utils;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.classifier.Classifier;
import org.mbari.aved.ui.message.ModalYesNoDialog;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.message.NonModalYesNoDialog;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventListModel.EventListModelEvent;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.model.SummaryModel.SummaryModelEvent;
import org.mbari.aved.ui.player.PlayerManager;
import org.mbari.aved.ui.table.TableController;
import org.mbari.aved.ui.thumbnail.ThumbnailController;
import org.mbari.aved.ui.thumbnail.ThumbnailView;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel.VideoPlayoutMode;
import org.mbari.aved.ui.utils.ExcelExporter;
import org.mbari.aved.ui.utils.ImageFileFilter;
import org.mbari.aved.ui.utils.ParseUtils;
import org.mbari.aved.ui.utils.URLUtils;
import org.mbari.aved.ui.utils.XmlFileFilter;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.mbari.aved.ui.utils.ProcessedResultsFileFilter;

public final class ApplicationController extends AbstractController implements ModelListener, WindowListener {

    /**
     * True when a save as operation is in progress. This is used
     * to sync the control correctly for looking for the video source
     * and mpeg of the results
     */
    private boolean isSaveAs = false;
 
    /** Classifier */
    private Classifier classifier;

    /**
     * Robust browser utility for launching web page for a given URL. This should
     * provide the same response for most platforms and web browsers (except
     * Mac)
     */
    private BrowserLauncher launcher;

    /** Controls the event table */
    private TableController tableController;

    /** Controls the thumbnail view */
    private ThumbnailController thumbnailController;

    /** Worker to handle transcoding video files */
    private VideoTranscodeWorker transcodeWorker;

    public ApplicationController() throws Exception {            
        setModel(new ApplicationModel());
        setView(new ApplicationView((ApplicationModel) getModel(), this));

        // Register as a window listener to the view
        getView().addWindowListener(this);

        // Register as listener to the model
        getModel().addModelListener(this);
        getModel().addModelListener(PlayerManager.getInstance());

        // Create child controllers for thumbnail and table data displays
        tableController     = new TableController(getModel());
        thumbnailController = new ThumbnailController(getModel());

        // Replace the tabbed panels with the customized ones
        getView().replaceThumbnailPanel(((ThumbnailView) thumbnailController.getView()).getForm());
        getView().replaceTablePanel(tableController.getTable());

        // Create the classifier
        try {
            classifier = new Classifier(getModel());
        } catch (Exception ex) {
            Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);

            return;
        }

        String s = System.getProperty("os.name").toLowerCase();

        if (s.indexOf("mac os x") != -1) {

            // needed on mac os x to display menus in the mac convention
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }  
        
        MainMenu menu = new MainMenu(getModel());

        getView().setJMenuBar(menu.buildJJMenuBar());

        // Initialize mouse listener for tabbed pane
        getView().getTabbedPane().addMouseListener(new MouseClickTabActionHandler());

        // Initialize the summary view mouse listener
        ((ApplicationView) getView()).getSummaryView().addMouseListener(new MouseClickFileActionHandler());

        /*try {

            // TODO: add the AbstractLogger, and ErrorHandler to this launcher,
            // otherwise will simply print stack traces
            launcher = new BrowserLauncher();
            launcher.setNewWindowPolicy(true);
        } catch (RuntimeException ex) {
            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);

            NonModalMessageDialog dialog;

            dialog = new NonModalMessageDialog(getView(), ex.getMessage());
            dialog.setVisible(true);
        } catch (BrowserLaunchingInitializingException e1) {
            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, e1);
        } catch (UnsupportedOperatingSystemException e1) {
            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, e1);
        }
        */ 
 
        getView().setVisible(true); 
    }

    /**
     * Helper function that returns the TableController
     */
    public TableController getTableController() {
        return this.tableController;
    }

    /** Helper function that returns type cast ApplicationModel */
    @Override
    public ApplicationModel getModel() {
        return ((ApplicationModel) super.getModel());
    }

    /** Helper function to type cast the view */
    @Override
    public ApplicationView getView() {
        return (ApplicationView) super.getView();
    }

    /**
     * Shutdown the application gracefully
     */
    public void shutdown() {
        reset();
        getClassifier().shutdown();
        System.exit(0);
    }

    /**
     * Resets the controller, closes all displays and clears
     * out all the data in the model
     */
    public void reset() {
        getView().setBusyCursor();

        // kill the transcode process and cleans files associated
        // with it. This can take a few seconds to run
        if (transcodeWorker != null) {
            transcodeWorker.reset();
            transcodeWorker = null;
        }

        // Clean-up the downloaded and created files and directories
        SummaryModel summary             = getModel().getSummaryModel();
        File         transcodeSourceFile = summary.getTranscodeSource();

        // Delete the transcode source file it is exists and was downloaded
        // to a temporary folder
        File tmpDir = UserPreferences.getModel().getScratchDirectory();

        if ((transcodeSourceFile != null) && transcodeSourceFile.toString().startsWith(tmpDir.toString())
                && transcodeSourceFile.exists() && transcodeSourceFile.canWrite()) {
            transcodeSourceFile.delete();
        }

        File testImageDir = summary.getTestImageDirectory();

        if ((testImageDir != null) && testImageDir.exists() && testImageDir.canWrite()) {
            Utils.deleteDir(testImageDir);
        }

        // Finally, reset the model
        getModel().reset();
        getView().setDefaultCursor();
    }

    /** Exports the model data to XML file user chooses */
    public void saveProcessedResultsAs() {
        try {
            isSaveAs = true;
            exportProcessedResults(browseForXMLExport());
        } catch (Exception e) {
            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Exports the model data to same XML file
     */
    public void saveProcessedResults() {
        SummaryModel model = getModel().getSummaryModel();

        exportProcessedResults(model.getXmlFile());
    }

    /**
     * Export the results in simple Excel format.
     * This will prompt the user first to browse for
     * a suitable file
     */
    public void exportProcessedResultsAsXls() {
        File   dir   = UserPreferences.getModel().getExportedExcelDirectory();
        File   xml   = getModel().getSummaryModel().getXmlFile();
        File   tmp   = new File(dir + "/" + ParseUtils.removeFileExtension(xml.getName()) + ".xls");
        File   f     = ExcelExporter.browseForXlsExport(tmp, getView());
        JTable table = tableController.getTable();

        if (f != null) {
            try {
                getView().setBusyCursor();
                ExcelExporter.exportTable(table, f);
                getView().setDefaultCursor();
            } catch (IOException ex) {
                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /** Exports the model data to XML file */
    private void exportProcessedResults(File xmlfile) {
        File newfile = xmlfile;

        // First test if this file has an xml extension, and if not add one
        String name = xmlfile.getAbsoluteFile().toString();

        if (!name.endsWith(".xml")) {
            newfile = new File(name.concat(".xml"));
        }

        SummaryModel model = getModel().getSummaryModel();

        // If have contents to write
        if (model.getXmlFile() != null) {
            try {
                if (newfile != null) {
                    if (newfile.exists() &&!newfile.canWrite()) {
                        String message = "Warning: " + newfile.getName() + " cannot be written."
                                         + "\nIt could be locked by another application, "
                                         + "or is read-only. Check the file.";
                        NonModalMessageDialog dialog = new NonModalMessageDialog((JFrame) this.getView(), message);

                        dialog.setVisible(true);

                        if (dialog.answer()) {
                            return;
                        }
                    } else if (newfile.exists() && newfile.canWrite()) {
                        String message = "Warning: " + newfile.getName() + " exists"
                                         + "\n\nExporting the results to this file will "
                                         + "permanently erase the original contents. "
                                         + "Are you sure you want to save to this file ?";
                        NonModalYesNoDialog dialog = new NonModalYesNoDialog((JFrame) this.getView(), message);

                        dialog.setVisible(true);

                        if (dialog.answer() == true) {
                            runExportXML(newfile);
                        }
                    } else {
                        runExportXML(newfile);
                    }
                }
            } catch (Exception e) {

                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Search for processed results, including XML file and associated video
     * clip
     */
    public void importProcessedResults() {
        File xmlfile = null;

        try {
            xmlfile = browseForXMLImport();
            importProcessedResults(xmlfile);
        } catch (Exception e) {
            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Looks for associated video clip with xmlfile, before starting import
     * process
     */
    public void importProcessedResults(File xmlfile) {
        try {
            if ((xmlfile != null) && xmlfile.exists()) {

                // Import the processed results
                runImportXML(xmlfile);
            }
        } catch (Exception ex) {
            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Helper function to import video files to associate with a XML file
     * @param source
     *           The video source URL associated with the xmlfile
     * @param xmlfile
     *            The xmlfile
     * @returns the URL to the video source file it found or null if none found
     */
    private static URL searchVideoSource(File xmlfile, URL source) {

        // Convert input source associated with the XML to frames
        // and use these frames to start the image loader to
        // extract images of the events
        File file = null;

        if (source != null) {
            try {
                if (URLUtils.isValidURL(source.toString())) {

                    // If the source is a valid URL
                    return source;
                } else if (!URLUtils.isValidURL(source.toString())) {

                    // If the video source is an invalid URL, alert user and search for
                    // a source in the same directory as the xmlfile
                    // File f = new File(UserPreferences.getModel().getImportVideoDir().toString());
                    File f = new File(xmlfile.toString());

                    if ((file = searchForClip(xmlfile, ".avi")) != null
                            || (file = searchForClip(xmlfile, ".avi", f)) != null
                            || (file = searchForClip(xmlfile, ".mov")) != null
                            || (file = searchForClip(xmlfile, ".mov", f)) != null
                            || (file = searchForClip(xmlfile, ".tar")) != null
                            || (file = searchForClip(xmlfile, ".tar", f)) != null
                            || (file = searchForClip(xmlfile, ".tar.gz")) != null
                            || (file = searchForClip(xmlfile, ".tar.gz", f)) != null) {
                        System.out.println("Found alternative video source " + file.toURL());

                        return file.toURL();
                    } else {

                        // if can't find a file return and don't transcode.
                        String question = "A local file reference to the video input source associated with the\n"
                                          + xmlfile.getName()
                                          + " has not been found.\n\n Would you like to search for it now ? ";
                        NonModalYesNoDialog dialog = new NonModalYesNoDialog(Application.getView(), question);

                        dialog.setVisible(true);

                        if (dialog.answer() == true) {
                            try {
                                File v = new File(UserPreferences.getModel().getImportVideoDir().toString());
                                File s = browseForVideoClip(v);

                                if (s != null) {
                                    return s.toURL();
                                }

                                return null;
                            } catch (MalformedURLException ex) {
                                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (Exception e1) {
                                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, e1);
                            }
                        }

                        return null;
                    }
                } else if (URLUtils.isFile(source.toString())) {

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
     * Helper function to import mpeg video clips of results to associate with a XML file
     *
     * @param xmlfile
     *            The xmlfile to associate with the video files
     *
     * @returns the video source file it found or null if none found
     */
    private File searchMpegResults(File xmlfile) {

        // Need a valid xml file
        if (xmlfile != null) {
            try {
                File clip;
                File f = new File(UserPreferences.getModel().getImportVideoDir());

                if ((clip = searchForClip(xmlfile, ".results.mpeg")) != null) {
                    return clip;
                }

                // If not found, then search in last imported directory
                if ((clip = searchForClip(xmlfile, ".results.mpeg", f.getParentFile())) != null) {
                    return clip;
                }

                if ((clip = searchForClip(xmlfile, ".events.mpeg")) != null) {
                    return clip;
                }

                // If not found, then search in last imported directory
                if ((clip = searchForClip(xmlfile, ".events.mpeg", f.getParentFile())) != null) {
                    return clip;
                }

                // Create the dialog
                String question = "The mpeg encoded video results associated with the " + xmlfile.getName()
                                  + " has not been found. \n\n" + "This is not required, but can be useful when "
                                  + "editing the results. Would you like to search for it now ? ";
                NonModalYesNoDialog dialog = new NonModalYesNoDialog((ApplicationView) getView(), question);

                dialog.setVisible(true);

                if (dialog.answer() == true) {
                    try {
                        return browseForVideoClip(f);
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return null;
    }

    /** Starts a separate worker to export the processed results */
    private void runExportXML(File xmlfile) throws Exception {
        SummaryModel summary = this.getModel().getSummaryModel();

        if ((xmlfile != null) && (this.getModel() != null) && (this.getModel().getSummaryModel() != null)) {
            ExportXMLWorker thread = new ExportXMLWorker(xmlfile, this, summary, summary.getEventDataStream());

            thread.execute();
        }

        // else TODO: display error message here
    }

    /** Starts a separate worker to import the processed results */
    private void runImportXML(File xmlfile) throws Exception {
        if (xmlfile != null) {
            ImportXMLWorker thread = new ImportXMLWorker(xmlfile, this.getModel(), this, true);

            thread.execute();
        }

        // else TODO: display error message here
    }

    /** Browse for XML file, starting in last imported directory */
    private File browseForXMLImport() throws Exception {
        File f = null;

        /*
         * Set the filechooser with the last imported directory and add a custom
         * file filter to only find XML files and disable the default
         */
        JFileChooser chooser = new JFileChooser();
        FileFilter   filter  = (FileFilter) new XmlFileFilter();

        chooser.addChoosableFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(UserPreferences.getModel().getXmlImportDirectory());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Choose XML to import");
        chooser.setFileFilter(filter);

        if (chooser.showOpenDialog((ApplicationView) getView()) == JFileChooser.APPROVE_OPTION) {
            f = chooser.getSelectedFile();
            UserPreferences.getModel().setImportXmlDirectory(new File(f.getParent()));
        } else {

            // TODO: print dialog message box with something meaningful here
            System.out.println("No Selection ");
        }

        return f;
    }

    /** Browse for XML file to save to, starting in last imported directory */
    private File browseForXMLExport() throws Exception {
        File f = null;

        /*
         * Browse for XML to import starting with the last exported directory
         */
        JFileChooser chooser = new JFileChooser();

        chooser.addChoosableFileFilter(new XmlFileFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(UserPreferences.getModel().getXmlExportDirectory());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Choose XML file to save");

        if (chooser.showDialog((ApplicationView) getView(), "Save") == JFileChooser.APPROVE_OPTION) {
            f = chooser.getSelectedFile();
            System.out.println(f.toString());
            UserPreferences.getModel().setExportXmlDirectory(new File(f.getParent()));
        } else {

            // TODO: print dialog message box with something meaningful here
            throw new Exception("No selection");
        }

        return f;
    }

    /** Browse for video clip, starting in last imported directory */
    private static File browseForVideoClip(File setCurrentDirectory) throws Exception {
        File f = null;

        // Add a custom file filter and disable the default
        JFileChooser chooser = new JFileChooser();

        chooser.addChoosableFileFilter(new ImageFileFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setDialogTitle("Choose Video Source File");
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

    /**
     * Searches for clip to associate with a url base name
     * appended with an extention.
     *
     * @return URL with clip name if it's found, otherwise null
     */
    private URL searchForURLClip(URL base, String ext) {
        if ((ext != null) && (base != null)) {
            URL clipurl = null;

            try {

                // Parse XML file and create events mpeg based on same name,
                // e.g. parse root out of "1234.events.XML" to create
                // 1234.results.mpeg
                clipurl = new URL(base.toString() + ext);

                // If file exists in the same path as the XML file, assumes this
                // is correct,
                // otherwise, give dialog box indicating results not found
                if (URLUtils.isValidURL(clipurl.toString())) {
                    return clipurl;
                } else {
                    return null;
                }
            } catch (Exception ex) {
                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);

                return clipurl;
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
    private static File searchForClip(File xmlFile, String ext, File dir) {
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
    private static File searchForClip(File xmlFile, String ext) {
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

    public void actionPerformed(ActionEvent event) {
        String actionCommand = event.getActionCommand();

        try {
            SummaryModel model = getModel().getSummaryModel();
            File         v     = new File(UserPreferences.getModel().getImportVideoDir().toString());

            if (actionCommand.equals("BrowseMaster")) {
                File f;

                if ((f = browseForVideoClip(v)) != null) {
                    model.setInputSourceURL(f.toURL());
                }
            } else if (actionCommand.equals("BrowseResults")) {
                File f;

                if ((f = browseForVideoClip(v)) != null) {
                    model.setMpegUrl(f.toURL());
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles mouse clicks on video links in the <code>SummaryView</code>
     * @param e
     */
    void actionClickVideoFile(final MouseEvent e) {
        final SummaryModel model = getModel().getSummaryModel();

        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Thread change = new Thread(new Runnable() {
                        public void run() {
                            try {
                                ApplicationView  view = (ApplicationView) getView();
                                VideoPlayoutMode mode = UserPreferences.getModel().getPlayoutMode();

                                if (mode == VideoPlayoutMode.DEFAULT) {
                                    String urlString = null;

                                    if (e.getSource().equals(view.getMpegLabel()) && (model.getMpegUrl() != null)) {
                                        urlString = model.getMpegUrl().toString();
                                    }

                                    if (e.getSource().equals(view.getMasterLabel())
                                            && (model.getInputSourceURL() != null)) {
                                        urlString = model.getInputSourceURL().toString();
                                    }

                                    if (urlString != null) {
                                        try {

                                            // This will launch a separate
                                            // browser window each time it's
                                            // called and this process is not
                                            // managed by this ui - the user
                                            // must close the launcher external
                                            // to this application (e.g.
                                            // Quicktime, or web browser, etc.)
                                            // Set the busy cursor just a few
                                            // secondss to indicate something is
                                            // occuring, otherwise there is no
                                            // indication an external process is
                                            // launching
                                            view.setBusyCursor();
                                            launcher.openURLinBrowser(urlString);
                                            view.setDefaultCursor();
                                        } catch (Exception ex) {
                                            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE,
                                                             null, ex);
                                        }
                                    }
                                } else if (mode == VideoPlayoutMode.OTHER) {
                                    String file = null;

                                    if (e.getSource().equals(view.getMpegLabel()) && (model.getMpegUrl() != null)) {
                                        file = model.getMpegUrl().getFile();
                                    }

                                    if (e.getSource().equals(view.getMasterLabel())
                                            && (model.getInputSourceURL() != null)) {
                                        file = model.getInputSourceURL().getFile();
                                    }

                                    if (file != null) {

                                        // launch separate process to play video
                                        String cmd = mode.command + " " + model.getInputSourceURL().getFile();

                                        System.out.println("Executing " + cmd);

                                        try {
                                            Runtime.getRuntime().exec(cmd);
                                        } catch (Exception ex) {
                                            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE,
                                                             null, ex);

                                            NonModalMessageDialog dialog = new NonModalMessageDialog(getView(),
                                                                               "Cannot execute " + cmd + " "
                                                                               + ex.getMessage());

                                            dialog.setVisible(true);
                                        }
                                    }
                                } else {

                                    // TODO: launch err message - cannot find
                                    // file - need to check the CLASSPATH
                                    // variable ?
                                }
                            } catch (Exception ex) {
                                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });

                    change.start();
                }
            });
        }
    }

    public void modelChanged(ModelEvent event) {
        try {
            if (event instanceof SummaryModel.SummaryModelEvent) {
                switch (event.getID()) {
                case SummaryModelEvent.TRANSCODE_OUTPUT_DIR_CHANGED :

                    // when the transcode output changes, change the test directory
                    // to be a subdirectory of this. Putting this test images
                    // in a subdirectory makes for easy deletion later
                    SummaryModel model      = getModel().getSummaryModel();
                    File         s          = model.getFrameSourceDir();
                    File         testingDir = new File(s + "/testimages_" + s.getName());

                    if (!testingDir.exists()) {
                        testingDir.mkdir();
                    }

                    model.setTestImageDir(testingDir);

                    break;

                case SummaryModelEvent.TRANSCODE_SOURCE_CHANGED :
                    model = getModel().getSummaryModel();

                    File file = model.getTranscodeSource();

                    transcodeWorker = new VideoTranscodeWorker(this, getModel(), file, true);

                    Thread transcodeThread = new Thread(new Runnable() {
                        public void run() {
                            SummaryModel model = getModel().getSummaryModel();
                            File         file  = model.getTranscodeSource();

                            try {
                                if ((file != null) && file.exists()) {
                                    transcodeWorker.execute();
                                }
                            } catch (Exception ex) {
                                NonModalMessageDialog dialog;

                                try {
                                    dialog = new NonModalMessageDialog((ApplicationView) getView(), ex.getMessage());
                                    dialog.setVisible(true);
                                } catch (Exception ex1) {
                                    Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null,
                                                     ex1);
                                }

                                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });

                    transcodeThread.start();

                    break;

                case SummaryModelEvent.XML_FILE_CHANGED :
                    break;

                case SummaryModelEvent.INPUT_SOURCE_URL_CHANGED :
                    runImportLogic(getModel().getSummaryModel());

                    break;

                default :
                    break;
                }
            }

            if (event instanceof EventListModel.EventListModelEvent) {
                EventListModel.EventListModelEvent e     = (EventListModel.EventListModelEvent) event;
                EventListModel                     model = getModel().getEventListModel();

                switch (event.getID()) {
                case EventListModel.EventListModelEvent.LIST_CLEARED :
                    this.getView().setDefaultCursor();

                    break;

                case EventListModel.EventListModelEvent.LIST_RELOADED :
                    runListReloadLogic();

                    break;

                case EventListModelEvent.ONE_ENTRY_REMOVED :
                    break;

                case EventListModelEvent.MULTIPLE_ENTRIES_CHANGED :
                    break;

                case EventListModelEvent.NUM_LOADED_IMAGES_CHANGED :

                    /*
                     *  if the number of loaded images has reached the model
                     * size kill the transcode worker in case it is still running.
                     * Note that the transcode worker should end on its own - this
                     * is useful when the video to transcode is much longer than
                     * if actually needed.
                     */
                    if ((e.getFlag() >= model.getSize()) && (transcodeWorker != null)) {
                        transcodeWorker.gracefulCancel();
                    }

                    break;

                default :
                    break;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Utility to download files from a URL. This assumes no user or password is
     * required to copy from the URL
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
            InputStream   in  = con.getInputStream();
            OutputStream  out = new FileOutputStream(target);

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

    public void windowOpened(WindowEvent e) {}

    public void windowClosing(WindowEvent e) {
        if (Application.getModel().getEventListModel().getSize() > 0) {
            String           question = "About to shut down the application. " + "Are you sure you saved your data ?\n";
            ModalYesNoDialog dialog   = new ModalYesNoDialog(Application.getView(), question);

            dialog.setVisible(true);

            if (dialog.answer() == true) {
                try {
                    getView().setBusyCursor();
                    shutdown();
                } catch (Exception ex) {
                    Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            try {
                getView().setBusyCursor();
                shutdown();
            } catch (Exception ex) {
                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void windowClosed(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowActivated(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    /**
     * Runs logic to run every time a new event file is loaded.
     * Starts the image loading cache,
     * and searches for associated results.mpeg clip.
     * Prompts the user if none exist.
     */
    private void runListReloadLogic() {
        Thread searchMpeg = new Thread(new Runnable() {
            public void run() {
                EventListModel model = getModel().getEventListModel();
                SummaryModel summary = getModel().getSummaryModel();
                URL         v = summary.getInputSourceURL();
                String s = ParseUtils.parseFileNameRemoveDirectory(v.toString());
                try {
                    if (s.toLowerCase().endsWith("tar.gz") || s.toLowerCase().endsWith("tar"))
                        model.loadImageCacheDataByFrame();
                    else
                        model.loadImageCacheDataByEvent();
                    
                } catch (Exception ex) {
                    Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                }

                /**
                 * When the list is reloaded, search for the mpeg associated with the
                 * results. This isn't required but can be useful for playing
                 * back the results in an external video player
                 */
                SummaryModel smodel = getModel().getSummaryModel();
                URL          mpeg   = smodel.getMpegUrl();

                if ((mpeg == null) ||!URLUtils.isValidURL(mpeg.toString())) {

                    /**
                     * If  doing a save-as operation, then bypass looking
                     * for a mpeg because we already have a valid mpeg
                     */
                    if (isSaveAs == true) {
                        isSaveAs = false;
                    } else {

                        /**
                         * If not doing a save-as operation, then look for the
                         * mpeg results.
                         */
                        try {

                            /**
                             * First try to find associated mpeg results
                             * clip in same URL format as the source clip
                             * Get the URL e.g. http://localhost/foobar.avi
                             */
                            URL lasturl = UserPreferences.getModel().getImportSourceUrl();
                            URL url;

                            // If found a valid starting url
                            if (lasturl != null) {

                                /**
                                 * Remove the file extension (this assumes only one . in the end
                                 *  of the string. If the video is formatted differently, e.g.
                                 * http://localhost/foobar.master.avi rework this code
                                 */
                                String fileName = lasturl.toString();

                                if (fileName.lastIndexOf('.') >= 0) {
                                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                                }

                                if ((url = searchForURLClip(new URL(fileName.toString()), ".results.mpeg")) != null) {
                                    smodel.setMpegUrl(url);

                                    return;
                                }
                            }

                            File f = searchMpegResults(smodel.getXmlFile());

                            if (f != null) {
                                smodel.setMpegUrl(f.toURL());
                            }
                        } catch (MalformedURLException ex) {
                            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        });

        searchMpeg.start();
    }

    /**
     * Runs logic following selecting a new file for import
     */
    public static void runImportLogic(final SummaryModel model) {
        Thread downloadUrlThread = new Thread(new Runnable() {
            public void run() {

                /**
                 * When the input source changes, search for the video source
                 * associated with it and copy it to the scratch directory
                 * if it's a http source for use in the transcoder
                 */
                URL url = model.getInputSourceURL();

                if (url != null) {

                    // If this is a http url reference and not a local file
                    if (url.getProtocol().startsWith("http:")) {
                        File file   = null;
                        File tmpDir = UserPreferences.getModel().getScratchDirectory();

                        // Initialize the transcoder output directory to be the temporary directory
                        if (!tmpDir.exists()) {
                            tmpDir.mkdir();
                        }

                        if (tmpDir != null) {
                            String v = tmpDir.toString() + "/" + ParseUtils.parseFileNameRemoveDirectory(url.getFile());

                            file = new File(v);
                        } else {
                            file = new File(ParseUtils.parseFileNameRemoveDirectory(url.getFile()));
                        }

                        try {

                            // Download the contents of the url to a local file if it doesn't exist
                            if (!file.exists()) {
                                download(url, file);
                            }

                            if ((file != null) && file.exists()) {
                                model.setTranscodeSource(file);
                            } else {

                                /**
                                 * Can't find the file automatically so prompt
                                 * the user for one.
                                 */
                                URL u = searchVideoSource(model.getXmlFile(), url);

                                model.setTranscodeSource(new File(u.getFile()));
                            }
                        } catch (Exception ex) {
                            NonModalMessageDialog dialog;

                            try {
                                dialog = new NonModalMessageDialog(Application.getView(), ex.getMessage());
                                dialog.setVisible(true);

                                /**
                                 * Can't find the file automatically so prompt
                                 * the user for one.
                                 */
                                URL u = searchVideoSource(model.getXmlFile(), url);

                                model.setTranscodeSource(new File(u.getFile()));
                            } catch (Exception ex1) {
                                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex1);
                            }

                            Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {

                        // Convert to to a local file reference
                        File xml  = model.getXmlFile();
                        File file = new File(url.getPath());
                        File localFile;

                        if (xml.getParent() != null) {
                            localFile = new File(xml.getParent() + "/" + file.getName());
                        } else {
                            localFile = file;
                        }

                        // If there is no root path in the source identifier
                        // assume it is in the same path as the XML,
                        // and set its root to the same path as the XML
                        if (localFile.exists()) {
                            model.setTranscodeSource(localFile);
                        } else {
                            try {
                                /**
                                 * Can't find the file automatically so prompt
                                 * the user for one.
                                 */
                                URL u = searchVideoSource(model.getXmlFile(), url);

                                model.setTranscodeSource(new File(u.getFile()));
                                model.setInputSourceURL(u);
                            } catch (MalformedURLException ex) {
                                Logger.getLogger(ApplicationController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        });

        downloadUrlThread.start();
    }

    /**
     * Helper function to return the classifier
     * @return the classifier singleton
     */
    Classifier getClassifier() {
        return classifier;
    }

    /**
     * Action handler for responding to clicking video file links
     * @author dcline
     */
    class MouseClickFileActionHandler implements MouseListener {
        public void mouseClicked(MouseEvent e) {
            actionClickVideoFile(e);
        }

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {}

        public void mouseReleased(MouseEvent e) {}
    }


    /**
     * Action handler for responding to clicking tabbed pane in
     * the AVED ApplicationView.
     * @author dcline
     */
    class MouseClickTabActionHandler implements MouseListener {
        public void mouseClicked(MouseEvent e) {
            JTabbedPane pane = (JTabbedPane) e.getSource();

            // If this is the thumbnail panel, then manually need to set focus
            // here.
            if (pane.getSelectedIndex() == 1) {
                thumbnailController.getView().requestFocus(true);
            } else {
                thumbnailController.getView().requestFocus(false);
            }
        }

        public void mouseEntered(MouseEvent e) {}

        public void mouseExited(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {}

        public void mouseReleased(MouseEvent e) {}
    }
}
