#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
certificate=${1:-"$script_dir/tls/fullchain.pem"}
certificate_name=kc-admin.mattermost.fysiksektionen.se.crt
trust_directory=/usr/local/share/ca-certificates
trusted_certificate="$trust_directory/$certificate_name"

if [ ! -f "$certificate" ]; then
  printf >&2 'Certificate not found: %s\n' "$certificate"
  exit 1
fi

if [ "$(id -u)" -ne 0 ]; then
  printf >&2 'Run this script as root, for example: sudo %s\n' "$0"
  exit 1
fi

if ! command -v update-ca-certificates >/dev/null 2>&1; then
  printf >&2 '%s is only supported on Debian-family Linux distributions.\n' "$0"
  printf >&2 'See install_kc-admin_crt.md for other Linux distributions.\n'
  exit 1
fi

install -D -m 0644 "$certificate" "$trusted_certificate"
update-ca-certificates

printf 'Trusted certificate installed at %s\n' "$trusted_certificate"
