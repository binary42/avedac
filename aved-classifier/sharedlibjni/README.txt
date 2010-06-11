The correct build order:

mvn nar:nar-javah;mvn package;mvn nar:nar-compile;mvn install;mvn nar:nar-integration-test

To debug the build, add -e -X to produce verbose messages, e.g.:

mvn -e -X nar:nar-compile

To run the integration tests 

# mvn nar:nar-integration-test -Dtest=*TestTrain*

To run the collect tests

# mvn nar:nar-integration-test -Dtest=*TestCollect*

To run all tests

# mvn nar:nar-integration-test

To recompile the JNI classes and native code

# mvn process-classes

To recompile just the test classes

# mvn test-compile 
