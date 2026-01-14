#!/usr/bin/env bash

# Salir si hay error
set -e

cd "$(dirname "$0")"
echo "== Instalación / preparación del proxy =="

# 1) Cargar variables del .env (si existe)
if [ -f .env ]; then
  echo "Cargando variables desde .env..."
  set -a
  source .env
  set +a
else
  echo "AVISO: No se encontró .env en $(pwd)"
fi

echo "REDIS_HOST=${REDIS_HOST:-NO DEFINIDO}"
echo "KAFKA_BROKER=${KAFKA_BROKER:-NO DEFINIDO}"
echo "CAT_SERVICE_URL=${CAT_SERVICE_URL:-NO DEFINIDO}"
echo "PROXY_GROUP_ID=${PROXY_GROUP_ID:-NO DEFINIDO}"

# 2) Descargar dependencias y compilar (equivalente a preparar entorno)
./mvnw clean install -DskipTests

echo "✅ Proxy compilado e instalado (dependencias descargadas)."
