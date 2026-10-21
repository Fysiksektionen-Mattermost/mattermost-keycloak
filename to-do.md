# To-do

## Deployment

- [ ] Provision a CA-issued certificate whose SAN includes `localhost`, and
      install it at
      `/etc/letsencrypt/live/mattermost.fysiksektionen.se/fullchain.pem` and
      `/etc/letsencrypt/live/mattermost.fysiksektionen.se/privkey.pem`.
- [ ] Set restrictive ownership and permissions on the Let’s Encrypt private
      key.
- [ ] Replace all placeholder values in `keycloak-secrets.env` with generated,
      unique secrets.
- [ ] Log in with the bootstrap administrator, create and verify a permanent
      administrator, delete the temporary account, and remove the
      `KC_BOOTSTRAP_ADMIN_*` variables from `keycloak-secrets.env`.
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
