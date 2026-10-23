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

## Remove the certificate

Remove only this certificate and the hosts-file entry. Do not remove other
certificates from the system trust store.

### Debian or Ubuntu

```sh
sudo rm -f \
  /usr/local/share/ca-certificates/kc-admin.mattermost.fysiksektionen.se.crt
sudo update-ca-certificates
sudo sed -i '\|^[[:space:]]*127\.0\.0\.1[[:space:]]\+kc-admin\.mattermost\.fysiksektionen\.se[[:space:]]*$|d' \
  /etc/hosts
```

### Fedora, RHEL, or CentOS

```sh
sudo rm -f \
  /etc/pki/ca-trust/source/anchors/kc-admin.mattermost.fysiksektionen.se.crt
sudo update-ca-trust
sudo sed -i '\|^[[:space:]]*127\.0\.0\.1[[:space:]]\+kc-admin\.mattermost\.fysiksektionen\.se[[:space:]]*$|d' \
  /etc/hosts
```

### macOS

Remove the certificate from the System keychain using its SHA-1 fingerprint:

```sh
security find-certificate -a -c kc-admin.mattermost.fysiksektionen.se \
  /Library/Keychains/System.keychain
sudo security delete-certificate -c kc-admin.mattermost.fysiksektionen.se \
  /Library/Keychains/System.keychain
sudo sed -i '' \
  '/^[[:space:]]*127\.0\.0\.1[[:space:]]*kc-admin\.mattermost\.fysiksektionen\.se[[:space:]]*$/d' \
  /etc/hosts
sudo dscacheutil -flushcache
sudo killall -HUP mDNSResponder 2>/dev/null || true
```

If the certificate was installed using **Keychain Access**, delete the
matching certificate from the **System** keychain there instead.

### Windows

Open **PowerShell as Administrator** and remove the certificate by its
thumbprint. First inspect matching certificates:

```powershell
Get-ChildItem Cert:\LocalMachine\Root |
  Where-Object Subject -Like '*CN=kc-admin.mattermost.fysiksektionen.se*' |
  Format-List Subject, Thumbprint
```

After checking the displayed thumbprint, replace `<THUMBPRINT>` and run:

```powershell
Remove-Item "Cert:\LocalMachine\Root\<THUMBPRINT>"
```

If it was installed for only the current user, use
`Cert:\CurrentUser\Root` instead. Remove the hosts-file entry and flush DNS:

```powershell
$hosts = "$env:SystemRoot\System32\drivers\etc\hosts"
(Get-Content $hosts) |
  Where-Object { $_ -notmatch '^\s*127\.0\.0\.1\s+kc-admin\.mattermost\.fysiksektionen\.se\s*$' } |
  Set-Content $hosts
ipconfig /flushdns
```

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
