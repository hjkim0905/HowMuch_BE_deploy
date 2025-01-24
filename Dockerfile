# Base image
FROM openjdk:17-jdk-slim

# Install wget and dependencies
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    curl \
    unzip \
    chromium \
    chromium-driver \
    xvfb \
    libxi6 \
    libgconf-2-4

# Set Chrome and ChromeDriver path
ENV CHROME_PATH=/usr/bin/chromium
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver

# Create directory for application
WORKDIR /app

# Copy WAR file
COPY build/libs/*.war /app/app.war

# Set environment variables for Chrome
ENV DISPLAY=:99

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.war"] 