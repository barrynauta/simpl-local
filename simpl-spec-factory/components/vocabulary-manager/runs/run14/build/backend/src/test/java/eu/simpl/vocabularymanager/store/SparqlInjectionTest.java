package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SparqlInjectionTest {

    @Test
    void literalValuesWithVariableNamesAreNotRewritten() {
        SparqlTemplate pss = Sparql.command("INSERT DATA { <urn:a> vm:d ?description ; vm:t ?now }");
        pss.setLiteral("description", "see ?now and $now \" } ; DROP ALL");
        pss.setLiteral("now", "X");
        String text = pss.toString();
        assertThat(text).contains("see ?now and $now");
        for (String other : new String[] {"a", "zz", "description2", "b1"}) {
            SparqlTemplate reverse =
                    Sparql.command("INSERT DATA { <urn:a> vm:d ?" + other + " ; vm:t ?now ; vm:u ?w }");
            reverse.setLiteral(other, "see ?now and ?w here");
            reverse.setLiteral("now", "X");
            reverse.setLiteral("w", "Y");
            assertThat(reverse.toString()).contains("see ?now and ?w here");
        }
    }
}
