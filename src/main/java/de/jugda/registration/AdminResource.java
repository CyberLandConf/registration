package de.jugda.registration;

import de.jugda.registration.model.Event;
import de.jugda.registration.model.Registration;
import de.jugda.registration.service.EmailService;
import de.jugda.registration.service.EventService;
import de.jugda.registration.service.ListService;
import de.jugda.registration.service.RegistrationService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@Path("admin/events")
@Produces(MediaType.TEXT_HTML)
public class AdminResource {

    @Inject
    ListService listService;
    @Inject
    EventService eventService;
    @Inject
    EmailService emailService;
    @Inject
    Config config;
    @Location("admin/overview")
    Template overview;
    @Location("admin/list")
    Template list;
    @Inject
    RegistrationService registrationService;

    @GET
    public TemplateInstance getAllEvents() {
        Map<String, Integer> events = listService.allEvents();

        return overview.data("tenant", config.tenant()).data("events", events);
    }

    @GET
    @Path("{eventId}")
    public TemplateInstance getEventList(@PathParam("eventId") String eventId) {
        List<Registration> registrations = listService.singleEventRegistrations(eventId);
        Event event = eventService.getEvent(eventId);
        Map<String, String> eventData = eventService.getEventData().get(eventId);

        return list.data("eventId", eventId)
            .data("event", event)
            .data("eventData", eventData)
            .data("tenant", config.tenant())
            .data("registrations", registrations);
    }

    @GET
    @Path("{eventId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Registration> getRegistrationList(@PathParam("eventId") String eventId) {
        return listService.singleEventRegistrations(eventId);
    }

    @PUT
    @Path("{eventId}/data")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putEventData(@PathParam("eventId") String eventId, Map<String, String> data) {
        eventService.putEventData(eventId, data);
        return Response.noContent().build();
    }

    @PUT
    @Path("{eventId}/message")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendMessage(@PathParam("eventId") String eventId, Map<String, Object> data) {
        String templateName = "custom";
        String subject = (String) data.get("subject");
        String message = (String) data.get("message");

        //noinspection unchecked
        List<String> registrationIds = (List<String>) data.get("registrationIds");
        if (null == registrationIds) {
            throw new IllegalArgumentException("Data does not contain any registrationIds");
        }

        AtomicInteger index = new AtomicInteger(0);
        Collection<List<Registration>> chunkedRegistrations = listService.singleEventRegistrations(eventId).stream()
            .filter(registration -> registrationIds.contains(registration.getId()))
            .collect(Collectors.groupingBy(x -> index.getAndIncrement() / 50)).values();

        if (!chunkedRegistrations.isEmpty()) {
            emailService.sendBulkEmail(chunkedRegistrations, templateName, subject, message);
        }

        return Response.noContent().build();
    }

    @PUT
    @Path("{eventId}/messageToAll")
    public Response sendMessageToAll(@PathParam("eventId") String eventId) {
        String templateName = "custom";
        String subject = "Link zum Online-Vortrag der {{tenant.name}} heute Abend";
        String message = """
Hallo {{name}}!

Du hast Dich für unseren heutigen Online-Vortrag angemeldet. Den Link zur Einwahl und weitere Informationen findest Du hier:

{{tenant.baseUrl}}/webinar/{{eventId}}

Bis heute Abend, wir freuen uns auf Dein Kommen. Im Anschluss an den Vortrag kannst Du übrigens gern noch bei unserem virtuellen Stammtisch mitplaudern oder einfach nur zuhören.

Viele Grüße
Dein {{tenant.name}} Orga-Team""";


        AtomicInteger index = new AtomicInteger(0);
        Collection<List<Registration>> chunkedRegistrations = listService.singleEventRegistrations(eventId).stream()
            .collect(Collectors.groupingBy(x -> index.getAndIncrement() / 50)).values();

        if (!chunkedRegistrations.isEmpty()) {
            emailService.sendBulkEmail(chunkedRegistrations, templateName, subject, message);
        }

        return Response.noContent().build();
    }


}
