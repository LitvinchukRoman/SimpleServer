FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY plugins ./plugins
RUN mvn -B -ntp -f plugins/source-aggregator-maven-plugin/pom.xml install \
 && mvn -B -ntp -f plugins/code-stats-maven-plugin/pom.xml install

COPY pom.xml .
COPY config ./config
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM tomcat:10.1-jdk17
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/SimpleServer.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
