FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/theOffShoreProxy-v1.jar theOffShoreProxy.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "theOffShoreProxy.jar"]

# docker build -t off-shore-proxy .
# docker run -p 9090:9090 off-shore-proxy
# docker run --name off-shore-proxy --network ship-network -p 9090:9090 off-shore-proxy