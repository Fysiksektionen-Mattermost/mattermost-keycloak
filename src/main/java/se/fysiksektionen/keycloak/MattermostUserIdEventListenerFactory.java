package se.fysiksektionen.keycloak;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public final class MattermostUserIdEventListenerFactory implements EventListenerProviderFactory {
    public static final String ID = "mattermost-user-id";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new MattermostUserIdEventListener(session);
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
