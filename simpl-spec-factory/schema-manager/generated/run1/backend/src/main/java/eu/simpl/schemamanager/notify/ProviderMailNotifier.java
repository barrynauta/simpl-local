package eu.simpl.schemamanager.notify;

import eu.simpl.schemamanager.config.Settings;
import eu.simpl.schemamanager.domain.Concept;
import eu.simpl.schemamanager.domain.SchemaType;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Provider e-mail for RESOURCE concepts only (D39, D40, D47). */
@Component
public class ProviderMailNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(ProviderMailNotifier.class);

    private final JavaMailSender mailSender;
    private final Settings settings;

    public ProviderMailNotifier(JavaMailSender mailSender, Settings settings) {
        this.mailSender = mailSender;
        this.settings = settings;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvent(SchemaEvent event) {
        if (event.concept().schemaType() != SchemaType.RESOURCE) {
            return;
        }
        List<String> to = settings.providerEmails();
        if (to.isEmpty()) {
            LOG.info("No PROVIDER_NOTIFICATION_EMAILS configured; skipping provider e-mail");
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(settings.notificationFrom());
        msg.setTo(to.toArray(String[]::new));
        msg.setSubject(subject(event));
        msg.setText(body(event));
        try {
            mailSender.send(msg);
        } catch (RuntimeException e) {
            LOG.error("Provider e-mail '{}' could not be sent: {}", msg.getSubject(), e.toString());
        }
    }

    public static String subject(SchemaEvent event) {
        Concept c = event.concept();
        return switch (event.kind()) {
            case PUBLISHED -> "Schema published: " + c.name() + " (version " + event.version() + ")";
            case NEW_VERSION_PUBLISHED -> "Schema new version published: " + c.name() + " (version " + event.version() + ")";
            case REVOKED -> "Schema revoked: " + c.name();
        };
    }

    public String body(SchemaEvent event) {
        Concept c = event.concept();
        return "Name: " + c.name() + "\n"
                + "Title: " + c.title() + "\n"
                + "Description: " + c.description() + "\n"
                + "Resource type: " + (c.resourceType() == null ? "" : c.resourceType()) + "\n"
                + "Version: " + event.version() + "\n"
                + "Change description: " + (event.changelog() == null ? "" : event.changelog()) + "\n"
                + "Timestamp: " + DateTimeFormatter.ISO_INSTANT.format(event.occurredAt().truncatedTo(ChronoUnit.SECONDS)) + "\n"
                + "Instructions: " + settings.providerInstructionsUrl() + "\n";
    }
}
