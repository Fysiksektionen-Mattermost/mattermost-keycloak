#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
tls_dir="$script_dir/tls"

openssl x509 -in "$tls_dir/fullchain.pem" -noout \
  -subject -issuer -dates -ext subjectAltName