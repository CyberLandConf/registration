package de.jugda.registration;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Exposes the roles of the logged-in user to the templates, reachable as {@code inject:currentUser}.
 * Named CDI bean rather than per-page template data: the menu is included on every admin page, so the
 * flag would otherwise have to be threaded through every single resource method.
 * <p>
 * This only governs what the navigation shows. The endpoints stay guarded by {@code @RolesAllowed} --
 * hiding a link is a courtesy, not a permission check.
 */
@Named("currentUser")
@RequestScoped
public class CurrentUser {

    @Inject
    SecurityIdentity identity;

    public boolean isAdmin() {
        return identity.hasRole("admin");
    }
}
