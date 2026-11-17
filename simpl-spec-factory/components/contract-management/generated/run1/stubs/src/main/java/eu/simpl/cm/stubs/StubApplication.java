package eu.simpl.cm.stubs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Stand-in for connector, signer and VC issuer, verifier, wallet, catalogue, directory (digest Appendix C). */
@SpringBootApplication
public class StubApplication {
    public static void main(String[] args) {
        SpringApplication.run(StubApplication.class, args);
    }
}
