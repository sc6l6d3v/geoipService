#!/usr/bin/env bash
REDISKEY='f1tdfbtp00t' REDISHOST=192.168.4.47 \
    MONGOURI="mongodb://crmacct:(4mVs3r{}|@wengen.iscs-i.com:8771/crm" MONGORO=false \
    GEOIPKEY=e4e5e2e7c82545e6829e8e580dc14ac3 \
    DBNAME=crm \
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
