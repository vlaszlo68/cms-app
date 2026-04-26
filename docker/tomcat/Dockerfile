FROM tomcat:9-jdk21-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY target/cms-app.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
