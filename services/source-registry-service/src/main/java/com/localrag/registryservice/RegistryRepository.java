package com.localrag.registryservice;

import com.localrag.common.registry.SourceRegistry;
import com.localrag.common.registry.SourceRegistryLoader;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RegistryRepository {
    private final RegistryConfig config;
    private final SourceRegistryLoader loader;
    private final AtomicReference<SourceRegistry> current = new AtomicReference<>();

    public RegistryRepository(RegistryConfig config, SourceRegistryLoader loader) {
        this.config = config;
        this.loader = loader;
    }

    public SourceRegistry current() {
        SourceRegistry existing = current.get();
        if (existing != null) {
            return existing;
        }
        return reload();
    }

    public SourceRegistry reload() {
        SourceRegistry loaded = loader.load(Path.of(config.path()));
        current.set(loaded);
        return loaded;
    }
}
