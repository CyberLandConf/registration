package de.jugda.registration;

import de.jugda.registration.domain.Tenant;
import io.quarkus.qute.RawString;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.regex.Pattern;

/**
 * Exposes the tenant's own stylesheet to the templates, reachable as {@code inject:tenantStyle}.
 * <p>
 * Named CDI bean rather than per-page template data on purpose: {@code tenant} means two different
 * things in the participant templates -- the bare id string in {@code registration.html} and the
 * {@link Tenant} entity in the webinar pages -- so there is no one expression every page could use.
 * The bean also reaches the pages that pass no tenant at all ({@code delete.html}).
 * <p>
 * The CSS is rendered unescaped inside a {@code <style>} element, hence {@link RawString}. The one
 * sequence that could break out of that element is a literal {@code </style}; it is rejected when the
 * form is saved ({@link #closesTheStyleElement(String)}) and stripped again here, so a row that
 * reached the database some other way cannot inject markup either.
 */
@Named("tenantStyle")
@RequestScoped
public class TenantStyle {

    /**
     * The HTML tokenizer leaves a {@code <style>} element only at {@code </style} -- whitespace after
     * the slash does not close it -- so this one sequence is the whole attack surface.
     */
    private static final Pattern STYLE_END_TAG = Pattern.compile("</style", Pattern.CASE_INSENSITIVE);

    @Inject
    TenantContext tenantCtx;

    private RawString css;

    /** The tenant's stylesheet, empty when it has none. */
    public RawString getCss() {
        if (css == null) {
            css = new RawString(load());
        }
        return css;
    }

    /** Whether there is anything to render -- keeps an empty {@code <style>} element out of the page. */
    public boolean isPresent() {
        return !getCss().toString().isEmpty();
    }

    private String load() {
        // Endpoints without a {tenant} path segment never get a tenant id from TenantAccessFilter
        if (tenantCtx.getTenantId() == null) {
            return "";
        }
        Tenant tenant = tenantCtx.getTenant();
        if (tenant == null || tenant.getCss() == null || tenant.getCss().isBlank()) {
            return "";
        }
        return STYLE_END_TAG.matcher(tenant.getCss()).replaceAll("");
    }

    /** @return whether the given CSS would end the {@code <style>} element it is rendered into */
    public static boolean closesTheStyleElement(String css) {
        return css != null && STYLE_END_TAG.matcher(css).find();
    }
}
