# One image recipe for every Java service. Build the jar first (./gradlew bootJar), then:
#   docker build -f deploy/docker/java-service.Dockerfile --build-arg MODULE=recon-engine/worker -t reconark/worker .
# (run from the code/ folder)
FROM eclipse-temurin:25-jre
ARG MODULE
WORKDIR /app
COPY ${MODULE}/build/libs/app.jar /app/app.jar
RUN mkdir -p /data/files && chown -R 1001:1001 /data
USER 1001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
