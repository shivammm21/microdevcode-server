# Code Compiler Integration Guide

This document explains how to use the integrated code compiler and execution system in the microdevcode platform.

## Overview

The system provides a secure, sandboxed environment for compiling and executing user-submitted code against test cases, similar to LeetCode but focused on microservice API development practice.

## Features

- **Multi-language Support**: Java, Python, JavaScript, C++, C
- **Framework Support**: Spring Boot, Express.js, FastAPI, Flask, Django REST, Gin, Fiber, Actix Web, Laravel
- **Docker Isolation**: Secure execution in Docker containers (when available)
- **Test Case Validation**: Automatic comparison of expected vs actual output
- **Time & Memory Limits**: Configurable execution constraints
- **Compilation Feedback**: Detailed error messages and compilation output

## API Endpoints

### 1. Compile Code
```http
POST /api/v1/code/compile
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "problemId": 1,
  "framework": "SPRING_BOOT",
  "language": "java",
  "code": {
    "OtpController.java": "...",
    "OtpService.java": "..."
  }
}
```

**Response:**
```json
{
  "success": true,
  "status": "COMPILED",
  "message": "Code compiled successfully",
  "compilationOutput": "...",
  "executionTime": 1250
}
```

### 2. Run Test Cases
```http
POST /api/v1/code/run
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "problemId": 1,
  "framework": "SPRING_BOOT",
  "language": "java",
  "code": {
    "OtpController.java": "...",
    "OtpService.java": "..."
  }
}
```

**Response:**
```json
{
  "success": true,
  "status": "ACCEPTED",
  "message": "Passed 2 out of 2 test cases",
  "totalTestCases": 2,
  "passedTestCases": 2,
  "executionTime": 3500,
  "testResults": [
    {
      "testCaseNumber": 1,
      "passed": true,
      "input": "POST /otp/request\n{\"phone\": \"+91-9876543210\"}",
      "expectedOutput": "{\"requestId\": \"req_abc123\", \"expiresIn\": 300}",
      "actualOutput": "{\"requestId\": \"req_def456\", \"expiresIn\": 300}",
      "executionTime": 1750
    }
  ]
}
```

### 3. Submit Solution
```http
POST /api/v1/code/submit
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "problemId": 1,
  "framework": "SPRING_BOOT",
  "language": "java",
  "code": {
    "OtpController.java": "...",
    "OtpService.java": "..."
  }
}
```

### 4. Get Supported Languages
```http
GET /api/v1/code/languages
```

### 5. Get Supported Frameworks
```http
GET /api/v1/code/frameworks
```

## Example Usage

### Java Spring Boot Example

```java
// OtpController.java
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {
    
    private final OtpService otpService;
    
    @PostMapping("/request")
    public ResponseEntity<OtpResponse> requestOtp(@RequestBody @Valid OtpRequest body) {
        return ResponseEntity.ok(otpService.requestOtp(body.getPhone()));
    }
    
    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verifyOtp(@RequestBody @Valid VerifyRequest body) {
        return ResponseEntity.ok(otpService.verifyOtp(body.getRequestId(), body.getOtp()));
    }
}
```

### Python FastAPI Example

```python
# main.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI()

class OtpRequest(BaseModel):
    phone: str

class OtpResponse(BaseModel):
    requestId: str
    expiresIn: int

@app.post("/otp/request", response_model=OtpResponse)
async def request_otp(request: OtpRequest):
    # Implementation here
    return OtpResponse(requestId="req_abc123", expiresIn=300)
```

### JavaScript Express.js Example

```javascript
// server.js
const express = require('express');
const app = express();

app.use(express.json());

app.post('/otp/request', (req, res) => {
    const { phone } = req.body;
    if (!phone) {
        return res.status(400).json({ error: 'phone required' });
    }
    
    res.json({
        requestId: 'req_abc123',
        expiresIn: 300
    });
});

app.listen(3000);
```

## Security Features

1. **Docker Isolation**: Code runs in isolated Docker containers
2. **Resource Limits**: Memory and CPU constraints
3. **Network Isolation**: No external network access
4. **Time Limits**: Execution timeout protection
5. **File System Restrictions**: Limited file system access
6. **User Permissions**: Non-root execution in containers

## Configuration

The system can be configured via `application.properties`:

```properties
# Enable/disable Docker execution
code.execution.docker-enabled=true

# Default timeout for code execution (seconds)
code.execution.default-timeout-seconds=30

# Maximum memory per execution (MB)
code.execution.max-memory-mb=256

# Maximum concurrent executions
code.execution.max-concurrent-executions=10

# Allowed programming languages
code.execution.allowed-languages=java,python,javascript,cpp,c
```

## Docker Setup

For optimal security, install Docker and pull the required images:

### Quick Setup
```bash
# Make the setup script executable and run it
chmod +x docker-setup.sh
./docker-setup.sh
```

### Manual Setup
```bash
# Install Docker (if not already installed)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Pull required images with correct tags
docker pull eclipse-temurin:17-jdk-alpine   # Java JDK (includes compiler)
docker pull python:3.11-alpine             # Python runtime  
docker pull node:18-alpine                 # Node.js runtime
docker pull gcc:12-alpine                  # C/C++ compiler
```

### Verify Installation
```bash
# Check if images are available
docker images | grep -E "(eclipse-temurin|python|node|gcc)"

# Test Docker execution
docker run --rm eclipse-temurin:17-jdk-alpine javac -version
docker run --rm eclipse-temurin:17-jdk-alpine java -version
```

## Error Handling

The system handles various error scenarios:

- **Compilation Errors**: Syntax errors, missing dependencies
- **Runtime Errors**: Exceptions, crashes during execution
- **Timeout Errors**: Code execution exceeds time limit
- **Memory Errors**: Code exceeds memory limit
- **Output Mismatch**: Actual output doesn't match expected

## Best Practices

1. **Code Structure**: Follow the framework-specific structure
2. **Error Handling**: Implement proper error handling in your code
3. **Input Validation**: Validate inputs according to problem requirements
4. **Output Format**: Ensure output matches expected format exactly
5. **Resource Usage**: Write efficient code to avoid timeouts

## Troubleshooting

### Common Issues

1. **Docker Image Not Found**: 
   - Error: `manifest for openjdk:17-alpine not found`
   - Solution: Run `./docker-setup.sh` to pull correct images
   
2. **Docker Not Available**: System falls back to local execution
   - Check if Docker is installed: `docker --version`
   - Check if Docker daemon is running: `docker info`
   
3. **Compilation Timeout**: Reduce code complexity or optimize imports

4. **Runtime Timeout**: Optimize algorithms and reduce time complexity

5. **Output Mismatch**: Check for extra whitespace or formatting issues

6. **Permission Denied**: 
   - Add user to docker group: `sudo usermod -aG docker $USER`
   - Restart terminal or logout/login

### Debug Tips

1. Use the `/compile` endpoint first to check for compilation errors
2. Test with simple inputs before complex test cases
3. Check the `compilationOutput` field for detailed error messages
4. Verify your code structure matches the expected framework pattern

## Integration with Frontend

The frontend can integrate with these APIs to provide:

1. **Code Editor**: Syntax highlighting for supported languages
2. **Real-time Compilation**: Check code as user types
3. **Test Results Display**: Show passed/failed test cases
4. **Progress Tracking**: Track user's problem-solving progress
5. **Leaderboard**: Compare performance with other users