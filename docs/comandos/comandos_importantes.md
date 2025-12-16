# Comandos Importantes Utilizados en el Proyecto

## 1. JHipster
### Generar entidades
```
jhipster entity Evento
jhipster entity Asiento
jhipster entity Venta
```

## 2. Maven
```
./mvnw
./mvnw -Pdev
```

## 3. Docker y servicios locales
### Levantar servicios principales
```
docker-compose up -d
```

### Levantar PostgreSQL y Redis desde JHipster
```
docker compose -f src/main/docker/postgresql.yml up -d
docker compose -f src/main/docker/redis.yml up -d
```

## 4. ZeroTier
```
sudo zerotier-cli join 93afae59630d1f1d
sudo zerotier-cli listnetworks
```

## 5. Postman (o cURL)
### Login
```
POST /api/authenticate
{
  "username": "user",
  "password": "user"
}
```

## 6. Verificar conexión con la cátedra
### HTTP
```
curl http://192.168.194.250:8080
```

### Redis remoto
```
redis-cli -h 192.168.194.250 -p 6379 ping
```

## 7. PostgreSQL dentro del contenedor
```
docker exec -it backendcatedra-postgresql-1 psql -U backendCatedra
\dt
```

## 8. Validación de migraciones Liquibase
```
./mvnw
```

Estos son todos los comandos reales que usaste durante el proyecto hasta ahora.

