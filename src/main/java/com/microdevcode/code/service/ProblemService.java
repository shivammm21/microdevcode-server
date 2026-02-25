package com.microdevcode.code.service;

import com.microdevcode.code.dto.AllProblems;
import com.microdevcode.code.dto.ProblemDto;
import com.microdevcode.code.entity.Problem;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public interface ProblemService {

    List<AllProblems> getProblems();
    ProblemDto getProblemById(Long id);
    ProblemDto getProblemByTitle(String title);
    boolean createProblem(ProblemDto problemDto);
}
