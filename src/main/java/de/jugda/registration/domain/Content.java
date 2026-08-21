package de.jugda.registration.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TenantId;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "content")
@IdClass(Content.ContentId.class)
public class Content extends PanacheEntityBase {
    @Id
    @TenantId
    @Column(nullable = false)
    public String tenant;
    @Id
    @Column(name = "`key`", nullable = false)
    public String key;
    @Column(name = "`value`", nullable = false, columnDefinition = "text")
    public String value;

    /**
     * The tenant has to be set by hand here: it is part of the composite id, so Hibernate's {@code @TenantId}
     * handling cannot fill it in on insert the way it does for a plain discriminator column.
     */
    public static Content of(String tenant, String key, String value) {
        Content content = new Content();
        content.tenant = tenant;
        content.key = key;
        content.value = value;
        return content;
    }

    public static Map<String, String> asMap() {
        return Content.<Content>streamAll()
            .collect(Collectors.toMap(c -> c.key, c -> c.value));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentId implements Serializable {
        public String tenant;
        public String key;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ContentId that)) return false;
            return Objects.equals(tenant, that.tenant) && Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenant, key);
        }
    }
}
