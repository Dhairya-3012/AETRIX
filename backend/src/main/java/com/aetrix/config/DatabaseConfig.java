package com.aetrix.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.aetrix.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // JPA configuration is handled via application.yml
    // This class enables JPA repositories, auditing, and transaction management
}
