#!/bin/bash

echo "🐳 Setting up Docker images for MicroDevCode execution environment..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    echo "Visit: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo "❌ Docker daemon is not running. Please start Docker."
    exit 1
fi

echo "✅ Docker is available"

# Pull required Docker images
echo "📥 Pulling Docker images..."

# Java - Eclipse Temurin (OpenJDK replacement) - JDK for compilation
echo "Pulling Java development kit..."
docker pull eclipse-temurin:17-jdk-alpine
if [ $? -eq 0 ]; then
    echo "✅ Java JDK image pulled successfully"
else
    echo "❌ Failed to pull Java JDK image"
fi

# Python
echo "Pulling Python runtime..."
docker pull python:3.11-alpine
if [ $? -eq 0 ]; then
    echo "✅ Python image pulled successfully"
else
    echo "❌ Failed to pull Python image"
fi

# Node.js
echo "Pulling Node.js runtime..."
docker pull node:18-alpine
if [ $? -eq 0 ]; then
    echo "✅ Node.js image pulled successfully"
else
    echo "❌ Failed to pull Node.js image"
fi

# GCC for C/C++
echo "Pulling GCC compiler..."
docker pull gcc:12-alpine
if [ $? -eq 0 ]; then
    echo "✅ GCC image pulled successfully"
else
    echo "❌ Failed to pull GCC image"
fi

echo ""
echo "🎉 Docker setup complete!"
echo ""
echo "Available images:"
docker images | grep -E "(eclipse-temurin|python|node|gcc)" | grep -E "(17-jdk-alpine|3.11-alpine|18-alpine|12-alpine)"

echo ""
echo "📋 Next steps:"
echo "1. Start your Spring Boot application"
echo "2. Test the code execution APIs using the Postman collection"
echo "3. If you encounter issues, check the logs for detailed error messages"

echo ""
echo "🔧 Troubleshooting:"
echo "- If images fail to pull, check your internet connection"
echo "- If Docker commands fail, ensure Docker daemon is running"
echo "- For permission issues, you may need to run with sudo or add your user to docker group"