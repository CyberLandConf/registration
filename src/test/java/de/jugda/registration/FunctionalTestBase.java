package de.jugda.registration;

import lombok.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public abstract class FunctionalTestBase {

    static final String EVENT_ID = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

    static final List<Participant> PARTICIPANTS = List.of(
        new Participant("John Doe", "john.doe@example.com"),
        new Participant("Jane Doe", "jane.doe@example.com"),
        new Participant("Jack Doe", "jack.doe@example.com")
    );

    @Value
    static class Participant {
        String name;
        String email;
    }
}
