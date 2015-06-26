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

# Set ~/torch as working directory
WORKDIR /root/

ADD build.sh build.sh
RUN chmod +x build.sh
CMD ["cd /root"]
CMD ["./build.sh"]

#ADD . /src
#ADD . /src
#RUN cd /src && make

#IN /foo { touch /hello.txt }

#IN /var/build {
#    FROM busybox
#    EXPOSE 80
#    ENTRYPOINT /usr/local/bin/app
#}

#RUN cp /src/build/app /var/build/usr/local/bin/app
#PUBLISH /var/build
#PUBLISH /foo
