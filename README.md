# avedac

## Overview
The Automated Visual Event Detection and Classification (avedac) software is designed to automated the detection of organisms in underwater video. The software was designed in collaboration between the Monterey Bay Aquarium Research Institute (MBARI), California Institute of Technology (Caltech), Pasadena, CA, and the University of Southern California.

Video or still frames are processed with a neuromorphic selective attention algorithm. The candidate objects of interest are tracked across video frames using linear Kalman filters or Nearest Neighbor approaches. If objects can be tracked successfully over several frames, they are labeled as potentially "interesting" and marked in the video frames. The plan is that the system will enhance the productivity of human video annotators and/or cue a subsequent object classification module by marking candidate objects.

This work was made possible with the generous support of the David and Lucile Packard Foundation. This project originated at the 2002 Workshop for Neuromorphic Engineering in Telluride, CO

avedac is automated event learning framework made with expression, speed, and modularity in mind.
It is developed by the Berkeley Vision and Learning Center ([BVLC](http://bvlc.eecs.berkeley.edu)) and community contributors.

Check out the [project site](http://www.mbari.org/aved) for further details

## Requirements
This code is freely available, however to build the classifier you will need a copy of Matlab, the Matlab compiler and Matlab Image Processing Toolkit. It is primarily built for Linux, however certain components such as the GUI for analyzing detected events and running the classifier will run on a Mac

If you would like a copy of this software to test and do no have the required Matlab components, instructions and the VM can be found on dropbox here: https://www.dropbox.com/sh/j9lls6nqhvwm72m/H-YmUJ0nrZ/AVEDac-CentOS-64-bit-dropbox-instructions/AVEDac%20virtual%20machine%20instructions.html You should be able to experiment with this on any platform supported by VMWare Player, like Windows, Linux, or a Mac. You'll need a high-bandwidth network connection to download this and please contact Danelle for the password to login.

Please contact Danelle at dcline@mbari.org to get the password to login to the VM to evaluate it. In your email, it would helpful to know what you are doing with the software to steer you in the right direction on using it.

## License and Citation

avedac is released under the [GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl.html).
