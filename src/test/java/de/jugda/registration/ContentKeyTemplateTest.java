package de.jugda.registration;

import de.jugda.registration.domain.ContentKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ContentKey claims to be the single source of truth for the help texts, but nothing links it to the
 * {@code helptext["..."]} lookups in the templates -- both sides are plain strings in different files.
 * Drift would show up in production as a silently empty spot on a page, or as a key the orga team can
 * edit on the "Texte" form without it appearing anywhere. This test is that link.
 */
class ContentKeyTemplateTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Pattern HELPTEXT_LOOKUP = Pattern.compile("helptext\\[\"([^\"]+)\"]");

    @Test
    void everyHelptextKeyInATemplateIsMaintainableAndViceVersa() throws IOException {
        Set<String> maintainable = Arrays.stream(ContentKey.values())
            .map(ContentKey::getKey)
            .collect(Collectors.toSet());

        Set<String> rendered;
        try (Stream<Path> templates = Files.walk(TEMPLATES)) {
            rendered = templates.filter(Files::isRegularFile)
                .flatMap(ContentKeyTemplateTest::helptextKeys)
                .collect(Collectors.toSet());
        }

        assertThat(rendered).as("templates below %s using helptext[...]", TEMPLATES).isNotEmpty();
        assertThat(rendered)
            .as("keys read by a template but missing from ContentKey -- nobody can fill these")
            .isSubsetOf(maintainable);
        assertThat(maintainable)
            .as("keys offered on the admin form but rendered by no template -- editing them does nothing")
            .isSubsetOf(rendered);
    }

    private static Stream<String> helptextKeys(Path template) {
        try {
            Matcher matcher = HELPTEXT_LOOKUP.matcher(Files.readString(template));
            return matcher.results().map(result -> result.group(1)).toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + template, e);
        }
    }
}
