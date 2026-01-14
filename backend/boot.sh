#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

GREEN="\033[1;32m"
BLUE="\033[1;34m"
YELLOW="\033[1;33m"
RESET="\033[0m"

echo -e "${GREEN}🚀 Iniciando backend...${RESET}"

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
echo "   • Profile: ${SPRING_PROFILES_ACTIVE:-NO DEFINIDO}"
echo "   • DB:      ${SPRING_DATASOURCE_URL:-NO DEFINIDO}"
echo "   • Redis:   ${REDIS_HOST:-NO DEFINIDO}:${REDIS_PORT:-NO DEFINIDO}"
echo "   • Proxy:   ${PROXY_BASE_URL:-NO DEFINIDO}"
if [ -n "${PROXY_TOKEN:-}" ]; then
  echo "   • ProxyToken: ${PROXY_TOKEN:0:12}..."
else
  echo "   • ProxyToken: NO DEFINIDO"
fi

echo -e "${GREEN}🌐 Levantando backend en puerto 8080...${RESET}"

# Si no seteaste profile, default a dev (para evitar que use application.yml base)
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

./mvnw spring-boot:run
