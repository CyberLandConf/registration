package de.jugda.registration.service;

import de.jugda.registration.TenantContext;
import de.jugda.registration.domain.Registration;
import de.jugda.registration.event.RegistrationConfirmed;
import de.jugda.registration.model.RegistrationDto;
import de.jugda.registration.model.RegistrationForm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriInfo;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class RegistrationService {

    @Inject
    Event<RegistrationConfirmed> registrationConfirmed;
    @Inject
    TenantContext tenantCtx;
    @Inject
    UriInfo uriInfo;

    /**
     * Records that the participant mail for this registration actually went out. Called from the
     * asynchronous observer in {@link EmailService}, where no transaction is running - hence a
     * separate bean, so the interceptor applies.
     */
    @Transactional
    public void markConfirmationSent(String registrationId) {
        Registration registration = Registration.findById(UUID.fromString(registrationId));
        if (registration != null) {
            registration.setConfirmationSentAt(LocalDateTime.now());
        }
    }

    public long getRegistrationCount(String eventId) {
        return Registration.count("eventId", eventId);
    }

    @Transactional
    public RegistrationDto handleRegistration(RegistrationForm form, int limit) {
        Registration registration = Registration.find("eventId = ?1 and email = ?2", form.getEventId(), form.getEmail()).firstResult();
        if (registration != null) {
            registration.updateFrom(form);
        } else {
            boolean waitlist = getRegistrationCount(form.getEventId()) >= limit;
            registration = Registration.of(form, waitlist);
        }
        // Pending until the mail actually goes out - also on re-registration, where a stale
        // timestamp would otherwise vouch for a mail that is only about to be sent.
        registration.setConfirmationSentAt(null);
        registration.persist();

        RegistrationDto dto = registration.toDto();
        // Fired inside the transaction, delivered only after it commits - see EmailService.
        registrationConfirmed.fire(new RegistrationConfirmed(tenantCtx.getTenantId(), uriInfo.getBaseUri(), dto));
        return dto;
    }

}
