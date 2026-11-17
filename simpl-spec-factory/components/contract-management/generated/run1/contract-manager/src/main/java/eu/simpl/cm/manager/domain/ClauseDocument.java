package eu.simpl.cm.manager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** D63 contract clause document. The content is stored, never returned. */
@Entity
@Table(name = "clause_document")
public class ClauseDocument {
    @Id
    @Column(length = 36)
    public String documentId;
    @Column(length = 500)
    public String documentName;
    @Column(length = 100)
    public String mediaType;
    public long size;
    @Column(length = 64)
    public String hash;
    @Column(columnDefinition = "bytea")
    public byte[] content;
    public Instant uploadedAt;
}
