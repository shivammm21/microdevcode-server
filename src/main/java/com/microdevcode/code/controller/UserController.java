package com.microdevcode.code.controller;

import com.microdevcode.code.dto.ProblemDto;
import com.microdevcode.code.dto.UserProfile;
import com.microdevcode.code.service.ProblemService;
import com.microdevcode.code.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final ProblemService problemService;

    @PostMapping("/create-problem")
    public ResponseEntity<?> createProblem(@RequestBody ProblemDto problemDto){
        try{

            HashMap<String,Object> map = new HashMap<>();

            if(problemDto==null){
                log.error("error on null values data");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            if(problemService.createProblem(problemDto)){
                map.put("success",true);
                map.put("message","success");
                return new ResponseEntity<>(map,HttpStatus.CREATED);
            }
            else{
                map.put("success",false);
                map.put("message","error");
                return new ResponseEntity<>(map,HttpStatus.BAD_REQUEST);
            }


        }catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        try{

            if(request == null){
                log.error("Get user profile failed all values are null");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            HashMap<String,Object> map = new HashMap<>();

            UserProfile userProfile = userService.getUserProfile(request);

            if(userProfile == null){
                log.error("Get user profile failed no user profile found");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            else{

                map.put("userProfile",userProfile);
                map.put("success",true);
                return new ResponseEntity<>(map,HttpStatus.OK);
            }


        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
