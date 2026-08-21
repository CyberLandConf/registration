package de.jugda.registration.service;

/**
 * Signals that a new JUG could not be created. The message is shown to the admin on the form, so it is
 * German like the rest of the admin UI.
 */
public class TenantProvisioningException extends RuntimeException {
    public TenantProvisioningException(String message) {
        super(message);
    }
}
