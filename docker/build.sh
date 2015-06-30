#!/bin/sh


#################### surpress storing passwords  #########################
mkdir ~/.subversion/
echo "store-plaintext-passwords=off" > ~/.subversion/config

#################### Build saliency #########################
export CFLAGS='-L /usr/lib/x86_64-linux-gnu/'
svn checkout --username $SALIENCY_USERNAME --password $SALIENCY_PASSWORD svn://isvn.usc.edu/software/invt/trunk/saliency
export SALIENCYROOT=`pwd`/saliency
pushd saliency
autoconf configure.ac > configure
./configure
# remove line that's failing in latest code
sed -i '/omnicamera/d' ./depoptions.in
make core
popd

#################### Build XML library #########################
svn co https://svn.apache.org/repos/asf/xerces/c/tags/Xerces-C_2_7_0
export XERCESCROOT=`pwd`/Xerces-C_2_7_0
pushd $XERCESCROOT/src/xercesc
./runConfigure -p linux -cgcc -xg++ -minmem -nsocket -tnative -rpthread -P /usr/local -b64 
make
popd

#################### Build OpenCV #########################
VERSION=3.0.0
wget http://sourceforge.net/projects/opencvlibrary/files/opencv-unix/3.0.0/opencv-3.0.0.zip/download -O opencv-$VERSION.zip
unzip opencv-$VERSION.zip; rm *.zip
mkdir opencv-$VERSION/build
pushd opencv-$VERSION/build
cmake -D CMAKE_BUILD_TYPE=RELEASE -D WITH_TBB=ON -D WITH_V4L=OFF -D INSTALL_C_EXAMPLES=OFF -D INSTALL_PYTHON_EXAMPLES=OFF -D BUILD_EXAMPLES=OFF -D WITH_OPENGL=ON ..
make
make install
popd

#################### Build avedac #########################
git clone https://github.com/danellecline/avedac.git
pushd avedac/aved-mbarivision/src/main/cpp
./configure --prefix=/usr/local  --with-saliency=${SALIENCYROOT} --with-xercesc=${XERCESCROOT}
popd
