package com.mlops.modellifecycle.persistence;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ModelJdbc {
    private final JdbcTemplate jdbc;

    public ModelJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(ModelId id, ModelKey key, ModelStatus status, String name) {
        jdbc.update("""
                INSERT INTO ml_model (id, business_key, status, name)
                VALUES (?, ?, ?, ?)
                """, id.getValue(), key.getValue(), status.name(), name);
    }

    public int count(ModelKey key) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM ml_model WHERE business_key = ?",
                Integer.class,
                key.getValue());
        return n == null ? 0 : n;
    }

    public Optional<ModelStatus> findStatus(ModelKey key) {
        List<String> rows = jdbc.queryForList(
                "SELECT status FROM ml_model WHERE business_key = ?",
                String.class,
                key.getValue());
        return rows.stream().findFirst().map(ModelJdbc::toStatus);
    }

    static ModelStatus toStatus(String text) {
        try {
            return ModelStatus.valueOf(text);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalStateException("unknown status in database: " + text, ex);
        }
    }
}