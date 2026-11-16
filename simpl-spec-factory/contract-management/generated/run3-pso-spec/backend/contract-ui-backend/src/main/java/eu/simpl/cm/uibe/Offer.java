package eu.simpl.cm.uibe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/** UIBE offer row (cdm.puml HumanReadableContractData, D9, D10). */
@Entity
@Table(name = "contract_offer")
public class Offer {
    @Id
    @Column(length = 100)
    public String contractNegotiationId;
    @Column(columnDefinition = "text", nullable = false)
    public String humanReadable;
    @Column(length = 200, nullable = false)
    public String hash;
    /** CREATED, ACCEPTED, REJECTED, TERMINATED (D10). */
    @Column(length = 20, nullable = false)
    public String status;
    @Column(nullable = false)
    public Instant createdDate;
    @Column(nullable = false)
    public Instant lastUpdatedDate;
    @Version
    public long version;
}
