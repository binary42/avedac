#ifndef MBARI_XML_PARSER_H_
#define MBARI_XML_PARSER_H_

#include "Component/ModelComponent.H"
#include "Media/FrameRange.H"
#include "Util/Types.H"
#include "rutz/shared_ptr.h"

#include "DetectionAndTracking/DetectionParameters.H"
#include "DetectionAndTracking/MbariVisualEvent.H"
#include "Utils/ErrReporter.H" 

#include <xercesc/util/PlatformUtils.hpp>
#include <xercesc/util/XMLString.hpp>
#include <xercesc/parsers/AbstractDOMParser.hpp>
#include <xercesc/dom/DOM.hpp>
#include <xercesc/parsers/XercesDOMParser.hpp>

#include <string>
#include <iostream>
#include <vector>

// ######################################################################
//! Includes all the functions to create and parse a AVED XML DOM document.
class MbariXMLParser
{
public:
  //! default Constructor. Locates the AVED schemas and creates the DOM writer object
  MbariXMLParser(std::string binDir);
	
  //! default Destructor. Deletes all private objects and terminates XML writers.
  virtual ~MbariXMLParser();
	
  void display();
	
  int displayElements(DOMNode *n, unsigned int nbr_child);
	
  //! Creates a DOM document
  /*! @param versionString the version string that represents the binary 
    @param fr frame range of this event sequence
    @param timecodefirst starting timecode of this event sequence, e.g. in 00:01:12
    @param timecodelast ending timecode of this event sequence, e.g. in 00:01:12
    @param params detection parameters 
    * */
  void creatDOMDocument(std::string versionString,
			int startFrame, 
			int endFrame, 
			std::string timecodefirst, 
			std::string timecodelast);

  //! Adds source meta data to the DOM output
  /*! @param metadataXMLFile the input xml file name that contains the source metadata
  **/
  void addSourceMetaData(std::string metadataXMLFile);
  
  void importSourceMetadata(std::string inputXML);
  void addDetectionParameters(DetectionParameters params);
  void add(bool saveNonInterestingEvents,
	   std::list<MbariVisualEvent::VisualEvent *> &eventList,
	   int eventframe,
	   std::string eventframetimecode);
  void writeDocument(std::string path);
  bool isXMLValid(std::string inputXML);

private:

  DOMWriter *itsDOMWriter;
  DOMImplementation *impl;  
  DOMDocument* itsXMLdoc;    
  XMLFormatTarget *itsXMLFileFormatTarget;
  XercesDOMParser *itsParser;
  ErrReporter *itsErrHandler;
  std::string itsEventDataSchemaLocation;	
  std::string itsSourceMetadataSchema;

  //! Generic parser that parses and xml file based on give input schema. Used for testing.
  DOMDocument* parseXMLFile(std::string inputXML, std::string inputSchema);
	
  std::string strcatX(std::string str,unsigned int x);
};

#endif /*MBARI_XML_PARSER_H_*/
