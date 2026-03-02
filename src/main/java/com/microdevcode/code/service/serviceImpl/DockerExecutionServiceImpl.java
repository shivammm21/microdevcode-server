package com.microdevcode.code.service.serviceImpl;

import com.microdevcode.code.dto.CompilerRequest;
import com.microdevcode.code.dto.CompilerResponse;
import com.microdevcode.code.service.DockerExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DockerExecutionServiceImpl implements DockerExecutionService {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/microdevcode/";
    private static final int DEFAULT_TIMEOUT = 30;

    @Override
    public CompilerResponse executeInDocker(CompilerRequest request) {
        if (!isDockerAvailable()) {
            log.warn("Docker not available, cannot execute in container");
            return CompilerResponse.builder()
                    .success(false)
                    .error("Docker not available. Please install Docker or enable local execution.")
                    .exitCode(-1)
                    .build();
        }

        String sessionId = UUID.randomUUID().toString();
        Path workingDir = createWorkingDirectory(sessionId);
        
        try {
            // Write files to working directory
            writeFilesToDirectory(request.getFiles(), workingDir);
            
            // Check if required Docker image exists
            if (!checkDockerImage(request.getLanguage())) {
                return CompilerResponse.builder()
                        .success(false)
                        .error("Required Docker image not found. Please run docker-setup.sh to pull required images.")
                        .exitCode(-1)
                        .build();
            }
            
            // Execute in Docker container
            return executeInContainer(request, workingDir, sessionId);
            
        } catch (Exception e) {
            log.error("Docker execution failed for session {}: {}", sessionId, e.getMessage());
            return CompilerResponse.builder()
                    .success(false)
                    .error("Docker execution failed: " + e.getMessage())
                    .exitCode(-1)
                    .build();
        } finally {
            cleanupDirectory(workingDir);
        }
    }

    @Override
    public boolean isDockerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Docker not available: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkDockerImage(String language) {
        try {
            String imageName = getDockerImageName(language);
            ProcessBuilder pb = new ProcessBuilder("docker", "images", "-q", imageName);
            Process process = pb.start();
            
            String output = readProcessOutput(process);
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            return finished && process.exitValue() == 0 && !output.trim().isEmpty();
        } catch (Exception e) {
            log.warn("Failed to check Docker image for language {}: {}", language, e.getMessage());
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
            case "node":
                return "node:18-alpine";
            case "cpp":
            case "c++":
            case "c":
                return "gcc:12-alpine";
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    private CompilerResponse executeInContainer(CompilerRequest request, Path workingDir, String sessionId) {
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> command = buildDockerCommand(request, workingDir, sessionId);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir.toFile());
            
            Process process = pb.start();
            
            // Send input if provided
            if (request.getInput() != null && !request.getInput().isEmpty()) {
                try (PrintWriter writer = new PrintWriter(process.getOutputStream())) {
                    writer.println(request.getInput());
                    writer.flush();
                }
            }
            
            String output = readProcessOutput(process);
            String error = readProcessError(process);
            
            int timeLimit = request.getTimeLimit() > 0 ? request.getTimeLimit() : DEFAULT_TIMEOUT;
            boolean finished = process.waitFor(timeLimit, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                // Also stop the Docker container
                stopContainer(sessionId);
                return CompilerResponse.builder()
                        .success(false)
                        .error("Execution timeout")
                        .exitCode(-1)
                        .build();
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            int exitCode = process.exitValue();
            
            return CompilerResponse.builder()
                    .success(exitCode == 0)
                    .output(output)
                    .error(error)
                    .executionTime(executionTime)
                    .exitCode(exitCode)
                    .build();
                    
        } catch (Exception e) {
            log.error("Container execution error: {}", e.getMessage());
            return CompilerResponse.builder()
                    .success(false)
                    .error("Container execution error: " + e.getMessage())
                    .exitCode(-1)
                    .build();
        }
    }

    private List<String> buildDockerCommand(CompilerRequest request, Path workingDir, String sessionId) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("--name");
        command.add("microdevcode-" + sessionId);
        command.add("-v");
        command.add(workingDir.toAbsolutePath() + ":/app");
        command.add("-w");
        command.add("/app");
        command.add("--memory=" + (request.getMemoryLimit() > 0 ? request.getMemoryLimit() : 256) + "m");
        command.add("--cpus=0.5");
        command.add("--network=none"); // No network access for security
        command.add("--user=1000:1000"); // Run as non-root user
        
        // Add language-specific Docker image and command
        switch (request.getLanguage().toLowerCase()) {
            case "java":
                command.add("eclipse-temurin:17-jdk-alpine");
                command.add("sh");
                command.add("-c");
                command.add("javac *.java && java Main");
                break;
                
            case "python":
                command.add("python:3.11-alpine");
                command.add("python3");
                command.add(request.getMainFile());
                break;
                
            case "javascript":
            case "node":
                command.add("node:18-alpine");
                command.add("node");
                command.add(request.getMainFile());
                break;
                
            case "cpp":
            case "c++":
                command.add("gcc:12-alpine");
                command.add("sh");
                command.add("-c");
                command.add("g++ -o main *.cpp && ./main");
                break;
                
            case "c":
                command.add("gcc:12-alpine");
                command.add("sh");
                command.add("-c");
                command.add("gcc -o main *.c && ./main");
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported language for Docker execution: " + request.getLanguage());
        }
        
        return command;
    }

    private String getMainClassName(CompilerRequest request) {
        // Extract main class name from main file
        String mainFile = request.getMainFile();
        if (mainFile.endsWith(".java")) {
            return mainFile.substring(0, mainFile.length() - 5);
        }
        return "Main";
    }

    private void stopContainer(String sessionId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "stop", "microdevcode-" + sessionId);
            Process process = pb.start();
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to stop container: {}", e.getMessage());
        }
    }

    private Path createWorkingDirectory(String sessionId) {
        try {
            Path tempDir = Paths.get(TEMP_DIR);
            Files.createDirectories(tempDir);
            
            Path workingDir = tempDir.resolve(sessionId);
            Files.createDirectories(workingDir);
            
            return workingDir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create working directory", e);
        }
    }

    private void writeFilesToDirectory(java.util.Map<String, String> files, Path workingDir) throws IOException {
        // For Java Spring Boot projects, we need to add necessary imports and mock classes
        if (files.keySet().stream().anyMatch(name -> name.endsWith(".java"))) {
            addJavaTestingSupport(files, workingDir);
        }
        
        // Process and write user files with necessary modifications
        for (java.util.Map.Entry<String, String> entry : files.entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();
            
            // If it's a Java file, preprocess it to add missing imports and constructors
            if (fileName.endsWith(".java")) {
                content = preprocessJavaFile(content, fileName);
            }
            
            Path filePath = workingDir.resolve(fileName);
            Files.write(filePath, content.getBytes());
        }
    }

    private String preprocessJavaFile(String content, String fileName) {
        StringBuilder processedContent = new StringBuilder();
        
        // Add necessary imports at the top
        if (!content.contains("import java.util.*")) {
            processedContent.append("import java.util.*;\n");
        }
        
        // Add the original content
        processedContent.append(content);
        
        // If the class has @RequiredArgsConstructor, add actual constructors
        if (content.contains("@RequiredArgsConstructor")) {
            String className = extractClassName(fileName);
            if (className != null) {
                String constructor = generateConstructor(content, className);
                if (!constructor.isEmpty()) {
                    // Insert constructor before the last closing brace
                    String contentStr = processedContent.toString();
                    int lastBrace = contentStr.lastIndexOf("}");
                    if (lastBrace > 0) {
                        processedContent = new StringBuilder(contentStr.substring(0, lastBrace));
                        processedContent.append("\n").append(constructor).append("\n}");
                    }
                }
            }
        }
        
        return processedContent.toString();
    }

    private String extractClassName(String fileName) {
        if (fileName.endsWith(".java")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return null;
    }

    private String generateConstructor(String content, String className) {
        StringBuilder constructor = new StringBuilder();
        
        // Find private final fields to generate constructor
        String[] lines = content.split("\n");
        List<String> finalFields = new ArrayList<>();
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("private final ") && line.contains(";")) {
                // Extract field name and type
                String fieldDeclaration = line.replace("private final ", "").replace(";", "").trim();
                String[] parts = fieldDeclaration.split("\\s+");
                if (parts.length >= 2) {
                    String type = parts[0];
                    String name = parts[1];
                    finalFields.add(type + " " + name);
                }
            }
        }
        
        if (!finalFields.isEmpty()) {
            constructor.append("    public ").append(className).append("(");
            
            // Add parameters
            for (int i = 0; i < finalFields.size(); i++) {
                constructor.append(finalFields.get(i));
                if (i < finalFields.size() - 1) {
                    constructor.append(", ");
                }
            }
            
            constructor.append(") {\n");
            
            // Add field assignments
            for (String field : finalFields) {
                String[] parts = field.split("\\s+");
                if (parts.length >= 2) {
                    String fieldName = parts[1];
                    constructor.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                }
            }
            
            constructor.append("    }\n");
        }
        
        return constructor.toString();
    }

    private void addJavaTestingSupport(java.util.Map<String, String> files, Path workingDir) throws IOException {
        // Create a simple testing framework that provides Spring Boot-like annotations and classes
        String testingFramework = createJavaTestingFramework();
        Path frameworkPath = workingDir.resolve("TestingFramework.java");
        Files.write(frameworkPath, testingFramework.getBytes());
        
        // Create DTO classes if they don't exist
        if (!files.containsKey("OtpRequest.java")) {
            String otpRequest = createOtpRequestClass();
            Path otpRequestPath = workingDir.resolve("OtpRequest.java");
            Files.write(otpRequestPath, otpRequest.getBytes());
        }
        
        if (!files.containsKey("OtpResponse.java")) {
            String otpResponse = createOtpResponseClass();
            Path otpResponsePath = workingDir.resolve("OtpResponse.java");
            Files.write(otpResponsePath, otpResponse.getBytes());
        }
        
        if (!files.containsKey("VerifyRequest.java")) {
            String verifyRequest = createVerifyRequestClass();
            Path verifyRequestPath = workingDir.resolve("VerifyRequest.java");
            Files.write(verifyRequestPath, verifyRequest.getBytes());
        }
        
        if (!files.containsKey("VerifyResponse.java")) {
            String verifyResponse = createVerifyResponseClass();
            Path verifyResponsePath = workingDir.resolve("VerifyResponse.java");
            Files.write(verifyResponsePath, verifyResponse.getBytes());
        }
        
        // Create a Main class that simulates the Spring Boot application
        String mainClass = createMainTestClass();
        Path mainPath = workingDir.resolve("Main.java");
        Files.write(mainPath, mainClass.getBytes());
    }

    private String createJavaTestingFramework() {
        return """
            import java.lang.annotation.*;
            import java.util.*;
            import java.util.concurrent.ConcurrentHashMap;
            
            // Mock Spring Boot annotations
            @interface RestController {}
            @interface RequestMapping { String value() default ""; }
            @interface PostMapping { String value() default ""; }
            @interface RequestBody {}
            @interface Valid {}
            @interface Service {}
            @interface Repository {}
            @interface RequiredArgsConstructor {}
            
            // Mock ResponseEntity class
            class ResponseEntity<T> {
                private T body;
                private int status;
                
                public ResponseEntity(T body, int status) {
                    this.body = body;
                    this.status = status;
                }
                
                public static <T> ResponseEntity<T> ok(T body) {
                    return new ResponseEntity<>(body, 200);
                }
                
                public T getBody() { return body; }
                public int getStatus() { return status; }
            }
            
            // Simple in-memory storage for testing
            class OtpStorage {
                private static final Map<String, OtpData> storage = new ConcurrentHashMap<>();
                
                public static void saveOtp(String requestId, String otp, long ttlSeconds) {
                    storage.put(requestId, new OtpData(otp, System.currentTimeMillis(), 0));
                }
                
                public static String getOtp(String requestId) {
                    OtpData data = storage.get(requestId);
                    if (data == null) return null;
                    
                    // Check if expired (5 minutes)
                    if (System.currentTimeMillis() - data.createdAt > 300000) {
                        storage.remove(requestId);
                        return null;
                    }
                    return data.otp;
                }
                
                public static long incrementAttempts(String requestId) {
                    OtpData data = storage.get(requestId);
                    if (data != null) {
                        data.attempts++;
                        return data.attempts;
                    }
                    return 0;
                }
                
                static class OtpData {
                    String otp;
                    long createdAt;
                    int attempts;
                    
                    OtpData(String otp, long createdAt, int attempts) {
                        this.otp = otp;
                        this.createdAt = createdAt;
                        this.attempts = attempts;
                    }
                }
            }
            """;
    }

    private String createOtpRequestClass() {
        return """
            public class OtpRequest {
                private String phone;
                
                public OtpRequest() {}
                
                public OtpRequest(String phone) {
                    this.phone = phone;
                }
                
                public String getPhone() { return phone; }
                public void setPhone(String phone) { this.phone = phone; }
            }
            """;
    }

    private String createOtpResponseClass() {
        return """
            public class OtpResponse {
                private String requestId;
                private int expiresIn;
                
                public OtpResponse() {}
                
                public OtpResponse(String requestId, int expiresIn) {
                    this.requestId = requestId;
                    this.expiresIn = expiresIn;
                }
                
                public String getRequestId() { return requestId; }
                public void setRequestId(String requestId) { this.requestId = requestId; }
                public int getExpiresIn() { return expiresIn; }
                public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
            }
            """;
    }

    private String createVerifyRequestClass() {
        return """
            public class VerifyRequest {
                private String requestId;
                private String otp;
                
                public VerifyRequest() {}
                
                public VerifyRequest(String requestId, String otp) {
                    this.requestId = requestId;
                    this.otp = otp;
                }
                
                public String getRequestId() { return requestId; }
                public void setRequestId(String requestId) { this.requestId = requestId; }
                public String getOtp() { return otp; }
                public void setOtp(String otp) { this.otp = otp; }
            }
            """;
    }

    private String createVerifyResponseClass() {
        return """
            public class VerifyResponse {
                private boolean verified;
                
                public VerifyResponse() {}
                
                public VerifyResponse(boolean verified) {
                    this.verified = verified;
                }
                
                public boolean isVerified() { return verified; }
                public void setVerified(boolean verified) { this.verified = verified; }
            }
            """;
    }

    private String createMainTestClass() {
        return """
            import java.util.*;
            
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Testing OTP Service Implementation...");
                    
                    try {
                        // Create service instances using reflection-like approach
                        OtpRepository otpRepository = new OtpRepositoryImpl();
                        
                        // Create OtpService instance
                        OtpService otpService;
                        try {
                            // Try constructor with parameter first
                            otpService = new OtpService(otpRepository);
                        } catch (Exception e) {
                            // If that fails, try default constructor and set field via reflection simulation
                            otpService = new OtpService();
                            // Set the repository field manually
                            setField(otpService, "otpRepository", otpRepository);
                        }
                        
                        // Create OtpController instance
                        OtpController otpController;
                        try {
                            // Try constructor with parameter first
                            otpController = new OtpController(otpService);
                        } catch (Exception e) {
                            // If that fails, try default constructor and set field
                            otpController = new OtpController();
                            setField(otpController, "otpService", otpService);
                        }
                        
                        // Test 1: Request OTP
                        System.out.println("\\n=== Test 1: Request OTP ===");
                        OtpRequest request = new OtpRequest("+91-9876543210");
                        ResponseEntity<OtpResponse> response1 = otpController.requestOtp(request);
                        OtpResponse otpResponse = response1.getBody();
                        
                        if (otpResponse != null && otpResponse.getRequestId() != null) {
                            System.out.println("✅ OTP Request successful");
                            System.out.println("Request ID: " + otpResponse.getRequestId());
                            System.out.println("Expires In: " + otpResponse.getExpiresIn() + " seconds");
                            
                            // Test 2: Verify OTP (simulate correct OTP)
                            System.out.println("\\n=== Test 2: Verify OTP ===");
                            String storedOtp = OtpStorage.getOtp(otpResponse.getRequestId());
                            if (storedOtp != null) {
                                VerifyRequest verifyRequest = new VerifyRequest(otpResponse.getRequestId(), storedOtp);
                                ResponseEntity<VerifyResponse> response2 = otpController.verifyOtp(verifyRequest);
                                VerifyResponse verifyResponse = response2.getBody();
                                
                                if (verifyResponse != null && verifyResponse.isVerified()) {
                                    System.out.println("✅ OTP Verification successful");
                                } else {
                                    System.out.println("❌ OTP Verification failed");
                                }
                            }
                        } else {
                            System.out.println("❌ OTP Request failed");
                        }
                        
                        System.out.println("\\n=== All Tests Completed ===");
                        
                    } catch (Exception e) {
                        System.out.println("❌ Test failed with error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                // Helper method to set private fields (simulates dependency injection)
                private static void setField(Object target, String fieldName, Object value) {
                    try {
                        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        field.set(target, value);
                    } catch (Exception e) {
                        System.out.println("Warning: Could not set field " + fieldName + ": " + e.getMessage());
                    }
                }
            }
            
            // Default repository implementation
            class OtpRepositoryImpl implements OtpRepository {
                public void saveOtp(String requestId, String otp, long ttlSeconds) {
                    OtpStorage.saveOtp(requestId, otp, ttlSeconds);
                }
                
                public String getOtp(String requestId) {
                    return OtpStorage.getOtp(requestId);
                }
                
                public long incrementAttempts(String requestId) {
                    return OtpStorage.incrementAttempts(requestId);
                }
            }
            """;
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString().trim();
    }

    private String readProcessError(Process process) throws IOException {
        StringBuilder error = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }
        return error.toString().trim();
    }

    private void cleanupDirectory(Path workingDir) {
        try {
            if (Files.exists(workingDir)) {
                Files.walk(workingDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete: {}", path);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup directory: {}", workingDir);
        }
    }
}