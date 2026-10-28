package eu.simpl.catalogue.cat.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** JDBC access to stored resource descriptions. */
@Repository
public class RdRepository {
    private static final String COLS = "id, sd_hash, issuer, provided_by, status, upload_ms, status_ms, resource_key, "
            + "version, schema_name, content, qar, quality, overall_score, overall_class";
    private static final RowMapper<RdRecord> MAPPER = (rs, i) -> new RdRecord(
            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
            rs.getLong(6), rs.getLong(7), rs.getString(8), rs.getInt(9), rs.getString(10),
            rs.getString(11), rs.getString(12), rs.getString(13),
            (Double) rs.getObject(14, Double.class), rs.getString(15));
    private static final String ORDER = " ORDER BY upload_ms DESC, id ASC";

    private final JdbcTemplate jdbc;

    public RdRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(RdRecord r) {
        jdbc.update("INSERT INTO rd (" + COLS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                r.id(), r.sdHash(), r.issuer(), r.providedBy(), r.status(), r.uploadMs(), r.statusMs(),
                r.resourceKey(), r.version(), r.schemaName(), r.content(), r.qar(), r.quality(),
                r.overallScore(), r.overallClass());
    }

    public Optional<RdRecord> byId(String id) {
        return jdbc.query("SELECT " + COLS + " FROM rd WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<RdRecord> byHash(String hash) {
        return jdbc.query("SELECT " + COLS + " FROM rd WHERE sd_hash = ?", MAPPER, hash).stream().findFirst();
    }

    public List<RdRecord> all() {
        return jdbc.query("SELECT " + COLS + " FROM rd" + ORDER, MAPPER);
    }

    public List<RdRecord> active() {
        return jdbc.query("SELECT " + COLS + " FROM rd WHERE status = 'active'" + ORDER, MAPPER);
    }

    public List<RdRecord> byResourceKey(String key) {
        return jdbc.query("SELECT " + COLS + " FROM rd WHERE resource_key = ? ORDER BY version DESC, id ASC", MAPPER, key);
    }

    public int activeCountForSchema(String schemaName) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM rd WHERE status = 'active' AND schema_name = ?",
                Integer.class, schemaName);
        return n == null ? 0 : n;
    }

    public void setStatus(String id, String status, long ms) {
        jdbc.update("UPDATE rd SET status = ?, status_ms = ? WHERE id = ?", status, ms, id);
    }

    public void setQuality(String id, String qar, String quality, Double score, String cls) {
        jdbc.update("UPDATE rd SET qar = ?, quality = ?, overall_score = ?, overall_class = ? WHERE id = ?",
                qar, quality, score, cls, id);
    }

    public void delete(String id) {
        jdbc.update("DELETE FROM rd WHERE id = ?", id);
    }
}
