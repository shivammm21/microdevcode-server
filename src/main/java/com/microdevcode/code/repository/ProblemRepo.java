package com.microdevcode.code.repository;

import com.microdevcode.code.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepo extends JpaRepository<Problem, Long> {

    Problem findByTitle(String title);
}
