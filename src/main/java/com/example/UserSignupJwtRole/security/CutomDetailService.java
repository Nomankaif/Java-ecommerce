package com.example.UserSignupJwtRole.security;


import com.example.UserSignupJwtRole.user.AppUser;
import com.example.UserSignupJwtRole.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CutomDetailService implements UserDetailsService {

    @Autowired
    public UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email=username.trim().toLowerCase(Locale.ROOT);
        AppUser appUser=userRepository.findByEmail(email).orElseThrow(()->
                new UsernameNotFoundException("user not found"));

        SimpleGrantedAuthority simpleGrantedAuthority=new SimpleGrantedAuthority("ROLE_" +appUser.getRole().name());

        return new User(appUser.getEmail(), appUser.getPassword(), List.of(simpleGrantedAuthority));
    }
}
