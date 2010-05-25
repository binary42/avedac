package org.mbari.aved.mbarivision.api.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.kohsuke.args4j.Option;
import org.mbari.aved.mbarivision.api.MbarivisionOptions;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parse a MbarivisionOptions into an XML file or Parse an XML file
 * into a MbarivisionOptions object
 */
public class MbarivisionOptionXMLParser {
	
	/* mbarivisionOptions instance */
	private MbarivisionOptions mbarivisionOptions;
	/* the xml document 
	 * Use the DOM model to write this because this is small so performance is not an issue
	 */
	private Document xmldoc;

	/**
	 * Constructor of the MbarivisionOptionXMLParser
	 * @param opt the MbarivisionOptions to Parse 
	 */
	public MbarivisionOptionXMLParser(MbarivisionOptions opt) {
		this.mbarivisionOptions = opt;
		xmldoc = new DocumentImpl();
	}

	/**
	 * Reads the given XML file to set the MbarivisionOptions object
	 * @param xmlFile The XML file where all AVED parameters are listed
	 * @throws MbarivisionOptionXMLParserException
	 */
	public void read(String xmlFile) throws MbarivisionOptionXMLParserException {
		
		// Instantiate the DOM parser.
		DOMParser parser = new DOMParser();
		
		try {
			parser.parse(xmlFile);
			Document doc = parser.getDocument();
			
			NodeList list = doc.getElementsByTagName("Option");

			// Now put the argments/options pairs in a string array
			for(int i=0; i<list.getLength(); i++) {
				// Get the argument and option attributes for the Option node
				Node node = list.item(i);
				NamedNodeMap map = node.getAttributes();
				Node arg = map.getNamedItem("argument");
				Node option = map.getNamedItem("option");
				if(option != null) {
					String optionValue = new String(option.getNodeValue().toString());	
					
					if (optionValue.equals(MbarivisionOptions.MBARI_SEGMENTATION_ALGORITHM_OPTION)) {
						mbarivisionOptions.segmentAlgorithm = MbarivisionOptions.SegmentationAlgorithm.valueOf(arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_MARK_INTERESTING_OPTION)) {
						mbarivisionOptions.eventStyle = MbarivisionOptions.MarkEventStyle.valueOf(arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_TRACKING_MODE_OPTION)) {
						mbarivisionOptions.trackingMode = MbarivisionOptions.TrackingMode.valueOf(arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_SAVE_EVENT_CLIP_OPTION)) {
						mbarivisionOptions.saveEventClip = arg.getNodeValue().toString();
					} else if (optionValue.equals(MbarivisionOptions.MBARI_SAVE_EVENT_SUMMARY_OPTION)) {
						mbarivisionOptions.eventSummary = new File (arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_SAVE_EVENTS_XML_OPTION)) {
						mbarivisionOptions.eventxml = new File (arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_SAVE_ONLY_INTERESTNIG_EVENTS_OPTION)) {
						mbarivisionOptions.saveOnlyInteresting = arg.getNodeValue().toString().equals("true");
					} else if (optionValue.equals(MbarivisionOptions.MBARI_NO_MARK_CANDIDATE_OPTION)) {
						mbarivisionOptions.noMarkCandidate = arg.getNodeValue().toString().equals("true");
					} else if (optionValue.equals(MbarivisionOptions.MBARI_NO_LABEL_EVENTS_OPTION)) {
						mbarivisionOptions.noLabelEvents = arg.getNodeValue().toString().equals("true");
					} else if (optionValue.equals(MbarivisionOptions.MBARI_NO_SAVE_OUTPUT_OPTION)) {
						mbarivisionOptions.noSaveOutput = arg.getNodeValue().toString().equals("true");
					} else if (optionValue.equals(MbarivisionOptions.MBARI_CACHE_SIZE_OPTION)) {
						mbarivisionOptions.cacheSize = Integer.parseInt(arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_MIN_EVENT_AREA_OPTION)) {
						mbarivisionOptions.minEventArea = Integer.parseInt(arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_MAX_EVENT_AREA_OPTION)) {
						mbarivisionOptions.maxEventArea = Integer.parseInt(arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_OPACITY_OPTION)) {
						mbarivisionOptions.opacity = Double.parseDouble(arg.getNodeValue().toString());
					} else if (optionValue.equals(MbarivisionOptions.MBARI_MASK_FILE_OPTION)) {
						mbarivisionOptions.maskFile = new File ((arg.getNodeValue().toString()));
					}
				}
			}
			
		} catch (SAXException e) {
			throw new MbarivisionOptionXMLParserException("Impossible to open AVED XML file options");
		} catch (IOException e) {
			throw new MbarivisionOptionXMLParserException("Impossible to open AVED XML file options");
		}	
	}	
	
