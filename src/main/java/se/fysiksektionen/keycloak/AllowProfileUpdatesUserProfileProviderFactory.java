package se.fysiksektionen.keycloak;

import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.DeclarativeUserProfileProviderFactory;
import org.keycloak.models.KeycloakSession;

public final class AllowProfileUpdatesUserProfileProviderFactory
        extends DeclarativeUserProfileProviderFactory {
    public static final String ID = "allow-profile-updates";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public DeclarativeUserProfileProvider create(KeycloakSession session) {
        return new AllowProfileUpdatesUserProfileProvider(session, this);
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY + 1;
    }
}
