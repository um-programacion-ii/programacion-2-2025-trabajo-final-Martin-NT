<p align="center">
  <img src="images/um_logo.png" alt="Universidad de Mendoza" />
</p>

# Programación II - Ingeniería en Informática

## Información   
- **Nombre:** Martin Navarro
- **Legajo:** 62181
- **Correo:** mt.navarro@alumno.um.edu.ar
- **Materia**: Programación II

---

Este proyecto implementa un sistema completo de venta de entradas para eventos, integrando:

- Backend (Spring Boot con JHipster)
- Proxy-service (intermediario con la cátedra)
- Frontend (Kotlin Multiplatform / Compose)
- Redis (estado de asientos)
- Kafka (notificaciones de cambios)
- JWT (seguridad)

La arquitectura respeta la consigna de la cátedra, utilizando Redis como fuente de verdad para el estado de los asientos y una base de datos local solo para persistencia e integridad referencial.

---

### Arquitectura General
```md
Frontend
   ↓
Backend 
   ↓
Proxy-Service
   ↓
Servidor Cátedra
   ├─ Redis (estado de asientos)
   └─ Kafka (eventos-actualizacion)
```

### 📂 Módulos
- backend/ → API principal, lógica de negocio, DB local
- proxy/ → Comunicación segura con la cátedra
- frontend/ → App cliente (Eventos, Asientos, Venta)

### ⚙️ Requisitos Previos

Antes de ejecutar el proyecto necesitás:

- Java 17
- Maven
- Docker + Docker Compose
- Acceso a Kafka y Redis provistos por la cátedra (no locales)
- Kafka y Redis de la cátedra activos

### Variables de Entorno
Crear un archivo .env (usado por backend y proxy):
```bash
# Redis cátedra
REDIS_HOST=IP_REDIS_CATEDRA
REDIS_PORT=6379

# Kafka cátedra
KAFKA_BROKER=IP_KAFKA_CATEDRA:9092
PROXY_GROUP_ID=

# URL cátedra
CAT_SERVICE_URL=http://IP_CATEDRA:8080/api

# Seguridad
CATEDRA_JWT_TOKEN=eyJhbGciOi...

SPRING_PROFILES_ACTIVE=dev
```

### Instalación y Ejecución

**1. Clonar el repositorio**
```bash
git clone git@github.com:um-programacion-ii/programacion-2-2025-trabajo-final-Martin-NT.git
```

**2. Ubicarse en el proyecto**
```bash
cd programacion-2-2025-trabajo-final-Martin-NT
```
**3. Levantar servicios (DB, Redis, Kafka)**

```bash
docker-compose up -d
```
**4. Levantar Backend**
```bash
cd backend
```
```bash
./install.sh
```
```bash
./boot.sh
```

**5. Levantar Proxy**
```bash
cd proxy
```
```bash
./install.sh
```
```bash
./boot.sh
```
**6. Ejecutar Frontend**

El frontend está desarrollado en **Kotlin Multiplatform con Compose**.

- Abrir el proyecto `frontend/` en **Android Studio** (recomendado).
- Seleccionar la plataforma de ejecución (Android Emulator / Desktop, según configuración).
- Ejecutar la aplicación desde el IDE.

> El frontend consume directamente los endpoints del **backend** y requiere que el backend y el proxy estén levantados previamente.


**7. Sincronización Inicial (IMPORTANTE)**

Antes de usar el sistema SIEMPRE se debe sincronizar:

En Postman
```bash
POST http://localhost:8080/api/eventos/sync-eventos
```

Esto:
- Descarga eventos desde la cátedra
- Actualiza/crea eventos locales (por externalId)
- Sincroniza grillas de asientos
- Deja la DB local alineada con Redis

### 🧪 Flujo de Pruebas
En docs/ se deja TP Final Programacion2.postman_collection.json con todas las pruebas ya armadas

#### 🔹 1. Login Backend
En Postman  
- POST http://localhost:8080/api/authenticate
```json
{
    "username": "user",
    "password": "user"
}
```
- Devuelve: 200 OK y el token
- Guardá ese JWT y usalo en todo lo de backend.


#### 🔹 2. Probar sincronización manual eventos/asientos

En Postman  
- POST http://localhost:8080/api/eventos/sync-eventos
- Headers: Authorization Bearer <token>
- Devuelve: 204 No Content

#### 🔹 3. Consultar eventos locales activos después de sincronizar

En Postman
- GET http://localhost:8080/api/eventos
- Headers: Authorization Bearer <token>

**Para buscar evento local por Id Local**
- GET http://localhost:8080/api/eventos/<id-local>
- Headers: Authorization Bearer <token>

#### 🔹 4. Listar eventos

**Completos**
- GET http://localhost:8080/api/eventos/completos
- Headers: Authorization Bearer <token>

**Resumidos**
- GET http://localhost:8080/api/eventos/resumidos
- Headers: Authorization Bearer <token>

**Buscar un evento**
- GET http://localhost:8080/api/eventos/4
- Headers: Authorization Bearer <token>

#### 🔹 4. Simular notificación desde proxy
En Postman 
- POST http://localhost:8080/api/proxy/notificacion-evento
- Headers: Authorization Bearer <token>
- JSON: 👉 Se envía este JSON solo para simular lo que mandaría el proxy cuando recibe un mensaje de Kafka:
```json
{
  "eventoId": 123,
  "origen": "postman-test"
}
```

#### 🔹 5. Ver los asientos remotos en el proxy   (Redis cátedra)
Prueba en Proxy con Postman
- GET http://localhost:8081/api/proxy/eventos/1/asientos
- Headers: Authorization Bearer <token>

#### 🔹 6. Ver estado de asientos desde el proxy (Redis) 
Prueba en Proxy con Postman
En Postman
- http://localhost:8081/api/proxy/eventos/<id>/estado-asientos
- Headers: Authorization Bearer <token>

#### 🔹 7. Ver los asientos ya sincronizados (Mapa Final de Asientos)
En Postman
- GET http://localhost:8080/api/eventos/<id>/asientos
- Headers: Authorization Bearer <token>

#### 🔹 8. Bloquear Asiento
En el paso anterior elegir un asiento libre.

En Postman (ejemplo id=3)
- POST http://localhost:8080/api/eventos/<Id>/bloqueos 
- Headers: Authorization Bearer <token>
```json
{
    "eventoId": 3,
    "asientos": [
        {
          "fila": 7,
          "columna": 3
        }
    ]
}
```

#### 🔹 9. Realizar Venta Asiento
Luego de bloquear asiento

En Postman
- POST http://localhost:8080/api/ventas/eventos/<id>/venta
- Headers: Authorization Bearer <token>
- JSON que se le pasa:
```json
{
  "eventoId": 3,
  "asientos": [
    {
      "fila": 7,
      "columna": 3,
      "persona": "Martin Navarro"
    }
  ]
}
```