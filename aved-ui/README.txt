Building the AVED GUI requires Maven

To build run:
	$ mvn -PMac32 compile

or
	$ mvn -PLinux32 compile

To increment the version (version displayed in the "About"
    box in the graphical user interface)
        $ mvn generate-sources  -P version-increment

To create a OS X package for Mac
	$ cd aved-ui
	$ mvn -PMac32 package 
