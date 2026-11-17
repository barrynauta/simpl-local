package eu.simpl.cm.manager.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A stored contract clause document (D63). */
@Entity
@Table(name = "clause_document")
public class ClauseDocument {
    @Id
    @Column(length = 40)
    public String documentId;
    @Column(length = 500)
    public String documentName;
    @Column(length = 100)
    public String mediaType;
    public long size;
    @Column(length = 64)
    public String hash;
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    public byte[] content;
    public Instant storedAt;
}
