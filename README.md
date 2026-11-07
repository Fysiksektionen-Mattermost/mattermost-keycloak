# Mattermost Keycloak

Rootless Docker deployment of Keycloak with PostgreSQL and an nginx proxy.

## Architecture

- **Keycloak** runs from the custom `Containerfile`.
- **PostgreSQL** stores Keycloak state on the named
  `keycloak-postgres-data` volume. PostgreSQL has no published host port.
- **nginx** runs on the Compose network and proxies user traffic and
  local administrative traffic to Keycloak over the internal `keycloak`
  service name.
- All services use the `keycloak` bridge network.

## Ports and access

| Port | Purpose | Exposure |
| --- | --- | --- |
| `8080` | HTTPS Keycloak user/application endpoint | Host loopback only (made public via redirect from system-wide nginx) |
| `9000` | nginx proxy to Keycloak health and metrics management interface | Host loopback only |
| `9001` | HTTPS admin proxy | Host loopback only |

The user endpoint is `https://kc.mattermost.fysiksektionen.se/`. nginx blocks
the `/admin` and `/metrics` paths on that public listener and uses the
Let’s Encrypt certificate for `kc.mattermost.fysiksektionen.se`. The local
administrative URL is `https://kc-admin.localhost:9001/admin/`; nginx redirects it to
the Keycloak master console at `/admin/master/console/` and uses a separate
local certificate for `kc-admin.localhost` and `localhost`.

## Interact with the admin interface

