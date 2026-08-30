package de.jugda.registration.model;

import de.jugda.registration.domain.Tenant;
import jakarta.ws.rs.FormParam;
import lombok.Data;

@Data
public class TenantForm {
    @FormParam("name")
    public String name;
    @FormParam("website")
    public String website;
    @FormParam("privacy")
    public String privacy;
    @FormParam("imprint")
    public String imprint;
    @FormParam("logo")
    public String logo;
    @FormParam("replyTo")
    public String replyTo;
    @FormParam("events")
    public String events;
    @FormParam("css")
    public String css;

    /** Fills the form for the GET, so the page renders from the form in both directions. */
    public static TenantForm of(Tenant tenant) {
        TenantForm form = new TenantForm();
        form.setName(tenant.getName());
        form.setWebsite(tenant.getWebsite());
        form.setPrivacy(tenant.getPrivacy());
        form.setImprint(tenant.getImprint());
        form.setLogo(tenant.getLogo());
        form.setReplyTo(tenant.getReplyTo());
        form.setEvents(tenant.getEvents());
        form.setCss(tenant.getCss());
        return form;
    }
}
