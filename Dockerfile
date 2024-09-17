FROM eclipse-temurin:21-jre
LABEL authors="xcodeassociated"

COPY ./build/libs/*.jar ./app.jar

EXPOSE 8082

ENTRYPOINT exec java $JAVA_OPTS -jar -Dspring.profiles.active=docker ./app.jar