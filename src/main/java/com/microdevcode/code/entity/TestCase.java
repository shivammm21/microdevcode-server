package com.microdevcode.code.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String expectedOutput;

    // ❌ boolean isHide  → Lombok generates isHide() which conflicts with Jackson
    // ✅ Change to Boolean or rename
    @Column(name = "is_hide", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isHide = false;  // ✅ default value
}