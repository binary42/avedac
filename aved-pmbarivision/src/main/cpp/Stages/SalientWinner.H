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
