#include <xercesc/sax/SAXParseException.hpp>
#include <iostream>
#include <string>

#include "Utils/ErrReporter.H"

using namespace std;

void ErrReporter::warning(const SAXParseException&) {
}

void ErrReporter::error(const SAXParseException& mess) {
  fSawErrors = true;
  cerr << "Error in \"" << XMLString::transcode(mess.getSystemId())
       << "\", row " << mess.getLineNumber()
       << ", column " << mess.getColumnNumber()
       << "\nError message : " << XMLString::transcode(mess.getMessage()) << endl;
}

void ErrReporter::fatalError(const SAXParseException& mess) {
  fSawErrors = true;
  cerr << "Fatal Error in \"" << XMLString::transcode(mess.getSystemId())
       << "\", row " << mess.getLineNumber()
       << ", column " << mess.getColumnNumber()
       << "\nError message : " << XMLString::transcode(mess.getMessage()) << endl;
}

void ErrReporter::resetErrors() {
  fSawErrors = false;
}
