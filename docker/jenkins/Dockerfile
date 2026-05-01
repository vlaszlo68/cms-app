FROM jenkins/jenkins:lts-jdk21

USER root

RUN apt-get update && \
    apt-get install -y docker.io

# docker group hozzáadása
RUN groupadd -g 987 docker || true
RUN usermod -aG docker jenkins

USER jenkins