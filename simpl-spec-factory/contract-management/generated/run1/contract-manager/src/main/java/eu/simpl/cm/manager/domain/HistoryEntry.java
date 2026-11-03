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

/** D61 append-only history entry. */
@Entity
@Table(name = "history_entry", indexes = @Index(name = "ix_history_nid", columnList = "contract_negotiation_id"))
public class HistoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "contract_negotiation_id", nullable = false, length = 100)
    public String contractNegotiationId;
    @Column(nullable = false)
    public Instant at;
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    public ContractStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    public ContractStatus toStatus;
    @Column(length = 40, nullable = false)
    public String entryType;
    @Column(length = 20, nullable = false)
    public String triggerType;
    @Column(length = 200)
    public String actor;
    @Column(length = 100)
    public String reasonCode;
    @Column(length = 2000)
    public String reason;
    @Column(length = 200)
    public String correlationId;
}
