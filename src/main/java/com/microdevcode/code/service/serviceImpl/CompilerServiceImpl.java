package com.microdevcode.code.service.serviceImpl;

import com.microdevcode.code.dto.CompilerRequest;
import com.microdevcode.code.dto.CompilerResponse;
import com.microdevcode.code.service.CompilerService;
import com.microdevcode.code.service.DockerExecutionService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class CompilerServiceImpl implements CompilerService {

    private final DockerExecutionService dockerExecutionService;
    
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/microdevcode/";
    private static final int DEFAULT_TIMEOUT = 30; // seconds

    @Override
    public CompilerResponse compile(CompilerRequest request) {
        // Try Docker first for better security
        if (dockerExecutionService.isDockerAvailable()) {
            log.info("Using Docker for code compilation");
            return dockerExecutionService.executeInDocker(request);
        }
        
        log.warn("Docker not available, using local compilation");
        return compileLocally(request);
    }

    @Override
    public CompilerResponse execute(CompilerRequest request) {
        // Try Docker first for better security
        if (dockerExecutionService.isDockerAvailable()) {
            log.info("Using Docker for code execution");
            return dockerExecutionService.executeInDocker(request);
        }
        
        log.warn("Docker not available, using local execution");
        return executeLocally(request);
    }

    @Override
    public CompilerResponse compileAndExecute(CompilerRequest request) {
        // Try Docker first for better security
        if (dockerExecutionService.isDockerAvailable()) {
            log.info("Using Docker for code compilation and execution");
            return dockerExecutionService.executeInDocker(request);
        }
        
        log.warn("Docker not available, using local compilation and execution");
        return compileAndExecuteLocally(request);
    }

    private CompilerResponse compileLocally(CompilerRequest request) {
        String sessionId = UUID.randomUUID().toString();
        Path workingDir = createWorkingDirectory(sessionId);
        
        try {
            // Write files to working directory
            writeFilesToDirectory(request.getFiles(), workingDir);
            
            // Compile based on language
            return compileCode(request, workingDir);
            
        } catch (Exception e) {
            log.error("Compilation failed for session {}: {}", sessionId, e.getMessage());
            return CompilerResponse.builder()
                    .success(false)
                    .error("Compilation failed: " + e.getMessage())
                    .exitCode(-1)
                    .build();
        } finally {
            cleanupDirectory(workingDir);
        }
    }

    private CompilerResponse executeLocally(CompilerRequest request) {
        String sessionId = UUID.randomUUID().toString();
        Path workingDir = createWorkingDirectory(sessionId);
        
        try {
            // Write files to working directory
            writeFilesToDirectory(request.getFiles(), workingDir);
            
            // Execute based on language
            return executeCode(request, workingDir);
            
        } catch (Exception e) {
            log.error("Execution failed for session {}: {}", sessionId, e.getMessage());
            return CompilerResponse.builder()
                    .success(false)
                    .error("Execution failed: " + e.getMessage())
                    .exitCode(-1)
                    .build();
        } finally {
            cleanupDirectory(workingDir);
        }
    }

    private CompilerResponse compileAndExecuteLocally(CompilerRequest request) {
        String sessionId = UUID.randomUUID().toString();
        Path workingDir = createWorkingDirectory(sessionId);
        
        try {
            // Write files to working directory
            writeFilesToDirectory(request.getFiles(), workingDir);
            
            // Compile first
            CompilerResponse compileResult = compileCode(request, workingDir);
            if (!compileResult.isSuccess()) {
                return compileResult;
            }
            
            // Then execute
            CompilerResponse executeResult = executeCode(request, workingDir);
            executeResult.setCompilationOutput(compileResult.getOutput());
            
            return executeResult;
            
        } catch (Exception e) {
            log.error("Compile and execute failed for session {}: {}", sessionId, e.getMessage());
            return CompilerResponse.builder()
                    .success(false)
                    .error("Compile and execute failed: " + e.getMessage())
                    .exitCode(-1)
                    .build();
        } finally {
            cleanupDirectory(workingDir);
        }
    }

    private CompilerResponse compileCode(CompilerRequest request, Path workingDir) {
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> command = buildCompileCommand(request, workingDir);
            if (command.isEmpty()) {
                // Interpreted language, no compilation needed
                return CompilerResponse.builder()
                        .success(true)
                        .output("No compilation needed for " + request.getLanguage())
                        .executionTime(0)
                        .exitCode(0)
                        .build();
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            String output = readProcessOutput(process);
            boolean finished = process.waitFor(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return CompilerResponse.builder()
                        .success(false)
                        .error("Compilation timeout")
                        .exitCode(-1)
                        .build();
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            int exitCode = process.exitValue();
            
            return CompilerResponse.builder()
                    .success(exitCode == 0)
                    .output(output)
                    .error(exitCode != 0 ? output : null)
                    .executionTime(executionTime)
                    .exitCode(exitCode)
                    .build();
                    
        } catch (Exception e) {
            log.error("Compilation error: {}", e.getMessage());
            return CompilerResponse.builder()
                    .success(false)
                    .error("Compilation error: " + e.getMessage())
                    .exitCode(-1)
                    .build();
        }
    }

    private CompilerResponse executeCode(CompilerRequest request, Path workingDir) {
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> command = buildExecuteCommand(request, workingDir);
            
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
            log.error("Execution error: {}", e.getMessage());
            return CompilerResponse.builder()
                    .success(false)
                    .error("Execution error: " + e.getMessage())
                    .exitCode(-1)
                    .build();
        }
    }

    private List<String> buildCompileCommand(CompilerRequest request, Path workingDir) {
        List<String> command = new ArrayList<>();
        
        switch (request.getLanguage().toLowerCase()) {
            case "java":
                command.add("javac");
                command.add("-cp");
                command.add(".");
                request.getFiles().keySet().stream()
                        .filter(name -> name.endsWith(".java"))
                        .forEach(command::add);
                break;
                
            case "cpp":
            case "c++":
                command.add("g++");
                command.add("-o");
                command.add("main");
                request.getFiles().keySet().stream()
                        .filter(name -> name.endsWith(".cpp") || name.endsWith(".cc"))
                        .forEach(command::add);
                break;
                
            case "c":
                command.add("gcc");
                command.add("-o");
                command.add("main");
                request.getFiles().keySet().stream()
                        .filter(name -> name.endsWith(".c"))
                        .forEach(command::add);
                break;
                
            // Interpreted languages don't need compilation
            case "python":
            case "javascript":
            case "node":
                // Return empty command for interpreted languages
                break;
                
            default:
                log.warn("Unsupported language for compilation: {}", request.getLanguage());
        }
        
        return command;
    }

    private List<String> buildExecuteCommand(CompilerRequest request, Path workingDir) {
        List<String> command = new ArrayList<>();
        
        switch (request.getLanguage().toLowerCase()) {
            case "java":
                command.add("java");
                command.add("-cp");
                command.add(".");
                // Extract main class name from main file
                String mainClass = request.getMainFile().replace(".java", "");
                command.add(mainClass);
                break;
                
            case "cpp":
            case "c++":
            case "c":
                command.add("./main");
                break;
                
            case "python":
                command.add("python3");
                command.add(request.getMainFile());
                break;
                
            case "javascript":
            case "node":
                command.add("node");
                command.add(request.getMainFile());
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported language: " + request.getLanguage());
        }
        
        return command;
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
                        .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
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