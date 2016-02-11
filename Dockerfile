# Base the container off of the official Microsoft ASP.NET container, version 1.0.0-rc1-update1
FROM ingensi/play-framework:latest

# Copy the website into the container
COPY . /app
WORKDIR /app

# Make port 5000 accessible on the container
EXPOSE 9000/tcp

# Run the webserver when the container is started
# ENTRYPOINT ["activator", "run"]
