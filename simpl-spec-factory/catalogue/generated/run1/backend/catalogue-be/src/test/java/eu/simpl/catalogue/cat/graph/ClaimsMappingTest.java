package eu.simpl.catalogue.cat.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimsMappingTest {
    private static Graph turtle(String ttl) {
        Graph g = GraphFactory.createDefaultGraph();
        RDFParser.fromString(ttl, Lang.TURTLE).parse(g);
        return g;
    }

    @Test
    @SuppressWarnings("unchecked")
    void typesBecomeLabelsLiteralsPropertiesAndLinksRelationships() {
        Graph g = turtle("""
                @prefix simpl: <http://w3id.org/gaia-x/simpl#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                <urn:rd:1> a simpl:DataOffering ;
                  simpl:generalServiceProperties [ simpl:name "Weather" ; simpl:price "1.5"^^xsd:decimal ;
                                                   simpl:keyword "a", "b" ] ;
                  simpl:version 2 .
                """);
        ClaimsMapping.Mapped m = ClaimsMapping.map(g, "urn:rd:1");
        assertThat(m.nodes()).hasSize(2);
        ClaimsMapping.GraphNode rd = m.nodes().stream().filter(n -> "urn:rd:1".equals(n.props().get("uri"))).findFirst().orElseThrow();
        assertThat(rd.labels()).containsExactly("Resource", "DataOffering");
        assertThat(rd.props()).containsEntry("version", 2L).containsEntry("claimsGraphUri", List.of("urn:rd:1"));
        ClaimsMapping.GraphNode gsp = m.nodes().stream().filter(n -> n != rd).findFirst().orElseThrow();
        assertThat(gsp.props()).containsEntry("name", "Weather").containsEntry("price", 1.5).doesNotContainKey("uri");
        assertThat((List<Object>) gsp.props().get("keyword")).containsExactlyInAnyOrder("a", "b");
        assertThat(m.rels()).containsExactly(new ClaimsMapping.GraphRel("urn:rd:1", "generalServiceProperties", gsp.key()));
    }

    @Test
    void localNames() {
        assertThat(ClaimsMapping.localName("http://purl.org/dc/terms/conformsTo")).isEqualTo("conformsTo");
        assertThat(ClaimsMapping.localName("http://w3id.org/gaia-x/simpl#offeringType")).isEqualTo("offeringType");
        assertThat(ClaimsMapping.localName("urn:simpl:thing")).isEqualTo("thing");
    }
}
