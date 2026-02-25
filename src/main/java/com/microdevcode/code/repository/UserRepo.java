package com.microdevcode.code.repository;

import com.microdevcode.code.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.Id;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User,Long>{

    boolean existsByEmailId(String emailid);
    boolean existsByUsername(String username);
    Optional<User> findByEmailId(String emailid);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailIdOrUsername(String email, String username);

    //Optional<User> findByEmailOrUsername(String username, String username1);
}
