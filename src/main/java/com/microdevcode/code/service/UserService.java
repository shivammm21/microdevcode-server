package com.microdevcode.code.service;

import com.microdevcode.code.dto.CreateUser;
import com.microdevcode.code.dto.LoginUser;
import com.microdevcode.code.dto.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    boolean createAccount(CreateUser createUser);
    boolean loginUser(LoginUser loginUser);
    String generateUserToken(String email);
    UserProfile getUserProfile(HttpServletRequest request);
    
}
