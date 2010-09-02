#/bin/sh 
# Copyright (c) MBARI 2009
# Author: D. Cline

# This will register this application with the Mac OSX launcher and only
# needs to be run once, unless the application name changes, then it should 
# be modified and run again on the target machine
DIR=$(cd "$(dirname "$0")"; pwd)
exec /System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -v -f "$DIR/AVEDac.app"
 