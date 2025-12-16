# Resumen de Todo lo Realizado Hasta Ahora (Etapa 1)

Este documento resume de forma clara y ordenada **todo lo que se ha hecho hasta el momento** en el proyecto del **TP Final de Programación II**, correspondiente a la **Etapa 1: Configuración del Backend y Conexión con la Cátedra**.

Su objetivo es que puedas entender exactamente qué se configuró, para qué sirve cada paso y en qué estado se encuentra el proyecto.

---

# 1. Generación del Backend con JHipster

Se creó el backend utilizando **JHipster**, un generador de aplicaciones que integra Spring Boot, Spring Security, caching, base de datos y una interfaz de administración.

### ✔ Decisiones tomadas
- **Tipo de aplicación:** Monolítica  
- **Framework Backend:** Spring Boot  
- **Seguridad:** Autenticación JWT  
- **Base de datos principal:** PostgreSQL o MySQL según necesidad  
- **Caché:** Redis (local)  
- **Herramientas incluidas por JHipster:**  
  - Angular UI administrativa  
  - Gestión de usuarios  
  - Logging  
  - Capa de servicios  
  - CRUD base

### ✔ Resultado
- La aplicación compila correctamente con `./mvnw`.
- Se accede a la UI en `http://localhost:8080`.
- Se pueden crear usuarios desde la sección **Administración**.

---

# 2. Configuración del Entorno Local (.env)

Para evitar credenciales fijas en el código, se creó un archivo `.env` con variables de entorno para:

- Base de datos local  
- Redis  
- URL del servidor de la cátedra  
- Token (pendiente del profesor)  
- Configuración del broker Kafka

Ejemplo del archivo `.env`:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/backendCatedra
SPRING_DATASOURCE_USERNAME=backendCatedra
SPRING_DATASOURCE_PASSWORD=

REDIS_HOST=localhost
REDIS_PORT=6379

CAT_SERVICE_URL=http://192.168.194.250:8080/api
CAT_SERVICE_TOKEN=

KAFKA_BROKER=192.168.194.250:9092
```

Este archivo fue añadido al `.gitignore` para **no versionar credenciales sensibles**.

---

# 3. Configuración de Spring Boot

En el archivo `application-dev.yml` se agregó soporte para variables externas:

```yaml
catservice:
  url: ${CAT_SERVICE_URL:http://SERVIDOR_CATEDRA:PUERTO/api}
  token: ${CAT_SERVICE_TOKEN:}
```

Además, el backend fue configurado para:

- Leer Redis desde variables  
- Leer el broker Kafka desde variables  
- Preparar comunicación con el servidor de la cátedra

---

# 4. Servicios Locales del Backend

JHipster proporciona contenedores Docker para PostgreSQL y Redis.

### ✔ Se levantaron con:
```bash
docker compose -f src/main/docker/postgresql.yml -f src/main/docker/redis.yml up -d
```

### ✔ Servicios confirmados:
- PostgreSQL aceptando conexiones  
- Redis respondiendo correctamente (`PONG`)  

---

# 5. Pruebas de Login y JWT

Se creó un usuario desde la UI y luego se probaron credenciales mediante Postman o curl.

### ✔ Petición:

```bash
POST http://localhost:8080/api/authenticate
```

### ✔ Body JSON:

```json
{
  "username": "user",
  "password": "user",
  "rememberMe": false
}
```

### ✔ Respuesta esperada:
Un objeto con un campo `id_token` (JWT válido).

Esto confirma:
- Seguridad operativa  
- Configuración de JHipster correcta  
- Flujos base funcionando  

---

# 6. ZeroTier: Red de la Cátedra

Se instaló **ZeroTier** y se unió el nodo del alumno a la red:

```bash
sudo zerotier-cli join 93afae59630d1f1d
```

Luego se envió la dirección asignada al profesor.

### ✔ Estado actual
- **Esperando habilitación del profesor**  
- Una vez habilitado, el servidor de la cátedra estará accesible en:

```
http://192.168.194.250:8080
Redis -> 192.168.194.250:6379
Kafka -> 192.168.194.250:9092
```

Cuando esto ocurra, se continuará con el Issue #4.

---

# 7. Validaciones Post-Habilitación (pendientes)
Cuando ZeroTier sea habilitado:

1. Confirmar conexión con el servidor HTTP  
2. Confirmar Redis de la cátedra  
3. Confirmar broker Kafka  
4. Actualizar `.env` con token real  
5. Probar lectura correcta desde Spring Boot  

Esto cerrará formalmente la Etapa 1.

---

# 8. Estado Actual de la Etapa 1

A la fecha, se ha completado:

### ✔ Backend generado correctamente  
### ✔ Entorno local con variables funcionando  
### ✔ Servicios PostgreSQL y Redis locales funcionando  
### ✔ Autenticación JWT probada y validada  
### ✔ Conexión ZeroTier establecida (esperando aprobación)  
### ✔ Documentación detallada de comandos y setup  
### ✔ Issues #2 y #3 completados  
### ⏳ Issue #4 pendiente de habilitación de la red  

---

# 9. Qué Sigue (Etapa 2)

Cuando el profesor habilite ZeroTier:

- Integración real con servidor de la cátedra  
- Conectar Kafka (consumidores y productores)  
- Conectar Redis de la cátedra  
- Crear el Proxy Service  
- Sincronizaciones  
- Inicio del cliente móvil  

---

# Conclusión

La Etapa 1 está virtualmente completada y lista para avanzar.  
Todo el entorno backend está instalado, configurado y probado correctamente, sólo resta habilitación de red para cerrar los últimos pasos.

Este documento resume todo lo que se hizo y establece el punto exacto en el que se encuentra el proyecto.

