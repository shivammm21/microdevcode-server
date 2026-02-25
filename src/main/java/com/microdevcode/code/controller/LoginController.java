package com.microdevcode.code.controller;

import com.microdevcode.code.dto.CreateUser;
import com.microdevcode.code.dto.LoginUser;
import com.microdevcode.code.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/login")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final UserService userService;

    @PostMapping("/create-account")
    public ResponseEntity<?> createAccount(@RequestBody CreateUser createUser) {

        try{

            if(createUser == null){
                log.error("Account creation failed all values are null");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            HashMap<String, String> map = new HashMap<>();



            if(userService.createAccount(createUser)){
                map.put("status", "success");
                map.put("message", "Account created successfully");
                map.put("token", userService.generateUserToken(createUser.getEmailid()));

                return new ResponseEntity<>(map, HttpStatus.CREATED);
            }
            else{
                map.put("status", "fail");
                map.put("message", "Account creation failed");
                return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
            }

        }catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginUser loginUser) {

        try{

            if(loginUser == null){
                log.error("Login user creation failed all values are null");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            HashMap<String, String> map = new HashMap<>();
            if(userService.loginUser(loginUser)){
                map.put("status", "success");
                map.put("message", "Login successfully");
                map.put("token",userService.generateUserToken(loginUser.getUsernameOrEmail()));
                return new ResponseEntity<>(map, HttpStatus.OK);

            }
            else {
                map.put("status", "fail");
                map.put("message", "Login failed");
                return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
            }


        }catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
}
