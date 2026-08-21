package de.jugda.registration.api;

import de.jugda.registration.TenantContext;
import de.jugda.registration.domain.Content;
import de.jugda.registration.model.RegistrationConfig;
import de.jugda.registration.model.RegistrationDto;
import de.jugda.registration.model.RegistrationForm;
import de.jugda.registration.service.RegistrationService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Path("registration/{tenant}")
@Produces(MediaType.TEXT_HTML)
public class RegistrationResource {

    @Inject
    RegistrationService registrationService;
    @Inject
    Validator validator;
    @Inject
    Template closed;
    @Inject
    Template not_yet_open;
    @Inject
    Template registration;
    @Inject
    Template thanks;

    @Inject
    TenantContext tenantCtx;

    @GET
    public TemplateInstance getForm(@QueryParam("eventId") String eventId,
                                    @QueryParam("limit") @DefaultValue("60") int limit,
                                    @QueryParam("showPub") @DefaultValue("false") boolean showPub,
                                    @QueryParam("showVideoRecording") @DefaultValue("false") boolean showVideoRecording,
                                    @QueryParam("hybrid") @DefaultValue("false") boolean hybrid,
                                    @QueryParam("deadline") String deadline,
                                    @QueryParam("opensBeforeInMonths") @DefaultValue("1") int opensBeforeInMonths) {
        if (deadline == null) {
            deadline = eventId + "T18:00:00+02:00";
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadlineTime = LocalDateTime.parse(deadline, DateTimeFormatter.ISO_DATE_TIME);
        LocalDate startDate = LocalDate.parse(eventId).minusMonths(opensBeforeInMonths);

        TemplateInstance response;
        if (now.isAfter(deadlineTime)) {
            response = closed.instance();
        } else if (now.toLocalDate().isBefore(startDate)) {
            response = not_yet_open.data("eventId", eventId)
                .data("startDate", startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        } else {
            long registrationCount = registrationService.getRegistrationCount(eventId);
            RegistrationForm form = new RegistrationForm();
            form.setEventId(eventId);
            form.setLimit(limit);
            form.setShowPub(showPub);
            form.setShowVideoRecording(showVideoRecording);
            form.setHybrid(hybrid);
            RegistrationConfig cfg = new RegistrationConfig(
                limit, limit - registrationCount, registrationCount,
                showPub, showVideoRecording, hybrid,
                registrationCount >= limit
            );
            response = registration.data("form", form)
                .data("cfg", cfg)
                .data("tenant", tenantCtx.getTenantId())
                .data("helptext", Content.asMap());
        }

        return response;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance postForm(@BeanParam RegistrationForm registrationForm) {
        Set<ConstraintViolation<RegistrationForm>> violations = validator.validate(registrationForm);
        if (violations.isEmpty()) {
            RegistrationDto registrationSaved = registrationService.handleRegistration(registrationForm, registrationForm.getLimit());
            return thanks
                .data("tenant", tenantCtx.getTenantId())
                .data("reg", registrationSaved);
        } else {
            violations.forEach(cv ->
                registrationForm.addValidationError(cv.getPropertyPath().toString(), cv.getMessage()));
            long registrationCount = registrationService.getRegistrationCount(registrationForm.getEventId());
            RegistrationConfig cfg = new RegistrationConfig(
                registrationForm.getLimit(),
                registrationForm.getLimit() - registrationCount,
                registrationCount,
                registrationForm.isShowPub(),
                registrationForm.isShowVideoRecording(),
                registrationForm.isHybrid(),
                registrationCount >= registrationForm.getLimit()
            );
            return registration.data("form", registrationForm)
                .data("cfg", cfg)
                .data("tenant", tenantCtx.getTenantId())
                .data("helptext", Content.asMap());
        }
    }

}
