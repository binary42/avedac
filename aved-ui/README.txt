Building the AVED GUI requires Maven

To build :
	$ mvn -PMac32 compile
	$ mvn -PMac64 compile

or
	$ mvn -PLinux32 compile
	$ mvn -PLinux64 compile

To increment the version (version displayed in the "About" box in the graphical user interface)
        $ mvn generate-sources  -P version-increment

To create a OS X package for Mac

1) Create the package

	$ mvn -PMac32 package 
or
	$ mvn -PMac64 package 


2) Delete the empty aved-ui-0.4.3-SNAPSHOT directory

3) Unzip the created zip files in the target directory

4) Open then PackageMaker document and drag in the entire folder aved-ui-0.4.3-SNAPSHOT-mac64-assembly/ and  aved-ui-0.4.3-SNAPSHOT

5) In the Configuration tab, set the Destination for both folders to /Applications/AVEDac

6) In the Contents tab, select the root folder, then set the owner to root and group to wheel for both folders, check the boxes for Read/Execute for Owner,Group, Others and Write for Owner/Group. Lastly, click Application Recommendations to apply these changes.

7) Click the Build and Run icon and try installing and running the package. It will install to Applications/AVEDac





