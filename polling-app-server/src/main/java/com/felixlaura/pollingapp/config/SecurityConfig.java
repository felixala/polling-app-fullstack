package com.felixlaura.pollingapp.config;

import com.felixlaura.pollingapp.security.CustomUserDetailsService;
import com.felixlaura.pollingapp.security.JwtAuthenticationEntryPoint;
import com.felixlaura.pollingapp.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.SecurityBuilder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity //This enables web security in the project
@EnableGlobalMethodSecurity(// This annotation enables "method level security" based on annotations.
        securedEnabled = true, // it enables the @Secured annotation, selecting what we can protect (controller/svc methods)
        jsr250Enabled = true, //This enables the @RolesAllowed annotation
        prePostEnabled = true //This enables based on access control syntax @PreAuthorize and @ PostAuthorize annotations
)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    //To authenticate a User or perform many role-based checks, Sepring security need to load user details somehow.
    //This object implements UserDetailsService interface and provides the implementation for loadUserByUsername() method.
    CustomUserDetailsService customUserDetailsService;

    //This class is used to return 401 unathorized error to clients that tr access to protected resource whihtout
    //proper authentication. It implemts Spring Security's AuthenticationEntryPoint interface
    //@Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    //This class implement a filter that: reads Jwt authentication token from the Authorization header, loads user details.
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder());

    }

    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    public AuthenticationManager authenticationManagerBean(){
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    protected void configure(HttpSecurity http) throws Exception{
        http
                .cors()
                .and()
                .csrf()
                .disable()
                .exceptionHandling()
                .authenticationEntryPoint(unauthorizedHandler)
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/",
                        "/favicon.ico",
                        "/**/*.png",
                        "/**/*.gif",
                        "/**/*.svg",
                        "/**/*.jpg",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js")
                .permitAll()
                .antMatchers("/api/auth/**")
                .permitAll()
                .antMatchers("/api/user/checkUsernameAvailability", "/api/user/checkEmailAvailability")
                .permitAll()
                .antMatchers(HttpMethod.GET, "/api/polls/**", "/api/users/**")
                .permitAll()
                .anyRequest()
                .authenticated();

        // Add our custom JWT security filter
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
