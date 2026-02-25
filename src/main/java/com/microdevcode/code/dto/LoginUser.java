package com.microdevcode.code.dto;

import lombok.Data;

@Data
public class LoginUser {

    private String usernameOrEmail;
    private String password;
    
}
