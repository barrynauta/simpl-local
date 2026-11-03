package eu.simpl.cm.uibe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "eu.simpl.cm")
@EnableScheduling
public class UibeApplication {
    public static void main(String[] args) {
        SpringApplication.run(UibeApplication.class, args);
    }
}
