package com.microdevcode.code.service;

import com.microdevcode.code.dto.CompilerRequest;
import com.microdevcode.code.dto.CompilerResponse;
import org.springframework.stereotype.Service;

@Service
public interface DockerExecutionService {
    CompilerResponse executeInDocker(CompilerRequest request);
    boolean isDockerAvailable();
}