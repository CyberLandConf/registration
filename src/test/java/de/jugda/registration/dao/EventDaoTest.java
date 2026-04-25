package de.jugda.registration.dao;

import de.jugda.registration.PostgresTestcontainersResource;
import de.jugda.registration.domain.Event;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(PostgresTestcontainersResource.class)
class EventDaoTest {

    @Inject
    private EventDao eventDaoUnderTest;

    @Test
    @Transactional
    public void roundTripGetAllEvents() {
        Event event = new Event();
        event.setTitle("Test");
        eventDaoUnderTest.createEvent(event);
        List<Event> allEvents = eventDaoUnderTest.getAllEvents();

        assertThat(allEvents).hasSize(1);
    }


    @Test
    @Transactional
    public void roundTripfindEvent() {
        Event event = new Event();
        event.setTitle("Test");
        eventDaoUnderTest.createEvent(event);
        Event findEvent = eventDaoUnderTest.getEventById(event.getUid());

        assertThat(findEvent.getTitle()).isEqualTo("Test");
    }
}
