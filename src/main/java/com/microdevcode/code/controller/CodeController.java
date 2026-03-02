package com.microdevcode.code.controller;

import com.microdevcode.code.dto.CodeExecutionResult;
import com.microdevcode.code.dto.CodeSubmissionRequest;
import com.microdevcode.code.service.CodeExecutionService;
import com.microdevcode.code.service.DockerExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/code")
@Slf4j
@RequiredArgsConstructor
public class CodeController {

    private final CodeExecutionService codeExecutionService;
    private final DockerExecutionService dockerExecutionService;

    @PostMapping("/compile")
    public ResponseEntity<?> compileCode(@RequestBody CodeSubmissionRequest request) {
        try {
            if (request == null || request.getCode() == null || request.getCode().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Code submission is required");
                return ResponseEntity.badRequest().body(error);
            }

            CodeExecutionResult result = codeExecutionService.executeCode(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("status", result.getStatus());
            response.put("message", result.getMessage());
            response.put("compilationOutput", result.getCompilationOutput());
            response.put("executionTime", result.getExecutionTime());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Code compilation failed: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/run")
    public ResponseEntity<?> runCode(@RequestBody CodeSubmissionRequest request) {
        try {
            if (request == null || request.getCode() == null || request.getCode().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Code submission is required");
                return ResponseEntity.badRequest().body(error);
            }

            if (request.getProblemId() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Problem ID is required");
                return ResponseEntity.badRequest().body(error);
            }

            CodeExecutionResult result = codeExecutionService.runTestCases(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("status", result.getStatus());
            response.put("message", result.getMessage());
            response.put("totalTestCases", result.getTotalTestCases());
            response.put("passedTestCases", result.getPassedTestCases());
            response.put("testResults", result.getTestResults());
            response.put("executionTime", result.getExecutionTime());
            response.put("compilationOutput", result.getCompilationOutput());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Code execution failed: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitCode(@RequestBody CodeSubmissionRequest request) {
        try {
            if (request == null || request.getCode() == null || request.getCode().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Code submission is required");
                return ResponseEntity.badRequest().body(error);
            }

            if (request.getProblemId() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Problem ID is required");
                return ResponseEntity.badRequest().body(error);
            }

            // For submission, we run all test cases including hidden ones
            CodeExecutionResult result = codeExecutionService.runTestCases(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("status", result.getStatus());
            response.put("message", result.getMessage());
            response.put("totalTestCases", result.getTotalTestCases());
            response.put("passedTestCases", result.getPassedTestCases());
            response.put("executionTime", result.getExecutionTime());
            
            // For submissions, we might want to hide detailed test results for security
            if ("ACCEPTED".equals(result.getStatus())) {
                response.put("verdict", "Accepted");
            } else {
                response.put("verdict", "Wrong Answer");
                // Only show first failed test case details
                if (result.getTestResults() != null && !result.getTestResults().isEmpty()) {
                    CodeExecutionResult.TestCaseResult firstFailed = result.getTestResults().stream()
                            .filter(tr -> !tr.isPassed())
                            .findFirst()
                            .orElse(null);
                    if (firstFailed != null) {
                        response.put("failedTestCase", firstFailed.getTestCaseNumber());
                        response.put("errorMessage", firstFailed.getErrorMessage());
                    }
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Code submission failed: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/languages")
    public ResponseEntity<?> getSupportedLanguages() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("languages", new String[]{
                "java", "python", "javascript", "cpp", "c"
        });
        return ResponseEntity.ok(response);
    }

    @GetMapping("/frameworks")
    public ResponseEntity<?> getSupportedFrameworks() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("frameworks", new String[]{
                "SPRING_BOOT", "EXPRESS_JS", "FAST_API", "FLASK", 
                "DJANGO_REST", "GIN", "FIBER", "ACTIX_WEB", "LARAVEL"
        });
        return ResponseEntity.ok(response);
    }

    @GetMapping("/docker/status")
    public ResponseEntity<?> getDockerStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Check if Docker is available
            boolean dockerAvailable = dockerExecutionService.isDockerAvailable();
            response.put("dockerAvailable", dockerAvailable);
            
            if (dockerAvailable) {
                // Check Docker images
                Map<String, Boolean> imageStatus = new HashMap<>();
                String[] languages = {"java", "python", "javascript", "cpp", "c"};
                
                for (String language : languages) {
                    try {
                        boolean imageExists = checkDockerImageExists(language);
                        imageStatus.put(language, imageExists);
                    } catch (Exception e) {
                        imageStatus.put(language, false);
                    }
                }
                
                response.put("imageStatus", imageStatus);
                response.put("message", "Docker is available");
            } else {
                response.put("message", "Docker is not available. System will use local execution.");
                response.put("recommendation", "Install Docker for better security and isolation");
            }
            
            response.put("success", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to check Docker status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private boolean checkDockerImageExists(String language) {
        try {
            String imageName = getDockerImageName(language);
            ProcessBuilder pb = new ProcessBuilder("docker", "images", "-q", imageName);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0 && !output.toString().trim().isEmpty();
            
        } catch (Exception e) {
            return false;
        }
    }

    private String getDockerImageName(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return "eclipse-temurin:17-jdk-alpine";
            case "python":
                return "python:3.11-alpine";
            case "javascript":
                return "node:18-alpine";
            case "cpp":
            case "c":
                return "gcc:12-alpine";
            default:
                return "unknown";
        }
    }
}
