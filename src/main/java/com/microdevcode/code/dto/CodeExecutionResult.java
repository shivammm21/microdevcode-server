package com.microdevcode.code.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionResult {
    private boolean success;
    private String status; // "ACCEPTED", "WRONG_ANSWER", "COMPILATION_ERROR", "RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED"
    private String message;
    private int totalTestCases;
    private int passedTestCases;
    private List<TestCaseResult> testResults;
    private long executionTime; // in milliseconds
    private String compilationOutput;
    private String runtimeOutput;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        private int testCaseNumber;
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private String errorMessage;
        private long executionTime;
    }
}