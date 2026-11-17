package eu.simpl.schemamanager.persistence;

import eu.simpl.schemamanager.domain.Concept;
import eu.simpl.schemamanager.domain.SchemaStatus;
import eu.simpl.schemamanager.domain.SchemaType;
import eu.simpl.schemamanager.domain.SchemaVersion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC access to concepts and versions (PostgreSQL, D58). */
@Repository
public class SchemaRepository {

    private static final String CONCEPT_COLUMNS =
            "c.id, c.schema_type, c.name, c.title, c.description, c.resource_type, c.status, c.created, c.modified, c.created_by";

    private static final String LATEST_JOIN =
            " join schema_version v on v.concept_id = c.id"
                    + " and v.version = (select max(v2.version) from schema_version v2 where v2.concept_id = c.id)";

    private final NamedParameterJdbcTemplate jdbc;

    public SchemaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Instant instant(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toInstant();
    }

    private static final RowMapper<Concept> CONCEPT = (rs, i) -> new Concept(
            rs.getObject("id", UUID.class),
            SchemaType.valueOf(rs.getString("schema_type")),
            rs.getString("name"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("resource_type"),
            SchemaStatus.valueOf(rs.getString("status")),
            instant(rs, "created"),
            instant(rs, "modified"),
            rs.getString("created_by"),
            null,
            null);

    private static final RowMapper<Concept> CONCEPT_WITH_LATEST = (rs, i) -> CONCEPT.mapRow(rs, i)
            .withLatest(rs.getInt("version"), rs.getString("changelog"));

    private static final RowMapper<SchemaVersion> VERSION = (rs, i) -> new SchemaVersion(
            rs.getObject("concept_id", UUID.class),
            rs.getInt("version"),
            rs.getString("changelog"),
            instant(rs, "created"),
            rs.getString("created_by"));

    public Optional<Concept> findConcept(SchemaType type, String name) {
        List<Concept> rows = jdbc.query(
                "select " + CONCEPT_COLUMNS + ", v.version, v.changelog from schema_concept c" + LATEST_JOIN
                        + " where c.schema_type = :t and c.name = :n",
                new MapSqlParameterSource("t", type.name()).addValue("n", name), CONCEPT_WITH_LATEST);
        return rows.stream().findFirst();
    }

    /** Locks the concept row; use inside a transaction. */
    public Optional<Concept> findConceptForUpdate(UUID id) {
        List<Concept> rows = jdbc.query(
                "select " + CONCEPT_COLUMNS + " from schema_concept c where c.id = :id for update",
                new MapSqlParameterSource("id", id), CONCEPT);
        return rows.stream().findFirst();
    }

    public boolean existsByTypeAndName(SchemaType type, String name) {
        Integer n = jdbc.queryForObject(
                "select count(*) from schema_concept where schema_type = :t and name = :n",
                new MapSqlParameterSource("t", type.name()).addValue("n", name), Integer.class);
        return n != null && n > 0;
    }

    /** True when a concept other than {@code exclude} is PUBLISHED in the slot. */
    public boolean publishedInSlot(String slotKey, UUID exclude) {
        Integer n = jdbc.queryForObject(
                "select count(*) from schema_concept where slot_key = :k and status = 'PUBLISHED'"
                        + (exclude == null ? "" : " and id <> :ex"),
                new MapSqlParameterSource("k", slotKey).addValue("ex", exclude), Integer.class);
        return n != null && n > 0;
    }

    /** Serialises all slot transitions of one slot (D16). Call inside a transaction. */
    public void lockSlot(String slotKey) {
        MapSqlParameterSource p = new MapSqlParameterSource("k", slotKey);
        jdbc.update("insert into slot_lock (slot_key) values (:k) on conflict do nothing", p);
        jdbc.queryForObject("select slot_key from slot_lock where slot_key = :k for update", p, String.class);
    }

    public void insertConcept(Concept c) {
        jdbc.update("insert into schema_concept (id, schema_type, name, title, description, resource_type, status,"
                        + " slot_key, created, modified, created_by) values (:id, :t, :n, :title, :d, :rt, :s, :k,"
                        + " :created, :modified, :by)",
                new MapSqlParameterSource("id", c.id())
                        .addValue("t", c.schemaType().name())
                        .addValue("n", c.name())
                        .addValue("title", c.title())
                        .addValue("d", c.description())
                        .addValue("rt", c.resourceType())
                        .addValue("s", c.status().name())
                        .addValue("k", c.slotKey())
                        .addValue("created", Timestamp.from(c.created()))
                        .addValue("modified", Timestamp.from(c.modified()))
                        .addValue("by", c.createdBy()));
    }

    public void updateStatus(UUID id, SchemaStatus status, Instant modified) {
        jdbc.update("update schema_concept set status = :s, modified = :m where id = :id",
                new MapSqlParameterSource("id", id).addValue("s", status.name())
                        .addValue("m", Timestamp.from(modified)));
    }

    public int maxVersion(UUID conceptId) {
        Integer v = jdbc.queryForObject("select coalesce(max(version), 0) from schema_version where concept_id = :id",
                new MapSqlParameterSource("id", conceptId), Integer.class);
        return v == null ? 0 : v;
    }

    public void insertVersion(SchemaVersion v, byte[] content) {
        jdbc.update("insert into schema_version (concept_id, version, content, changelog, created, created_by)"
                        + " values (:c, :v, :content, :log, :created, :by)",
                new MapSqlParameterSource("c", v.conceptId())
                        .addValue("v", v.version())
                        .addValue("content", content)
                        .addValue("log", v.changelog())
                        .addValue("created", Timestamp.from(v.created()))
                        .addValue("by", v.createdBy()));
    }

    public Optional<SchemaVersion> findVersion(UUID conceptId, int version) {
        List<SchemaVersion> rows = jdbc.query(
                "select concept_id, version, changelog, created, created_by from schema_version"
                        + " where concept_id = :c and version = :v",
                new MapSqlParameterSource("c", conceptId).addValue("v", version), VERSION);
        return rows.stream().findFirst();
    }

    public byte[] content(UUID conceptId, int version) {
        List<byte[]> rows = jdbc.query("select content from schema_version where concept_id = :c and version = :v",
                new MapSqlParameterSource("c", conceptId).addValue("v", version), (rs, i) -> rs.getBytes(1));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public long countVersions(UUID conceptId) {
        Long n = jdbc.queryForObject("select count(*) from schema_version where concept_id = :c",
                new MapSqlParameterSource("c", conceptId), Long.class);
        return n == null ? 0 : n;
    }

    /** Versions in numeric order (D26). */
    public List<SchemaVersion> versions(UUID conceptId, int offset, int limit) {
        return jdbc.query("select concept_id, version, changelog, created, created_by from schema_version"
                        + " where concept_id = :c order by version asc limit :lim offset :off",
                new MapSqlParameterSource("c", conceptId).addValue("lim", limit).addValue("off", offset), VERSION);
    }

    private static String listWhere(MapSqlParameterSource p, SchemaType type, SchemaStatus status, String resourceType) {
        StringBuilder where = new StringBuilder(" where c.schema_type = :t");
        p.addValue("t", type.name());
        if (status != null) {
            where.append(" and c.status = :s");
            p.addValue("s", status.name());
        }
        if (resourceType != null) {
            where.append(" and c.resource_type = :rt");
            p.addValue("rt", resourceType);
        }
        return where.toString();
    }

    public long countConcepts(SchemaType type, SchemaStatus status, String resourceType) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = listWhere(p, type, status, resourceType);
        Long n = jdbc.queryForObject("select count(*) from schema_concept c" + where, p, Long.class);
        return n == null ? 0 : n;
    }

    /** One row per concept with its highest version; ordered by name (code point order), then type (D26). */
    public List<Concept> listConcepts(SchemaType type, SchemaStatus status, String resourceType, int offset, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = listWhere(p, type, status, resourceType);
        p.addValue("lim", limit).addValue("off", offset);
        return jdbc.query("select " + CONCEPT_COLUMNS + ", v.version, v.changelog from schema_concept c" + LATEST_JOIN
                + where + " order by c.name collate \"C\" asc, c.schema_type asc limit :lim offset :off", p,
                CONCEPT_WITH_LATEST);
    }
}
