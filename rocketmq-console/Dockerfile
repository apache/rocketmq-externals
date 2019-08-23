FROM maven:3-alpine as builder
ADD . /app
WORKDIR /app
RUN cd rocketmq-console \
  && mvn clean package -DskipTests

FROM openjdk:8-jre-alpine
COPY --from=builder /app/rocketmq-console/target/rocketmq-console-ng-1.0.1.jar /app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
