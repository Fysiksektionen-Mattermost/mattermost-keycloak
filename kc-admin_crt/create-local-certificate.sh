#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
tls_dir="$script_dir/tls"

mkdir -p "$tls_dir"

openssl req -x509 -nodes -newkey rsa:4096 \
  -keyout "$tls_dir/local-ca-key.pem" \
  -out "$tls_dir/local-ca.crt" \
  -days 3650 \
  -subj "/CN=Mattermost Keycloak Admin Local CA" \
  -addext "basicConstraints=critical,CA:TRUE,pathlen:0" \
  -addext "keyUsage=critical,keyCertSign,cRLSign"

openssl req -nodes -newkey rsa:2048 \
  -keyout "$tls_dir/privkey.pem" \
  -out "$tls_dir/server.csr" \
  -subj "/CN=kc-admin.localhost" \
  -addext "subjectAltName=DNS:kc-admin.localhost,DNS:localhost,IP:127.0.0.1,IP:::1"

extensions_file="$tls_dir/server-extensions.cnf"
cat >"$extensions_file" <<'EOF'
basicConstraints = critical,CA:FALSE
keyUsage = critical,digitalSignature,keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = DNS:kc-admin.localhost,DNS:localhost,IP:127.0.0.1,IP:::1
EOF

openssl x509 -req \
  -in "$tls_dir/server.csr" \
  -CA "$tls_dir/local-ca.crt" \
  -CAkey "$tls_dir/local-ca-key.pem" \
  -CAcreateserial \
  -out "$tls_dir/server.crt" \
  -days 825 \
  -extfile "$extensions_file"

cat "$tls_dir/server.crt" "$tls_dir/local-ca.crt" >"$tls_dir/fullchain.pem"
rm -f "$tls_dir/server.csr" "$tls_dir/server-extensions.cnf" "$tls_dir/local-ca.srl"
chmod 600 "$tls_dir/local-ca-key.pem" "$tls_dir/privkey.pem"
