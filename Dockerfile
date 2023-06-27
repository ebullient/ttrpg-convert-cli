# Use the official maven/Java 17 image to create a build artifact.
FROM maven:3.8.4-openjdk-17 AS build

# Set the working directory in the image to /app
WORKDIR /app

# Copy the pom.xml file to download dependencies
COPY pom.xml .

# Download the dependencies
RUN mvn dependency:go-offline -B

# Copy the rest of the code
COPY src ./src

# Package the application
RUN mvn package -DskipTests

# The second stage of Dockerfile to create the final docker image
FROM openjdk:17

# Set the working directory in the image to /app
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set the working directory in the image to /app/data
WORKDIR /app/data

# Run the application
ENTRYPOINT exec java -jar /app/app.jar $0 $@
