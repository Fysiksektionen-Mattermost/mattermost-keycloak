FROM maven:3.9.9-eclipse-temurin-17 AS provider-builder

WORKDIR /build
COPY pom.xml .
COPY src src
COPY scripts scripts
RUN mvn --batch-mode package

FROM quay.io/keycloak/keycloak:latest@sha256:79c623a22abacf3fe6c00a5fb30f5b7843d717b234033b80995891c06116d244 AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure a database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak
ADD --chown=keycloak:keycloak https://github.com/rfyuz/mattermost-keycloakify-theme/releases/download/latest/mattermost-keycloak-login-theme-from-shadcn.jar /opt/keycloak/providers/
# Unused: ADD --chown=keycloak:keycloak https://github.com/rfyuz/mattermost-keycloakify-theme/releases/download/latest/mattermost-keycloak-email-theme.jar /opt/keycloak/providers/
COPY --from=provider-builder /build/target/mattermost-username-id-introemail-providers-1.0.0.jar /opt/keycloak/providers/
COPY --from=provider-builder /build/target/mattermost-email-mapper.jar /opt/keycloak/providers/
ADD --chown=keycloak:keycloak --checksum=sha256:190752f2db820174013466deb728dadd3bb2e260bc4126dbeb5973604c0ba267 https://repo1.maven.org/maven2/fr/cnieg/keycloak/attribute-login-provider/1.11.37/attribute-login-provider-1.11.37.jar /opt/keycloak/providers/attribute-login-provider-1.11.37.jar
# The preview feature is necessary to allow the mattermost email mapper script.
RUN /opt/keycloak/bin/kc.sh build --features=preview,scripts:v1

FROM quay.io/keycloak/keycloak:latest@sha256:79c623a22abacf3fe6c00a5fb30f5b7843d717b234033b80995891c06116d244

COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Database connection settings are supplied at runtime by compose.
ENV KC_DB=postgres
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]