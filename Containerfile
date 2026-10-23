FROM maven:3.9.11-eclipse-temurin-25 AS provider-builder

WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn --batch-mode package

FROM quay.io/keycloak/keycloak:latest@sha256:79c623a22abacf3fe6c00a5fb30f5b7843d717b234033b80995891c06116d244 AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure a database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak
COPY --from=provider-builder /build/target/mattermost-username-provider-1.0.0.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:latest@sha256:79c623a22abacf3fe6c00a5fb30f5b7843d717b234033b80995891c06116d244

COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Database connection settings are supplied at runtime by compose.
ENV KC_DB=postgres
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]