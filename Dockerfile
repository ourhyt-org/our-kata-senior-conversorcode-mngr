# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -q clean package -DskipTests

# Lambda runtime stage
FROM public.ecr.aws/lambda/java:21

# Copy legacy-jar layout (what you have in target/)
COPY --from=build /app/target/*-runner.jar ${LAMBDA_TASK_ROOT}/app.jar
COPY --from=build /app/target/lib/ ${LAMBDA_TASK_ROOT}/lib/

# Run as a normal Java process inside Lambda container
CMD ["java", "-cp", "app.jar:lib/*", "io.quarkus.runner.GeneratedMain"]