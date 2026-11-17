package eu.simpl.cm.manager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** Append-only history entry (D61). */
@Entity
@Table(name = "history_entry", indexes = @Index(name = "ix_history_nid", columnList = "contractNegotiationId"))
public class HistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100, updatable = false)
    public String contractNegotiationId;
    @Column(nullable = false, updatable = false)
    public Instant at;
    @Enumerated(EnumType.STRING)
    @Column(length = 40, updatable = false)
    public ContractStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(length = 40, updatable = false)
    public ContractStatus toStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    public EntryType entryType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    public Trigger trigger;
    @Column(length = 200, updatable = false)
    public String actor;
    @Column(length = 100, updatable = false)
    public String reasonCode;
    @Column(length = 2000, updatable = false)
    public String reason;
    @Column(length = 200, updatable = false)
    public String correlationId;
}
