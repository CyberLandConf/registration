package de.jugda.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jugda.registration.dao.EventDao;
import de.jugda.registration.domain.Event;
import de.jugda.registration.dao.EventDao;
import de.jugda.registration.model.EventDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;

import java.util.Map;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@ApplicationScoped
public class EventService {

    @Inject
    EventDao eventDao;

    public EventDto getEvent(String eventId) {
        Event event = eventDao.getEventByEventId(eventId);
        return event == null ? null : event.toDto();
    }

    @SneakyThrows
    public void putEventData(String eventId, EventDto eventDto) {
        eventDto.eventId = eventId;
        eventDao.createEvent(Event.fromDto(eventDto));
    }
}
