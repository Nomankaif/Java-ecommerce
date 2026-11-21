package com.example.UserSignupJwtRole.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    public JwtService jwtService;

    @Autowired
    public  CutomDetailService cutomDetailService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null ||
                !header.regionMatches(true, 0, "Bearer ", 0, 7)) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        try{

            String email=jwtService.validateAndGetEmail(token);

            UserDetails userDetails=cutomDetailService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            SecurityContext contextHolder=SecurityContextHolder.createEmptyContext();

            contextHolder.setAuthentication(authenticationToken);

            SecurityContextHolder.setContext(contextHolder);


        }catch (JwtException |
                IllegalArgumentException |
                AuthenticationException exception) {

            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write(
                    "{\"message\":\"Invalid or expired access token\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}
