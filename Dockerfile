# ==========================================
# Stage 1: Build the JAR file inside Docker
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy only the pom.xml first to cache dependencies (speeds up future builds)
COPY pom.xml .

# Download dependencies (this layer is cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy the actual source code
COPY src ./src

# Build the application package, skipping tests to speed up deployment
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Create the slim runtime image
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the compiled JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Inform Render about the port, but rely on the environment variable at runtime
EXPOSE 8080

# Configure memory limits for Render's free tier and bind to the dynamic $PORT
ENTRYPOINT ["sh", "-c", "java -Xmx512m -jar app.jar --server.port=${PORT:-8080}"]