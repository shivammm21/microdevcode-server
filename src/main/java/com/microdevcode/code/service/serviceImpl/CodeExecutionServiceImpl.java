package com.microdevcode.code.service.serviceImpl;

import com.microdevcode.code.dto.*;
import com.microdevcode.code.entity.Problem;
import com.microdevcode.code.entity.TestCase;
import com.microdevcode.code.repository.ProblemRepo;
import com.microdevcode.code.service.CodeExecutionService;
import com.microdevcode.code.service.CompilerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionServiceImpl implements CodeExecutionService {

    private final CompilerService compilerService;
    private final ProblemRepo problemRepo;

    @Override
    public CodeExecutionResult executeCode(CodeSubmissionRequest request) {
        try {
            // Get problem details
            Optional<Problem> problemOpt = problemRepo.findById(request.getProblemId());
            if (problemOpt.isEmpty()) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .status("ERROR")
                        .message("Problem not found")
                        .build();
            }

            Problem problem = problemOpt.get();
            
            // Validate framework support
            if (!problem.getDefaultCode().containsKey(request.getFramework())) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .status("ERROR")
                        .message("Framework not supported for this problem")
                        .build();
            }

            // Prepare compiler request
            CompilerRequest compilerRequest = CompilerRequest.builder()
                    .language(request.getLanguage())
                    .files(request.getCode())
                    .mainFile(getMainFile(request))
                    .timeLimit(30) // 30 seconds timeout
                    .memoryLimit(256) // 256 MB memory limit
                    .build();

            // Test compilation first
            CompilerResponse compileResult = compilerService.compile(compilerRequest);
            if (!compileResult.isSuccess()) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .status("COMPILATION_ERROR")
                        .message("Compilation failed")
                        .compilationOutput(compileResult.getError())
                        .build();
            }

            return CodeExecutionResult.builder()
                    .success(true)
                    .status("COMPILED")
                    .message("Code compiled successfully")
                    .compilationOutput(compileResult.getOutput())
                    .executionTime(compileResult.getExecutionTime())
                    .build();

        } catch (Exception e) {
            log.error("Code execution failed: {}", e.getMessage());
            return CodeExecutionResult.builder()
                    .success(false)
                    .status("ERROR")
                    .message("Internal error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public CodeExecutionResult runTestCases(CodeSubmissionRequest request) {
        try {
            // Get problem details
            Optional<Problem> problemOpt = problemRepo.findById(request.getProblemId());
            if (problemOpt.isEmpty()) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .status("ERROR")
                        .message("Problem not found")
                        .build();
            }

            Problem problem = problemOpt.get();
            List<TestCase> testCases = problem.getExampleTestCases();
            
            if (testCases == null || testCases.isEmpty()) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .status("ERROR")
                        .message("No test cases found for this problem")
                        .build();
            }

            // Validate framework support
            if (!problem.getDefaultCode().containsKey(request.getFramework())) {
                return CodeExecutionResult.builder()
                        .success(false)
                        .status("ERROR")
                        .message("Framework not supported for this problem")
                        .build();
            }

            List<CodeExecutionResult.TestCaseResult> testResults = new ArrayList<>();
            int passedCount = 0;
            long totalExecutionTime = 0;

            // Run each test case
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                
                // Skip hidden test cases for now (can be enabled for final submission)
                if (testCase.getIsHide()) {
                    continue;
                }

                CodeExecutionResult.TestCaseResult result = runSingleTestCase(
                        request, testCase, i + 1);
                
                testResults.add(result);
                totalExecutionTime += result.getExecutionTime();
                
                if (result.isPassed()) {
                    passedCount++;
                }
            }

            // Determine overall status
            String status;
            if (passedCount == testResults.size()) {
                status = "ACCEPTED";
            } else if (passedCount == 0) {
                status = "WRONG_ANSWER";
            } else {
                status = "PARTIAL_ACCEPTED";
            }

            return CodeExecutionResult.builder()
                    .success(passedCount > 0)
                    .status(status)
                    .message(String.format("Passed %d out of %d test cases", passedCount, testResults.size()))
                    .totalTestCases(testResults.size())
                    .passedTestCases(passedCount)
                    .testResults(testResults)
                    .executionTime(totalExecutionTime)
                    .build();

        } catch (Exception e) {
            log.error("Test case execution failed: {}", e.getMessage());
            return CodeExecutionResult.builder()
                    .success(false)
                    .status("ERROR")
                    .message("Internal error: " + e.getMessage())
                    .build();
        }
    }

    private CodeExecutionResult.TestCaseResult runSingleTestCase(
            CodeSubmissionRequest request, TestCase testCase, int testNumber) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Prepare compiler request with test input
            CompilerRequest compilerRequest = CompilerRequest.builder()
                    .language(request.getLanguage())
                    .files(request.getCode())
                    .mainFile(getMainFile(request))
                    .input(testCase.getInput())
                    .timeLimit(10) // 10 seconds per test case
                    .memoryLimit(256)
                    .build();

            // Compile and execute
            CompilerResponse result = compilerService.compileAndExecute(compilerRequest);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!result.isSuccess()) {
                return CodeExecutionResult.TestCaseResult.builder()
                        .testCaseNumber(testNumber)
                        .passed(false)
                        .input(testCase.getInput())
                        .expectedOutput(testCase.getExpectedOutput())
                        .actualOutput("")
                        .errorMessage(result.getError())
                        .executionTime(executionTime)
                        .build();
            }

            // Compare output
            String actualOutput = result.getOutput().trim();
            String expectedOutput = testCase.getExpectedOutput().trim();
            boolean passed = compareOutputs(actualOutput, expectedOutput);

            return CodeExecutionResult.TestCaseResult.builder()
                    .testCaseNumber(testNumber)
                    .passed(passed)
                    .input(testCase.getInput())
                    .expectedOutput(expectedOutput)
                    .actualOutput(actualOutput)
                    .errorMessage(passed ? null : "Output mismatch")
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return CodeExecutionResult.TestCaseResult.builder()
                    .testCaseNumber(testNumber)
                    .passed(false)
                    .input(testCase.getInput())
                    .expectedOutput(testCase.getExpectedOutput())
                    .actualOutput("")
                    .errorMessage("Runtime error: " + e.getMessage())
                    .executionTime(executionTime)
                    .build();
        }
    }

    private boolean compareOutputs(String actual, String expected) {
        // Normalize whitespace and line endings
        String normalizedActual = actual.replaceAll("\\s+", " ").trim();
        String normalizedExpected = expected.replaceAll("\\s+", " ").trim();
        
        return normalizedActual.equals(normalizedExpected);
    }

    private String getMainFile(CodeSubmissionRequest request) {
        // Determine main file based on framework and language
        switch (request.getFramework()) {
            case SPRING_BOOT:
                return request.getCode().keySet().stream()
                        .filter(name -> name.contains("Controller") || name.contains("Application"))
                        .findFirst()
                        .orElse("Main.java");
                        
            case EXPRESS_JS:
            case FAST_API:
            case FLASK:
            case DJANGO_REST:
                return request.getCode().keySet().stream()
                        .filter(name -> name.contains("main") || name.contains("app") || name.contains("server"))
                        .findFirst()
                        .orElse("main.py");
                        
            case GIN:
            case FIBER:
                return "main.go";
                
            case ACTIX_WEB:
                return "main.rs";
                
            case LARAVEL:
                return request.getCode().keySet().stream()
                        .filter(name -> name.contains("Controller"))
                        .findFirst()
                        .orElse("main.php");
                        
            default:
                // Return first file as fallback
                return request.getCode().keySet().iterator().next();
        }
    }
}