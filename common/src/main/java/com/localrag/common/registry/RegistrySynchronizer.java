package com.localrag.common.registry;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Array;
import java.util.List;

public class RegistrySynchronizer {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RegistrySynchronizer(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    public void synchronize(SourceRegistry registry) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("SET CONSTRAINTS ALL DEFERRED");
            jdbcTemplate.update("""
                    INSERT INTO device_profile(device_id, display_name, active)
                    VALUES (?, ?, true)
                    ON CONFLICT (device_id)
                    DO UPDATE SET display_name = EXCLUDED.display_name, active = true, updated_at = now()
                    """, registry.deviceId(), registry.deviceId());

            for (ProjectRegistration project : registry.projects()) {
                jdbcTemplate.update(connection -> {
                    var statement = connection.prepareStatement("""
                            INSERT INTO project_registration(project_id, display_name, repo_path, primary_source_id, default_context_source_ids, active)
                            VALUES (?, ?, ?, ?, ?, ?)
                            ON CONFLICT (project_id)
                            DO UPDATE SET display_name = EXCLUDED.display_name,
                                          repo_path = EXCLUDED.repo_path,
                                          primary_source_id = EXCLUDED.primary_source_id,
                                          default_context_source_ids = EXCLUDED.default_context_source_ids,
                                          active = EXCLUDED.active,
                                          updated_at = now()
                            """);
                    statement.setString(1, project.projectId());
                    statement.setString(2, project.displayName());
                    statement.setString(3, project.repoPath());
                    statement.setString(4, project.primarySourceId());
                    Array contextArray = connection.createArrayOf("text", project.defaultContext().toArray(String[]::new));
                    statement.setArray(5, contextArray);
                    statement.setBoolean(6, project.active());
                    return statement;
                });
            }

            for (SourceRoot source : registry.sources()) {
                jdbcTemplate.update(connection -> {
                    var statement = connection.prepareStatement("""
                            INSERT INTO source_root(source_id, project_id, source_type, ssot_role, root_path, priority,
                                                    active, sensitivity_default, read_policy, write_policy,
                                                    include_globs, exclude_globs)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT (source_id)
                            DO UPDATE SET project_id = EXCLUDED.project_id,
                                          source_type = EXCLUDED.source_type,
                                          ssot_role = EXCLUDED.ssot_role,
                                          root_path = EXCLUDED.root_path,
                                          priority = EXCLUDED.priority,
                                          active = EXCLUDED.active,
                                          sensitivity_default = EXCLUDED.sensitivity_default,
                                          read_policy = EXCLUDED.read_policy,
                                          write_policy = EXCLUDED.write_policy,
                                          include_globs = EXCLUDED.include_globs,
                                          exclude_globs = EXCLUDED.exclude_globs,
                                          updated_at = now()
                            """);
                    statement.setString(1, source.sourceId());
                    statement.setString(2, source.projectId());
                    statement.setString(3, source.type());
                    statement.setString(4, source.ssotRole());
                    statement.setString(5, source.path().toString());
                    statement.setInt(6, source.priority());
                    statement.setBoolean(7, source.active());
                    statement.setString(8, source.sensitivityDefault());
                    statement.setString(9, source.readPolicy());
                    statement.setString(10, source.writePolicy());
                    statement.setArray(11, connection.createArrayOf("text", source.include().toArray(String[]::new)));
                    statement.setArray(12, connection.createArrayOf("text", source.exclude().toArray(String[]::new)));
                    return statement;
                });
            }
            deactivateMissingSources(registry.sources().stream().map(SourceRoot::sourceId).toList());
            deactivateMissingProjects(registry.projects().stream().map(ProjectRegistration::projectId).toList());
        });
    }

    private void deactivateMissingSources(List<String> sourceIds) {
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    UPDATE source_root
                    SET active = false, updated_at = now()
                    WHERE NOT (source_id = ANY (?))
                    """);
            statement.setArray(1, connection.createArrayOf("text", sourceIds.toArray(String[]::new)));
            return statement;
        });
    }

    private void deactivateMissingProjects(List<String> projectIds) {
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    UPDATE project_registration
                    SET active = false, updated_at = now()
                    WHERE NOT (project_id = ANY (?))
                    """);
            statement.setArray(1, connection.createArrayOf("text", projectIds.toArray(String[]::new)));
            return statement;
        });
    }
}
