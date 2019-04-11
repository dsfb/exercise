FROM openjdk:8-jdk-alpine
COPY build/libs/exercise-0.0.1-SNAPSHOT.jar exercise-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/exercise-0.0.1-SNAPSHOT.jar"]
