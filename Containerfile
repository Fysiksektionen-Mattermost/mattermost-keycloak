FROM quay.io/keycloak/keycloak:latest AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure a database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak
# For production, use a CA-signed TLS certificate whose SAN matches the Keycloak
# hostname, store its private key securely, and provide it via a secret or mounted
# keystore rather than generating a self-signed certificate in the image.
# TODO: Change
RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:latest

COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Database connection settings are supplied at runtime by compose.
ENV KC_DB=postgres
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]