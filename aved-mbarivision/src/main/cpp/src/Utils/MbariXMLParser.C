/*
 * Copyright 2010 MBARI
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
 *
 * This is a program to automate detection and tracking of events in underwater 
 * video. This is based on modified version from Dirk Walther's 
 * work that originated at the 2002 Workshop  Neuromorphic Engineering 
 * in Telluride, CO, USA. 
 * 
 * This code requires the The iLab Neuromorphic Vision C++ Toolkit developed
 * by the University of Southern California (USC) and the iLab at USC. 
 * See http://iLab.usc.edu for information about this project. 
 *  
 * This work would not be possible without the generous support of the 
 * David and Lucile Packard Foundation
 */ 

#include "DetectionAndTracking/DetectionParameters.H"

#include "Utils/MbariXMLParser.H"
#include "Utils/Version.H"
#include "DetectionAndTracking/TrackingModes.H"
#include "DetectionAndTracking/MbariVisualEvent.H"

#include <xercesc/util/OutOfMemoryException.hpp>
#include <xercesc/framework/LocalFileFormatTarget.hpp>
#include <xercesc/util/XMLDateTime.hpp>
#include <sstream>
#include <iostream>
#include <fstream>
using namespace std;

MbariXMLParser::MbariXMLParser(std::string binDir)
{
  ifstream file;

  if(getenv("AVED_BIN")) {
    itsEventDataSchemaLocation = string(string(getenv("AVED_BIN"))+string("/schema/EventDataSet.xsd"));
    itsSourceMetadataSchema = string(string(getenv("AVED_BIN"))+string("/schema/SourceMetadata.xsd"));
  }
  else {
    itsEventDataSchemaLocation = binDir + std::string("/schema/EventDataSet.xsd");
    itsSourceMetadataSchema = binDir + std::string("/schema/SourceMetadata.xsd");
  }

  try {
    XMLPlatformUtils::Initialize();
  } catch(const XMLException& toCatch) {
    char *pMsg = XMLString::transcode(toCatch.getMessage());
    LERROR("Error during Xerces-c Initialization. Exception message: %s", pMsg);
    XMLString::release(&pMsg);
  }
  // check for the existance of the schema file before starting
  file.open(itsEventDataSchemaLocation.c_str(), ifstream::in);
  if (file.is_open() == false)
    LFATAL("Error - cannot find the schema file %s. Check the environment variable AVED_BIN.", itsEventDataSchemaLocation.c_str());
  file.close();

  // check for the existance of the schema file before starting
  file.open(itsSourceMetadataSchema.c_str(), ifstream::in);
  if (file.is_open() == false)
    LFATAL("Error - cannot find the schema file %s. Check the environment variable AVED_BIN", itsSourceMetadataSchema.c_str());
  file.close();

  XMLCh* value = XMLString::transcode("LS");
  impl = DOMImplementationRegistry::getDOMImplementation(value);
  XMLString::release(&value);

  if(impl != 0) {
    try {

      // Create the writer
      itsDOMWriter = ((DOMImplementationLS*)impl)->createDOMWriter();

      if (itsDOMWriter->canSetFeature(XMLUni::fgDOMWRTSplitCdataSections, true))
	itsDOMWriter->setFeature(XMLUni::fgDOMWRTSplitCdataSections, true);

      if (itsDOMWriter->canSetFeature(XMLUni::fgDOMWRTFormatPrettyPrint, true))
	itsDOMWriter->setFeature(XMLUni::fgDOMWRTFormatPrettyPrint, true);

    } catch (const OutOfMemoryException&) {
      LERROR("OutOfMemoryException");
    } catch (const DOMException& e) {
      LERROR("DOMException code is:  %d ", e.code);
    }
  }
}

MbariXMLParser::~MbariXMLParser()
{
 if(itsXMLdoc != NULL)
  delete itsXMLdoc;
 if(impl != NULL)
  delete impl;
 if(itsDOMWriter != NULL)
  delete itsDOMWriter;
 if(itsErrHandler != NULL)
  delete itsErrHandler;
 if(itsParser != NULL)
  delete itsParser;
  XMLPlatformUtils::Terminate();
}

