package com.felixlaura.pollingapp.config;
/**
*To enable JPA Auditing, we need to add @EnableJpaAuditing annotation to our main class or
*any other configuration class.
 */

import com.felixlaura.pollingapp.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.swing.text.html.Option;
import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class AuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider(){
        return new SpringSecurityAuditAwareImpl();
    }

    class SpringSecurityAuditAwareImpl implements AuditorAware<Long>{

        public Optional<Long> getCurrentAuditor(){
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if(authentication == null ||
                !authentication.isAuthenticated() ||
                        authentication instanceof AnonymousAuthenticationToken){
                    return Optional.empty();
                }
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return Optional.ofNullable(userPrincipal.getId());
        }
    }
}
