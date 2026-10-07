# *************************
# Build Stage
# *************************

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests


# *************************
# Run Stage
# *************************

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]