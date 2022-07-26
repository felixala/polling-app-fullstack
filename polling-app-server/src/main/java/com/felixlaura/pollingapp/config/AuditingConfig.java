package com.felixlaura.pollingapp.config;
/**
*To enable JPA Auditing, we need to add @EnableJpaAuditing annotation to our main class or
*any other configuration class.
 */

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AuditingConfig {
}
