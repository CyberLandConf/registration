package de.jugda.registration.service;

import de.jugda.registration.TenantContext;
import de.jugda.registration.event.RegistrationConfirmed;
import de.jugda.registration.event.RegistrationEvent;
import de.jugda.registration.event.WaitlistPromoted;
import de.jugda.registration.model.EventDto;
import de.jugda.registration.model.RegistrationDto;
import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Qute;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;
    @Inject
    EventService eventService;
    @Location("mail/registration")
    Template tplRegistration;
    @Location("mail/waitlist2attendee")
    Template tplWaitlist2attendee;

    @Inject
    TenantContext tenantCtx;
    @Inject
    UriInfo uriInfo;
    @Inject
    Event<RegistrationEvent> registrationEvents;

    /**
     * Runs on the request thread, but only once the triggering transaction has committed, so no mail
     * leaves the house for a registration that ends up being rolled back. Re-firing the event
     * asynchronously takes the actual delivery off the request thread. {@code fireAsync} only reaches
     * {@code @ObservesAsync} methods, so this does not loop back into the relay.
     * <p>
     * Observing the sealed supertype covers every {@link RegistrationEvent}: CDI resolves observers by
     * the runtime class of the fired object and its supertypes.
     */
    void relayAfterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) RegistrationEvent event) {
        registrationEvents.fireAsync(event);
    }

    /**
     * Runs on a worker thread with no HTTP request attached. ArC activates the request context around
     * observer notification, but the tenant of the originating request is gone, so it is restored from
     * the event payload before Qute and {@link EventService} resolve it. Exceptions of an asynchronous
     * observer end up in a {@code CompletionStage} nobody looks at, so they are logged here instead of
     * being propagated - a failed mail no longer rolls the registration back.
     */
    void onRegistrationEvent(@ObservesAsync RegistrationEvent event) {
        try {
            tenantCtx.setTenantId(event.tenantId());
            sendParticipantMail(event);
        } catch (Exception e) {
            Log.errorf(e, "Could not send %s mail for event %s to registration %s",
                event.getClass().getSimpleName(), event.registration().eventId, event.registration().id);
        }
    }

    /**
     * The switch is exhaustive over the sealed {@link RegistrationEvent}, so a new event type fails to
     * compile here instead of silently sending no mail.
     */
    private void sendParticipantMail(RegistrationEvent event) {
        ParticipantMail kind = switch (event) {
            case RegistrationConfirmed ignored -> new ParticipantMail(tplRegistration, "Anmeldebestätigung");
            case WaitlistPromoted ignored -> new ParticipantMail(tplWaitlist2attendee, "Dein Wartelisten-Eintrag");
        };

        RegistrationDto registration = event.registration();
        EventDto eventData = eventService.getEvent(registration.eventId).orElse(null);
        String tenantName = tenantCtx.getTenant().getName();
        String subject = eventData != null
            ? String.format("[%s] %s für \"%s\" am %s",
                tenantName, kind.subjectLabel(), eventData.summary, eventData.startDate())
            : String.format("[%s] %s", tenantName, kind.subjectLabel());

        String mailBody = kind.template()
            .data("tenant", tenantCtx.getTenant())
            .data("registration", registration)
            .data("event", eventData)
            .data("baseUrl", event.baseUrl())
            .render();

        sendMail(Mail.withHtml(registration.email, subject, mailBody));
    }

    private record ParticipantMail(Template template, String subjectLabel) {
    }

    public void sendBulkEmail(Collection<List<RegistrationDto>> chunkedRegistrations, String subject, String body) {
        subject = sanitize(subject);
        body = sanitize(body);

        String subjectRendered = Qute.fmt(subject).data("tenant", tenantCtx.getTenant()).render();
        // Fmt re-parses the body on every render(). Deliberately not cached: Qute's template cache is
        // unbounded and these bodies are free text typed by an admin.
        Qute.Fmt messageTemplate = Qute.fmt(body)
            .data("tenant", tenantCtx.getTenant());
        String replyTo = tenantCtx.getTenant().getReplyTo();
        URI baseUrl = uriInfo.getBaseUri();

        // One send call per chunk: the mailer dispatches a batch concurrently, which is what the
        // caller's chunking is for - flattening it away sends one blocking mail at a time.
        chunkedRegistrations.forEach(chunk -> {
            Mail[] mails = chunk.stream()
                .map(registration -> {
                    String emailMessage = messageTemplate
                        .data("name", registration.getName())
                        .data("eventId", registration.eventId)
                        .data("baseUrl", baseUrl)
                        .render();
                    return Mail.withHtml(registration.email, subjectRendered, emailMessage).setReplyTo(replyTo);
                })
                .toArray(Mail[]::new);
            mailer.send(mails);
        });
    }

    private void sendMail(Mail mail) {
        mail.setReplyTo(tenantCtx.getTenant().getReplyTo());
        mailer.send(mail);
    }

    private String sanitize(String s) {
        return s.replace("{{", "{").replace("}}", "}");
    }

}
