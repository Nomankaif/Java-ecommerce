package com.example.UserSignupJwtRole.user;


import com.example.UserSignupJwtRole.user.dto.AuthResponse;
import com.example.UserSignupJwtRole.user.dto.SignInRequest;
import com.example.UserSignupJwtRole.user.dto.SignUpRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/signin")
    public AuthResponse signin(@Valid @RequestBody SignInRequest request) {
        return authService.signIn(request);
    }

    public void setTokenCookies(HttpServletResponse http, String token){

        ResponseCookie cookie= ResponseCookie.from("access_token").
                httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(15*60)
                .build();

    }
}
