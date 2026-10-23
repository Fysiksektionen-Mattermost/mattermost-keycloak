# Install the Keycloak admin certificate

This directory contains a self-signed certificate for:

- `kc-admin.mattermost.fysiksektionen.se`
- `localhost`
- `127.0.0.1`
- `::1`

Install only `tls/local-ca.crt` (the local CA certificate). The server
certificate in `tls/fullchain.pem` contains the server certificate and the CA chain for
nginx. *Never copy `local-ca-key.pem` or `privkey.pem` to an administrator's
computer.*

The certificate is valid only for the names above. If the administrative
hostname is used through an SSH tunnel, resolve it to the local tunnel on
the client machine, for example:

```text
127.0.0.1 kc-admin.mattermost.fysiksektionen.se
```

## Firefox

1. Resolve kc-admin.mattermost.fysiksektionen.se as localhost according to the OS-specific instructions.
2. Import local-ca.crt to Firefox manually.

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
sudo install -D -m 0644 tls/local-ca.crt \
  /etc/pki/ca-trust/source/anchors/kc-admin.mattermost.fysiksektionen.se.crt
sudo update-ca-trust
```

## macOS

Copy `tls/local-ca.crt` to the Mac, then run:

```sh
sudo security add-trusted-cert \
  -d \
  -r trustRoot \
  -k /Library/Keychains/System.keychain \
  /path/to/local-ca.crt
```

Alternatively, open **Keychain Access**, import `local-ca.crt` into the
**System** keychain, open the certificate, expand **Trust**, and select
**Always Trust**. Restart browsers after changing trust settings.

## Windows

Copy `tls/local-ca.crt` to the Windows computer. In PowerShell, run:

```powershell
Import-Certificate `
  -FilePath .\local-ca.crt `
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
  -subject -issuer -dates -ext subjectAltName -ext basicConstraints

The certificate to trust is the CA certificate:

```sh
openssl x509 -in tls/local-ca.crt -noout \
  -subject -issuer -dates -ext basicConstraints -ext keyUsage
```
```

With the SSH tunnel active:

```sh
ssh -N -L 9001:127.0.0.1:9001 user@server
```

open `https://kc-admin.mattermost.fysiksektionen.se:9001/admin/` or
`https://localhost:9001/admin/`.

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