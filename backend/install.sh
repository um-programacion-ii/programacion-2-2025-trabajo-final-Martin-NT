#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"
echo "== Instalación / preparación del backend =="

# 1) Cargar variables desde .env (si existe)
if [ -f .env ]; then
  echo "Cargando variables desde .env..."
  set -a
  source .env
  set +a
else
  echo "AVISO: No se encontró .env en $(pwd)"
fi

echo "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-NO DEFINIDO}"
echo "SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:-NO DEFINIDO}"
echo "REDIS_HOST=${REDIS_HOST:-NO DEFINIDO}:${REDIS_PORT:-NO DEFINIDO}"
echo "PROXY_BASE_URL=${PROXY_BASE_URL:-NO DEFINIDO}"
echo "PROXY_TOKEN=${PROXY_TOKEN:+(definido)}"

# 2) Descargar dependencias y compilar
./mvnw clean install -DskipTests

echo "✅ Backend compilado e instalado (dependencias descargadas)."
