package com.microdevcode.code.service;

import com.microdevcode.code.dto.CompilerRequest;
import com.microdevcode.code.dto.CompilerResponse;
import org.springframework.stereotype.Service;

@Service
public interface CompilerService {
    CompilerResponse compile(CompilerRequest request);
    CompilerResponse execute(CompilerRequest request);
    CompilerResponse compileAndExecute(CompilerRequest request);
}