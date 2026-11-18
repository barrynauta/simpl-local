package eu.simpl.vocabularymanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point of the Vocabulary Manager backend. */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class VocabularyManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocabularyManagerApplication.class, args);
    }
}
