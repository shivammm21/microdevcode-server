package com.microdevcode.code.dto;

import lombok.Data;

@Data
public class CreateUser {

    private String fullName;
    private String emailid;
    private String username;
    private String password;
}