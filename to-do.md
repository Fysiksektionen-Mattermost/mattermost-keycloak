# To-do

## Deployment

- [ ] Provision a CA-issued certificate whose SAN includes `localhost`, and
      install it as `tls/fullchain.pem` and `tls/privkey.pem`.
- [ ] Set restrictive ownership and permissions on `tls/privkey.pem`.
- [ ] Set `KEYCLOAK_REALM_BK_FOLDER` in the deployment environment and verify
      the imported realm file is present.
- [ ] Replace all placeholder values in `keycloak-secrets.env` with generated,
      unique secrets.
- [ ] Log in with the bootstrap administrator, create and verify a permanent
      administrator, delete the temporary account, and remove the
      `KC_BOOTSTRAP_ADMIN_*` variables from `keycloak-secrets.env`.
- [ ] Set the `mattermost-keycloak` realm's **Login theme** to
      `mattermostLikeTheme` under **Realm settings → Themes**.
- [ ] Confirm the host firewall does not forward or expose ports `9000` or
      `9001` beyond the local host.

## Operations

- [ ] Configure trusted local monitoring for the health and metrics endpoints
      on port `9000`.
- [ ] Add backup and restore procedures for the PostgreSQL volume and realm
      configuration.
- [ ] Define an on-call procedure for Keycloak, PostgreSQL, nginx, and
      certificate failures.
- [ ] Test certificate renewal and container restart behavior before production.

## Maintenance

- [ ] Establish a scheduled process for reviewing and updating the pinned image
      digests.
- [ ] Re-run Compose, image-build, and nginx syntax validation after updates.
- [ ] Review Keycloak release notes before each version update, including
      deprecated features reported during the image build.
- [ ] Periodically review Keycloak realm settings, administrator accounts,
      enabled identity providers, and session policies.
