package de.jugda.registration.model;

import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * One editable help text together with its labelling for the admin form.
 */
@TemplateData
@RegisterForReflection
public record ContentDto(String key, String label, String hint, String value) {
}
