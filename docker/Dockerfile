FROM ubuntu:14.10
MAINTAINER Danelle Cline <dcline@mbari.org>

RUN apt-get update && apt-get install -q -y \
  wget \
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
  liblapack-dev 
 
  #mesa-common-dev \

# Set /root as working directory
WORKDIR /root/

ADD build.sh build.sh
RUN chmod +x build.sh
CMD ["cd /root"]
RUN cd /saliency && ./build.sh
