FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/ba-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=70.0"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
