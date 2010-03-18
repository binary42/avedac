package org.mbari.aved.mbarivision.api.parser;

/**
 * MbarivisionOptionXMLParserException is an Exception throws when 
 * a problem appears when attempting to Parse an XML File
 */
public class MbarivisionOptionXMLParserException extends Exception {

	private static final long serialVersionUID = 1L;

	public MbarivisionOptionXMLParserException () {
        // Constructor.  Create a MbarivisionOptionXMLParserException object containing
        // the default message as its error message.
    	super("Error when attempting to parse the XML option file");
    }
	
    public MbarivisionOptionXMLParserException (String message) {
        // Constructor.  Create a MbarivisionOptionXMLParserException object containing
        // the given message as its error message.
    	super(message);
    }
    
}
