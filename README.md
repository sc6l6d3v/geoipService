# Covid stats

Working Scala REST backend using IP geolocation API to obtain data for IP address with redis caching.


### Docker

To run in production mode inside a Docker container we first have to build the image. E.g.

```
docker build --build-arg rediskey=$REDISKEY \
             --build-arg redishost=$REDISHOST \
             --build-arg mongourl=$MONGOURI \
             --build-arg mongoro=$MONGORO \
             --build-arg censuskey=$GEOIPKEY \
             -t geoip:rest .
```

The aforementioned command will build the image and tag it with the latest commit hash.

To run said image:

```
docker run --env MONGOURI=$MONGOURI --env MONGORO=false -d -p 8080:8080 geoip:rest
```

To attach to said image via shell:

```
docker exec -it <imagehash> /bin/bash
```
