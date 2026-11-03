package eu.simpl.cm.manager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** D47 idempotency on messageId. */
@Entity
@Table(name = "processed_message")
public class ProcessedMessage {
    @Id
    @Column(length = 100)
    public String messageId;
    public Instant processedAt;
}
