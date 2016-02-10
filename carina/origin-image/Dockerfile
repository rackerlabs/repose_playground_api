FROM node:argon

MAINTAINER Dimitry Ushakov <dimitry.ushakov@rackspace.com>

RUN apt-get update && apt-get install -y wget vim libxml2-utils build-essential gcc

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY package.json /usr/src/app/
RUN npm install

COPY . /usr/src/app

EXPOSE 8000

CMD [ "npm", "start" ]