void MbariXMLParser::creatDOMDocument(std::string versionString,
				      	              int startFrame,
                                      int endFrame,
                                      std::string startTimeCode,
                                      std::string endTimeCode) {
  try {

    // Create the document
    XMLCh* rootvalue = XMLString::transcode("EventDataSet");
    XMLCh* rootnamespace = XMLString::transcode("http://www.w3.org/2001/XMLSchema-instance");
    itsXMLdoc = impl->createDocument(NULL, rootvalue, NULL);
    XMLCh* xmlnsxsi = XMLString::transcode("xmlns:xsi");
    itsXMLdoc->getDocumentElement()->setAttributeNS(XMLUni::fgXMLNSURIName, xmlnsxsi ,rootnamespace);
    XMLString::release(&xmlnsxsi);
    XMLString::release(&rootvalue);
    XMLString::release(&rootnamespace);

    itsXMLdoc->setStandalone(false);

    DOMElement* root = itsXMLdoc->getDocumentElement();

    // Add some comments
    std::string commentstring = "Created by: MBARI software " + versionString;
    XMLCh* comment = XMLString::transcode(commentstring.c_str());
    DOMComment *domcomment = itsXMLdoc->createComment(comment);
    root->appendChild(domcomment);
    XMLString::release(&comment);

    char datestamp[24];
    time_t time_of_day;
    time_of_day = time( NULL );
    strftime(datestamp, 24, "%Y-%m-%d %X %Z", localtime(&time_of_day));

    XMLCh* creationdatestring = XMLString::transcode("CreationDate");
    XMLCh* creationdate = XMLString::transcode(datestamp);
    root->setAttribute(creationdatestring, creationdate);

    std::ostringstream smin, smax;
    // if conversion from frame to string worked,
    if(smin << startFrame) {
      XMLCh* startframe = XMLString::transcode("StartFrame");
      XMLCh* startframevalue = XMLString::transcode(smin.str().c_str());
      root->setAttribute(startframe, startframevalue);
      XMLString::release(&startframe);
      XMLString::release(&startframevalue);
    }
    if(smax << endFrame) {
      XMLCh* endframe = XMLString::transcode("EndFrame");
      XMLCh* endframevalue = XMLString::transcode(smax.str().c_str());
      root->setAttribute(endframe, endframevalue);
      XMLString::release(&endframe);
      XMLString::release(&endframevalue);
    }
    if (startTimeCode.length() > 0) {
      XMLCh* starttimecode = XMLString::transcode("StartTimecode");
      XMLCh* starttimecodevalue = XMLString::transcode(startTimeCode.c_str());
      root->setAttribute(starttimecode, starttimecodevalue);
      XMLString::release(&starttimecode);
      XMLString::release(&starttimecodevalue);
    }
    if (endTimeCode.length() > 0) {
      XMLCh* endtimecode = XMLString::transcode("EndTimecode");
      XMLCh* endtimecodevalue = XMLString::transcode(endTimeCode.c_str());
      root->setAttribute(endtimecode, endtimecodevalue);
      XMLString::release(&endtimecode);
      XMLString::release(&endtimecodevalue);
    }
  }
  catch(const DOMException& err) {
    LFATAL("Erreur pendant la creation du document XML ( DOM Exception ) :\n");
  }
}

void MbariXMLParser::addSourceMetaData(std::string inputXML) {

  DOMDocument* sourcemetadata = NULL;
  sourcemetadata = parseXMLFile(inputXML, itsSourceMetadataSchema);

  if (sourcemetadata != NULL) {
    DOMNodeList *list = sourcemetadata->getElementsByTagName(XMLString::transcode("SourceMetadata"));
    if(list != NULL) {
    	DOMNode *node = list->item(0);
    	DOMNode* newRoot = itsXMLdoc->importNode(node, false);
    	itsXMLdoc->getDocumentElement()->appendChild(newRoot);
    	display();
    }
  } else {
    LINFO("Warning: the source metadata file given is not found or not valid, data will not be imported. \
    AVED metadata should be formatted to the following schema: %s)",itsSourceMetadataSchema.c_str());
  }
}

