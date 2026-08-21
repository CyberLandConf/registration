package de.jugda.registration;

import io.quarkus.mailer.Mail;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@QuarkusTest
@TestSecurity(user = "alice", roles = {"test"})
public class AdminFunctionalTest extends FunctionalTestBase {

    @BeforeAll
    static void createParticipants() {
        int port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
        PARTICIPANTS.forEach(participant -> given().port(port).contentType(ContentType.URLENC)
            .formParams("eventId", EVENT_ID, "name", participant.getName(), "email", participant.getEmail(), "limit", 60)
            .post("/registration/" + TENANT).then().statusCode(200));
    }

    @Test
    void testEventsOverview() {
        given().get("/admin/" + TENANT + "/events")
            .then()
            .statusCode(200)
            .body("html.body.div.div.div.h2", equalTo("Event-Anmeldungen"))
            .body("html.body.div.div.div.table.tbody.tr.size()", is(1)) // 1 Event
        ;
    }

    @Test
    void testEventRegistrations() {
        given().accept(ContentType.HTML)
            .pathParam("eventId", EVENT_ID)
            .get("/admin/" + TENANT + "/events/{eventId}")
            .then()
            .statusCode(200)
            .body(containsString("Anmeldungen für Event am"))
            .body("html.body.div.div.div.table.tbody.tr.size()", is(PARTICIPANTS.size())) // all participants should be registered
        ;
    }

    // Upload additional webinar data to event
    @Test
    void testUploadEventData() {
        given().contentType(ContentType.JSON)
            .pathParam("eventId", EVENT_ID)
            .body("{\"webinarLink\" : \"https://example.com/webinar\"}")
            .put("/admin/" + TENANT + "/events/{eventId}/data")
            .then()
            .statusCode(204);
    }

    @Test
    void testSendBulkEmailToParticipants() {
        given().contentType(ContentType.JSON)
            .pathParam("eventId", EVENT_ID)
            .body("{\"subject\" : \"Test Event\", \"summary\" : \"Herzlich willkommen\", \"registrationIds\" : []}")
            .put("/admin/" + TENANT + "/events/{eventId}/message")
            .then()
            .statusCode(204);
    }

    // With real recipients the mails go out in one batched send per chunk, not one at a time
    @Test
    void testBulkEmailReachesEverySelectedParticipant() {
        List<String> registrationIds = given().accept(ContentType.JSON)
            .pathParam("eventId", EVENT_ID)
            .get("/admin/" + TENANT + "/events/{eventId}")
            .then().statusCode(200)
            .extract().jsonPath().getList("id", String.class);
        assertThat(registrationIds).hasSameSizeAs(PARTICIPANTS);

        String recipient = PARTICIPANTS.get(0).getEmail();
        int before = mailbox.getMailsSentTo(recipient).size();

        given().contentType(ContentType.JSON)
            .pathParam("eventId", EVENT_ID)
            .body("{\"subject\" : \"Rundmail {tenant.name}\", \"message\" : \"Hallo {name}\", \"registrationIds\" : "
                + registrationIds.stream().collect(java.util.stream.Collectors.joining("\",\"", "[\"", "\"]")) + "}")
            .put("/admin/" + TENANT + "/events/{eventId}/message")
            .then()
            .statusCode(204);

        // The bulk send is synchronous, so the mails are in the box once the response is back
        List<Mail> mails = mailbox.getMailsSentTo(recipient);
        assertThat(mails).hasSize(before + 1);
        Mail bulk = mails.get(mails.size() - 1);
        assertThat(bulk.getSubject()).isEqualTo("Rundmail Test-JUG");
        assertThat(bulk.getHtml()).contains("Hallo " + PARTICIPANTS.get(0).getName());
        PARTICIPANTS.forEach(participant ->
            assertThat(mailbox.getMailsSentTo(participant.getEmail())).isNotEmpty());
    }

