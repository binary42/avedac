package org.mbari.aved.mbarivision.api.parser;

import java.lang.reflect.Field;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.mbari.aved.mbarivision.api.AvedVideo;
import org.mbari.aved.mbarivision.api.MbarivisionOptions;

/**
 * Parse a MbarivisionOptions into command line or Parse a commande line
 * into a MbarivisionOptions object
 */
public class MbarivisionOptionCmdLineParser extends CmdLineParser {

	/* command to lunch the AVED main */
	public static final String MBARIVISION_COMMAND = "mbarivision";
	/* MbarivisionOptions instance. */
	private final MbarivisionOptions mbarivisionOptions;
	private final AvedVideo videoToProcess;
	
	/**
	 * Parse a MbarivisionOptions object into the mbarivision commandline
	 * @param mopt the MbarivisionOptions object to parse
	 * @param v the AVED video
	 */
	public MbarivisionOptionCmdLineParser(MbarivisionOptions mopt, AvedVideo v) {		
	
            super(mopt);
		this.mbarivisionOptions = mopt;
		this.videoToProcess = v;
	}

	/**
	 * Reads a command line and sets the MbarivisionOptions object
	 * @param args the argument of the Java main classe
	 * @throws CmdLineException 
	 */
	public void read(String[] args) throws CmdLineException {
		this.parseArgument(args);
	}

	/**
	 * Creates the command line to lunch AVED
	 * @return the command line to execute
	 */
	public String getCommand () {
			
		/* get the mbarivision command */
		String cmd = MbarivisionOptionCmdLineParser.MBARIVISION_COMMAND;
		cmd += " ";
		/* add the options */
		Class<? extends MbarivisionOptions> c = mbarivisionOptions.getClass();
		for( Field f : c.getDeclaredFields() ) {
			Option o = f.getAnnotation(Option.class);					
			if(o != null) {
				try {
					if(f.get(mbarivisionOptions) != null) {
						cmd += o.name() + "=" + f.get(mbarivisionOptions).toString() + " ";
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
					
			}
		}
		/* add the video parameters */
		cmd += "--input-frames=000000-";
		for (int i = String.valueOf(this.videoToProcess.getNbFrame()).length(); i < 6; i++)
			cmd += "0";
		cmd += this.videoToProcess.getNbFrame() + "@0 ";
		
		cmd += "--output-frames=000000-"; //001805@0 
		for (int i = String.valueOf(this.videoToProcess.getNbFrame()).length(); i < 6; i++)
			cmd += "0";
		cmd += this.videoToProcess.getNbFrame() + "@0 ";		
		
		cmd += videoToProcess.getOutputDirectory().toString() + "/f0 " + this.videoToProcess.getName() + "_";
		return cmd;
			
	}

}
