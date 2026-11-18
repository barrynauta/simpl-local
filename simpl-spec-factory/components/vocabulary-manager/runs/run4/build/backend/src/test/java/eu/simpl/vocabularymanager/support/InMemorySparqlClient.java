package eu.simpl.vocabularymanager.support;

import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.store.SparqlClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateAction;

/** A transactional in-memory Jena dataset that runs the same SPARQL as Fuseki; can simulate an unreachable store. */
public class InMemorySparqlClient implements SparqlClient {

    private final Dataset dataset = DatasetFactory.createTxnMem();
    private final AtomicBoolean down = new AtomicBoolean(false);
    private final AtomicInteger updates = new AtomicInteger();

    public void setDown(boolean value) {
        down.set(value);
    }

    public int updateCount() {
        return updates.get();
    }

    public void clear() {
        dataset.executeWrite(() -> dataset.asDatasetGraph().clear());
    }

    @Override
    public List<QuerySolution> select(String query) {
        failWhenDown();
        dataset.begin(ReadWrite.READ);
        try (QueryExecution execution = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = execution.execSelect();
            List<QuerySolution> rows = new ArrayList<>();
            results.forEachRemaining(rows::add);
            return rows;
        } finally {
            dataset.end();
        }
    }

    @Override
    public void update(String update) {
        failWhenDown();
        updates.incrementAndGet();
        dataset.executeWrite(() -> UpdateAction.parseExecute(update, dataset));
    }

    @Override
    public boolean ping(Duration timeout) {
        return !down.get();
    }

    private void failWhenDown() {
        if (down.get()) {
            throw Problems.storeUnavailable(null);
        }
    }
}
