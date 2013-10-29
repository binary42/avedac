Building the AVED GUI requires Maven

1.  Installation on Linux
-----------------------------------------------------------------------------
1) Build 
	$ mvn -PMac64 compile

2) Create the package

	$ mvn -PMac64 package 

3) Unzip the created zip files in the target directory, e.g.

	$ unzip -o target/aved-ui-0.4.3-SNAPSHOT-assembly.zip -d ~/Desktop/gui/


2.  Installation on MacOSX
-----------------------------------------------------------------------------
1) Build 

	$ mvn -PLinux64 compile

2) Create the package

	$ mvn -PMac64 package 

3) Delete the empty aved-ui-0.4.3-SNAPSHOT directory

4) Unzip the created zip files in the target directory, e.g.

5) Open the Mac PackageMaker document and drag in the entire folder aved-ui-0.4.3-SNAPSHOT-mac64-assembly and  aved-ui-0.4.3-SNAPSHOT

6) In the Configuration tab, set the Destination for both folders to /Applications/AVEDac, and set the distribution title to AVEDac

7) In the Contents tab, select the root folder, then set the owner to root and group to wheel for both folders,
check the boxes for Read/Execute for Owner,Group, Others and Write for Owner/Group.
Lastly, click Apply Recommendations to apply these changes.

8) Add the MCRInstaller.zip package to open following installation. In the Distributions icon, Click actions, then Edit button in PostInstall. Select
Open File in the Actions. In the Type drop-down box, select Absolute Path and enter /Applications/AVEDac/tools/MCRInstaller.zip  

TODO: replace steps 7 and 8 with a script - this no longer works in Matlab R2012a
9) Add the MCRInstaller.dmg package to run following installation. In the Distributions icon, Click actions, then Edit button in PostInstall. Select
Open File in the Actions. In the Type drop-down box, select Absolute Path and enter /Applications/AVEDac/tools/MCRInstaller/MCRInstaller.app  

10) Click the Build and Run icon and try installing and running the package. It will install to  /Applications/AVEDac


3.  Developer Notes
-----------------------------------------------------------------------------
To increment the version (version displayed in the "About" box in the graphical user interface)
        $ mvn generate-sources  -P version-increment

Error logs are written by default on Linux to /var/tmp/
