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

    1.1. The software project management and comprehension tool Maven.
        Maven is based on the concept of a project object model (POM).
        If you look in each of the major subdirectories in this software
        you will see a pom.xml file, and in each of these files is a description
        of the build and its dependencies, whether it be a java build,
        c++ build, or otherwise.

        Download Maven from http://maven.apache.org/download.html

    1.2. The iLab Neuromorphic Vision C++ Toolkit developed
        by the University of Southern California (USC) and the iLab at USC.
        See http://iLab.usc.edu for information about this project and
        the wiki page about the version required for this project.

        The Toolkit is required to build the aved-mbarivision module and
	it is free, but you will need to ask for it.
 	See http://ilab.usc.edu/toolkit/downloads.shtml for more information.

    1.3. The Java Advanced Imaging Binary version 1.1.3 for Linux/MacOSX
	from here:
	
	https://jai.dev.java.net/binary-builds.html

    1.4. The The Java Advanced Imaging Image I/O Tools for Linux/MacOSX
         from here:

	https://jai-imagio.dev.java.net/binary-builds.html


    1.5. A Matlab installation on your machine that includes a license for:

        a.  The Matlab Image Processing Toolkit version: 4.10 (R2009a)
        b.  The Matlab Compiler version: 4.10 (R2009a)


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

	3.1. Modify the environment variables

	     Edit aved-classifier/setup
 	     Modify the following properties to match your installation, e.g.:
	     
	     export MATLAB_ROOT=/home/aved/matlab7.2
             export JAVA_HOME=/usr/java/latest
	     export MATLAB_JAVA=/usr/java/jdk1.6.0_20/jre

	3.2. Modify the pom.xml 

	     Edit the pom.xml in the same directory as this README.txt
	     Modify the following properties to match your installation:

	      <installPath>${HOME}/aved</installPath>
	      <xercesRoot>${HOME}/aved/Xerces-2_7_0</xercesRoot>
              <saliencyRoot>${HOME}/aved/saliency</saliencyRoot> 

	3.3. Installation command  

	     Run build from a command-line, e.g.

	     	$ ./build


4.  Installation on MacOSX
-----------------------------------------------------------------------------

Currently aved-mbarivision and aved-pmbarivision will not build on MacOSX.
However, the graphical interface and classifier code will build fine on MacOSX.
To build: 

   	4.1. Modify the environment variables

	     Edit aved-classifier/setup
 	     Modify the following property to suit your installation:
	     
	     export MATLAB_ROOT=/Applications/MATLAB_R2009a.app


	4.2. Modify the pom.xml 

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

                $ source ./aved-classifier/setup
		$ mvn clean release:prepare -B -D dryRun=true

	5.2. Creating a release

	    5.2.2.  Increment the version displayed in the "About"
    	    	    box in the aved-ui graphical user interface

                        $ source ./aved-classifier/setup
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
  
