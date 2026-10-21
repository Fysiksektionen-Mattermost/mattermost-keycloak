package se.fysiksektionen.keycloak;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public final class MattermostUsernameEventListener implements EventListenerProvider {
    private static final String ATTRIBUTE = "mattermostUsername";
    private static final int MAX_LENGTH = 22;

    private final KeycloakSession session;

    public MattermostUsernameEventListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.VERIFY_EMAIL) {
            return;
        }

        RealmModel realm = session.realms().getRealm(event.getRealmId());
        UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null || hasText(user.getFirstAttribute(ATTRIBUTE))) {
            return;
        }

        String base = baseName(user.getFirstName(), user.getLastName());
        user.setSingleAttribute(ATTRIBUTE, availableName(realm, base));
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
    }

    private String availableName(RealmModel realm, String base) {
        for (int suffix = 0; ; suffix++) {
            String candidate = fit(base, suffix);
            try (Stream<UserModel> users = session.users()
                    .searchForUserStream(realm, Map.of(ATTRIBUTE, candidate), 0, 1)) {
                if (users.findAny().isEmpty()) {
                    return candidate;
                }
            }
        }
    }

    private static String baseName(String firstName, String lastName) {
        String first = slug(firstName);
        String last = slug(lastName);
        String base = (first + "." + last).replaceAll("^\\.+|\\.+$", "");
        return base.isEmpty() ? "user" : base;
    }

    private static String slug(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");
    }

    private static String fit(String base, int suffix) {
        String suffixText = suffix == 0 ? "" : Integer.toString(suffix);
        int baseLength = MAX_LENGTH - suffixText.length();
        String shortened = base.substring(0, Math.min(base.length(), baseLength));
        return shortened + suffixText;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public void close() {
    }
}
