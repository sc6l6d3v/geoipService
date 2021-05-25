#!/usr/bin/env bash
REDISKEY='f1tdfbtp00t' REDISHOST=192.168.4.46 MONGOURI="mongodb://electuser:el3ctus3r%21%40%23@wengen.iscs-i.com:8771/elections-2016" MONGORO=false GEOIPKEY=e4e5e2e7c82545e6829e8e580dc14ac3 \
docker build --build-arg rediskey=$REDISKEY \
             --build-arg redishost=$REDISHOST \
             --build-arg mongourl=$MONGOURI \
             --build-arg mongoro=$MONGORO \
             --build-arg geoipkey=$GEOIPKEY \
             --build-arg dbname=$DBNAME \
             --build-arg port=$PORT \
             --build-arg bindhost=$BINDHOST \
             --build-arg threadpool=$THREADPOOL \
             -t geoip:rest .
