package se.fysiksektionen.keycloak;

import java.io.IOException;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.DeclarativeUserProfileProviderFactory;
import org.keycloak.userprofile.UserProfileMetadata;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.config.UPConfigUtils;

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
        applyRealmProfileConfiguration(configured, session);

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
        return "true".equalsIgnoreCase(value);
    }

    private void applyRealmProfileConfiguration(
            UserProfileMetadata configured,
            KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return;
        }

        String rawConfig = realm
                .getComponentsStream(realm.getId(), UserProfileProvider.class.getName())
                .filter(component -> component.get(UP_COMPONENT_CONFIG_KEY) != null)
                .filter(component -> AllowProfileUpdatesUserProfileProviderFactory.ID
                        .equals(component.getProviderId()))
                .map(component -> component.get(UP_COMPONENT_CONFIG_KEY))
                .findFirst()
                .orElse(null);

        if (rawConfig == null) {
            rawConfig = realm
                    .getComponentsStream(realm.getId(), UserProfileProvider.class.getName())
                    .filter(component -> component.get(UP_COMPONENT_CONFIG_KEY) != null)
                    .map(component -> component.get(UP_COMPONENT_CONFIG_KEY))
                    .findFirst()
                    .orElse(null);
        }

        if (rawConfig == null) {
            return;
        }

        try {
            UPConfig profileConfig = UPConfigUtils.parseConfig(rawConfig);
            decorateUserProfileForCache(configured, profileConfig);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot restore the realm user profile configuration",
                    e);
        }
    }
}
