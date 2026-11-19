package eu.simpl.vocabulary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Vocabulary Manager backend: the Vocabulary Management API of the Simpl-Open Vocabulary Hub. */
@SpringBootApplication
public class VocabularyManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocabularyManagerApplication.class, args);
    }
}
