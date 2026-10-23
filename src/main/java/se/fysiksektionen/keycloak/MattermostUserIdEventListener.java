package se.fysiksektionen.keycloak;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public final class MattermostUserIdEventListener implements EventListenerProvider {
    private static final String ID_ATTRIBUTE = "id";
    private static final int ID_NUMBER_BOUND = 100_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KeycloakSession session;

    public MattermostUserIdEventListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.REGISTER || event.getUserId() == null) {
            return;
        }

        assignId(event.getRealmId(), event.getUserId());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (event.getOperationType() != OperationType.CREATE
                || event.getResourceType() != ResourceType.USER) {
            return;
        }

        String userId = userIdFromResourcePath(event.getResourcePath());
        if (userId != null) {
            assignId(event.getRealmId(), userId);
        }
    }

    private void assignId(String realmId, String userId) {
        RealmModel realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return;
        }

        UserModel user = session.users().getUserById(realm, userId);
        if (user != null && !hasText(user.getFirstAttribute(ID_ATTRIBUTE))) {
            user.setSingleAttribute(ID_ATTRIBUTE, availableId(realm));
        }
    }

    private String availableId(RealmModel realm) {
        for (;;) {
            String candidate = "keycloak" + String.format(Locale.ROOT, "%08d",
                    RANDOM.nextInt(ID_NUMBER_BOUND));
            try (Stream<UserModel> users = session.users()
                    .searchForUserStream(realm, Map.of(ID_ATTRIBUTE, candidate), 0, 1)) {
                if (users.findAny().isEmpty()) {
                    return candidate;
                }
            }
        }
    }

    private static String userIdFromResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        int separator = resourcePath.lastIndexOf('/');
        String userId = resourcePath.substring(separator + 1);
        return hasText(userId) ? userId : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public void close() {
    }
}
