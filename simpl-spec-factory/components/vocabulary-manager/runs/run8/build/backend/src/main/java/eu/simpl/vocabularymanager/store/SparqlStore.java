package eu.simpl.vocabularymanager.store;

import java.time.Duration;
import java.util.List;
import org.apache.jena.query.QuerySolution;

/** Access to the RDF store through the SPARQL 1.1 protocol (REUSE-04). */
public interface SparqlStore {

    /**
     * Runs a read-only SELECT query with the read user. Retried on connection failure or timeout (D44).
     *
     * @param query the query, with a LIMIT (D85)
     * @return the solutions
     * @throws StoreUnavailableException when the store stays unreachable or times out
     * @throws StoreException on any other store error
     */
    List<QuerySolution> select(String query);

    /**
     * Runs a read-only ASK query with the read user.
     *
     * @param query the query
     * @return the answer
     * @throws StoreUnavailableException when the store stays unreachable or times out
     * @throws StoreException on any other store error
     */
    boolean ask(String query);

    /**
     * Runs a SPARQL update with the update user, as one store transaction. Never retried (D44).
     *
     * @param update the update request
     * @throws StoreUnavailableException when the store is unreachable or times out
     * @throws StoreException on any other store error
     */
    void update(String update);

    /**
     * @param timeout the probe timeout
     * @return true when the store answers a trivial query in time
     */
    boolean ping(Duration timeout);
}