bool MbariXMLParser::isXMLValid(std::string inputXML) {
  DOMDocument* sourcemetadata = parseXMLFile(inputXML, itsEventDataSchemaLocation);
  return (sourcemetadata != NULL);
}

void MbariXMLParser::addDetectionParameters(DetectionParameters params) {

  // Add the DetectionParameters values
  XMLCh* detectionparameters = XMLString::transcode("EventDetectionParameters");
  DOMElement* eventsroot = itsXMLdoc->createElement(detectionparameters);
  std::ostringstream cachesizevalue, mineventareavalue, maxeventareavalue, \
    maskxposvalue, maskyposvalue, maskwidthvalue, maskheightvalue,maxWTApointsvalue, \
  maxevolvetimevalue, maxframeseventvalue, minframeseventvalue, maxcostvalue;

  // This is actually better to put into DetectionParameters.C for maintenance purposes
  //...but we will leave them here for now. These should be all the model options
  // that one can set via pmbarision/mbarivision command line.
  if(cachesizevalue << params.itsSizeAvgCache) {
    XMLCh* cachesize = XMLString::transcode("CacheSize");
    XMLCh* cachesizexmlstring = XMLString::transcode(cachesizevalue.str().c_str());
    eventsroot->setAttribute(cachesize, cachesizexmlstring);
    XMLString::release(&cachesize);
    XMLString::release(&cachesizexmlstring);
  }
  if(mineventareavalue << params.itsMinEventArea) {
    XMLCh* mineventarea = XMLString::transcode("MinEventArea");
    XMLCh* mineventareaxmlstring = XMLString::transcode(mineventareavalue.str().c_str());
    eventsroot->setAttribute(mineventarea, mineventareaxmlstring);
    XMLString::release(&mineventarea);
    XMLString::release(&mineventareaxmlstring);
  }
  if(maxeventareavalue << params.itsMaxEventArea) {
    XMLCh* maxeventarea = XMLString::transcode("MaxEventArea");
    XMLCh* maxeventareaxmlstring = XMLString::transcode(maxeventareavalue.str().c_str());
    eventsroot->setAttribute(maxeventarea, maxeventareaxmlstring);
    XMLString::release(&maxeventarea);
    XMLString::release(&maxeventareaxmlstring);
  }
  XMLCh* trackingmode = XMLString::transcode("TrackingMode");
  std::string tmstring = trackingModeName(params.itsTrackingMode);
  XMLCh* trackingmodexmlstring = XMLString::transcode(tmstring.c_str());
  eventsroot->setAttribute(trackingmode, trackingmodexmlstring);
  XMLString::release(&trackingmode);
  XMLString::release(&trackingmodexmlstring);

  SegmentAlgorithmType satype= params.itsSegmentAlgorithm;
  std::string sastring = segmentAlgorithmType(satype);
  XMLCh* segmentationalgorithm = XMLString::transcode("SegmentAlgorithm");
  XMLCh* segmentationalgorithmxmlstring = XMLString::transcode(sastring.c_str());
  eventsroot->setAttribute(segmentationalgorithm, segmentationalgorithmxmlstring);
  XMLString::release(&segmentationalgorithm);
  XMLString::release(&segmentationalgorithmxmlstring);

  SegmentAlgorithmInputImageType saitype= params.itsSegmentAlgorithmInputType;
  std::string saistring = segmentAlgorithmInputImageType(saitype);
  XMLCh* segmentalgorithmtype = XMLString::transcode("SegmentAlgorithmInputImageType");
  XMLCh* segmentalgorithmtypexmlstring = XMLString::transcode(saistring.c_str());
  eventsroot->setAttribute(segmentalgorithmtype, segmentalgorithmtypexmlstring);
  XMLString::release(&segmentalgorithmtype);
  XMLString::release(&segmentalgorithmtypexmlstring);

  SaliencyInputImageType sitype= params.itsSaliencyInputType;
  std::string sistring = saliencyInputImageType(sitype);
  XMLCh* saliencyinputtype = XMLString::transcode("SaliencyInputImageType");
  XMLCh* saliencyinputtypexmlstring = XMLString::transcode(sistring.c_str());
  eventsroot->setAttribute(saliencyinputtype, saliencyinputtypexmlstring);
  XMLString::release(&saliencyinputtype);
  XMLString::release(&saliencyinputtypexmlstring);

  XMLCh* savenoninteresting = XMLString::transcode("SaveNonInteresting");
  XMLCh* savenoninterestingxmlstring =  XMLString::transcode(params.itsSaveNonInteresting ? "1" :"0");
  eventsroot->setAttribute(savenoninteresting,savenoninterestingxmlstring);
  XMLString::release(&savenoninteresting);
  XMLString::release(&savenoninterestingxmlstring);

  XMLCh* keepWTAboring = XMLString::transcode("KeepWTABoring");
  XMLCh* keepWTAboringxmlstring =  XMLString::transcode(params.itsKeepWTABoring ? "1" :"0");
  eventsroot->setAttribute(keepWTAboring,keepWTAboringxmlstring);
  XMLString::release(&keepWTAboring);
  XMLString::release(&savenoninterestingxmlstring);

  if (maxWTApointsvalue << params.itsMaxWTAPoints) {
        XMLCh* maxWTApoints = XMLString::transcode("MaxWTAPoints");
        XMLCh* maxWTApointsxmlstring = XMLString::transcode(maxWTApointsvalue.str().c_str());
        eventsroot->setAttribute(maxWTApoints, maxWTApointsxmlstring);
        XMLString::release(&maxWTApoints);
        XMLString::release(&maxWTApointsxmlstring);
    }
  if (maxevolvetimevalue << params.itsMaxEvolveTime) {
        XMLCh* maxevolvetime = XMLString::transcode("MaxEvolveTime");
        XMLCh* maxevolvetimexmlstring = XMLString::transcode(maxevolvetimevalue.str().c_str());
        eventsroot->setAttribute(maxevolvetime, maxevolvetimexmlstring);
        XMLString::release(&maxevolvetime);
        XMLString::release(&maxevolvetimexmlstring);
    }
  if (maxframeseventvalue << params.itsMaxEventFrames) {
        XMLCh* maxframesevent = XMLString::transcode("MaxFramesEvent");
        XMLCh* maxframeseventxmlstring = XMLString::transcode(maxframeseventvalue.str().c_str());
        eventsroot->setAttribute(maxframesevent, maxframeseventxmlstring);
        XMLString::release(&maxframesevent);
        XMLString::release(&maxframeseventxmlstring);
    }

  if (minframeseventvalue << params.itsMinEventFrames) {
        XMLCh* minframesevent = XMLString::transcode("MinFramesEvent");
        XMLCh* minframeseventxmlstring = XMLString::transcode(minframeseventvalue.str().c_str());
        eventsroot->setAttribute(minframesevent, minframeseventxmlstring);
        XMLString::release(&minframesevent);
        XMLString::release(&minframeseventxmlstring);
    }
 
  if (maxcostvalue << params.itsMaxCost) {
        XMLCh* maxcost = XMLString::transcode("MaxCost");
        XMLCh* maxcostxmlstring = XMLString::transcode(maxcostvalue.str().c_str());
        eventsroot->setAttribute(maxcost, maxcostxmlstring);
        XMLString::release(&maxcost);
        XMLString::release(&maxcostxmlstring);
    }

  // Only output the mask parameters if a valid path is set
  if(params.itsMaskPath.length() > 0) {
    XMLCh* maskpath = XMLString::transcode("MaskPath");
    XMLCh* maskpathstring = XMLString::transcode( params.itsMaskPath.c_str());
    eventsroot->setAttribute(maskpath, maskpathstring);
    XMLString::release(&maskpath);
    XMLString::release(&maskpathstring);

    if(maskxposvalue << params.itsMaskXPosition) {
      XMLCh* maskxpos = XMLString::transcode("MaskXPosition");
      XMLCh* maskxposstring = XMLString::transcode(maskxposvalue.str().c_str());
      eventsroot->setAttribute(maskxpos, maskxposstring);
      XMLString::release(&maskxpos);
      XMLString::release(&maskxposstring);
    }
    if(maskyposvalue << params.itsMaskYPosition) {
      XMLCh* maskypos = XMLString::transcode("MaskYPosition");
      XMLCh* maskyposstring = XMLString::transcode(maskyposvalue.str().c_str());
      eventsroot->setAttribute(maskypos, maskyposstring);
      XMLString::release(&maskypos);
      XMLString::release(&maskyposstring);
    }
    if(maskwidthvalue << params.itsMaskWidth) {
      XMLCh* maskwidth = XMLString::transcode("MaskHeight");
      XMLCh* maskwidthstring = XMLString::transcode(maskwidthvalue.str().c_str());
      eventsroot->setAttribute(maskwidth, maskwidthstring);
      XMLString::release(&maskwidth);
      XMLString::release(&maskwidthstring);
    }
    if(maskheightvalue << params.itsMaskYPosition) {
      XMLCh* maskheight = XMLString::transcode("MaskWidth");
      XMLCh* maskheightstring = XMLString::transcode(maskheightvalue.str().c_str());
      eventsroot->setAttribute(maskheight, maskheightstring);
      XMLString::release(&maskheight);
      XMLString::release(&maskheightstring);
    }
  }

  DOMElement* root = itsXMLdoc->getDocumentElement();
  root->appendChild(eventsroot);
}

