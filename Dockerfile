#
# Scala and sbt Dockerfile
#
# original file from
# https://github.com/hseeberger/scala-sbt
#

# Pull base image
FROM bellsoft/liberica-openjdk-alpine:21-cds

# Env variables
ENV SCALA_VERSION=3.3.8
ENV SBT_VERSION=1.10.5
ENV APP_NAME=geoipService
ENV APP_VERSION=0.1-SNAPSHOT

# ENV variables for App
RUN apk add --no-cache curl bash busybox-extras

# Define working directory
WORKDIR /root
ENV PROJECT_HOME=/usr/src

RUN mkdir -p $PROJECT_HOME/data

WORKDIR $PROJECT_HOME/data

# We are running http4s on this port so expose it
EXPOSE 8080
EXPOSE 5050

COPY target/scala-${SCALA_VERSION}/${APP_NAME}-assembly-$APP_VERSION.jar $PROJECT_HOME/data/$APP_NAME.jar

# This will run at start, it points to the .sh file in the bin directory to start the play app
ENTRYPOINT [ "sh", "-c", "exec java -Djava.net.preferIPv4Stack=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5050 -jar \"$PROJECT_HOME/data/$APP_NAME.jar\"" ]
