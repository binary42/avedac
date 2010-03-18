#include <xercesc/util/XercesDefs.hpp>
#include <xercesc/sax/ErrorHandler.hpp>
#include <iostream>

XERCES_CPP_NAMESPACE_USE

// ######################################################################
//! SAXParser Erro Reporter utility class for getting/settings errors
class ErrReporter : public ErrorHandler {
public:
  ErrReporter() : fSawErrors(false) {
  }

  ~ErrReporter() {
  }


  // Méthodes d'implémentation de ErrorHandler
  void warning(const SAXParseException& mess);
  void error(const SAXParseException& mess);
  void fatalError(const SAXParseException& mess);
  void resetErrors();

  // Méthode Get
  bool getSawErrors() const;

  // Donnée privée permettant de savoir si des erreurs
  // ont eu lieu avec getSawErrors() et de remettre le
  // compteur d'erreurs à zéro
  bool fSawErrors;
};

inline bool ErrReporter::getSawErrors() const {
  return fSawErrors;
}
