package eu.simpl.cm.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "eu.simpl.cm")
@EnableScheduling
public class CmApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmApplication.class, args);
    }
}
