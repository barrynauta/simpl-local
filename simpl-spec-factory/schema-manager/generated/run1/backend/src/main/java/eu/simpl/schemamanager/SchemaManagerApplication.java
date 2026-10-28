package eu.simpl.schemamanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SchemaManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaManagerApplication.class, args);
    }
}
