package com.localrag.indexer;

import com.localrag.common.registry.SourceRegistry;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import com.localrag.common.registry.SourceRoot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class IndexerAutomation {
    private final IndexerSettings settings;
    private final SourceRegistryLoader loader;
    private final SourceRegistryValidator validator;
    private final IndexerService indexerService;
    private final AtomicBoolean watchThreadStarted = new AtomicBoolean(false);

    public IndexerAutomation(
            IndexerSettings settings,
            SourceRegistryLoader loader,
            SourceRegistryValidator validator,
            IndexerService indexerService
    ) {
        this.settings = settings;
        this.loader = loader;
        this.validator = validator;
        this.indexerService = indexerService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWatcher() {
        if (!settings.watchEnabled() || !watchThreadStarted.compareAndSet(false, true)) {
            return;
        }
        Thread watcher = new Thread(this::watchLoop, "local-rag-indexer-watch");
        watcher.setDaemon(true);
        watcher.start();
    }

    @Scheduled(fixedDelayString = "${local-rag.indexer.scan-interval-millis:300000}", initialDelayString = "${local-rag.indexer.scan-interval-millis:300000}")
    public void periodicScan() {
        indexerService.scan(null);
    }

    private void watchLoop() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            for (SourceRoot source : activeSources()) {
                registerTree(watchService, source.path());
            }
            while (true) {
                WatchKey key = watchService.take();
                drain(key);
                waitForQuietPeriod(watchService);
                indexerService.scan(null);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException | IOException exception) {
            watchThreadStarted.set(false);
        }
    }

    private void waitForQuietPeriod(WatchService watchService) throws InterruptedException {
        long debounceNanos = TimeUnit.MILLISECONDS.toNanos(settings.watchDebounceMillis());
        long quietUntil = System.nanoTime() + debounceNanos;
        while (true) {
            long remainingNanos = quietUntil - System.nanoTime();
            if (remainingNanos <= 0) {
                return;
            }
            WatchKey nextKey = watchService.poll(remainingNanos, TimeUnit.NANOSECONDS);
            if (nextKey != null) {
                drain(nextKey);
                quietUntil = System.nanoTime() + debounceNanos;
            }
        }
    }

    private void drain(WatchKey key) {
        key.pollEvents();
        key.reset();
    }

    private List<SourceRoot> activeSources() {
        SourceRegistry registry = loader.load(Path.of(settings.registryPath()));
        List<String> errors = validator.validate(registry, settings.registryRequirePaths());
        if (!errors.isEmpty()) {
            return List.of();
        }
        return registry.sources().stream().filter(SourceRoot::active).toList();
    }

    private void registerTree(WatchService watchService, Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
