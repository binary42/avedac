To run in a docker container:

```docker run -ti -e DISPLAY=$DISPLAY danellecline/saliency /bin/bash```

To build docker container
```docker build -t -e SALIENCY_PASSWORD='password' -e SALIENCY_USERNAME='username' danellecline/saliency ```

