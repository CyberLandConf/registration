package de.jugda.registration.domain;

import de.jugda.registration.model.RegistrationDto;
import de.jugda.registration.model.RegistrationForm;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "registration")
@Getter @Setter
@ToString
public class Registration extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @TenantId
    @Column(name = "tenant")
    private String tenant;
    private String eventId;
    private String name;
    private String email;
    private boolean pub;
    private boolean waitlist;
    private boolean privacy;
    private boolean videoRecording;
    private boolean remote;
    private LocalDateTime created;
    /** When the participant mail for the current state was successfully sent; null = still owed one. */
    private LocalDateTime confirmationSentAt;
    private Long ttl;

    public static Registration of(RegistrationForm form, boolean waitlist) {
        Registration registration = new Registration();
        registration.setEventId(form.getEventId());
        registration.setName(form.getName().trim());
        registration.setEmail(form.getEmail().trim().toLowerCase());
        registration.setPrivacy(onOrOff(form.getPrivacy()));
        registration.setVideoRecording(onOrOff(form.getVideoRecording()));
        registration.setPub(onOrOff(form.getPub()));
        registration.setRemote(onOrOff(form.getRemote()));
        registration.setWaitlist(waitlist);
        registration.setTtl(LocalDate.parse(form.getEventId()).plusWeeks(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC));
        registration.setCreated(LocalDateTime.now());
        return registration;
    }

    private static boolean onOrOff(String s) {
        return (s == null || s.isBlank() ? "off" : s).equalsIgnoreCase("on");
    }

    public RegistrationDto toDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setId(this.id.toString());
        dto.setName(this.name);
        dto.setEmail(this.email);
        dto.setEventId(this.eventId);
        dto.setWaitlist(this.waitlist);
        dto.setPrivacy(this.privacy);
        dto.setVideoRecording(this.videoRecording);
        dto.setRemote(this.remote);
        dto.setCreated(this.created);
        dto.setConfirmationSentAt(this.confirmationSentAt);
        dto.setTtl(this.ttl);
        return dto;
    }

    public void updateFrom(RegistrationForm form) {
        this.name = form.getName().trim();
        this.email = form.getEmail().trim().toLowerCase();
        this.pub = onOrOff(form.getPub());
        this.privacy = onOrOff(form.getPrivacy());
        this.videoRecording = onOrOff(form.getVideoRecording());
        this.remote = onOrOff(form.getRemote());
    }
}
