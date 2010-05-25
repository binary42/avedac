Steps to build the AVED Classifier
Created: 7/1/2009 D.Cline
MBARI
######################################################################
Overview
######################################################################
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


######################################################################
Building the AVED Classifier 
######################################################################

Requirements:

To compile this, you need Matlab, the Matlab Image Processing Toolbox,
and the Matlab compiler.

1) Make sure the matlab compiler is in your path

	>> mcc

if not, you may need to add it. The location is different, depending
on whether using Linux or Mac OS X, e.g. to add a link: 

Mac OS X:
	>> sudo ln -s /Applications//MATLAB_R2009a.app/bin/mcc /usr/bin/mcc

Linux:
	>> sudo ln -s /usr/local/matlab7.2/bin/mcc /usr/local/bin/mcc

2) Setup the correct environment for command-line build 

If you are getting a message similar to the following :
ERROR: could not load library sharedlib:/Users/dcline/.m2/repository/org/mbari/aved/classifier/sharedlib/0.4.1-SNAPSHOT/nar/lib/i386-MacOSX-g++/shared/libsharedlib.dylib:  Library not loaded: @loader_path/libmwmclmcrrt.7.10.dylib   Referenced from: /Users/dcline/.m2/repository/org/mbari/aved/classifier/sharedlib/0.4.1-SNAPSHOT/nar/lib/i386-MacOSX-g++/shared/libsharedlib.dylib   Reason: image not found

Your environment variables are not set correctly.
The environment variables needed are in setup. 
Before running the build, you must first either copy the environment
variables , e.g. .cshrc, .bashrc, etc. file, or source the variables
in your current shell environment with 

	>> source setup

If you have installed Matlab in a non-standard location, you
need to change the environment variables MATLAB_ROOT, where 
$MATLAB_ROOT is the MATLAB installation directory on your machine 
and can be found by typing 'matlabroot' at the MATLAB command prompt.

On Linux, MATLAB_ROOT is typically /usr/local/matlab7.2/bin/glx86

On Mac OS X, MATLAB_ROOT is typically /Applications/MATLAB_R2009a.app

WARNING:
The cdefs.h file is missing in Mac OS X 10.5.8 and causes the build 
to fail, so it copy it to the SDK directory first:
	>> sudo cp /usr/include/sys/cdefs.h /Developer/SDKs/MacOSX10.5.sdk/usr/include/sys/cdefs.h

IMPORTANT NOTE: Running compiled Matlab on UNIX or Linux: 
To run deployed components on Linux, the $MCR_ROOT/runtime 
directory must appear on your DYLD_LIBRARY_PATH/LD_LIBRARY_PATH 
*before* $MATLAB_ROOT/bin/maci, and XAPPLRESDIR should point 
$MCR_ROOT/X11/app-defaults. 

3) Setting up the correct environment for NetBeans build
Installed Mac OS X applications, like NetBeans, do not see 
environment variables defined in ~/.cshrc or any other of 
the standard unix configuration files.  So, if you are 
working in a application, like NetBeans, to make them visible, 
copy the file environment.plist to ~/.MacOSX/environment.plist.
Ceate the directory and the file yourself. If this file exists,
simply cut and paste the new environment  variables from
the included example. You will need to logout and login to 
see the changes. 


4) Finally, to build this, in the parent directory to this file,
   run the Maven install
	>> cd ..
	>> mvn install 

######################################################################
Developer notes
######################################################################

If you make any changes to the Matlab library, to rebuild only the 
matlab shared library

	>> cd sharedlib
	>> mvn install

Important to note is that changes in the Matlab function arguments will 
also require modifications and rebuilding of the matlabsharedlib/jni modules.


