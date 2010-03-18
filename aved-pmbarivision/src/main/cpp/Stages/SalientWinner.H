#ifndef SALIENTWINNER_H_
#define SALIENTWINNER_H_

// ######################################################################
//! Stores winning location and voltage at point
// A simplified version of the WTAWinner class for 
// simplifying passing the winners with MPI 
class SalientWinner{
public:

  //! default Constructor  
  SalientWinner() {};
  
  /* !@param winner Salient winner in image coordinates
     @param winnnerSMV saliency map voltage at winner point*/
  SalientWinner(const Point2D<int> &winner, const double &winnnerSMV) {
    itsWinner = winner;
    itsWinnerSMV = winnnerSMV;
  };
  
  // ######################################################################
  void writeToStream(std::ostream& os)  {
    os << itsWinner.i << " " << itsWinner.j << "\n";
    os << itsWinnerSMV << "\n";
  };
  
  // ######################################################################
  void readFromStream(std::istream& is){
    is >> itsWinner.i;
    is >> itsWinner.j;
    is >> itsWinnerSMV; 
  };

  Point2D<int> itsWinner;
  double itsWinnerSMV;
};

#endif /*SALIENTWINNER_H_*/
