package com.microdevcode.code.dto;

import com.microdevcode.code.entity.Framework;
import lombok.Data;

import java.util.Map;

@Data
public class CodeSubmissionRequest {
    private Long problemId;
    private Framework framework;
    private Map<String, String> code; // filename -> code content
    private String language; // "java", "javascript", "python", etc.
}