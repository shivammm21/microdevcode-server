package com.microdevcode.code.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilerRequest {
    private String language;
    private Map<String, String> files; // filename -> content
    private String mainFile; // entry point file
    private String input; // test input
    private int timeLimit; // in seconds
    private int memoryLimit; // in MB
}