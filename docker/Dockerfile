FROM ubuntu:14.10
MAINTAINER Danelle Cline <dcline@mbari.org>

RUN apt-get update && apt-get install -q -y \
  git \
  wget \
  cmake \
  unzip \
  build-essential \
  subversion \
  autoconf \
  flex \
  zlib1g-dev \
  tcl \
  libjpeg-dev \
  libx11-dev \
  libxext-dev \
  freeglut3-dev \ 
  liblapack-dev \ 
  mesa-common-dev \
  libgl1-mesa-dev


# Set /code as working directory
WORKDIR /code/
RUN cd /code

# Build OpenCV 
ENV VERSION 3.0.0
RUN wget http://sourceforge.net/projects/opencvlibrary/files/opencv-unix/3.0.0/opencv-3.0.0.zip/download -O opencv-${VERSION}.zip
RUN unzip opencv-${VERSION}.zip
RUN mkdir opencv-${VERSION}/build
WORKDIR /code/opencv-${VERSION}/build
RUN pwd 
RUN cmake -D CMAKE_BUILD_TYPE=RELEASE -D WITH_TBB=ON -D WITH_V4L=OFF -D INSTALL_C_EXAMPLES=OFF -D INSTALL_PYTHON_EXAMPLES=OFF -D BUILD_EXAMPLES=OFF -D WITH_OPENGL=ON ..
RUN make
RUN make install
RUN cd .. 
#RUN rm *.zip

# Supress warning about storing unencrypted password that prompts yes/no answer 
RUN mkdir ~/.subversion
RUN echo "[global]" > ~/.subversion/config
RUN echo "store-plaintext-passwords=off" >> ~/.subversion/config

# Build saliency
RUN export CFLAGS='-L /usr/lib/x86_64-linux-gnu/'
RUN svn checkout --username anonsvn --password SALIENCY_SVN_PASSWORD svn://isvn.usc.edu/software/invt/trunk/saliency
RUN export SALIENCYROOT=`pwd`/saliency
RUN cd saliency
RUN autoconf configure.ac > configure
RUN ./configure
# remove line that's failing in latest code
RUN sed -i '/omnicamera/d' ./depoptions.in
RUN make core
RUN cd .. 
