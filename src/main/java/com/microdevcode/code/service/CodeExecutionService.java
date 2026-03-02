package com.microdevcode.code.service;

import com.microdevcode.code.dto.CodeExecutionResult;
import com.microdevcode.code.dto.CodeSubmissionRequest;
import org.springframework.stereotype.Service;

@Service
public interface CodeExecutionService {
    CodeExecutionResult executeCode(CodeSubmissionRequest request);
    CodeExecutionResult runTestCases(CodeSubmissionRequest request);
}