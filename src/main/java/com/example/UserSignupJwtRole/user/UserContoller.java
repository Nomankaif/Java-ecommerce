package com.example.UserSignupJwtRole.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserContoller {

    @Autowired
    public UserRepository userRepository;

    @GetMapping("/user")
    public List<AppUser> getAllUser(){

        List<AppUser> appUsers=userRepository.findAll();

        return appUsers;
    }
}
