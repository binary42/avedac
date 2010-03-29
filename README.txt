###################################################
# Building the AVED software suite requires Maven,
# Matlab, Matlab Image Processing Toolkit,
# and the Matlab compiler
#
# If you don't have maven, get it from here:
# maven.apache.org
###################################################
About this software
TODO: need license information here:
###################################################
TODO: add module information here


###################################################
Compile this entire maven project:
###################################################
	$ ./build.sh


###################################################
Create a Mac OSX package of the aved-ui (graphical
user interface module) only
###################################################
	$ mvn compile 
	$ mvn install
	$ cd aved-ui
	$ mvn package osxappbundle:bundle

The zipped package will be in aved-ui/target


###################################################
Create a release
###################################################
1.  Increment the version (version displayed in the "About"
    box in the graphical user interface)
 	$ mvn generate-sources  -P version-increment

2.  Run the release script
	$ ./release.sh
 
###################################################
Notes:

(optional) To test before releasing:
	$ mvn clean release:prepare -B -D dryRun=true

