package com.microdevcode.code.entity;

import com.microdevcode.code.entity.convertor.FrameworkMapConverter;
import com.microdevcode.code.entity.convertor.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

@Entity
@Table(name = "problems")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String difficulty;

    private double acceptance;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> tags;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> constraints;

    // ✅ Enum as key
    @Column(columnDefinition = "TEXT")
    @Convert(converter = FrameworkMapConverter.class)
    private HashMap<Framework, HashMap<String, String>> defaultCode;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "problem_id")
    private List<TestCase> exampleTestCases;
}