# Mattermost Keycloak

Rootless Docker deployment of Keycloak with PostgreSQL and a local-only nginx
admin proxy.

## Architecture

- **Keycloak** runs from the custom `Containerfile`.
- **PostgreSQL** stores Keycloak state on the named
  `keycloak-postgres-data` volume. PostgreSQL has no published host port.
- **nginx** runs on the Compose network and proxies to Keycloak over the
  internal `keycloak` service name.
- All services use the `keycloak` bridge network.

## Ports and access

| Port | Purpose | Exposure |
| --- | --- | --- |
| `8080` | Keycloak application | Host loopback only |
| `9000` | Keycloak health and metrics management interface | Host loopback only |
| `9001` | HTTPS admin proxy | Host loopback only; nginx also allows only `127.0.0.1` and `::1` |

The local URL is `https://localhost:9001/admin/`; nginx redirects it to the
Keycloak master console at `/admin/master/console/`. Keycloak and nginx use the
same HTTPS localhost origin so the admin console's browser security checks work
correctly. The certificate mounted from `tls/` must include `localhost` in its
subject alternative names.

## Configuration and startup

1. Copy [`keycloak-secrets.env.example`](./keycloak-secrets.env.example) to
   `keycloak-secrets.env` and replace every placeholder with strong, unique
   values. Set `KC_BOOTSTRAP_ADMIN_USERNAME` and
   `KC_BOOTSTRAP_ADMIN_PASSWORD` for the temporary initial administrator.
   Never commit the secrets file.
2. Place the trusted certificate and private key at:
   - `tls/fullchain.pem`
   - `tls/privkey.pem`
3. Start the stack:

   ```sh
   docker compose up -d --build
   ```

4. Review service status and logs:

   ```sh
   docker compose ps
   docker compose logs --tail=100 keycloak nginx
   ```

## Temporary bootstrap administrator

Keycloak reads `KC_BOOTSTRAP_ADMIN_USERNAME` and
`KC_BOOTSTRAP_ADMIN_PASSWORD` from `keycloak-secrets.env` when it starts with
an empty database. These credentials are used to create the initial
administrator and are not a replacement for normal user management.

After logging in through `https://localhost:9001`:

1. Create a permanent administrator with the required minimum privileges.
2. Verify that the permanent account works.
3. Delete the temporary bootstrap account.
4. Remove `KC_BOOTSTRAP_ADMIN_USERNAME` and
   `KC_BOOTSTRAP_ADMIN_PASSWORD` from `keycloak-secrets.env`.

The bootstrap credentials do not reset an existing Keycloak database, and
changing or removing them does not delete an already-created account.

## Optional realm import

Realm import is deliberately disabled during normal startup. This prevents a
routine container restart from treating the bootstrap JSON as an import
operation against the live database. The import profile uses Keycloak's
one-shot `import` command and exits after the operation.

To explicitly import the realm, set `KEYCLOAK_REALM_BK_FOLDER` to a directory
containing `mattermost-keycloak-realm-0.json`, then run:

```sh
docker compose --profile import run --rm keycloak-import
```

The import service is a one-shot operation and is not started by the default
Compose profile.

## Executive security decisions

- **Local-only administrative access:** ports `9000` and `9001` are bound to
  host loopback. The nginx container is not directly published on any
  non-loopback host address.
- **Encrypted administrative traffic:** nginx requires TLS 1.2 or newer and
  sends HSTS. Certificates and private keys are mounted at runtime rather than
  embedded in an image.
- **Trusted proxy handling:** Keycloak uses forwarded headers and nginx passes
  the original host including its non-default port, protocol, client address,
  and forwarding chain.
- **Reduced exposure:** PostgreSQL is internal-only and no database port is
  published.
- **Rootless-compatible hardening:** host networking is avoided; services drop
  all Linux capabilities and disable privilege escalation. nginx has a
  read-only filesystem with only required temporary filesystems writable.
- **Resource containment:** Keycloak has process and memory limits.
- **Reproducible supply chain:** PostgreSQL, nginx, and Keycloak images are
  pinned to immutable registry digests.
- **No baked-in development certificate:** the previous self-signed keystore
  generation was removed from the Keycloak image build.

## Security boundaries and remaining assumptions

- Loopback binding prevents remote access to the admin and management ports
  only when the host's network and firewall configuration are not forwarding
  those ports elsewhere.
- The TLS private key must be readable by the rootless Docker user but should
  have restrictive host filesystem permissions.
- Health and metrics endpoints are intentionally available to host-local
  monitoring. Monitoring access should be limited to trusted local processes.
- Image digest updates must be reviewed and tested before changing the pinned
  values.

## Validation

The current configuration has been validated with:

```sh
KEYCLOAK_REALM_BK_FOLDER=/tmp docker compose config
docker build --file Containerfile .
docker run ... nginx:1.27-alpine nginx -t
```

The nginx check requires certificate files to be mounted into the validation
container.
