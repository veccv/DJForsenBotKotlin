FROM openjdk:21 AS builder
WORKDIR /workspace/app
COPY . .
RUN ./gradlew build -x test

FROM openjdk:21 AS runner
WORKDIR /app
COPY --from=builder /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
