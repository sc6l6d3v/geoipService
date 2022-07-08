#!/bin/bash
# load env vars
export  $(cat .env | grep -v ^\# | xargs)

    MONGORO=false \
    PORT=8080 \
    BINDHOST=0.0.0.0 \
    CLIENTPOOL=128 \
    SERVERPOOL=128 \
docker build --build-arg rediskey=$REDISKEY \
             --build-arg redishost=$REDISHOST \
             --build-arg mongouri=$MONGOURI \
             --build-arg mongoro=$MONGORO \
             --build-arg geoipkey=$GEOIPKEY \
             --build-arg dbname=$DBNAME \
             --build-arg port=$PORT \
             --build-arg bindhost=$BINDHOST \
             --build-arg clientpool=$CLIENTPOOL \
             --build-arg serverpool=$SERVERPOOL \
             -t geoip:rest .
