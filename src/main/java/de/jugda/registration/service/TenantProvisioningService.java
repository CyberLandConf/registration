package de.jugda.registration.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.regex.Pattern;

/**
 * Creates a new JUG by cloning the {@value #TEMPLATE_TENANT} tenant: master data and help texts are copied,
 * only id and name come from the form. That way a fresh JUG has working defaults everywhere instead of an
 * empty registration form its orga team then has to fill field by field.
 * <p>
 * All statements are native SQL on purpose. {@code Tenant} and {@code Content} carry a Hibernate
 * {@code @TenantId}, so every JPA query would be filtered down to the tenant of the current request --
 * exactly the isolation this one operation has to reach across. Native SQL keeps that crossing explicit
 * and local to this class.
 */
@ApplicationScoped
public class TenantProvisioningService {

    /** The JUG whose data every new JUG starts out with. */
    public static final String TEMPLATE_TENANT = "test";

    /** The id ends up in URLs and has to match a Keycloak role name, hence the narrow character set. */
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]{1,49}");

    @Inject
    EntityManager em;

    /**
     * @return the normalized id of the new JUG, so callers do not have to repeat the normalization rules
     */
    @Transactional
    public String create(String id, String name) {
        String tenantId = id == null ? "" : id.strip();
        String tenantName = name == null ? "" : name.strip();

        if (!ID_PATTERN.matcher(tenantId).matches()) {
            throw new TenantProvisioningException(
                "Die ID darf nur Kleinbuchstaben, Ziffern und Bindestriche enthalten und muss mit "
                    + "einem Buchstaben oder einer Ziffer beginnen (2 bis 50 Zeichen).");
        }
        if (tenantName.isEmpty()) {
            throw new TenantProvisioningException("Bitte einen Namen für die JUG angeben.");
        }
        if (exists(tenantId)) {
            throw new TenantProvisioningException("Es gibt bereits eine JUG mit der ID '" + tenantId + "'.");
        }

        // Keep this column list in sync with the Tenant entity: a column added there and forgotten here
        // clones as empty, which is exactly the silent half-configured JUG this page exists to prevent.
        int created = em.createNativeQuery("""
                insert into tenant (id, name, website, privacy, imprint, logo, reply_to, events, css)
                select :id, :name, website, privacy, imprint, logo, reply_to, events, css
                  from tenant where id = :template
                """)
            .setParameter("id", tenantId)
            .setParameter("name", tenantName)
            .setParameter("template", TEMPLATE_TENANT)
            .executeUpdate();

        if (created == 0) {
            // Without the template there is nothing to copy; better to fail loudly than to create a half tenant
            throw new TenantProvisioningException(
                "Die Vorlage-JUG '" + TEMPLATE_TENANT + "' existiert nicht, es wurde nichts angelegt.");
        }

        em.createNativeQuery("""
                insert into content (tenant, "key", "value")
                select :id, "key", "value" from content where tenant = :template
                """)
            .setParameter("id", tenantId)
            .setParameter("template", TEMPLATE_TENANT)
            .executeUpdate();

        return tenantId;
    }

    private boolean exists(String tenantId) {
        Number count = (Number) em.createNativeQuery("select count(*) from tenant where id = :id")
            .setParameter("id", tenantId)
            .getSingleResult();
        return count.intValue() > 0;
    }
}
