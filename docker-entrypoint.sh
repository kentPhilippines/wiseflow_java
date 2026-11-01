#!/bin/sh
set -e
host="$(hostname 2>/dev/null || true)"
if [ -n "$host" ] && ! grep -qw "$host" /etc/hosts 2>/dev/null; then
  echo "127.0.0.1 ${host}" >> /etc/hosts
fi
cd /app
exec java -jar /app/app.jar
