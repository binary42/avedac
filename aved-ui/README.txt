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

4) Open the Mac PackageMaker document and drag in the entire folder aved-ui-0.4.3-SNAPSHOT-mac64-assembly and  aved-ui-0.4.3-SNAPSHOT

5) In the Configuration tab, set the Destination for both folders to /Applications/AVEDac, and set the distribution title to AVEDac

6) In the Contents tab, select the root folder, then set the owner to root and group to wheel for both folders,
check the boxes for Read/Execute for Owner,Group, Others and Write for Owner/Group.
Lastly, click Apply Recommendations to apply these changes.

7) Add the MCRInstaller.dmg package to run following installation. In the Distributions icon, Click actions, then Edit button in PostInstall. Select
Open File in the Actions. In the Type drop-down box, select Absolute Path and enter /Applications/AVEDac/tools/MCRInstaller.dmg  

8) Add the MCRInstaller.dmg package to run following installation. In the Distributions icon, Click actions, then Edit button in PostInstall. Select
Open File in the Actions. In the Type drop-down box, select Absolute Path and enter /Applications/AVEDac/tools/MCRInstaller.dmg  

8) Click the Build and Run icon and try installing and running the package. It will install to  /Applications/AVEDac






8) Click the Build and Run icon and try installing and running the package. It will install to  /Applications/AVEDac