// Fonction permettant de concaténer X fois
// une chaine str
std::string MbariXMLParser::strcatX(std::string str,unsigned int x) {
  for(unsigned int y = 0; y <= x; y++)
    str += "  ";
  return str;
}


void MbariXMLParser::display() {
  displayElements((DOMNode*)itsXMLdoc->getDocumentElement(),0);
}

// Fonction displayElements :
// fonction récursive qui affiche tous les noeuds ayant pour valeur
// "directory" ou "file" en attribut "type"
int MbariXMLParser::displayElements(DOMNode *n, unsigned int nbr_child) {
  DOMNode *child;
  // Compte les éléments
  unsigned int count = 0;

  // Compte le nombre d'appels à la fonction (récursive)
  // afin de pouvoir établir une tabulation au texte de sortie
  // de l'exécutable. Un appel = un répertoire
  static unsigned count_call = 0;

  // Rajoute un nombre nbr_child de tabulations afin de faciliter
  // la lisibilité de l'arborescence
  std::string xTab = strcatX("  ",nbr_child);

  if(n) {
    if(n->getNodeType() == DOMNode::ELEMENT_NODE) {
      char *nodename = XMLString::transcode(n->getNodeName());
      LINFO("%s<%s>", xTab.c_str(), nodename);

      if(n->hasAttributes()) {
	++count_call;

	for (child = n->getFirstChild(); child != 0; child=child->getNextSibling())
	  count += displayElements(child, count_call);

	--count_call;
	++count;
      } else {
	++count_call;
	for (child = n->getFirstChild(); child != 0; child=child->getNextSibling())
	  count += displayElements(child,count_call);
	--count_call;
      }
      LINFO("%s</%s>", xTab.c_str(), nodename);
    }
  }
  return count;
}

