#!/usr/bin/env bash
REDISKEY='f1tdf!@#' REDISHOST=192.168.4.46 MONGOURI="mongodb://electuser:el3ctus3r%21%40%23@wengen.iscs-i.com:8771/elections-2016" MONGORO=false CENSUSKEY=ee0fbe76aa7bb2e1dd36228a5582b009de649014C \
docker build --build-arg rediskey=$REDISKEY \
             --build-arg redishost=$REDISHOST \
             --build-arg mongourl=$MONGOURI \
             --build-arg mongoro=$MONGORO \
             --build-arg censuskey=$CENSUSKEY \
             -t covid:rest .
