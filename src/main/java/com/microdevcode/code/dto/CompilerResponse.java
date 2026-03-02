package com.microdevcode.code.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilerResponse {
    private boolean success;
    private String output;
    private String error;
    private long executionTime; // in milliseconds
    private int exitCode;
    private String compilationOutput;
}