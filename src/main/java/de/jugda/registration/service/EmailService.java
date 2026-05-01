package de.jugda.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jugda.registration.Config;
import de.jugda.registration.model.Event;
import de.jugda.registration.model.Registration;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Qute;
import io.quarkus.qute.Template;
import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;
    @Inject
    Config config;
    @Inject
    EventService eventService;
    @Inject
    ObjectMapper objectMapper;
    @Location("mail/registration")
    Template tplRegistration;
    @Location("mail/waitlist2attendee")
    Template tplWaitlist2attendee;
    @Inject
    LaunchMode launchMode;

    void sendRegistrationConfirmation(Registration registration) {
        Event event = eventService.getEvent(registration.eventId);
        String subject = String.format("[%s] Anmeldebestätigung für \"%s\" am %s",
            config.email().subjectPrefix(), event.summary, event.startDate());

        String mailBody = tplRegistration
            .data("tenant", config.tenant())
            .data("registration", registration)
            .data("event", event)
            .render();

        sendEmail(registration, subject, mailBody);
    }

    void sendWaitlistToAttendeeConfirmation(Registration registration) {
        Event event = eventService.getEvent(registration.eventId);
        String subject = String.format("[%s] Dein Wartelisten-Eintrag für \"%s\" am %s",
            config.email().subjectPrefix(), event.summary, event.startDate());

        String mailBody = tplWaitlist2attendee
            .data("tenant", config.tenant())
            .data("registration", registration)
            .data("event", event)
            .render();

        sendEmail(registration, subject, mailBody);
    }

    private void sendEmail(Registration registration, String subject, String mailBody) {
        Mail mail = Mail.withHtml(registration.email, subject, mailBody);
        mailer.send(mail);
    }

    public void sendBulkEmail(Collection<List<Registration>> chunkedRegistrations, String templateName, String subject, String body) {
        Qute.Fmt messageTemplate = Qute.fmt(body)
            .data("tenant", config.tenant());

        chunkedRegistrations.stream().flatMap(Collection::stream).forEach(registration -> {
            String emailMessage = messageTemplate.data("name", registration.getName()).data("eventId", registration.eventId).render();
            mailer.send(Mail.withHtml(registration.email, Qute.fmt(subject).data("name", registration.name).render(), emailMessage));
        });


    }



}
