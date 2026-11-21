package com.example.UserSignupJwtRole.config;


import com.example.UserSignupJwtRole.security.CutomDetailService;
import com.example.UserSignupJwtRole.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    public CutomDetailService cutomDetailService;

    @Autowired
    public JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http){

        return
                http
                        .csrf(AbstractHttpConfigurer::disable)
                        .formLogin(AbstractHttpConfigurer::disable)
                        .httpBasic(AbstractHttpConfigurer::disable)
                        .logout(AbstractHttpConfigurer::disable)

                        .sessionManagement(session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                        )

                        .authorizeHttpRequests(auth -> auth
                                .dispatcherTypeMatchers(DispatcherType.ERROR)
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/auth/signup",
                                        "/auth/signin"
                                )
                                .permitAll()

                                .requestMatchers(HttpMethod.GET, "/product", "/product/**")
                                .authenticated()
                                .requestMatchers("/product", "/product/**")
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/user"
                                ).hasRole("ADMIN")

                                .requestMatchers("/h2-console/**").permitAll()

                                .requestMatchers(HttpMethod.GET, "/orders", "/orders/")
                                .hasRole("ADMIN")
                                .requestMatchers("/orders", "/orders/**")
                                .authenticated()

                                .anyRequest()
                                .authenticated()
                        )

                        .exceptionHandling(errors -> errors
                                .authenticationEntryPoint((request, response, ex) ->
                                        response.setStatus(
                                                HttpServletResponse.SC_UNAUTHORIZED
                                        )
                                )
                        )

                        .addFilterBefore(
                                jwtAuthenticationFilter,
                                UsernamePasswordAuthenticationFilter.class
                        )

                        .headers(headers -> headers
                                .frameOptions(frame -> frame.sameOrigin())
                        )

                        .build();


    }
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
    jwtFilterRegistration() {

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(jwtAuthenticationFilter);

        // The filter should run only inside Spring Security's chain.
        registration.setEnabled(false);

        return registration;
    }
}
