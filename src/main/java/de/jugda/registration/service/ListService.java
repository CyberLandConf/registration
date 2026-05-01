package de.jugda.registration.service;

import de.jugda.registration.dao.RegistrationDao;
import de.jugda.registration.domain.Registration;
import de.jugda.registration.model.RegistrationDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@ApplicationScoped
public class ListService {

    @Inject
    RegistrationDao registrationDao;

    public List<RegistrationDto> singleEventRegistrations(String eventId) {
        return registrationDao.findByEventId(eventId).stream().map(Registration::toDto).toList();
    }

    public Map<String, Integer> allEvents() {
        List<RegistrationDto> registrations = registrationDao.findAll().stream().map(Registration::toDto).toList();

        Map<String, Integer> events = new LinkedHashMap<>();
        registrations.forEach(reg -> events.put(reg.getEventId(), events.getOrDefault(reg.getEventId(), 0) + 1));

        return events;
    }

}
