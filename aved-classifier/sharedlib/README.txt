After modifications to the Matlab functions and successful compile
a manual update of the packaging phase is required. This only needs
to be run once following the changes.

Run with

    >> mvn prepare-package

The reason for this is, although the header files are 
automatically generate in each compile phase, they are not copied
to the src/main/include folder on each compile phase, because this 
causes the release mojo to fail.
