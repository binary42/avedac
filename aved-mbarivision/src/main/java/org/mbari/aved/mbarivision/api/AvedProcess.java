package org.mbari.aved.mbarivision.api;

import java.io.File;
import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.mbari.aved.mbarivision.api.parser.MbarivisionOptionCmdLineParser;
import org.mbari.aved.mbarivision.api.parser.MbarivisionOptionXMLParser;
import org.mbari.aved.mbarivision.api.parser.MbarivisionOptionXMLParserException;
import org.mbari.aved.mbarivision.api.utils.StreamGobbler;

/**
 * Requires mbarivision set on the machine
 * AvedProcess take a Folder with PNMs in and process the input folder with the options given.
 * The options could be either provided by command line (bash usage), reed directly from an XML file (webService usage)
 * or directly set into the object (GUI usage)
 */
public class AvedProcess extends Thread {

    private MbarivisionOptions mbariOptions;
    public final static String DEFAULT_PATH_TO_AVED_XML_CONFIGURATION_FILE = "configuration.xml";
    private String pathToXML;
    /* AVED take for parameter only a Folder containing frames */
    private AvedVideo videoToProcess;

    public AvedProcess(AvedVideo v) {
        mbariOptions = new MbarivisionOptions(); // Set the options as default
        pathToXML = AvedProcess.DEFAULT_PATH_TO_AVED_XML_CONFIGURATION_FILE;
        this.videoToProcess = v;
        mbariOptions.eventSummary = new File(this.videoToProcess.getName() + ".events.summary");
        mbariOptions.eventxml = new File(this.videoToProcess.getName() + ".events.xml");
    }

    /**
     * Set the MbarivisionOptions object
     * @param mopt The MbarivisionOptions object
     * @throws MbarivisionOptionXMLParserException
     */
    public void setMbarivisionOptions(MbarivisionOptions mopt) throws MbarivisionOptionXMLParserException {
        mbariOptions = mopt;
    }

    /**
     * Set the MbarivisionOptions object by reading the providing xml file
     * @throws MbarivisionOptionXMLParserException
     */
    public void setMbarivisionOptions() throws MbarivisionOptionXMLParserException {
        MbarivisionOptionXMLParser parser = new MbarivisionOptionXMLParser(mbariOptions);
        parser.read(this.pathToXML);
    }

    /**
     * Set the MbarivisionOptions object by reading the arguments from a command line
     * @param args arguments from command line
     * @throws CmdLineException
     */
    public void setMbarivisionOptions(String[] args) throws CmdLineException {
        MbarivisionOptionCmdLineParser parser = new MbarivisionOptionCmdLineParser(mbariOptions, videoToProcess);
        parser.read(args);
    }

    /**
     * @return the Mbarivision options
     */
    public MbarivisionOptions getMbarivisionOptions() {
        return mbariOptions;
    }

    /*
     * Options Profiles
     * TODO modify this code to move to a Profile folder with different XML file embeding different profiles
     */
    public enum Profiles {

        Benthic, Midwater
    };

    /**
     * Sets several parameters throught a profile
     * @param p Profile
     */
    public void setProfiles(Profiles p) {
        if (p == AvedProcess.Profiles.Benthic) {
            this.mbariOptions.cacheSize = 25;
            this.mbariOptions.trackingMode = MbarivisionOptions.TrackingMode.BoundingBox;
            this.mbariOptions.minEventArea = 100;
            this.mbariOptions.maxEventArea = 10000;
        } else if (p == AvedProcess.Profiles.Midwater) {
            this.mbariOptions.cacheSize = 10;
            this.mbariOptions.trackingMode = MbarivisionOptions.TrackingMode.KalmanFilter;
            this.mbariOptions.minEventArea = 50;
            this.mbariOptions.maxEventArea = 1000;
        }
    }

    /**
     * Sets several parameters throught a profile
     * @param p Profile
     */
    public void setProfiles(String p) {
        this.setProfiles(AvedProcess.Profiles.valueOf(p));
    }

    /**
     * Sets the path to the XML File where AVED parameters will be stored
     * @param p the path to the XML File
     */
    public void setPathToXML(String p) {
        pathToXML = p;
    }

    /**
     * Runs the process
     * @throws ProcessException 
     */
    public synchronized void run() {
        MbarivisionOptionCmdLineParser parser = new MbarivisionOptionCmdLineParser(mbariOptions, videoToProcess);
        try {
            /* get the command line */
            String cmd = parser.getCommand();
            Runtime rt = Runtime.getRuntime();
            /* execute the command */
            Process proc = rt.exec(cmd);

            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), System.out);
            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), System.out, "OUTPUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error???
            int exitVal = proc.waitFor();
            System.out.println("ExitValue: " + exitVal);

        } catch (InterruptedException e) {
            throw new AvedRuntimeException("The AVED process has been interupted");
        } catch (IOException e) {
            throw new AvedRuntimeException("Error launching the mbarivision command, check if mbarivision command is instal or set in your path");
        }
    }
}
