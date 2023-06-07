FROM openjdk:8-jdk-alpine

WORKDIR /dtrtoolkit

COPY target/*.jar dtrtoolkit.jar

ENTRYPOINT ["java","-jar","/dtrtoolkit/dtrtoolkit.jar"]