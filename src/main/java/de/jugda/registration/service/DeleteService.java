package de.jugda.registration.service;

import de.jugda.registration.TenantContext;
import de.jugda.registration.domain.Registration;
import de.jugda.registration.event.WaitlistPromoted;
import de.jugda.registration.model.DeregistrationForm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriInfo;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DeleteService {

    @Inject
    Event<WaitlistPromoted> waitlistPromoted;
    @Inject
    TenantContext tenantCtx;
    @Inject
    UriInfo uriInfo;

    @Transactional
    public Optional<String> deleteFromUi(DeregistrationForm form) {
        Registration registration = Registration
            .find("eventId = ?1 and email = ?2", form.getEventId(), form.getEmail().toLowerCase())
            .firstResult();
        if (registration == null) {
            return Optional.empty();
        }
        return deleteFromUri(registration.getId());
    }

    @Transactional
    public Optional<String> deleteFromUri(UUID id) {
        Registration registration = Registration.findById(id);
        if (registration == null) {
            return Optional.empty();
        }
        if (!registration.isWaitlist()) {
            processWaitlist(registration.getEventId());
        }
        String name = registration.getName();
        Registration.deleteById(id);
        return Optional.of(name);
    }

    @Transactional
    public void delete(UUID id) {
        Registration.deleteById(id);
    }

    private void processWaitlist(String eventId) {
        Registration.find("eventId = ?1 and waitlist = true order by created asc", eventId)
            .<Registration>firstResultOptional()
            .ifPresent(waiter -> {
                waiter.setWaitlist(false);
                // The promotion mail is owed again - keep a stale confirmation from vouching for it
                waiter.setConfirmationSentAt(null);
                waiter.persist();
                // Delivered only after this transaction commits - see EmailService.
                waitlistPromoted.fire(new WaitlistPromoted(tenantCtx.getTenantId(), uriInfo.getBaseUri(), waiter.toDto()));
            });
    }

}
