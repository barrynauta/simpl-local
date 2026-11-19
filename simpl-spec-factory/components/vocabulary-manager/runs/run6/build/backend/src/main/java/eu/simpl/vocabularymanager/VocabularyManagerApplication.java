package eu.simpl.vocabularymanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Vocabulary Manager backend: the Vocabulary Management API of Simpl-Open. */
@SpringBootApplication
public class VocabularyManagerApplication {

    /**
     * Starts the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(VocabularyManagerApplication.class, args);
    }
}
