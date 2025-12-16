#!/usr/bin/env bash

set -e

cd "$(dirname "$0")"
echo "== Inicio del proxy =="

# Cargar .env de nuevo por las dudas (por si tocaste algo desde la última instalación)
if [ -f .env ]; then
  echo "Cargando variables desde .env..."
  set -a
  source .env
  set +a
else
  echo "AVISO: No se encontró .env en $(pwd)"
fi

echo "Usando configuración:"
echo "  REDIS_HOST=${REDIS_HOST:-NO DEFINIDO}"
echo "  REDIS_PORT=${REDIS_PORT:-NO DEFINIDO}"
echo "  KAFKA_BROKER=${KAFKA_BROKER:-NO DEFINIDO}"
echo "  CAT_SERVICE_URL=${CAT_SERVICE_URL:-NO DEFINIDO}"
echo "  PROXY_GROUP_ID=${PROXY_GROUP_ID:-NO DEFINIDO}"

echo "Levantando proxy en puerto 8081..."
./mvnw spring-boot:run
