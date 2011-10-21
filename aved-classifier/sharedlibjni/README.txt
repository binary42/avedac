To debug the build, add -e -X to produce verbose messages, e.g.:
# mvn -e -X  -PLinux freehep-nar:nar-compile

To recompile just the test classes - these are not compiled by default
# mvn -PLinux64 test-compile 

To run the training tests 
# mvn -PLinux64 test-compile freehep-nar:nar-integration-test -Dtest=*TestTrain*

To run the collect tests
# mvn -PLinux64 test-compile freehep-nar:nar-integration-test -Dtest=*TestCollect*

To run all tests
# mvn -PLinux64 freehep-nar:nar-integration-test

To recompile the JNI classes and native code
# mvn -PLinux64 process-classes

