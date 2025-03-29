#!/bin/bash
# Generate self-signed SSL certificates for PostgreSQL and MySQL
# For production, use certificates from a CA (Let's Encrypt, DigiCert, etc.)

set -e

CERT_DIR="./certs"
DAYS_VALID=365

mkdir -p "$CERT_DIR"

echo "=== Generating PostgreSQL certificate ==="
openssl req -x509 -newkey rsa:2048 -keyout "$CERT_DIR/postgres.key" \
  -out "$CERT_DIR/postgres.crt" -days "$DAYS_VALID" -nodes \
  -subj "/CN=postgres/O=Process Automation Monitor" 2>/dev/null
chmod 600 "$CERT_DIR/postgres.key"
chmod 644 "$CERT_DIR/postgres.crt"
echo "✓ PostgreSQL cert: $CERT_DIR/postgres.crt"
echo "✓ PostgreSQL key: $CERT_DIR/postgres.key"

echo ""
echo "=== Generating MySQL CA certificate ==="
openssl req -x509 -newkey rsa:2048 -keyout "$CERT_DIR/mysql-ca.key" \
  -out "$CERT_DIR/mysql-ca.crt" -days "$DAYS_VALID" -nodes \
  -subj "/CN=mysql-ca/O=Process Automation Monitor" 2>/dev/null
echo "✓ MySQL CA: $CERT_DIR/mysql-ca.crt"

echo ""
echo "=== Generating MySQL server certificate ==="
openssl req -newkey rsa:2048 -keyout "$CERT_DIR/mysql.key" \
  -out "$CERT_DIR/mysql.csr" -nodes \
  -subj "/CN=mysql/O=Process Automation Monitor" 2>/dev/null
openssl x509 -req -days "$DAYS_VALID" -in "$CERT_DIR/mysql.csr" \
  -CA "$CERT_DIR/mysql-ca.crt" -CAkey "$CERT_DIR/mysql-ca.key" \
  -CAcreateserial -out "$CERT_DIR/mysql.crt" 2>/dev/null
rm "$CERT_DIR/mysql.csr"
chmod 600 "$CERT_DIR/mysql.key"
chmod 644 "$CERT_DIR/mysql.crt"
echo "✓ MySQL server cert: $CERT_DIR/mysql.crt"
echo "✓ MySQL server key: $CERT_DIR/mysql.key"

echo ""
echo "=== All certificates generated ==="
echo "⚠️  These are self-signed certs for development/testing only."
echo "For production: use certificates from a trusted CA."
