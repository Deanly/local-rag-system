package com.localrag.registryservice;

import com.localrag.common.registry.RegistrySynchronizer;
import com.localrag.common.registry.SourceRegistry;
import com.localrag.common.registry.SourceRegistryValidator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegistryStartup {
    private final RegistryRepository repository;
    private final SourceRegistryValidator validator;
    private final RegistryConfig config;
    private final RegistrySynchronizer synchronizer;

    public RegistryStartup(
            RegistryRepository repository,
            SourceRegistryValidator validator,
            RegistryConfig config,
            RegistrySynchronizer synchronizer
    ) {
        this.repository = repository;
        this.validator = validator;
        this.config = config;
        this.synchronizer = synchronizer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeRegistry() {
        SourceRegistry registry = repository.reload();
        List<String> errors = validator.validate(registry, config.requirePaths());
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Source registry validation failed: " + errors);
        }
        synchronizer.synchronize(registry);
    }
}
