package eu.simpl.vocabulary.config;

import eu.simpl.vocabulary.store.JenaStore;
import eu.simpl.vocabulary.validation.ContentValidator;
import java.util.function.Supplier;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the RDF store. {@code app.fuseki.url} points to a Fuseki dataset (D27). The special value {@code mem}
 * uses an in-memory transactional dataset, for tests and local runs without Fuseki.
 */
@Configuration
public class JenaConfig {

    @Bean
    public JenaStore jenaStore(AppProperties props) {
        String url = props.fuseki().url();
        Supplier<RDFConnection> connections;
        if ("mem".equals(url)) {
            Dataset dataset = DatasetFactory.createTxnMem();
            connections = () -> RDFConnection.connect(dataset);
        } else {
            connections = () -> RDFConnection.connect(url);
        }
        return new JenaStore(connections);
    }

    @Bean
    public ContentValidator contentValidator(AppProperties props) {
        return new ContentValidator(props.normalizedReferenceBase());
    }
}
