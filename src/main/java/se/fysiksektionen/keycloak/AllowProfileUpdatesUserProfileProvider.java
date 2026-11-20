package se.fysiksektionen.keycloak;

import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.DeclarativeUserProfileProviderFactory;
import org.keycloak.userprofile.UserProfileMetadata;

public final class AllowProfileUpdatesUserProfileProvider extends DeclarativeUserProfileProvider {
    static final String ALLOW_PROFILE_UPDATES_ATTRIBUTE = "allowProfileUpdates";

    public AllowProfileUpdatesUserProfileProvider(
            KeycloakSession session,
            DeclarativeUserProfileProviderFactory factory) {
        super(session, factory);
    }

    @Override
    protected UserProfileMetadata configureUserProfile(
            UserProfileMetadata metadata,
            KeycloakSession session) {
        UserProfileMetadata configured = super.configureUserProfile(metadata, session);

        if (configured.getContext().isAdminContext()) {
            return configured;
        }

        for (String attributeName : List.of(
                UserModel.FIRST_NAME,
                UserModel.LAST_NAME)) {
            for (AttributeMetadata attribute : configured.getAttribute(attributeName)) {
                attribute.addWriteCondition(context ->
                        !isProfileUpdateLocked(context));
            }
        }

        return configured;
    }

    private boolean isProfileUpdateLocked(AttributeContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            return false;
        }

        String value = user.getFirstAttribute(ALLOW_PROFILE_UPDATES_ATTRIBUTE);
        return "False".equalsIgnoreCase(value);
    }
}