DOMDocument* MbariXMLParser::parseXMLFile (std::string inputXML, std::string inputSchema) {

  // DOM parser instance
  itsParser = new XercesDOMParser();

  /*itsParser->setDoNamespaces(true);
  itsParser->setDoSchema(true);
  itsParser->setValidationScheme(XercesDOMParser::Val_Always);
  itsParser->setExternalNoNamespaceSchemaLocation(inputSchema.c_str());
  */

  // Error handler instance for the parser
  itsErrHandler = new ErrReporter();
  itsParser->setErrorHandler(itsErrHandler);

  // Create the document
  DOMDocument* domdoc = impl->createDocument(NULL, NULL, NULL);

  // Parse le fichier XML et récupère le temps mis pour le parsing
  try {
    itsParser->resetDocumentPool();
    itsParser->parse(inputXML.c_str());

    if (itsParser->getErrorCount() == 0) {
      domdoc = itsParser->getDocument();
      return domdoc;
    }
    else {
      LINFO("Error when attempting to parse the XML file : %s ", inputXML.c_str());
      return NULL;
    }
  }
  // Exception XML
  catch (const XMLException& err) {
    LINFO("Error during XML parsing of file : %s ", inputXML.c_str());
  }
  // Exception DOM
  catch (const DOMException& err) {
    const unsigned int maxChars = 2047;
    XMLCh errText[maxChars + 1];
    LINFO("Error during XML parsing of file  : %s \n", inputXML.c_str());
    if (DOMImplementation::loadDOMExceptionMsg(err.code, errText, maxChars))
      LFATAL("Exception DOM : %s ", XMLString::transcode(errText));
  } catch (...) {
    LINFO("Error during XML parsing of file  : %s", inputXML.c_str());
  }
  return NULL;
}

