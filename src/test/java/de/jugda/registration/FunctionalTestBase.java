package de.jugda.registration;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import jakarta.inject.Inject;
import lombok.Value;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public abstract class FunctionalTestBase {

    static final String TENANT = "test";
    static final LocalDate TEST_EVENT_DATE = LocalDate.now().plusMonths(1);
    static final String EVENT_ID = TEST_EVENT_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE);

    private static final Duration MAIL_TIMEOUT = Duration.ofSeconds(10);

    static final List<Participant> PARTICIPANTS = List.of(
        new Participant("John Doe", "john.doe@example.com"),
        new Participant("Jane Doe", "jane.doe@example.com"),
        new Participant("Jack Doe", "jack.doe@example.com")
    );

    @Inject
    MockMailbox mailbox;

    /**
     * Participant mails are sent by an asynchronous CDI observer, so they have not necessarily arrived
     * by the time the HTTP response is back. Polling with an assertion rather than a boolean means a
     * timeout reports the number of mails actually seen, not just that the wait expired.
     */
    void awaitTotalMails(int expected) {
        await("total mails sent")
            .atMost(MAIL_TIMEOUT)
            .untilAsserted(() -> assertThat(mailbox.getTotalMessagesSent()).isEqualTo(expected));
    }

    List<Mail> awaitMailsTo(String email, int expected) {
        await("mails sent to " + email)
            .atMost(MAIL_TIMEOUT)
            .untilAsserted(() -> assertThat(mailbox.getMailsSentTo(email)).hasSize(expected));
        return mailbox.getMailsSentTo(email);
    }

    @Value
    static class Participant {
        String name;
        String email;
    }
}
