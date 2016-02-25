FROM java:7

MAINTAINER Dimitry Ushakov <dimitry.ushakov@rackspace.com>

RUN apt-get update && apt-get install -y wget vim libxml2-utils
RUN wget -O - http://repo.openrepose.org/debian/pubkey.gpg | apt-key add - && echo "deb http://repo.openrepose.org/debian stable main" > /etc/apt/sources.list.d/openrepose.list
RUN apt-get update && apt-get install -y repose-valve=REPOSE_VERSION repose-filter-bundle=REPOSE_VERSION repose-extensions-filter-bundle=REPOSE_VERSION

RUN rm -rf /etc/repose/*

COPY repose_config /etc/repose

RUN find /etc/repose -maxdepth 1 -type f -exec xmllint --format -o {} {} \;

VOLUME /etc/repose
WORKDIR /etc/repose

USER repose

RUN touch /var/log/repose/current.log

USER root

EXPOSE 8080 9777

CMD service repose-valve start && tail -f /var/log/repose/current.log
