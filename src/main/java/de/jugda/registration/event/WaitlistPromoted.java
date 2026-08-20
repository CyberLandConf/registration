package de.jugda.registration.event;

import de.jugda.registration.model.RegistrationDto;

import java.net.URI;

/**
 * Domain event: a spot became free and the longest waiting registration moved up to be an attendee.
 */
public record WaitlistPromoted(String tenantId, URI baseUrl, RegistrationDto registration)
    implements RegistrationEvent {
}
