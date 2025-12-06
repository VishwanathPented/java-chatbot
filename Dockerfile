# =========================
# 1) Build Stage
# =========================
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom.xml first (for caching dependencies)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Now copy the whole source
COPY src ./src

# Build the JAR
RUN mvn -q clean package -DskipTests

# =========================
# 2) Run Stage
# =========================
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built JAR from previous stage
COPY --from=build /app/target/*.jar app.jar

# Render will inject PORT environment variable
EXPOSE 8080

# Start the Spring Boot app
CMD ["java", "-jar", "app.jar"]
