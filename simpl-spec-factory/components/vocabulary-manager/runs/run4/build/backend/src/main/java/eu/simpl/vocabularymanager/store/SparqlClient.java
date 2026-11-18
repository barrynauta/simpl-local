package eu.simpl.vocabularymanager.store;

import java.time.Duration;
import java.util.List;
import org.apache.jena.query.QuerySolution;

/**
 * Access to the SPARQL store. Implementations raise VocabularyManagerException with SERVICE_UNAVAILABLE when the store
 * cannot be reached or times out, and with INTERNAL for any other store error (decision D44).
 */
public interface SparqlClient {

    /** Runs a read-only SELECT query (retried on connection failure or timeout). */
    List<QuerySolution> select(String query);

    /** Runs one SPARQL update request; the store applies it in one transaction. Never retried. */
    void update(String update);

    /** True when the store answers a trivial query within the timeout. Never throws. */
    boolean ping(Duration timeout);
}
