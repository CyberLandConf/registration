package de.jugda.registration.event;

import de.jugda.registration.model.RegistrationDto;

import java.net.URI;

/**
 * Domain event: a registration has been stored successfully.
 */
public record RegistrationConfirmed(String tenantId, URI baseUrl, RegistrationDto registration)
    implements RegistrationEvent {
}
