package com.microdevcode.code.controller;

import com.microdevcode.code.dto.ProblemDto;
import com.microdevcode.code.service.ProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/problem")
@Slf4j
@RequiredArgsConstructor
public class ProblemsController {

    private final ProblemService problemService;

    @GetMapping("/get-allproblems")
    public ResponseEntity<?> getAllProblems(){

        try{

            HashMap<String,Object> map = new HashMap<>();

            map.put("message","All problems found");
            map.put("problems",problemService.getProblems());

            return new  ResponseEntity<>(map,HttpStatus.OK);

        }catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/get-problem/{identifier}")
    public ResponseEntity<?> getProblem(@PathVariable("identifier") String identifier) {
        try {
            HashMap<String, Object> map = new HashMap<>();
            ProblemDto problem;

            if (identifier.matches("\\d+")) {
                problem = problemService.getProblemById(Long.parseLong(identifier));
            } else {
                problem = problemService.getProblemByTitle(identifier);
            }

            map.put("success", true);
            map.put("data", problem);
            return ResponseEntity.ok(map);

        } catch (Exception e) {
            HashMap<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

}
