FROM openjdk:21.0.3+9.0.LTS AS builder
WORKDIR /workspace/app
COPY . .
RUN ./gradlew build -x test

FROM openjdk:21.0.3+9.0.LTS AS runner
WORKDIR /app
COPY --from=builder /workspace/app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
