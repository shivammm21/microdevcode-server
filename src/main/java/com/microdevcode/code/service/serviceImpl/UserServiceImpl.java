package com.microdevcode.code.service.serviceImpl;

import com.microdevcode.code.dto.CreateUser;
import com.microdevcode.code.dto.LoginUser;
import com.microdevcode.code.dto.UserProfile;
import com.microdevcode.code.entity.User;
import com.microdevcode.code.repository.UserRepo;
import com.microdevcode.code.service.UserService;
import com.microdevcode.code.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserServiceImpl implements UserDetailsService, UserService {

    private final UserRepo userRepo;

    private final PasswordEncoder passwordEncoder;
    
    private final JwtUtil jwtUtil;

    private void save(User user){
        userRepo.save(user);
    }

    @Override
    public String generateUserToken(String emailOrUsername) {

        if(userRepo.existsByEmailId(emailOrUsername)){
            return jwtUtil.generateToken(emailOrUsername);
        }

        Optional<User> user = userRepo.findByUsername(emailOrUsername);

        return user.map(value -> jwtUtil.generateToken(value.getEmailId())).orElse(null);

    }


    @Override
    public boolean createAccount(CreateUser createUser){

        if(userRepo.existsByEmailId(createUser.getEmailid())) return false;

        User user = new User();

        user.setEmailId(createUser.getEmailid());
        user.setPassword(passwordEncoder.encode(createUser.getPassword()));
        user.setUsername(createUser.getUsername());
        user.setFullName(createUser.getFullName());
        save(user);

        return true;

    }

    @Override
    public boolean loginUser(LoginUser loginUser){

        if(loginUser.getUsernameOrEmail().contains("@") && userRepo.existsByEmailId(loginUser.getUsernameOrEmail())){

            Optional<User> user = userRepo.findByEmailId(loginUser.getUsernameOrEmail());

            if(user.isPresent()){
                User existUser = user.get();
                if(passwordEncoder.matches(loginUser.getPassword(), existUser.getPassword())){

                    return true;
                }
            }
        }
        else if(userRepo.existsByUsername(loginUser.getUsernameOrEmail())){

            Optional<User> user = userRepo.findByUsername(loginUser.getUsernameOrEmail());

            if(user.isPresent()){
                User existUser = user.get();
                if(passwordEncoder.matches(loginUser.getPassword(), existUser.getPassword())){

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public UserProfile getUserProfile(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        // ✅ Correct extraction
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("No token provided");
        }

        String token = authHeader.substring(7); // removes "Bearer " prefix
        String username = jwtUtil.extractUsername(token);

        Optional<User> existUser = userRepo.findByEmailIdOrUsername(username, username);

        UserProfile profile = new UserProfile();

        if(existUser.isPresent()){
            profile.setUsername(existUser.get().getUsername());
            profile.setFullName(existUser.get().getFullName());
            profile.setEmail(existUser.get().getEmailId());
        }

        return profile;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {

        User user = userRepo.findByEmailIdOrUsername(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmailId())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}
