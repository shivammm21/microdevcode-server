package com.microdevcode.code.dto;

import com.microdevcode.code.entity.Framework;
import com.microdevcode.code.entity.TestCase;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class ProblemDto {
    private Long id;
    private String title;
    private String description;
    private String difficulty;
    private double acceptance;
    private List<String> tags;
    private List<String> constraints;
    private HashMap<Framework, HashMap<String,String>> defaultCode;
    private List<TestCase> ExampleTestCases;

}
