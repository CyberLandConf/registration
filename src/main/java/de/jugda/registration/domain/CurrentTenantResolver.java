package de.jugda.registration.domain;

import de.jugda.registration.TenantContext;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@PersistenceUnitExtension
@RequestScoped
public class CurrentTenantResolver implements TenantResolver {

    @Inject
    TenantContext ctx;

    @Override
    public String getDefaultTenantId() {
        return "public";
    }

    @Override
    public String resolveTenantId() {
        String t = ctx.getTenantId();
        return t != null ? t : getDefaultTenantId();   // fallback for non-request contexts (jobs!)
    }
}