	/**
	 * Parse an MbarivisionOptions object into an XML file
	 */
	private void parseToXML () throws MbarivisionOptionXMLParserException {
		
		/* 
		 * Create the header of the XML file
		 * 
		 * Create root element and embed the schema. For now this is on the oceana Confluence web page. 
		 * TODO: move the schema location to an external web server, or remove it from here altogether
		 */
		Element root = xmldoc.createElement("MbarivisionOptions");
		root.setAttribute("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
		root.setAttribute("xsi:noNamespaceSchemaLocation","http://oceana.shore.mbari.org:8081/download/attachments/927/Mbarivision+Options+Schema+-+Vers+1.xsd?version=1");
		xmldoc.appendChild(root);
		
		/* 
		 * Create the body of the XML file
		 * 
		 * Extract all the option names and fields in the bean and write as individual nodes
		 */		
		Class<? extends MbarivisionOptions> c = mbarivisionOptions.getClass();
		for( Field f : c.getDeclaredFields() ) {
			Option o = f.getAnnotation(Option.class);					
			if(o != null) {

				// Write out the option and argument, unless the argument is null
				try {
					if(f.get(mbarivisionOptions) != null) {
						// Create child option/argument nodes and append to root node
						Element option = xmldoc.createElementNS(null, "Option");
						option.setAttributeNS(null, "option", o.name());
						option.setAttributeNS(null, "argument", f.get(mbarivisionOptions).toString());
						root.appendChild(option); 
					}
				} catch (IllegalArgumentException e) {
					throw new MbarivisionOptionXMLParserException("Illegal argument in the AVED XML file");
				} catch (DOMException e) {
					throw new MbarivisionOptionXMLParserException();
				} catch (IllegalAccessException e) {
					throw new MbarivisionOptionXMLParserException("Illegal access to the AVED XML file");
				}							

			}
		}
			
	}
	
	/**
	 * Write mbarivisionOptions into an XML file at the given location 
	 */
	public void write(String xmlfile) throws MbarivisionOptionXMLParserException {

		try {
			/* Build the  XML File */
			this.parseToXML();
			
			FileOutputStream fos;
			fos = new FileOutputStream(xmlfile);
			
			OutputFormat of = new OutputFormat("XML","UTF-8",true);
			of.setIndent(1);
			of.setIndenting(true);				
			
			XMLSerializer serializer = new XMLSerializer(fos,of);
			serializer.asDOMSerializer();
			serializer.serialize( xmldoc.getDocumentElement() );
			serializer.endDocument();
			fos.close();
			
		} catch (IOException e) {
			throw new MbarivisionOptionXMLParserException("Impossible to write the AVED XML file options at " + xmlfile);
		} catch (SAXException e) {
			throw new MbarivisionOptionXMLParserException();
		} catch (MbarivisionOptionXMLParserException e) {
			throw new MbarivisionOptionXMLParserException(e.getMessage());
		}

	}
	
	/**
	 * Gets the XML file
	 * @return the XML file
	 */
	public Document getXML () {	return xmldoc; }
	/**
	 * Gets the Mbarivision options
	 * @return the Mbarivision options
	 */
	public MbarivisionOptions getOptions () {	return mbarivisionOptions; }
	

}
