#
# Scala and sbt Dockerfile
#
# original file from
# https://github.com/hseeberger/scala-sbt
#
# Pull base image
FROM bellsoft/liberica-openjdk-alpine:17.0.5

# Env variables
ENV SCALA_VERSION 2.13.8
ENV SBT_VERSION   1.0.2
ENV APP_NAME      geoipService
ENV APP_VERSION   0.1-SNAPSHOT

ARG rediskey=key
ARG redishost=host
ARG geoipkey=key
ARG mongouri=mongodb://localhost:27017
ARG mongoro=false
ARG dbname=db
ARG port=8080
ARG bindhost=0.0.0.0
ARG clientpool=128
ARG serverpool=128

ENV REDISKEY=$rediskey
ENV REDISHOST=$redishost
ENV GEOIPKEY=$geoipkey
ENV DBNAME=$dbname
ENV PORT=$port
ENV BINDHOST=$bindhost
ENV CLIENTPOOL=$clientpool
ENV SERVERPOOL=$serverpool
ARG MONGOURI=$mongouri
ARG MONGORO=$mongoro

# ENV variables for App
RUN \
   apk add --no-cache curl bash busybox-extras

# Define working directory
WORKDIR /root
ENV PROJECT_HOME /usr/src

RUN mkdir -p $PROJECT_HOME/data

WORKDIR $PROJECT_HOME/data

# We are running http4s on this port so expose it
EXPOSE 8080
EXPOSE 5005
# Expose this port if you want to enable remote debugging: 5005

COPY target/scala-2.13/${APP_NAME}-assembly-$APP_VERSION.jar $PROJECT_HOME/data/$APP_NAME.jar

# This will run at start, it points to the .sh file in the bin directory to start the play app
ENTRYPOINT java -jar $PROJECT_HOME/data/$APP_NAME.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
# Add this arg to the script if you want to enable remote debugging: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005