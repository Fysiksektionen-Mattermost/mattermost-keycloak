# Install the Keycloak admin certificate

This directory contains a self-signed certificate for:

- `kc-admin.mattermost.fysiksektionen.se`
- `localhost`
- `127.0.0.1`
- `::1`

Install only `tls/fullchain.pem` (the public certificate). *The private key
`tls/privkey.pem` should only live on the server.*

The certificate is valid only for the names above. If the administrative
hostname is used through an SSH tunnel, resolve it to the local tunnel on
the client machine, for example:

```text
127.0.0.1 kc-admin.mattermost.fysiksektionen.se
```

## Linux

From this directory, on Debian or Ubuntu:

```sh
sudo ./trust-local-certificate.sh
```

The script installs the certificate into
`/usr/local/share/ca-certificates/` and runs `update-ca-certificates`.
Restart browsers that were open during installation.

On Fedora, RHEL, CentOS, or another `update-ca-trust` distribution:

```sh
sudo install -D -m 0644 tls/fullchain.pem \
  /etc/pki/ca-trust/source/anchors/kc-admin.mattermost.fysiksektionen.se.crt
sudo update-ca-trust
```

## macOS

Copy `tls/fullchain.pem` to the Mac, then run:

```sh
sudo security add-trusted-cert \
  -d \
  -r trustRoot \
  -k /Library/Keychains/System.keychain \
  /path/to/fullchain.pem
```

Alternatively, open **Keychain Access**, import `fullchain.pem` into the
**System** keychain, open the certificate, expand **Trust**, and select
**Always Trust**. Restart browsers after changing trust settings.

## Windows

Copy `tls/fullchain.pem` to the Windows computer. In PowerShell, run:

```powershell
Import-Certificate `
  -FilePath .\fullchain.pem `
  -CertStoreLocation Cert:\CurrentUser\Root
```

This trusts the certificate for the current user.

Restart browsers after importing the certificate. Some browsers, including
Firefox in certain configurations, use their own certificate store; import
the certificate into that browser's authorities when necessary.

## Verify the certificate

Inspect the names before installing it:

```sh
openssl x509 -in tls/fullchain.pem -noout \
  -subject -issuer -dates -ext subjectAltName
```

With the SSH tunnel active:

```sh
ssh -N -L 9001:127.0.0.1:9001 user@server
```

open `https://kc-admin.mattermost.fysiksektionen.se:9001/admin/` or
`https://localhost:9001/admin/`.
