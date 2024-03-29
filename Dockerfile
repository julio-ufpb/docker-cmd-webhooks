FROM ubuntu:latest

# Install Docker client and Java
RUN apt-get update

RUN apt-get install -y \
    docker.io

RUN apt-get install -y \
	openjdk-17-jdk

ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64

# Expose Docker socket
VOLUME /var/run/docker.sock
VOLUME /usr/local/bin

# Expose port
EXPOSE 8080

COPY webhook-action.sh /usr/local/bin/webhook-action.sh
RUN chmod +x /usr/local/bin/webhook-action.sh

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]