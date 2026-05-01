package de.jugda.registration.dao;

import de.jugda.registration.PostgresTestcontainersResource;
import de.jugda.registration.domain.Registration;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresTestcontainersResource.class)
class RegistrationDaoTest {

    @Inject
    RegistrationDao registrationDaoUnderTest;

    @Test
    public void roundTripFindByEventIdAndEmailRegistration() {
        Registration registration = new Registration();
        registration.setEventId("eventId1");
        registration.setEmail("email1");

        registrationDaoUnderTest.save(registration);

        Registration foundRegistration = registrationDaoUnderTest.findByEventIdAndEmail(registration);

        assertThat(foundRegistration.getEventId()).isEqualTo(registration.getEventId());
        assertThat(foundRegistration.getEmail()).isEqualTo(registration.getEmail());
    }

    @Test
    public void roundTripFindAllRegistration() {
        Registration registration = new Registration();
        registration.setEventId("eventId");
        registration.setEmail("email");

        registrationDaoUnderTest.save(registration);

        List<Registration> foundRegistration = registrationDaoUnderTest.findAll();

        assertThat(foundRegistration).hasSize(1);
    }

    @Test
    public void roundTripFindByEventId(){
        Registration registration = new Registration();
        registration.setEventId("eventId3");
        registration.setEmail("email3");

        registrationDaoUnderTest.save(registration);

        List<Registration> foundRegistration = registrationDaoUnderTest.findByEventId(registration.getEventId());

        assertThat(foundRegistration).hasSize(1);
    }

    @Test
    public void roundtripFindWaitlistByEventId(){
        Registration registration = new Registration();
        registration.setEventId("eventId4");
        registration.setEmail("email4");
        registration.setWaitlist(true);

        registrationDaoUnderTest.save(registration);

        List<Registration> waitingList = registrationDaoUnderTest.findWaitlistByEventId(registration.getEventId());

        assertThat(waitingList).hasSize(1);
    }

    @Test
    public void rountripGetCount(){
        Registration registration = new Registration();
        registration.setEventId("eventId5");
        registration.setEmail("email5");
        registration.setRemote(false);

        registrationDaoUnderTest.save(registration);

        int count = registrationDaoUnderTest.getCount(registration.getEventId());
        assertThat(count).isEqualTo(1);
    }

}
