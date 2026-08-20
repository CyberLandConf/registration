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

@ApplicationScoped
public class RegistrationService {

    @Inject
    Event<RegistrationConfirmed> registrationConfirmed;
    @Inject
    TenantContext tenantCtx;
    @Inject
    UriInfo uriInfo;

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
        registration.persist();

        RegistrationDto dto = registration.toDto();
        // Fired inside the transaction, delivered only after it commits - see EmailService.
        registrationConfirmed.fire(new RegistrationConfirmed(tenantCtx.getTenantId(), uriInfo.getBaseUri(), dto));
        return dto;
    }

}
