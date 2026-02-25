package com.microdevcode.code.service.serviceImpl;

import com.microdevcode.code.dto.AllProblems;
import com.microdevcode.code.dto.ProblemDto;
import com.microdevcode.code.entity.Problem;
import com.microdevcode.code.entity.TestCase;
import com.microdevcode.code.repository.ProblemRepo;
import com.microdevcode.code.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepo problemRepo;

    @Override
    public List<AllProblems> getProblems() {

        List<AllProblems> allProblems = new ArrayList<>();

        List<Problem> problems = problemRepo.findAll();

        for (Problem problem : problems) {
            AllProblems allProblem = new AllProblems();

            allProblem.setId(problem.getId());
            allProblem.setTitle(problem.getTitle());
            allProblem.setAcceptance(problem.getAcceptance());
            allProblem.setDifficulty(problem.getDifficulty());
            allProblem.setTags(problem.getTags());

            allProblems.add(allProblem);
        }
        return allProblems;
    }

    @Override
    public ProblemDto getProblemById(Long id) {

        Problem problem = problemRepo.findById(id).orElse(null);
        ProblemDto problemDto = new ProblemDto();

        problemDto.setId(problem.getId());
        problemDto.setTitle(problem.getTitle());
        problemDto.setDescription(problem.getDescription());
        problemDto.setDifficulty(problem.getDifficulty());
        problemDto.setTags(problem.getTags());
        problemDto.setAcceptance(problem.getAcceptance());
        problemDto.setConstraints(problem.getConstraints());
        problemDto.setDefaultCode(problem.getDefaultCode());

        List<TestCase> testCases = new  ArrayList<>();

        for(int i=0;i<problem.getExampleTestCases().size();i++){
            if(!problem.getExampleTestCases().get(i).getIsHide()) testCases.add(problem.getExampleTestCases().get(i));
        }

        problemDto.setExampleTestCases(testCases);

        return problemDto;


    }

    @Override
    public ProblemDto getProblemByTitle(String title) {
        Problem problem = problemRepo.findByTitle(title);

        if(problem == null) return null;

        ProblemDto problemDto = new ProblemDto();

        problemDto.setId(problem.getId());
        problemDto.setTitle(problem.getTitle());
        problemDto.setDescription(problem.getDescription());
        problemDto.setDifficulty(problem.getDifficulty());
        problemDto.setTags(problem.getTags());
        problemDto.setAcceptance(problem.getAcceptance());
        problemDto.setConstraints(problem.getConstraints());
        problemDto.setDefaultCode(problem.getDefaultCode());

        List<TestCase> testCases = new  ArrayList<>();

        for(int i=0;i<problem.getExampleTestCases().size();i++){
            if(!problem.getExampleTestCases().get(i).getIsHide()) testCases.add(problem.getExampleTestCases().get(i));
        }

        problemDto.setExampleTestCases(testCases);

        return problemDto;
    }

    @Override
    public boolean createProblem(ProblemDto problemDto) {

        if(problemDto == null) return false;

        Problem problem = new Problem();

        problem.setTitle(problemDto.getTitle());
        problem.setDescription(problemDto.getDescription());
        problem.setAcceptance(problemDto.getAcceptance());
        problem.setDifficulty(problemDto.getDifficulty());
        problem.setTags(problemDto.getTags());
        problem.setDefaultCode(problemDto.getDefaultCode());
        problem.setConstraints(problemDto.getConstraints());
        problem.setExampleTestCases(problemDto.getExampleTestCases());

        problemRepo.save(problem);
        return true;

    }
}
