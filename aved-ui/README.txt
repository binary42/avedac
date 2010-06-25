Building the AVED GUI requires Maven

To build run:
	$ mvn -PMac compile

or
	$ mvn -PLinux compile

To increment the version (version displayed in the "About"
    box in the graphical user interface)
        $ mvn generate-sources  -P version-increment

To create a OS X package for Mac
	$ cd aved-ui
	$ mvn -PMac package osxappbundle:bundle