    // A mail that never went out must be visible where the orga team already looks
    @Test
    void testParticipantListShowsWhetherTheConfirmationMailWentOut() {
        String email = PARTICIPANTS.get(0).getEmail();
        await("confirmation stamp for " + email)
            .atMost(java.time.Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(confirmationSentAt(email)).isNotNull());

        given().accept(ContentType.HTML)
            .pathParam("eventId", EVENT_ID)
            .get("/admin/" + TENANT + "/events/{eventId}")
            .then()
            .statusCode(200)
            .body(containsString("bi-envelope-check"))
            .body(not(containsString("bi-envelope-exclamation")));
    }

    private static String confirmationSentAt(String email) {
        return given().accept(ContentType.JSON)
            .pathParam("eventId", EVENT_ID)
            .get("/admin/" + TENANT + "/events/{eventId}")
            .then().statusCode(200)
            .extract().jsonPath()
            .getString("find { it.email == '" + email + "' }.confirmationSentAt");
    }

    // The "Zum Online-Meeting" button needs the tenant in its path, otherwise it 404s
    @Test
    void testEventRegistrationsLinkToWebinarPageOfThisTenant() {
        given().contentType(ContentType.JSON)
            .pathParam("eventId", EVENT_ID)
            .body("{\"webinarLink\" : \"https://example.com/webinar\"}")
            .put("/admin/" + TENANT + "/events/{eventId}/data")
            .then().statusCode(204);

        given().accept(ContentType.HTML)
            .pathParam("eventId", EVENT_ID)
            .get("/admin/" + TENANT + "/events/{eventId}")
            .then()
            .statusCode(200)
            .body(containsString("href=\"/webinar/" + TENANT + "/" + EVENT_ID + "\""))
        ;
    }

    @Test
    void testContentPageListsAllMaintainableTexts() {
        given().get("/admin/" + TENANT + "/content")
            .then()
            .statusCode(200)
            .body(containsString("registration.name"))
            .body(containsString("registration.email"))
            .body(containsString("registration.video"))
            .body(containsString("registration.disclaimer"))
            .body(containsString("registration.waitlist"))
            .body(containsString("webinar.tools"));
    }

    // What the orga team saves here has to reach the public form -- otherwise they are editing into the void
    @Test
    void testEditedContentShowsUpOnRegistrationForm() {
        String original = contentValue("registration.name");
        String edited = "Wir brauchen Deinen Namen fuer die Teilnehmerliste.";
        try {
            saveContent("registration.name", edited);

            given().get("/admin/" + TENANT + "/content")
                .then().statusCode(200)
                .body(containsString(edited));

            given().queryParam("eventId", EVENT_ID)
                .queryParam("limit", 60)
                .queryParam("opensBeforeInMonths", 12)
                .get("/registration/" + TENANT)
                .then().statusCode(200)
                .body(containsString(edited));
        } finally {
            saveContent("registration.name", original);
        }
    }

    // A key no template reads must not end up in the database
    @Test
    void testUnknownContentKeysAreIgnored() {
        given().contentType(ContentType.URLENC)
            .formParam("bogus.key", "nirgends sichtbar")
            .redirects().follow(false)
            .post("/admin/" + TENANT + "/content")
            .then().statusCode(302);

        given().get("/admin/" + TENANT + "/content")
            .then().statusCode(200)
            .body(not(containsString("bogus.key")));
    }

    private static void saveContent(String key, String value) {
        given().contentType(ContentType.URLENC)
            .formParam(key, value)
            .redirects().follow(false)
            .post("/admin/" + TENANT + "/content")
            .then().statusCode(302);
    }

    private static String contentValue(String key) {
        return adminPageValue("content", "**.find { it.name() == 'textarea' && it.@name == '" + key + "' }");
    }

    /** Reads one value out of an admin page, addressed by a GPath expression over the rendered HTML. */
    private static String adminPageValue(String page, String gpath) {
        return given().get("/admin/" + TENANT + "/" + page)
            .then().statusCode(200)
            .extract().response().htmlPath()
            .getString(gpath);
    }

    @Test
    void testWebinarPage() {
        given()
            .get("/webinar/" + TENANT + "/" + EVENT_ID)
            .then()
            .statusCode(200)
            .body("html.body.div.div[1].div.h3", equalTo("Link zu unserem Online-Meeting"))
        ;
    }
}
