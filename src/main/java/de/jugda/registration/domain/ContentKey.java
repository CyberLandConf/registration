package de.jugda.registration.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The help texts the templates read -- and therefore exactly the keys that are editable in the admin UI.
 * A key no template renders would only be clutter for the orga team; the other way round,
 * {@link de.jugda.registration.service.ContentService} creates missing rows on save, so a JUG never ends
 * up with an unresolvable {@code helptext[...]}.
 * <p>
 * Labels and hints are the German UI texts shown on the admin form.
 */
@Getter
@RequiredArgsConstructor
public enum ContentKey {

    REGISTRATION_NAME("registration.name", "Hinweis: Name",
        "Steht als Hilfetext unter dem Feld „Name“ im Anmeldeformular."),
    REGISTRATION_EMAIL("registration.email", "Hinweis: E-Mail",
        "Steht als Hilfetext unter dem Feld „E-Mail“ im Anmeldeformular."),
    REGISTRATION_VIDEO("registration.video", "Hinweis: Videoaufzeichnung",
        "Erläuterung zur Aufzeichnung, direkt an der Videoaufzeichnungs-Checkbox."),
    REGISTRATION_DISCLAIMER("registration.disclaimer", "Datenschutzhinweis",
        "Text über der Einwilligungs-Checkbox am Ende des Anmeldeformulars."),
    REGISTRATION_WAITLIST("registration.waitlist", "Hinweis: Warteliste",
        "Erscheint statt der normalen Einleitung, sobald das Event ausgebucht ist."),
    WEBINAR_TOOLS("webinar.tools", "Hinweis: Konferenzwerkzeuge",
        "Steht auf der Webinar-Seite unter dem Meeting-Link.");

    private final String key;
    private final String label;
    private final String hint;
}
