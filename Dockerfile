# ==== Build stage ====
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (optional optimization step)
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# ==== Run stage ====
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose default app port (Render will still route using $PORT)
EXPOSE 8080

# Start the Spring Boot app
ENTRYPOINT ["java","-jar","app.jar"]
