package se.fysiksektionen.keycloak;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public final class MattermostUsernameEventListenerFactory implements EventListenerProviderFactory {
    public static final String ID = "mattermost-username";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new MattermostUsernameEventListener(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }
}