void MbariXMLParser::add(bool saveNonInterestingEvents,
                         std::list<MbariVisualEvent::VisualEvent *> &eventList,
			 int eventframe,
			 std::string eventframetimecode)
{
  // Create the body of the DOMDocument
  try {

    // add another leaf to root node for detection parameters
    XMLCh* frameeventsetstring = XMLString::transcode("FrameEventSet");
    DOMElement*  frameeventset = itsXMLdoc->createElement(frameeventsetstring);
    XMLString::release(&frameeventsetstring);

    std::ostringstream number;
    // if conversion from frame to string worked,
    if(number << eventframe) {
      XMLCh* framenumberstring = XMLString::transcode("FrameNumber");
      XMLCh* framenumbervalue = XMLString::transcode(number.str().c_str());
      frameeventset->setAttribute(framenumberstring, framenumbervalue);
      XMLString::release(&framenumberstring);
      XMLString::release(&framenumbervalue);
    }

    XMLCh* timecode = XMLString::transcode("TimeCode");
    XMLCh* timecodevaluevalue = XMLString::transcode(eventframetimecode.c_str());
    frameeventset->setAttribute(timecode, timecodevaluevalue);
    XMLString::release(&timecode);
    XMLString::release(&timecodevaluevalue);


    std::list<MbariVisualEvent::VisualEvent *>::iterator i;
    for(i=eventList.begin(); i != eventList.end(); ++i) {
      // if also saving non-interesting events and this is BORING event, be sure to save this
      // otherwise, save all INTERESTING events
      if(  (saveNonInterestingEvents && (*i)->getCategory() == MbariVisualEvent::VisualEvent::BORING) ||
	   (*i)->getCategory() == MbariVisualEvent::VisualEvent::INTERESTING)
	{
	  std::ostringstream s1,s2,s3,s4,s5,s6,s7;
	  uint eframe = (*i)->getEndFrame();
	  MbariVisualEvent::Token tke = (*i)->getToken(eframe);

	  //create event object element and add attributes
	  XMLCh* eventobjectstring = XMLString::transcode("EventObject");
	  DOMElement* eventObject = itsXMLdoc->createElement(eventobjectstring);
	  XMLString::release(&eventobjectstring);

	  s1 << (*i)->getEventNum();
	  XMLCh* objectidstring = XMLString::transcode("ObjectID");
	  XMLCh* objectidvalue = XMLString::transcode(s1.str().c_str());
	  eventObject->setAttribute(objectidstring, objectidvalue);
	  XMLString::release(&objectidstring);
	  XMLString::release(&objectidvalue);

	  s2 << (*i)->getStartFrame();
	  XMLCh* startframenumberstring = XMLString::transcode("StartFrameNumber");
	  XMLCh* startframenumbervalue = XMLString::transcode(s2.str().c_str());
	  eventObject->setAttribute(startframenumberstring, startframenumbervalue);
	  XMLString::release(&startframenumberstring);
	  XMLString::release(&startframenumbervalue);

	  if ( (*i)->getStartTimecode().length() > 0 ) {
	    s3 << (*i)->getStartTimecode();
	    XMLCh* starttimecodetring = XMLString::transcode("StartTimecode");
	    XMLCh* starttimecodevalue = XMLString::transcode(s3.str().c_str());
	    eventObject->setAttribute(starttimecodetring, starttimecodevalue);
	    XMLString::release(&starttimecodetring);
	    XMLString::release(&starttimecodevalue);
	  }

	  s4 << tke.bitObject.getSMV();
	  XMLCh* saliencystring = XMLString::transcode("Saliency");
	  XMLCh* saliencyvalue = XMLString::transcode(s4.str().c_str());
	  eventObject->setAttribute(saliencystring, saliencyvalue);
	  XMLString::release(&saliencystring);
	  XMLString::release(&saliencyvalue);

	  s5 << tke.bitObject.getArea();
	  XMLCh* currsizestring = XMLString::transcode("CurrSize");
	  XMLCh* currsizevalue = XMLString::transcode(s5.str().c_str());
	  eventObject->setAttribute(currsizestring, currsizevalue);
	  XMLString::release(&currsizestring);
	  XMLString::release(&currsizevalue);

	  Point2D<int> p = tke.bitObject.getCentroid();
	  s6 << p.i;
	  XMLCh* currxstring = XMLString::transcode("CurrX");
	  XMLCh* currxvalue = XMLString::transcode(s6.str().c_str());
	  eventObject->setAttribute(currxstring, currxvalue);
	  XMLString::release(&currxstring);
	  XMLString::release(&currxvalue);

	  s7 << p.i;
	  XMLCh* currystring = XMLString::transcode("CurrY");
	  XMLCh* curryvalue = XMLString::transcode(s7.str().c_str());
	  eventObject->setAttribute(currystring, curryvalue);
	  XMLString::release(&currystring);
	  XMLString::release(&curryvalue);

	  Rectangle r = tke.bitObject.getBoundingBox();
	  std::ostringstream llx,lly,urx,ury;
	  llx << r.left(); lly << r.bottomI(); urx << r.rightI(); ury << r.top();

	  // create bounding box element and add attributes
	  XMLCh* boundingboxstring = XMLString::transcode("BoundingBox");
	  DOMElement* boundingBox = itsXMLdoc->createElement(boundingboxstring);
	  XMLString::release(&boundingboxstring);

	  XMLCh* lowerleftxstring = XMLString::transcode("LowerLeftX");
	  XMLCh* lowerleftxvalue = XMLString::transcode(llx.str().c_str());
	  boundingBox->setAttribute(lowerleftxstring, lowerleftxvalue);
	  XMLString::release(&lowerleftxstring);
	  XMLString::release(&lowerleftxvalue);

	  XMLCh* lowerleftystring = XMLString::transcode("LowerLeftY");
	  XMLCh* lowerleftyvalue = XMLString::transcode(lly.str().c_str());
	  boundingBox->setAttribute(lowerleftystring, lowerleftyvalue);
	  XMLString::release(&lowerleftystring);
	  XMLString::release(&lowerleftyvalue);

	  XMLCh* upperrightxstring = XMLString::transcode("UpperRightX");
	  XMLCh* upperrightxvalue = XMLString::transcode(urx.str().c_str());
	  boundingBox->setAttribute(upperrightxstring, upperrightxvalue);
	  XMLString::release(&upperrightxstring);
	  XMLString::release(&upperrightxvalue);

	  XMLCh* upperrightystring = XMLString::transcode("UpperRightY");
	  XMLCh* upperrightyvalue = XMLString::transcode(ury.str().c_str());
	  boundingBox->setAttribute(upperrightystring, upperrightyvalue);
	  XMLString::release(&upperrightystring);
	  XMLString::release(&upperrightyvalue);

	  // append to appropriate elements
	  eventObject->appendChild(boundingBox);
	  frameeventset->appendChild(eventObject);
	}
    }

    DOMElement* root = itsXMLdoc->getDocumentElement();
    root->appendChild(frameeventset);


  } catch (const OutOfMemoryException&) {
    LERROR( "OutOfMemoryException");
  } catch (const DOMException& e) {
    LERROR( "DOMException code is:  %d", e.code);
  } catch (...) {
    LERROR( "An error occurred creating the document" );
  }
}

void MbariXMLParser::writeDocument(std::string path) {

  XMLCh* out = XMLString::transcode(path.c_str());
  itsXMLFileFormatTarget = new LocalFileFormatTarget(out);
  XMLString::release(&out);
  itsDOMWriter->writeNode(itsXMLFileFormatTarget, *itsXMLdoc);
  delete itsXMLFileFormatTarget;

}

