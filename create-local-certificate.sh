#!/bin/sh
set -eu

mkdir -p tls
openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout tls/privkey.pem \
  -out tls/fullchain.pem \
  -days 365 \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1,IP:::1"
chmod 600 tls/privkey.pem
