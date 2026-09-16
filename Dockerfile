FROM eclipse-temurin:17-jdk

WORKDIR /type-api

COPY . .
RUN ./gradlew bootJar

RUN cp build/libs/dtr-toolkit*.jar dtrtoolkit.jar

CMD ["java","-jar","dtrtoolkit.jar"]
