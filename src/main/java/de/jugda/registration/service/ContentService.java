package de.jugda.registration.service;

import de.jugda.registration.TenantContext;
import de.jugda.registration.domain.Content;
import de.jugda.registration.domain.ContentKey;
import de.jugda.registration.model.ContentDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContentService {

    @Inject
    TenantContext tenantCtx;

    /**
     * All editable texts of the current tenant, including those not stored yet -- the form shows those as an
     * empty field instead of hiding them.
     */
    public List<ContentDto> allTexts() {
        Map<String, String> current = Content.asMap();
        return Arrays.stream(ContentKey.values())
            .map(key -> new ContentDto(key.getKey(), key.getLabel(), key.getHint(),
                current.getOrDefault(key.getKey(), "")))
            .toList();
    }

    /**
     * Applies the submitted form values. Keys the form does not send stay untouched; unknown keys are
     * ignored, so a tampered form cannot create rows nothing ever reads.
     */
    @Transactional
    public void save(MultivaluedMap<String, String> form) {
        Map<String, Content> existing = Content.<Content>streamAll()
            .collect(Collectors.toMap(content -> content.key, content -> content));

        for (ContentKey key : ContentKey.values()) {
            String value = form.getFirst(key.getKey());
            if (value == null) {
                continue; // key not part of this submit -> leave it alone
            }
            Content content = existing.get(key.getKey());
            if (content != null) {
                content.value = value.strip();
            } else {
                Content.of(tenantCtx.getTenantId(), key.getKey(), value.strip()).persist();
            }
        }
    }
}
