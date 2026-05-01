package de.jugda.registration.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@Data
@TemplateData
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto {
    public String eventId;
    public String uid;
    public String summary;
    public String title;
    public String description;
    public String speaker;
    public String twitter;
    public String location;
    public String url;
    public String webinarLink;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime start;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime end;
    public String timezone;

    public String startDate() {
        return start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public String startTime() {
        return start.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getStartDateTimeBasic() {
        return start.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm"));
    }

    public String getEndDateTimeBasic() {
        return end.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm"));
    }
}
