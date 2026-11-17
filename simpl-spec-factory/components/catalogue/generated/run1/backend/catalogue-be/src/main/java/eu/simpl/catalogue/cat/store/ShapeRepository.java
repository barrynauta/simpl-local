package eu.simpl.catalogue.cat.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Shape store (D58) and the statuses remembered from schema webhooks (D60). */
@Repository
public class ShapeRepository {
    public record Shape(String schemaId, String version, String status, String content) {}
    public record Remembered(String name, String status, String version) {}

    private final JdbcTemplate jdbc;

    public ShapeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Shape> byId(String id) {
        return jdbc.query("SELECT schema_id, version, status, content FROM shape WHERE schema_id = ?",
                (rs, i) -> new Shape(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)), id)
                .stream().findFirst();
    }

    public List<String> ids() {
        return jdbc.queryForList("SELECT schema_id FROM shape ORDER BY schema_id", String.class);
    }

    public void insert(Shape s) {
        jdbc.update("INSERT INTO shape (schema_id, version, status, content, created_ms) VALUES (?,?,?,?,?)",
                s.schemaId(), s.version(), s.status(), s.content(), System.currentTimeMillis());
    }

    public void upsertSeed(Shape s) {
        if (byId(s.schemaId()).isPresent()) {
            jdbc.update("UPDATE shape SET content = ? WHERE schema_id = ?", s.content(), s.schemaId());
        } else {
            insert(s);
        }
    }

    public boolean delete(String id) {
        return jdbc.update("DELETE FROM shape WHERE schema_id = ?", id) > 0;
    }

    public void setStatus(String id, String status, String version) {
        if (version != null) {
            jdbc.update("UPDATE shape SET status = ?, version = ? WHERE schema_id = ?", status, version, id);
        } else {
            jdbc.update("UPDATE shape SET status = ? WHERE schema_id = ?", status, id);
        }
    }

    public Optional<Remembered> remembered(String name) {
        return jdbc.query("SELECT name, status, version FROM schema_status WHERE name = ?",
                (rs, i) -> new Remembered(rs.getString(1), rs.getString(2), rs.getString(3)), name).stream().findFirst();
    }

    public void remember(String name, String status, String version) {
        if (remembered(name).isPresent()) {
            if (version != null) {
                jdbc.update("UPDATE schema_status SET status = ?, version = ? WHERE name = ?", status, version, name);
            } else {
                jdbc.update("UPDATE schema_status SET status = ? WHERE name = ?", status, name);
            }
        } else {
            jdbc.update("INSERT INTO schema_status (name, status, version) VALUES (?,?,?)", name, status, version);
        }
    }
}
