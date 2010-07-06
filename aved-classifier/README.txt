Table of Contents

1. Overview

2. System Requirements

3. Installation on Linux or MacOSX

4. Developer Notes


1. Overview
-----------------------------------------------------------------------------
The AVED Classifier is simply Matlab code, but to simplify its use
a graphical interface to run it. The following describes
how to build the graphical interface

The code tree consists of the following:

File/Directory		Description
-------------------------------------------------------------------------
pom.xml		The Maven parent build file

sharedlib 	The Matlab code and a Maven build to compile a shared library
		using the Matlab compiler

sharedlibjni	The Java Native Interface to the Matlab code shared library
	 	built in sharedlib

narmojo		The Mojo for unpacking nar dependencies into
		a deployable package


2. System Requirements
-----------------------------------------------------------------------------

****Important ****
This not pure Java software. This software includes C++, Matlab,
and Java code.  It currently only works on Linux/Fedora and MacOSX.
To build the this module, you need:

    1.1. The software project management and comprehension tool Maven.
        Maven is based on the concept of a project object model (POM).
        If you look in each of the major subdirectories in this software
        you will see a pom.xml file, and in each of these files is a description
        of the build and its dependencies, whether it be a java build,
        c++ build, or otherwise.

        Download Maven from http://maven.apache.org/download.html

    1.2.  A Matlab installation on your machine that includes a license for:

        a.  The Matlab Image Processing Toolkit version: 4.10 (R2009a)
        b.  The Matlab Compiler version: 4.10 (R2009a)


        Matlab is required to build the aved-classifier module
	which is used in the graphical interface module aved-ui.


3.  Installation on Linux or Mac
-----------------------------------------------------------------------------

To compile this, you need Matlab, the Matlab Image Processing Toolbox,
and the Matlab compiler. See the README.txt in the root folder for 
instructions. 


4.  Developer Notes
-----------------------------------------------------------------------------

    4.1 Setup the correct environment for command-line build

        If you are getting a message similar to the following :

        ERROR: could not load library ...libsharedlib.dylib:
        Library not loaded: @loader_path/libmwmclmcrrt.7.10.dylib   ...Referenced from:

        Your environment variables are not set correctly.
        The environment variables needed are in the file called setup.

    4.2 Determining MATLAB_ROOT location

        If you have installed Matlab in a non-standard location, you
        need to change the environment variables MATLAB_ROOT, where
        $MATLAB_ROOT is the MATLAB installation directory on your machine
        and can be found by typing 'matlabroot' at the MATLAB command prompt.

        On Linux, MATLAB_ROOT is typically /usr/local/matlab7.2/bin/glx86

        On Mac OS X, MATLAB_ROOT is typically /Applications/MATLAB_R2009a.app

    4.3 Missing cdefs.h file

        The cdefs.h file is missing in Mac OS X 10.5.8 and causes the build
        to fail, so it copy it to the SDK directory first:

	>> sudo cp /usr/include/sys/cdefs.h /Developer/SDKs/MacOSX10.5.sdk/usr/include/sys/cdefs.h


    4.4 DYLD_LIBRARY_PATH/LD_LIBRARY_PATH path order when running compiled Matlab on Linux

        To run deployed components on Linux, the $MCR_ROOT/runtime
        directory must appear on your DYLD_LIBRARY_PATH/LD_LIBRARY_PATH
        *before* $MATLAB_ROOT/bin/maci, and XAPPLRESDIR should point
        $MCR_ROOT/X11/app-defaults.

    4.5 NetBeans 6.8 or later

        This code can be compiled on NetBeans 6.8 or higher.

        Installed Mac OS X applications, like NetBeans, do not see
        environment variables defined in ~/.cshrc or any other of
        the standard unix configuration files.  So, if you are
        working in a application, like NetBeans and relying on your
        bash/csh environment you are out of luck.

        To add environment variables that are visible to NetBeans:

            On Linux, modify the nbactions-Linux.xml file, replacing the
            environment variables with those in the setup file

            On Mac, modify the nbactions-Mac.xml file, replacing the
            environment variables with those in the setup file

            IMPORTANT NOTE:
            A separate nbactions file is needed for each Maven module,
            so you will need to propagate the nbactions-Mac/Linux.xml
            to *every* module directory it's needed in.

   4.6  Modifications to the Matlab library

        If you make any changes to the Matlab .m files, to rebuild only the
        matlab shared library from command-line

	>> cd sharedlib
	>> mvn install

        IMPORTANT NOTE:
        Most changes in the Matlab functions will also require modifications
        and rebuilding of the matlabsharedlib/jni modules.

	>> cd sharedlibjni
	>> mvn install
