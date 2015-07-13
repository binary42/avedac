## Requirements
* Install docker-compose according to these instructions: 
 ```
 curl -L https://github.com/docker/compose/releases/download/1.3.1/docker-compose-`uname -s`-`uname -m` > docker-compose```
 sudo mv docker-compose /usr/bin/```
 chmod +x /usr/bin/docker-compose```
 
* Build
Get the password from http://ilab.usc.edu/toolkit/downloads.shtml> for the toolkit, then run the Docker build with that 

```build.sh <password>```

Alternatively, edit Docker file replacing the SALIENCY_SVN_PASSWORD with the password then run  

```docker build -t saliency```

* Run
```docker run -ti -e DISPLAY=$DISPLAY danellecline/saliency /bin/bash```

* Check environment variables in the docker compose file with
```docker-compose run saliency  env```

