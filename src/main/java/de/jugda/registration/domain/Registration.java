package de.jugda.registration.domain;

import de.jugda.registration.model.RegistrationDto;
import de.jugda.registration.model.RegistrationForm;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Entity
public class Registration {
    @Id
    @GeneratedValue
    private String id;
    private String eventId;
    private String name;
    private String email;
    private boolean pub;
    private boolean waitlist;
    private boolean privacy;
    private boolean videoRecording;
    private boolean remote;
    private LocalDateTime created;
    private Long ttl;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isPub() {
        return pub;
    }

    public void setPub(boolean pub) {
        this.pub = pub;
    }

    public boolean isWaitlist() {
        return waitlist;
    }

    public void setWaitlist(boolean waitlist) {
        this.waitlist = waitlist;
    }

    public boolean isPrivacy() {
        return privacy;
    }

    public void setPrivacy(boolean privacy) {
        this.privacy = privacy;
    }

    public boolean isVideoRecording() {
        return videoRecording;
    }

    public void setVideoRecording(boolean videoRecording) {
        this.videoRecording = videoRecording;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public static Registration of(RegistrationForm form) {
        Registration registration = new Registration();
        registration.setEventId(form.getEventId());
        registration.setName(form.getName().trim());
        registration.setEmail(form.getEmail().trim().toLowerCase());
        registration.setPrivacy(onOrOff(form.getPrivacy()));
        registration.setVideoRecording(onOrOff(form.getVideoRecording()));
        registration.setPub(onOrOff(form.getPub()));
        registration.setRemote(onOrOff(form.getRemote()));
        registration.setWaitlist(form.isWaitlist());
        registration.setTtl(LocalDate.parse(form.getEventId()).plusWeeks(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC));
        registration.setCreated(LocalDateTime.now());
        return registration;
    }

    private static boolean onOrOff(String s) {
        return (s == null || s.isBlank() ? "off" : s).equalsIgnoreCase("on");
    }

    public RegistrationDto toDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setId(this.id);
        dto.setName(this.name);
        dto.setEmail(this.email);
        dto.setEventId(this.eventId);
        dto.setPrivacy(this.privacy);
        dto.setVideoRecording(this.videoRecording);
        dto.setRemote(this.remote);
        dto.setCreated(this.created);
        dto.setTtl(this.ttl);
        return dto;
    }
}
