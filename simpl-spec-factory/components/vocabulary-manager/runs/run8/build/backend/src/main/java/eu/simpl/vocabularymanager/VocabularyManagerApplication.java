package eu.simpl.vocabularymanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point of the Vocabulary Manager backend. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class VocabularyManagerApplication {

    /**
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(VocabularyManagerApplication.class, args);
    }
}
