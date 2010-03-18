package org.mbari.aved.mbarivision.api;

/**
 * AvedRuntimeException is an Exception throws when AVED acount an Error during its runtime
 */
public class AvedRuntimeException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public AvedRuntimeException () {
        // Constructor.  Create a ProcessException object containing
        // the default message as its error message.
    	super("Error launching the AVED process, check if mbarivision and/or transcode commands are installed or set in your path");
    }
	
    public AvedRuntimeException (String message) {
        // Constructor.  Create a ProcessException object containing
        // the given message as its error message.
    	super(message);
    }
	
}
