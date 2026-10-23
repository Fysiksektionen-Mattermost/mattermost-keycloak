# Install the Keycloak admin certificate

This directory contains a self-signed certificate for:

- `kc-admin.mattermost.fysiksektionen.se`
- `localhost`
- `127.0.0.1`
- `::1`

Install only `tls/fullchain.pem` (the public certificate). *The private key
`tls/privkey.pem` should only live on the server.*

The certificate is valid only for the names above. If the administrative
hostname is used through an SSH tunnel, add this line to the client
computer's hosts file:

```text
127.0.0.1 kc-admin.mattermost.fysiksektionen.se
```

This affects only `kc-admin.mattermost.fysiksektionen.se`; the public
`mattermost.fysiksektionen.se` hostname continues to resolve normally.

## Linux

### 1. Point the admin hostname to the SSH tunnel

Run this command from this directory:

```sh
printf '%s\n' '127.0.0.1 kc-admin.mattermost.fysiksektionen.se' |
  sudo tee -a /etc/hosts
```

The hosts file is `/etc/hosts` on all common Linux distributions.

### 2. Trust the certificate

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

### 1. Point the admin hostname to the SSH tunnel

Run:

```sh
printf '%s\n' '127.0.0.1 kc-admin.mattermost.fysiksektionen.se' |
  sudo tee -a /etc/hosts
sudo dscacheutil -flushcache
sudo killall -HUP mDNSResponder 2>/dev/null || true
```

### 2. Trust the certificate

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

Open **PowerShell as Administrator** and run:

```powershell
Add-Content -Path "$env:SystemRoot\System32\drivers\etc\hosts" `
  -Value "127.0.0.1 kc-admin.mattermost.fysiksektionen.se"
ipconfig /flushdns
```

Then copy `tls/fullchain.pem` to the Windows computer and, in PowerShell,
run:

```powershell
Import-Certificate `
  -FilePath .\fullchain.pem `
  -CertStoreLocation Cert:\LocalMachine\Root
```

This trusts the certificate for all users on the computer. To trust it only
for the current user, use `Cert:\CurrentUser\Root` instead; that does not
require an elevated PowerShell window.

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
