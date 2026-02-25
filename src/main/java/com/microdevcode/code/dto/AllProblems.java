package com.microdevcode.code.dto;

import lombok.Data;

import java.util.List;

@Data
public class AllProblems {
    private Long id;
    private String title;
    private List<String> tags;
    private String difficulty;
    private double acceptance;
}
