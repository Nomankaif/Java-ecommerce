package com.example.UserSignupJwtRole.user;

import com.example.UserSignupJwtRole.security.CutomDetailService;
import com.example.UserSignupJwtRole.security.JwtService;
import com.example.UserSignupJwtRole.user.dto.AuthResponse;
import com.example.UserSignupJwtRole.user.dto.SignInRequest;
import com.example.UserSignupJwtRole.user.dto.SignUpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    @Autowired
    public UserRepository userRepository;

    @Autowired
    public JwtService jwtService;

    @Autowired
    public CutomDetailService cutomDetailService;

    public AuthResponse signUp(SignUpRequest request){

        String email=request.email();
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at most 72 UTF-8 bytes");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email is already registered");
        }
        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(email);
        PasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String passwordHash = encoder.encode(request.password());
        user.setPassword(passwordHash);
        user.setRole(UserRole.USER);

        AppUser savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // Also handle two simultaneous signup requests for the same email.
            if (userRepository.existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Email is already registered");
            }
            throw exception;
        }

        return new AuthResponse(jwtService.generateToken(savedUser.getEmail()),"Bearer");

    }

    public AuthResponse signIn(SignInRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String password = request.password();

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        // 1. Find the user.
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        // 2. Check the password against the stored BCrypt hash.
        PasswordEncoder encoder = new BCryptPasswordEncoder(12);

        boolean passwordMatches =
                encoder.matches(password, user.getPassword());

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        // 3. Generate a token after successful verification.
        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token, "Bearer");
    }

}