1.  Tunnel port 9001 to the server if it is not running locally
```
ssl -N -L 9001:127.0.0.1:9001 SSL_SERVER_NAME
```
2. Map the domain `kc-admin.localhost` to localhost by adding the following line to /etc/hosts or similar
```
127.0.0.1   kc-admin.localhost
```
3. Import [./kc-admin_crt/tls/local-ca.crt](./kc-admin_crt/tls/local-ca.crt) to your browser's certificate store. See [./kc-admin_crt/install_kc-admin_crt.md](./kc-admin_crt/install_kc-admin_crt.md) for more ways to do this.
4. Open [https://kc-admin.localhost:9001](https://kc-admin.localhost:9001) in your browser.

## Configuration and startup

1. Copy [`keycloak-secrets.env.example`](./keycloak-secrets.env.example) to
   `keycloak-secrets.env` and replace every placeholder with strong, unique
   values. Set `KC_BOOTSTRAP_ADMIN_USERNAME` and
   `KC_BOOTSTRAP_ADMIN_PASSWORD` for the temporary initial administrator.
   Never commit the secrets file.
2. Generate the local admin certificate:

   ```sh
   ./kc-admin_crt/create-local-certificate.sh
   ```

   The generated certificate is in
   `kc-admin_crt/tls/fullchain.pem` and covers both
   `kc-admin.localhost` and `localhost`.
3. Start the stack:

   ```sh
   docker compose up -d --build
   ```

4. Review service status and logs:

   ```sh
   docker compose ps
   docker compose logs --tail=100
   ```

## Mattermost user provisioning

The image includes Keycloak event listeners that provision the
`mattermostUsername` user attribute when the user completes e-mail
verification, the `id` user attribute when a user is created, and send a
custom introductory e-mail once a user completes their first authentication to
the Mattermost client.

The username listener uses the first available value in this sequence:

```text
firstname.lastname
firstname.lastname1
firstname.lastname2
```

Names are lowercased, accents are removed, and characters outside
Mattermost's username character set are removed. Values are limited to
Mattermost's 22-character username limit. The user ID listener handles
registration and administrative user-creation events, and populates the `id`
user attribute with the first available value in the form
`keycloak########`, where the eight digits are randomly generated. Existing
`mattermostUsername` and `id` values are not changed.

The introductory e-mail listener (`mattermost-introductory-email`) acts on `LOGIN`
events for the `mattermost` client in the `mattermost` realm. It checks the custom
user attribute `introductory-email-sent`. If this attribute is not present (or set
to `false`), it sends the introductory email using Keycloak's configured SMTP
server and records `introductory-email-sent = true` on the user profile. If delivery
fails (e.g. temporary SMTP outage), the error is logged and the attribute is not
marked as sent so delivery can be retried on subsequent logins without blocking
user authentication.

After starting the image, enable the listeners for the relevant realm in the
Keycloak administration console:

1. Open **Realm settings** → **Events** → **Config**.
2. Add `mattermost-username`, `mattermost-user-id`, and
   `mattermost-introductory-email` to **Event listeners** and save.
3. Add `mattermost-user-id` to **Admin event listeners** and save.
4. Leave **Events** → **Listeners** configured for user events. The username
   listener acts on `VERIFY_EMAIL`; the user ID listener acts on registration;
   the introductory e-mail listener acts on client `LOGIN`.
   Administrative user creation is handled through the admin event listener.

Keycloak runs these listeners synchronously in their respective transactions.
The `mattermostUsername` attribute is written during e-mail verification,
the `id` attribute is written during user creation before the transaction is
committed, and the introductory email is sent and recorded on first client login.

### Customizing the introductory e-mail

The introductory e-mail has built-in responsive HTML and plain-text defaults. To
customize it, use **Realm settings** → **Localization** in the admin console and
add overrides for the desired locale using these message keys:

- `mattermostIntroductoryEmailSubject`
- `mattermostIntroductoryEmailText`
- `mattermostIntroductoryEmailHtml`

If no override exists, the built-in template is used.

The subject and bodies support dynamic placeholders in `{placeholder}` or
`${placeholder}` format:

- `{firstName}`: Recipient's first name (falls back to "there").
- `{lastName}`: Recipient's last name.
- `{fullName}`: Recipient's full name.
- `{username}`: Keycloak username.
- `{mattermostUsername}`: Provisioned Mattermost username.
- `{email}`: Recipient's e-mail address.
- `{realm}`: Realm name (`mattermost`).
- `{clientId}`: Client ID (`mattermost`).

The listener is packaged against the Keycloak version declared in
[`pom.xml`](./pom.xml). Update that dependency together with the Keycloak
image when upgrading Keycloak.

### Mattermost e-mail mapper

The [`scripts/mattermost-email-mapper.js`](./scripts/mattermost-email-mapper.js)
Script Mapper is packaged with
[`META-INF/keycloak-scripts.json`](./scripts/META-INF/keycloak-scripts.json)
into `conditional-mapper.jar` and installed as a Keycloak provider. It forwards
the `mattermostEmail` user attribute when it contains a value; otherwise, it
forwards the user's standard `email` attribute. Configure the mapper with:

- **Mapper Type:** `Script Mapper`
- **Script:** contents of `scripts/mattermost-email-mapper.js`
- **Token Claim Name:** `email`
- **Claim JSON Type:** `String`
- **Add to ID token:** enabled
- **Add to access token:** enabled
- **Add to userinfo:** enabled

## Temporary bootstrap administrator

Keycloak reads `KC_BOOTSTRAP_ADMIN_USERNAME` and
`KC_BOOTSTRAP_ADMIN_PASSWORD` from `keycloak-secrets.env` when it starts with
an empty database. These credentials are used to create the initial
administrator and are not a replacement for normal user management.

After logging in through `https://kc-admin.localhost:9001`:

1. Create a permanent administrator with the required minimum privileges.
2. Verify that the permanent account works.
3. Delete the temporary bootstrap account.
4. Remove `KC_BOOTSTRAP_ADMIN_USERNAME` and
   `KC_BOOTSTRAP_ADMIN_PASSWORD` from `keycloak-secrets.env`.

The bootstrap credentials do not reset an existing Keycloak database, and
changing or removing them does not delete an already-created account.

## Security decisions

- **Separated endpoint exposure:** the user endpoint is published on port
  `8080`, while ports `9000` and `9001` are bound to host loopback. nginx
  blocks the administrative `/admin` and `/metrics` paths on the public
  listener.
- **Encrypted traffic:** nginx requires TLS 1.2 or newer and sends HSTS on both
  the public user endpoint (system-wide) and the local admin endpoint. The local admin certificate is mounted at runtime rather than embedded in an image. A local certificate with a domain name ending in `.localhost` was chosen by recommendation of https://letsencrypt.org/docs/certificates-for-localhost/.
- **Trusted proxy handling:** Keycloak uses forwarded headers and nginx passes
  the original host including its non-default port, protocol, client address,
  and forwarding chain.
- **Reduced exposure:** PostgreSQL is internal-only and no database port is
  published.
- **Rootless-compatible hardening:** host networking is avoided; services drop
  all Linux capabilities and disable privilege escalation. nginx starts as
  container root and runs workers as the image's unprivileged `nginx` user.
  nginx receives only the `CHOWN`, `SETGID`, and `SETUID` capabilities required
  to prepare its temporary directories and drop worker privileges. It has a
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
- The local TLS private key should have restrictive host filesystem
  permissions.
- Health and metrics endpoints are intentionally available to host-local
  monitoring. Monitoring access should be limited to trusted local processes.
- Image digest updates must be reviewed and tested before changing the pinned
  values.
- There is no internal backup and restore procedure for the PostgreSQL volume
  or the realm configuration, consider adding this.