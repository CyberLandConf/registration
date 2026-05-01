package de.jugda.registration;

import de.jugda.registration.model.RegistrationDto;
import de.jugda.registration.model.RegistrationForm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class RegistrationDtoTest {
    @Test
    public void testStaticOfForm() {
        RegistrationForm form = new RegistrationForm();
        form.setEventId("2018-12-31");
        form.setName("John Doe");
        form.setEmail("john@doe.com");
        form.setPub("on");

        RegistrationDto reg = RegistrationDto.of(form);

        assertEquals("2018-12-31", reg.getEventId());
        assertEquals("John Doe", reg.getName());
        assertEquals("john@doe.com", reg.getEmail());
        assertTrue(reg.isPub());
        assertEquals(1546819200L, reg.getTtl().longValue());
    }
}
