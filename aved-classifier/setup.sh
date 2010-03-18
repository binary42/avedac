#/bin/bash
#set -x
# Copyright (c) MBARI 2009
# Author: D. Cline
# Date: December 28, 2009 - Created
#
#exit on error; this will exit this bash script when any command exists with
#a non-zero exit code
set -e
export MATLAB_ROOT=/Applications/MATLAB_R2009a.app
export DYLD_LIBRARY_PATH=~/aved/lib:$MATLAB_ROOT/runtime:$MATLAB_ROOT/bin/maci:$MATLAB_ROOT/sys/os/maci:/System/Library/Frameworks/JavaVM.framework/JavaVM:/System/Library/Frameworks/JavaVM.framework/Libraries
export XAPPLRESDIR=$MATLAB_ROOT/X11/app-defaults

