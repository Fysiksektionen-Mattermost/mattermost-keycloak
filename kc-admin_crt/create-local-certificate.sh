#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
tls_dir="$script_dir/tls"

mkdir -p "$tls_dir"
openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout "$tls_dir/privkey.pem" \
  -out "$tls_dir/fullchain.pem" \
  -days 3650 \
  -subj "/CN=kc-admin.mattermost.fysiksektionen.se" \
  -addext "subjectAltName=DNS:kc-admin.mattermost.fysiksektionen.se,DNS:localhost,IP:127.0.0.1,IP:::1"
chmod 600 "$tls_dir/privkey.pem"
