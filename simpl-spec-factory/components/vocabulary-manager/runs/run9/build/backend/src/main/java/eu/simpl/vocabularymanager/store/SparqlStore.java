package eu.simpl.vocabularymanager.store;

import java.time.Duration;
import java.util.List;

/** Access to the SPARQL endpoints of the RDF store (E3). */
public interface SparqlStore {

    /**
     * Runs a SELECT query; retried on connection failure or timeout (D44).
     *
     * @param query the query text, built with Jena
     * @return all solutions
     * @throws StoreUnavailableException when the store does not answer
     * @throws StoreFailureException when the store answers with an error
     */
    List<Row> select(String query);

    /**
     * Runs an ASK query; retried like {@link #select(String)}.
     *
     * @param query the query text
     * @return the answer
     * @throws StoreUnavailableException when the store does not answer
     * @throws StoreFailureException when the store answers with an error
     */
    boolean ask(String query);

    /**
     * Runs an update request as one store transaction; never retried (D44).
     *
     * @param update the update text, built with Jena
     * @throws StoreUnavailableException when the store does not answer
     * @throws StoreFailureException when the store answers with an error
     */
    void update(String update);

    /**
     * Health probe of the store.
     *
     * @param timeout how long to wait
     * @return whether the store answered a query in time
     */
    boolean ping(Duration timeout);
}
