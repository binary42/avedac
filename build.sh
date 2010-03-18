#/bin/bash
# Copyright (c) MBARI 2009
# Author: D. Cline
# Date: December 28, 2009 - Created
#
#exit on error; this will exit this bash script when any command exists with
#a non-zero exit code
set -e
source aved-classifier/setup.sh
mvn reactor:make -Dmake.folders=aved-ui 
