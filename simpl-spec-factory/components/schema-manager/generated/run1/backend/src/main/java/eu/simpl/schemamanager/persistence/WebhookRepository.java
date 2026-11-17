package eu.simpl.schemamanager.persistence;

import eu.simpl.schemamanager.domain.Webhook;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WebhookRepository {

    private static final RowMapper<Webhook> MAPPER = (rs, i) -> new Webhook(
            rs.getObject("id", UUID.class),
            rs.getString("target_url"),
            Arrays.asList(rs.getString("events").split(",")));

    private final NamedParameterJdbcTemplate jdbc;

    public WebhookRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Canonical form of an event set, used for the duplicate check (D41). */
    public static String eventsKey(List<String> events) {
        return String.join(",", new TreeSet<>(events));
    }

    public boolean exists(String targetUrl, List<String> events) {
        Integer n = jdbc.queryForObject("select count(*) from webhook where target_url = :u and events_key = :k",
                new MapSqlParameterSource("u", targetUrl).addValue("k", eventsKey(events)), Integer.class);
        return n != null && n > 0;
    }

    public void insert(Webhook w, Instant created) {
        jdbc.update("insert into webhook (id, target_url, events, events_key, created) values (:id, :u, :e, :k, :c)",
                new MapSqlParameterSource("id", w.webhookId())
                        .addValue("u", w.targetUrl())
                        .addValue("e", String.join(",", w.events()))
                        .addValue("k", eventsKey(w.events()))
                        .addValue("c", Timestamp.from(created)));
    }

    public List<Webhook> findAll() {
        return jdbc.query("select id, target_url, events from webhook order by created, id", MAPPER);
    }

    public List<Webhook> findByEvent(String event) {
        return findAll().stream().filter(w -> w.events().contains(event)).toList();
    }

    public Optional<Webhook> findById(UUID id) {
        return jdbc.query("select id, target_url, events from webhook where id = :id",
                new MapSqlParameterSource("id", id), MAPPER).stream().findFirst();
    }

    public boolean delete(UUID id) {
        return jdbc.update("delete from webhook where id = :id", new MapSqlParameterSource("id", id)) > 0;
    }
}
