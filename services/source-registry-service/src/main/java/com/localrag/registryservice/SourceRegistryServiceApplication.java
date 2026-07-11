package com.localrag.registryservice;

import com.localrag.common.registry.RegistrySynchronizer;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootApplication(scanBasePackages = {"com.localrag.registryservice", "com.localrag.common"})
public class SourceRegistryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SourceRegistryServiceApplication.class, args);
    }

    @Bean
    SourceRegistryLoader sourceRegistryLoader(Environment environment) {
        return new SourceRegistryLoader(environment::getProperty);
    }

    @Bean
    SourceRegistryValidator sourceRegistryValidator() {
        return new SourceRegistryValidator();
    }

    @Bean
    RegistrySynchronizer registrySynchronizer(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        return new RegistrySynchronizer(jdbcTemplate, new TransactionTemplate(transactionManager));
    }
}
