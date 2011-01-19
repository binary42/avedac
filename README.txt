Table of Contents

1. System Requirements

2. About this Software

3. Installation on Linux

4. Installation on MacOSX

5. Developer Notes


1. System Requirements
-----------------------------------------------------------------------------

****Important ****
This not a pure Java software package. This software includes C++, Matlab,
and Java code.  It currently only works on Linux/Fedora, however
certain components will also build on MacOSx. To build the entire package,
you need:

    1.1  JDK 6 
	Download from http://java.sun.com/javase/downloads/ 

    1.2. The software project management and comprehension tool Maven.
        Maven is based on the concept of a project object model (POM).
        If you look in each of the major subdirectories in this software
        you will see a pom.xml file, and in each of these files is a description
        of the build and its dependencies, whether it be a java build,
        c++ build, or otherwise.

        Download Maven from http://maven.apache.org/download.html
	version 2.2.1

	Maven is a Java tool, so you must have Java installed in order to
	proceed. More precisely, you need a Java Development Kit (JDK), the Java
	Runtime Environment (JRE) is not sufficient.

    1.3. The iLab Neuromorphic Vision C++ Toolkit developed
        by the University of Southern California (USC) and the iLab at USC.
        See http://iLab.usc.edu for information about this project and
        the wiki page about the version required for this project.

        The Toolkit is required to build the aved-mbarivision module and
	it is free, but you will need to ask for it.
 	See http://ilab.usc.edu/toolkit/downloads.shtml for more information.

    1.4. The Java Advanced Imaging Binary version 1.1.3 for Linux/MacOSX
	from here:
	
	https://jai.dev.java.net/binary-builds.html

    1.5. The The Java Advanced Imaging Image I/O Tools for Linux/MacOSX
         from here:

	https://jai-imageio.dev.java.net/binary-builds.html


    1.6. A Matlab installation on your machine that includes a license for:

        a.  The Matlab Image Processing Toolkit version: 4.11 (R2010b)
        b.  The Matlab Compiler version: 4.11 (R2010b)


        Matlab is required to build the aved-classifier module
	which is used in the graphical interface module aved-ui.

2.  About this Software
-----------------------------------------------------------------------------
This software is used to automate detection and tracking of animals
in underwater video. It was developed in collaboration between the Monterey
Bay Aquarium Research Institute (MBARI), The California Institute of Technology.
(CalTech), and The University of Southern California (USC).

This work would not be possible without the generous support of the
David and Lucile Packard Foundation.


3.  Installation on Linux
-----------------------------------------------------------------------------

	3.1. Set the environment following variables to match your installation
	     
	     export MATLAB_ROOT=/home/aved/matlab7.2
             export JAVA_HOME=/usr/java/latest
	     export MCR_ROOT=/opt/MATLAB/MATLAB_Compiler_Runtime/v710
	     export SALIENCY_ROOT=/home/aved/saliency
	     export XERCESC_ROOT=/home/aved/Xercesc-2_7_0

	3.2. Run the setup script. This will install the necessary environment
	     variables to your ~/.bashrc 

	3.3. (optional) Modify the pom.xml to change the install path 

	     Edit the pom.xml in the same directory this README.txt is in
	     Modify the following properties to suit your installation:

	3.4. Installation command  

	     Run build from a command-line, e.g.

	     	$ ./build


4.  Installation on MacOSX
-----------------------------------------------------------------------------

Currently aved-mbarivision and aved-pmbarivision will not build on MacOSX.
However, the graphical interface and classifier code will build on MacOSX.
This means that you can run the event detection and tracking on a non-Mac
machine, e.g. a Linux Box, and edit the results and run the classifier  
on a Mac.  This is a workflow that works well for MBARI.

To build the GUI and classifier: 

   	4.1. Modify the environment variables for your Matlab installation
	     
	     export MATLAB_ROOT=/Applications/MATLAB_R2009a.app
	     export MCR_ROOT=/Applications/MATLAB/MATLAB_Compiler_Runtime/v710

	4.2. Run the setup script. This will install the necessary environment
	     variables to your ~/.bash_profile

	     $ cd aved-classifier
	     $ ./setup  
 

	4.3. (optional) Modify the pom.xml to change the install path 

	     Edit the pom.xml in the same directory this README.txt is in
	     Modify the following properties to suit your installation:

	      <installPath>${HOME}/aved</installPath> 
 
	4.3. Installation command  

	     Run build from a command-line, e.g.

	     	$ ./build

   	4.4. Build only the graphical interface

	     To create a MacOSX package of the graphical interface:

	 	$ ./build
		$ cd aved-ui
		$ mvn package osxappbundle:bundle

             The zipped package is located in src/aved-ui/target.
             Double-click to unzip then run the MacOSX package

5.  Developer Notes
-----------------------------------------------------------------------------

	5.1. Test before releasing (optional)

		$ mvn clean release:prepare -B -D dryRun=true

	5.2. Creating a release

	    5.2.2.  Increment the version displayed in the "About"
    	    	    box in the aved-ui graphical user interface

			$ mvn generate-sources  -P version-increment

	    5.2.3.  Create a release

			$ ./release
 
	5.3. Speeding up your build time

            After all of the dependencies have been built, you can modify
            your ~/.m2/settings.xml  file to go "offline" to avoid
            downloading dependencies - this will speed-up your build time.
            To switch to "offline" add the following to your ~/.m2/settings.xml:

            <settings>
            <offline>false</offline>
                ...
            </settings>
  
