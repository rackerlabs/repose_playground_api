# Dockerfile for Repose (www.openrepose.org)

FROM ubuntu

MAINTAINER Jenny Vo (jenny.vo@rackspace.com)
ENV REPOSE_VER 7.1.2.0
RUN apt-get install -y wget
RUN wget -O - http://repo.openrepose.org/debian/pubkey.gpg | apt-key add - && echo "deb http://repo.openrepose.org/debian stable main" > /etc/apt/sources.list.d/openrepose.list
RUN apt-get update && apt-get install -y repose-valve=${REPOSE_VER} repose-filter-bundle=${REPOSE_VER} repose-extensions-filter-bundle=${REPOSE_VER}

# Remove default Repose configuration files
RUN rm /etc/repose/*.cfg.xml

# Copy our configuration files in.
ADD ./repose_configs/*.cfg.xml /etc/repose/

# Expose Port 8000 -- Change this to use other ports for Repose
EXPOSE 8000

# Start Repose
CMD java -jar /usr/share/repose/repose-valve.jar

