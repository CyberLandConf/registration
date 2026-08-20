package de.jugda.registration.event;

import de.jugda.registration.model.RegistrationDto;

import java.net.URI;

/**
 * Something happened to a registration that a participant needs to be told about by mail.
 * <p>
 * The observers run asynchronously on a worker thread, so the event has to carry everything
 * the mail rendering needs: neither {@code UriInfo} nor the request-scoped {@code TenantContext}
 * of the triggering HTTP request are available there.
 */
public sealed interface RegistrationEvent permits RegistrationConfirmed, WaitlistPromoted {

    String tenantId();

    URI baseUrl();

    RegistrationDto registration();
}
