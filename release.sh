#/bin/bash
# Copyright (c) MBARI 2009
# Author: D. Cline
# Date: December 28, 2009 - Created
#
#exit on error; this will exit this bash script when any command exits with
#a non-zero exit code
set -e
source aved-classifier/setup.sh
echo "Check in the code"
hg commit -m "final check-in before release"
echo "Prepare the release"
mvn -B -Dmaven.skip.test=true -Dresume=false install release:clean clean release:prepare release:perform
