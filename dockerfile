FROM openjdk:8-jdk-alpine

WORKDIR /dtrtoolkit

COPY dtrtoolkit.jar dtrtoolkit.jar

ENTRYPOINT ["java","-jar","/dtrtoolkit/dtrtoolkit.jar"]
