#!/usr/bin/env bash

set -e

cd "$(dirname "$0")"

GREEN="\033[1;32m"
BLUE="\033[1;34m"
YELLOW="\033[1;33m"
RESET="\033[0m"

echo -e "${GREEN}🚀 Iniciando proxy-service...${RESET}"

# Cargar .env
if [ -f .env ]; then
  echo -e "${BLUE}📦 Cargando variables desde .env...${RESET}"
  set -a
  source .env
  set +a
else
  echo -e "${YELLOW}⚠️  AVISO: No se encontró .env en $(pwd)${RESET}"
fi

echo -e "${BLUE}🔧 Configuración activa:${RESET}"
echo "   • Redis:  ${REDIS_HOST:-NO DEFINIDO}:${REDIS_PORT:-NO DEFINIDO}"
echo "   • Kafka:  ${KAFKA_BROKER:-NO DEFINIDO}"
echo "   • Cátedra: ${CAT_SERVICE_URL:-NO DEFINIDO}"
echo "   • Group ID: ${PROXY_GROUP_ID:-NO DEFINIDO}"

echo -e "${GREEN}🌐 Levantando proxy en puerto 8081...${RESET}"
./mvnw spring-boot:run
