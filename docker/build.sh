#!/bin/sh
export CFLAGS='-L /usr/lib/x86_64-linux-gnu/'
svn checkout --username $SALIENCY_USERNAME --password $SALIENCY_PASSWORD svn://isvn.usc.edu/software/invt/trunk/saliency
cd saliency
autoconf configure.ac > configure
# remove line that's failing in latest code
sed -i '/omnicamera/d' ./depoptions.in
./configure
make core
