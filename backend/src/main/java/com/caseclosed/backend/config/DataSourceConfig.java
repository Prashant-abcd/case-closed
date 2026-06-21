package com.caseclosed.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${DATABASE_USER:postgres}")
    private String defaultUser;

    @Value("${DATABASE_PASSWORD:Baliyan@11}")
    private String defaultPassword;

    @Bean
    public DataSource dataSource() {
        if (databaseUrl != null && !databaseUrl.trim().isEmpty() && databaseUrl.startsWith("postgresql://")) {
            // Parse Railway DATABASE_URL: postgresql://user:password@host:port/dbname
            String cleanUrl = databaseUrl.replace("postgresql://", "");
            String[] parts = cleanUrl.split("@");
            String[] credentials = parts[0].split(":");
            String username = credentials[0];
            String password = credentials.length > 1 ? credentials[1] : "";
            
            String jdbcUrl = "jdbc:postgresql://" + parts[1];
            
            return DataSourceBuilder.create()
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }
        
        // Fallback for local testing
        return DataSourceBuilder.create()
                .url(databaseUrl != null && !databaseUrl.isEmpty() ? databaseUrl : "jdbc:postgresql://localhost:5432/caseclosed")
                .username(defaultUser)
                .password(defaultPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
