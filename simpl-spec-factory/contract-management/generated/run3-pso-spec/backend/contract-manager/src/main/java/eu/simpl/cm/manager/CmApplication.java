package eu.simpl.cm.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"eu.simpl.cm.common", "eu.simpl.cm.manager"})
@ConfigurationPropertiesScan
public class CmApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmApplication.class, args);
    }
}
