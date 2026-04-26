package de.jugda.registration;

import io.quarkus.test.common.QuarkusTestResource;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@QuarkusTestResource(LocalstackResource.class)
public abstract class FunctionalTestBase {

    static final String EVENT_ID = "2026-04-26";

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
