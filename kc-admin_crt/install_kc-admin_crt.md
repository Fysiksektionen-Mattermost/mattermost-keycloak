# Install the Keycloak admin certificate

This directory contains a self-signed certificate for:

- `kc-admin.localhost`
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
127.0.0.1 kc-admin.localhost
```

> **NOTE:**
> Make sure you use the remote local-ca.crt (it is also duplicated in bk_admin_crt)
> and not a local one that you have generated yourself.
> 
> You can run:
> ```bash
> git update-index --skip-worktree ...
> ```
> to avoid accidentally pushing overwrites to the remote, but this
> does not help against you installing the wrong local-ca on your system.

## Recommended import: Browsers

*Firefox has its own certificate store so it requires manual import.*

1. Resolve kc-admin.localhost as localhost.
2. Import local-ca.crt to the browser manually.

# Other ways of importing the certificate.

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

## Removing the certificate

Remove only this certificate and the hosts-file entry. Do not remove other
certificates from the system trust store.

### Debian or Ubuntu

```sh
sudo rm -f \
  /usr/local/share/ca-certificates/kc-admin.localhost.crt
sudo update-ca-certificates
```

### Fedora, RHEL, or CentOS

```sh
sudo rm -f \
  /etc/pki/ca-trust/source/anchors/kc-admin.localhost.crt
sudo update-ca-trust
```

### macOS

Remove the certificate from the System keychain using its SHA-1 fingerprint:

```sh
security find-certificate -a -c kc-admin.localhost \
  /Library/Keychains/System.keychain
sudo security delete-certificate -c kc-admin.localhost \
  /Library/Keychains/System.keychain
```

If the certificate was installed using **Keychain Access**, delete the
matching certificate from the **System** keychain there instead.

### Windows

Open **PowerShell as Administrator** and remove the certificate by its
thumbprint. First inspect matching certificates:

```powershell
Get-ChildItem Cert:\LocalMachine\Root |
  Where-Object Subject -Like '*CN=kc-admin.localhost*' |
  Format-List Subject, Thumbprint
```

After checking the displayed thumbprint, replace `<THUMBPRINT>` and run:

```powershell
Remove-Item "Cert:\LocalMachine\Root\<THUMBPRINT>"
```

If it was installed for only the current user, use
`Cert:\CurrentUser\Root` instead.