openssl x509 -noout -modulus -in ./tls/local-ca.crt | openssl md5
openssl rsa -noout -modulus -in ./tls/local-ca-key.pem | openssl md5