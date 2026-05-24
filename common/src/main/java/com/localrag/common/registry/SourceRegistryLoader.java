package com.localrag.common.registry;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceRegistryLoader {
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");

    public SourceRegistry load(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Source registry does not exist: " + path);
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            Object raw = new Yaml().load(inputStream);
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Source registry must be a YAML object");
            }
            return parse(map);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to load source registry: " + path, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private SourceRegistry parse(Map<?, ?> map) {
        int version = asInt(map.get("version"), 1);
        String deviceId = requireString(map, "device_id");
        String defaultProjectId = requireString(map, "default_project_id");

        List<ProjectRegistration> projects = new ArrayList<>();
        for (Object item : asList(map.get("projects"))) {
            Map<String, Object> project = (Map<String, Object>) item;
            projects.add(new ProjectRegistration(
                    requireString(project, "project_id"),
                    stringValue(project.get("display_name"), requireString(project, "project_id")),
                    stringValue(project.get("repo_path"), null),
                    requireString(project, "primary_source_id"),
                    stringList(project.get("default_context")),
                    boolValue(project.get("active"), true)
            ));
        }

        List<SourceRoot> sources = new ArrayList<>();
        for (Object item : asList(map.get("sources"))) {
            Map<String, Object> source = (Map<String, Object>) item;
            sources.add(new SourceRoot(
                    requireString(source, "source_id"),
                    requireString(source, "project_id"),
                    requireString(source, "type"),
                    requireString(source, "ssot_role"),
                    Path.of(resolveEnv(requireString(source, "path"))),
                    asInt(source.get("priority"), 50),
                    boolValue(source.get("active"), true),
                    stringValue(source.get("sensitivity_default"), "private"),
                    stringList(source.get("include")),
                    stringList(source.get("exclude")),
                    requireString(source, "read_policy"),
                    requireString(source, "write_policy")
            ));
        }
        return new SourceRegistry(version, deviceId, defaultProjectId, List.copyOf(projects), List.copyOf(sources));
    }

    public static String resolveEnv(String value) {
        Matcher matcher = ENV_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String envName = matcher.group(1);
            String envValue = System.getenv(envName);
            if (envValue == null) {
                envValue = System.getProperty(envName, "");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(envValue));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static List<?> asList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Expected YAML list but got " + value.getClass().getSimpleName());
    }

    private static List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        for (Object item : asList(value)) {
            result.add(String.valueOf(item));
        }
        return List.copyOf(result);
    }

    private static String requireString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing required registry key: " + key);
        }
        return String.valueOf(value);
    }

    private static String stringValue(Object value, String defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private static int asInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static boolean boolValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
