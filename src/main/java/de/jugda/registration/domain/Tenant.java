package de.jugda.registration.domain;

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

@Entity
@Table(name = "tenant")
@Getter @Setter
@ToString
public class Tenant extends PanacheEntityBase {
    @Id
    @TenantId
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private String website;
    private String privacy;
    private String imprint;
    private String logo;
    @Column(name = "reply_to")
    private String replyTo;
    private String events;
    /** Optional, tenant-owned stylesheet for the participant pages, see {@link de.jugda.registration.TenantStyle}. */
    @Column(columnDefinition = "text")
    private String css;
}
