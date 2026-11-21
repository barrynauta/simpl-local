package eu.simpl.vocabularymanager;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Vocabulary Manager backend: manages vocabularies and external vocabularies (BP02B). */
@SpringBootApplication
@EnableConfigurationProperties(VocabularyManagerProperties.class)
public class VocabularyManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocabularyManagerApplication.class, args);
    }
}
