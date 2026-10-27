package eu.simpl.vocabulary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VocabularyManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(VocabularyManagerApplication.class, args);
    }
}
