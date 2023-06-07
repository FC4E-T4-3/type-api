FROM eclipse-temurin:17-jdk-alpine

WORKDIR /dtrtoolkit

COPY build/libs/type-0.0.1-SNAPSHOT.jar dtrtoolkit.jar
COPY src/main/config/config.toml src/main/config/config.toml 


ENTRYPOINT ["java","-jar","/dtrtoolkit/dtrtoolkit.jar"]
