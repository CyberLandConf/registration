package de.jugda.registration;

import io.quarkus.mailer.Mail;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@QuarkusTest
public class RegistrationAndDeletionFunctionalTest extends FunctionalTestBase {

    @Inject
    EntityManager em;

    @BeforeEach
    @Transactional
    void cleanup(){
        mailbox.clear();
        em.createNativeQuery("DELETE FROM registration").executeUpdate();
    }

    @Test
    void testWaitlistIsEnforcedServerSide() {
        // Fill the single available slot; include limit as the hidden field would in a real browser
        given().contentType(ContentType.URLENC)
            .formParams("eventId", EVENT_ID, "name", PARTICIPANTS.get(0).getName(), "email", PARTICIPANTS.get(0).getEmail(), "limit", 1)
            .post("/registration/" + TENANT)
            .then().statusCode(200)
            .body(containsString("Wir haben Deine Anmeldung erhalten.</p>"));

        // Second participant: limit comes from the form body (as it would via the hidden field),
        // waitlist flag is computed server-side from count vs limit — not from any submitted field
        given().contentType(ContentType.URLENC)
            .formParams("eventId", EVENT_ID, "name", PARTICIPANTS.get(1).getName(), "email", PARTICIPANTS.get(1).getEmail(), "limit", 1)
            .post("/registration/" + TENANT)
            .then().statusCode(200)
            .body(containsString("auf die Warteliste gesetzt"));

        // GET with the same limit should now show the waitlist heading
        given()
            .queryParam("eventId", EVENT_ID)
            .queryParam("limit", 1)
            .queryParam("deadline", EVENT_ID + "T23:59:59+02:00")
            .queryParam("opensBeforeInMonths", 12)
            .get("/registration/" + TENANT)
            .then().statusCode(200)
            .body("html.body.form.h3", equalTo("Warteliste"));
    }

    @Test
    void testWaitlistPromotionMailIsSentAfterAttendeeDeregisters() {
        Participant attendee = PARTICIPANTS.get(0);
        Participant waiting = PARTICIPANTS.get(1);

        // One slot, so the second registration lands on the waitlist
        given().contentType(ContentType.URLENC)
            .formParams("eventId", EVENT_ID, "name", attendee.getName(), "email", attendee.getEmail(), "limit", 1)
            .post("/registration/" + TENANT)
            .then().statusCode(200);
        given().contentType(ContentType.URLENC)
            .formParams("eventId", EVENT_ID, "name", waiting.getName(), "email", waiting.getEmail(), "limit", 1)
            .post("/registration/" + TENANT)
            .then().statusCode(200)
            .body(containsString("auf die Warteliste gesetzt"));

        awaitTotalMails(2);

        // The attendee frees the slot, which promotes the waiting participant
        given().contentType(ContentType.URLENC)
            .formParams("eventId", EVENT_ID, "email", attendee.getEmail())
            .post("/registration/" + TENANT + "/delete")
            .then().statusCode(200)
            .body("html.body.h3", equalTo("Vielen Dank, " + attendee.getName()));

        List<Mail> mails = awaitMailsTo(waiting.getEmail(), 2);
        assertThat(mails.get(1).getSubject()).contains("Dein Wartelisten-Eintrag");
        assertThat(mails.get(1).getHtml()).contains("von der Warteliste auf die reguläre Teilnehmerliste");
        assertThat(mailbox.getTotalMessagesSent()).isEqualTo(3);
    }

    // Der Nachrichtentyp ist der Vertrag mit den einbettenden JUG-Seiten (docs/handbuch.adoc)
    @Test
    void testEmbeddedPagesReportTheirHeightToTheHostPage() {
        given()
            .queryParam("eventId", EVENT_ID)
            .queryParam("deadline", EVENT_ID + "T23:59:59+02:00")
            .queryParam("opensBeforeInMonths", 12)
            .get("/registration/" + TENANT)
            .then()
            .statusCode(200)
            .body(containsString("window.parent.postMessage"))
            .body(containsString("ijug-registration:height"));
    }

    @Test
    void testGetRegistrationForm() {
        given()
            .queryParam("eventId", EVENT_ID)
            .queryParam("deadline", EVENT_ID + "T23:59:59+02:00")
            .queryParam("opensBeforeInMonths", 12)
            .get("/registration/" + TENANT)
            .then()
            .statusCode(200)
            .body("html.body.form.h3", equalTo("Anmeldung"));
    }

    @Test
    void testCreateRegistrationAndDeleteViaDeleteRequest() {
        Participant participant = PARTICIPANTS.get(0);
        String link = given().contentType(ContentType.URLENC)
            .formParams(
                "eventId", EVENT_ID,
                "name", participant.getName(),
                "email", participant.getEmail()
            )
            .post("/registration/" + TENANT)
            .then()
            .statusCode(200)
            .body("html.body.h3", equalTo("Vielen Dank, " + participant.getName()))
            .extract()
            .response()
            .htmlPath()
            .getString("html.body.p[2].a");
        String registrationId = link.substring(link.indexOf("?id=") + 4);

        awaitTotalMails(1);

        given()
            .queryParam("id", registrationId)
            .delete("/registration/" + TENANT + "/delete")
            .then()
            .statusCode(204);
    }

    @Test
    void testCreateRegistrationAndDeleteViaDirectLink() {
        Participant participant = PARTICIPANTS.get(1);
        String link = given().contentType(ContentType.URLENC)
            .formParams(
                "eventId", EVENT_ID,
                "name", participant.getName(),
                "email", participant.getEmail()
            )
            .post("/registration/" + TENANT)
            .then()
            .statusCode(200)
            .body("html.body.h3", equalTo("Vielen Dank, " + participant.getName()))
            .extract()
            .response()
            .htmlPath()
            .getString("html.body.p[2].a");
        String registrationId = link.substring(link.indexOf("?id=") + 4);

        awaitTotalMails(1);

        given()
            .queryParam("id", registrationId)
            .get("/registration/" + TENANT + "/delete")
            .then()
            .statusCode(200)
            .body("html.body.h3", equalTo("Vielen Dank, " + participant.getName()));
    }

    @Test
    void testCreateRegistrationAndDeleteViaFormPost() {
        Participant participant = PARTICIPANTS.get(2);
        given().contentType(ContentType.URLENC)
            .formParams(
                "eventId", EVENT_ID,
                "name", participant.getName(),
                "email", participant.getEmail()
            )
            .post("/registration/" + TENANT)
            .then()
            .statusCode(200)
            .body("html.body.h3", equalTo("Vielen Dank, " + participant.getName()));


        awaitTotalMails(1);

        // test if the deletion form returns
        given()
            .queryParam("eventId", EVENT_ID)
            .get("/registration/" + TENANT + "/delete")
            .then()
            .statusCode(200)
            .body("html.body.form.h3", equalTo("Abmeldung"));

        // test if the deletion itself works
        given().contentType(ContentType.URLENC)
            .formParams(
                "eventId", EVENT_ID,
                "email", participant.getEmail()
            )
            .post("/registration/" + TENANT + "/delete")
            .then()
            .statusCode(200)
            .body("html.body.h3", equalTo("Vielen Dank, " + participant.getName()));
    }

}
