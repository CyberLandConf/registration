package de.jugda.registration.domain;

import de.jugda.registration.model.EventDto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Event {

    @Id
    @GeneratedValue
    private String uid;
    private String eventId;
    private String summary;
    private String title;
    private String description;
    private String speaker;
    private String twitter;
    private String location;
    private String url;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String timezone;

    public static Event fromDto(EventDto eventDto) {
        Event event = new  Event();
        event.eventId = eventDto.getEventId();
        event.summary = eventDto.getSummary();
        event.title = eventDto.getTitle();
        event.description = eventDto.getDescription();
        event.speaker = eventDto.getSpeaker();
        event.twitter = eventDto.getTwitter();
        event.location = eventDto.getLocation();
        event.url = eventDto.getUrl();
        event.startDate = eventDto.getStart();
        event.endDate = eventDto.getEnd();
        return event;
    }

    public String getUid() {
        return uid;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }


    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime start) {
        this.startDate = start;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime end) {
        this.endDate = end;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Event event)) return false;
        return Objects.equals(uid, event.uid) && Objects.equals(summary, event.summary) && Objects.equals(title, event.title) && Objects.equals(description, event.description) && Objects.equals(speaker, event.speaker) && Objects.equals(twitter, event.twitter) && Objects.equals(location, event.location) && Objects.equals(url, event.url) && Objects.equals(startDate, event.startDate) && Objects.equals(endDate, event.endDate) && Objects.equals(timezone, event.timezone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, summary, title, description, speaker, twitter, location, url, startDate, endDate, timezone);
    }

    @Override
    public String toString() {
        return "Event{" +
            "uid='" + uid + '\'' +
            ", summary='" + summary + '\'' +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", speaker='" + speaker + '\'' +
            ", twitter='" + twitter + '\'' +
            ", location='" + location + '\'' +
            ", url='" + url + '\'' +
            ", start=" + startDate +
            ", end=" + endDate +
            ", timezone='" + timezone + '\'' +
            '}';
    }

    public EventDto toDto() {
        EventDto eventDto = new EventDto();
        eventDto.setEventId(uid);
        eventDto.setSummary(summary);
        eventDto.setTitle(title);
        eventDto.setDescription(description);
        eventDto.setSpeaker(speaker);
        eventDto.setTwitter(twitter);
        eventDto.setLocation(location);
        eventDto.setUrl(url);
        eventDto.setStart(startDate);
        eventDto.setEnd(endDate);
        return eventDto;
    }
}
