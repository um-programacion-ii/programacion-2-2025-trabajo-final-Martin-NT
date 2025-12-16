# Resumen del Proxy hasta el issue #18

## 1. Panorama general

El **proxy-service** es un microservicio Spring Boot independiente del backend JHipster.  
Su rol es ser el **único punto de comunicación con la cátedra**:

- Lee **eventos** del servidor HTTP de la cátedra.
- Consume mensajes de **Kafka** (topic `eventos-actualizacion`).
- Lee el estado de asientos desde el **Redis remoto** de la cátedra.
- Expone una API propia `/api/proxy/**` para que el backend (o Postman) consulte todo **siempre via proxy**, nunca directo contra los servicios de la cátedra.
- Agrega un primer nivel de **seguridad y autenticación** hacia la cátedra usando JWT.

---

## 2. Issues realizados

### #12 – Crear y Configurar Proxy

- Se generó el proyecto **proxy/** con Spring Initializr, con los starters:
  `web`, `actuator`, `kafka`, `data-redis`, `security`.
- Se configuró `application.yml` para:
  - Levantar el proxy en el **puerto 8081**.
  - Configurar Redis remoto con `spring.data.redis.host` y `spring.data.redis.port`.
  - Configurar Kafka remoto con `spring.kafka.bootstrap-servers`, `spring.kafka.consumer.group-id`, etc.
  - Definir la URL base del servidor de la cátedra en `catservice.url`.
- Se creó el archivo **`.env`** del proxy con:
  - `REDIS_HOST`, `REDIS_PORT`
  - `CAT_SERVICE_URL`
  - `KAFKA_BROKER`
  - `PROXY_GROUP_ID` (groupId único del alumno).
- Se comprobó que:
  - El proyecto compila con `./mvnw`.
  - Arranca correctamente en `http://localhost:8081`.
  - No hay errores al inicializar Redis ni Kafka.
- Rama usada: **`feature/proxy-service`**.

---

### #13 – Configurar consumo de Kafka en Proxy

- Se aseguró la configuración de Kafka en `application.yml`:
  - `spring.kafka.bootstrap-servers=${KAFKA_BROKER}`
  - `spring.kafka.consumer.group-id=${PROXY_GROUP_ID}`
  - `spring.kafka.consumer.auto-offset-reset=earliest`
  - `spring.kafka.listener.missing-topics-fatal=false`.
- Se creó el listener **`EventoKafkaListener`**:
  - Anotado con `@KafkaListener(topics = "eventos-actualizacion", groupId = "${PROXY_GROUP_ID:grupo-alumno}")`.
  - Por ahora solo recibe el payload como `String` y lo loguea:
    `📥 [Kafka] Mensaje recibido en eventos-actualizacion: ...`.
- Se agregaron scripts de ejecución:
  - `install.sh` → compila y prepara el proxy.
  - `boot.sh` → carga variables de `.env` y levanta el proxy en perfil `dev`.
- Prueba manual:
  - Desde Postman: `GET http://192.168.194.250:8080/api/endpoints/v1/forzar-actualizacion` con token válido.
  - La cátedra emite un mensaje en el topic `eventos-actualizacion`.
  - En los logs del proxy se ve:
    - Suscripción al topic.
    - Intentos de conexión al broker remoto.
    - Uso del **groupId único** `martin-nt-proxy`.

---

### #14 – Configurar acceso a Redis remoto en Proxy

- Se confirmó la configuración de Redis remoto en `application.yml` usando variables de `.env`:
  - `spring.data.redis.host=${REDIS_HOST}`
  - `spring.data.redis.port=${REDIS_PORT}`.
- Se creó el servicio **`EstadoAsientosRedisService`**:
  - Inyecta `StringRedisTemplate` para leer valores `String`.
  - Método `obtenerEstadoAsientosRaw(Long eventoId)`:
    - Construye la key `evento_{id}` (ej: `evento_1`).  
    - Llama a Redis con `opsForValue().get(key)`.
    - Loguea si el valor fue ENCONTRADO o NO ENCONTRADO.
  - Método `obtenerEstadoAsientos(Long eventoId)`:
    - Lee el JSON crudo desde Redis.
    - Si la key no existe → devuelve DTO con lista vacía y log informativo.
    - Si el JSON existe → lo parsea con `ObjectMapper` a `EstadoAsientosRemotoDTO`.
    - Maneja errores de parseo con try/catch, log de error y DTO seguro (lista vacía).
- Se crearon los DTOs remotos:
  - **`AsientoRemotoDTO`**: `fila`, `columna`, `estado`, `expira`.
  - **`EstadoAsientosRemotoDTO`**: `eventoId`, `List<AsientoRemotoDTO> asientos`.
- Se agregó un **`RedisTestRunner`** (CommandLineRunner en perfil dev) para probar:
  - Lectura de `evento_1` en Redis.
  - Log del JSON crudo.
  - Parseo correcto al DTO y cantidad de asientos.  
  - Manejo seguro cuando la key no existe o el formato es incorrecto.

---

### #15 – Cliente HTTP del proxy para consumir endpoints de la cátedra (versión RestTemplate)

> Esta versión fue luego migrada a Feign, pero forma parte del proceso histórico.

- Se configuró un `RestTemplate` en la clase `CatServiceHttpConfig` (luego obsoleta).
- Se creó el servicio **`CatServiceClient`** (fachada HTTP hacia la cátedra) con métodos:
  - `listarEventosResumidos()` → GET `/endpoints/v1/eventos-resumidos`
  - `listarEventosCompletos()` → GET `/endpoints/v1/eventos`
  - `obtenerEventoPorId(Long id)` → GET `/endpoints/v1/evento/{id}`
  - `forzarActualizacion()` → GET `/endpoints/v1/forzar-actualizacion`
- Cada método:
  - Armaba la URL con la base `catservice.url` del `application.yml`.
  - Llamaba a la cátedra con `RestTemplate`.
  - Devolvía el body como `String` (JSON crudo).
  - Logueaba URL, status y `body.length()` para diagnóstico.
- Se creó **`CatServiceClientTestRunner`** (CommandLineRunner en perfil `dev`) para probar todos los métodos y verificar en logs el funcionamiento.

---

### #16 – Migrar cliente HTTP del proxy a Feign

- Se agregó la dependencia de **Spring Cloud OpenFeign** y se habilitó `@EnableFeignClients` en **`ProxyApplication`**.
- Se creó la interfaz **`CatServiceFeignClient`** en el paquete `client`:
  - Anotada con `@FeignClient` apuntando a `catservice.url`.
  - Define los métodos que representan los endpoints remotos.
- Se refactorizó **`CatServiceClient`** para:
  - Dejar de usar `RestTemplate`.
  - Inyectar `CatServiceFeignClient`.
  - Mantener los mismos métodos públicos de fachada:
    - `listarEventosResumidos`, `listarEventosCompletos`, `obtenerEventoPorId`, `forzarActualizacion`.
  - Centralizar logs y manejo de errores (try/catch, logs claros, retorno seguro).
- La clase `CatServiceHttpConfig` quedó obsoleta y se dejó de usar.
- Se mantuvo **`CatServiceClientTestRunner`** sin cambios en la firma, ahora probando el flujo “vía Feign”.
- Resultado:
  - El proxy arranca sin errores con Feign habilitado.
  - La interfaz Feign se detecta correctamente.
  - Todas las llamadas HTTP hacia la cátedra se realizan ahora de forma declarativa (interfaces), con una fachada única `CatServiceClient` para logs y tratamiento de errores.

---

### #17 – Exponer API de eventos en el proxy (vía Feign)

- Se creó el controlador REST **`ProxyEventosResource`** en `web.rest`.
- Endpoints expuestos hacia el backend / Postman:
  - `GET /api/proxy/eventos-resumidos`
  - `GET /api/proxy/eventos`
  - `GET /api/proxy/eventos/{id}`
  - `GET /api/proxy/eventos/forzar-actualizacion`
- Cada método del controlador:
  - Loguea la invocación.
  - Llama al método correspondiente de `CatServiceClient` (que a su vez usa Feign).
  - Si el body es distinto de `null` → devuelve `200 OK` con el JSON crudo.
  - Si hay error y el cliente devuelve `null` → devuelve `502 Bad Gateway` con un JSON de error simple.
- Se probó todo desde Postman, verificando que el backend podría usar estos mismos endpoints en la siguiente etapa.

---

### #18 – Proxy: Autenticación JWT hacia la cátedra + estado de asientos

**Autenticación JWT hacia la cátedra**

- Se agregó una propiedad de configuración para el token de la cátedra en `application-dev.yml`:
  - `catedra.jwt-token: ${CATEDRA_JWT_TOKEN:}`
- Se definió la variable `CATEDRA_JWT_TOKEN` en el `.env` del proxy (token admin provisto por la cátedra).
- Se creó la clase de configuración **`CatServiceFeignConfig`**:
  - Define un `RequestInterceptor` de Feign.
  - Lee el valor de `catedra.jwt-token`.
  - Si el token NO está vacío → agrega a **todas** las requests Feign el header:
    - `Authorization: Bearer <token>`.
  - Si el token está vacío → loguea un `WARN` indicando que se llamará sin Authorization.
- Se asoció esta configuración al cliente Feign `CatServiceFeignClient` con
  `configuration = CatServiceFeignConfig.class`.
- Verificación:
  - `CatServiceClient` loguea mensajes como “Llamando a listarEventosResumidos vía Feign”.
  - Se loguea `bodyLength` de las respuestas (>0 cuando todo está bien).
  - En los logs de la cátedra / o con logging de Feign se puede ver el header `Authorization` enviado.
  - Dejan de aparecer errores **401 Unauthorized** cuando el token es válido.

**Seguridad mínima del proxy en dev**

- Se creó **`SecurityConfig`** en `config` con un `SecurityFilterChain` muy simple para desarrollo:
  - `csrf().disable()` → el proxy es un API, no usa formularios.
  - `httpBasic().disable()` y `formLogin().disable()` → se desactiva autenticación por Basic y formularios.
  - `authorizeHttpRequests().anyRequest().permitAll()` → en **perfil dev** permite todas las requests sin autenticación.
- Comentado en el código que esta es una configuración **provisoria** para poder probar fácilmente desde Postman y que más adelante se endurecerá la seguridad de los endpoints del proxy.

**Nuevo endpoint: estado de asientos vía Redis remoto**

- Se añadió al controlador **`ProxyEventosResource`** el endpoint:
  - `GET /api/proxy/eventos/{id}/estado-asientos`
- Comportamiento:
  - Loguea la invocación.
  - Llama a `EstadoAsientosRedisService.obtenerEstadoAsientos(id)`.
  - Devuelve un JSON con la estructura del DTO `EstadoAsientosRemotoDTO`:
    - `eventoId`
    - `asientos` (lista de `fila`, `columna`, `estado`, `expira`).
  - Si no hay datos en Redis para ese evento:
    - Devuelve `eventoId` y `asientos: []` (lista vacía), sin lanzar excepciones.
  - Si hay un error inesperado de integración:
    - Loguea el error.
    - Devuelve `502 Bad Gateway` con un JSON de error controlado.
- Pruebas desde Postman:
  - `GET http://localhost:8081/api/proxy/eventos-resumidos` → **200 OK** con JSON real de la cátedra.
  - `GET http://localhost:8081/api/proxy/eventos/1/estado-asientos` → **200 OK** con `eventoId=1` y lista de asientos bloqueados/vendidos leídos desde Redis remoto.

---

## 3. Estructura actual del proxy-service (qué hace cada archivo)

### Paquete raíz `ar.edu.um.proxyservice`

- **`ProxyApplication`**
  - Clase principal de Spring Boot.
  - Contiene el `main` y la anotación `@SpringBootApplication`.
  - Habilita Feign con `@EnableFeignClients`.
  - Arranca el contexto del proxy y aplica el perfil `dev`.

---

### Paquete `client`

- **`CatServiceFeignClient`**
  - Interfaz declarativa Feign para el servidor de la cátedra.
  - Define los métodos que representan los endpoints remotos:
    - `getEventosResumidos()`
    - `getEventosCompletos()`
    - `getEventoPorId(Long id)`
    - `forzarActualizacion()`
  - Feign se encarga de construir las requests HTTP usando la URL base `catservice.url` y la configuración de `CatServiceFeignConfig`.

---

### Paquete `config`

- **`CatServiceFeignConfig`**
  - Configuración de Feign para agregar el header `Authorization: Bearer <token>` a todas las llamadas.
  - Lee `catedra.jwt-token` desde `application-dev.yml` / `.env`.
  - Loguea `WARN` si se llama sin token.

- **`SecurityConfig`**
  - Configuración mínima de seguridad para el proxy (por ahora solo en dev):
    - Desactiva CSRF, Basic Auth y login por formularios.
    - Permite cualquier request sin autenticación.
  - Deja preparado el lugar para en un futuro endurecer el acceso al proxy (JWT o API key entre backend ↔ proxy).

- **`RedisTestRunner`**
  - `CommandLineRunner` en perfil `dev`.
  - Se ejecuta al inicio para probar la integración con Redis remoto:
    - Llama a `EstadoAsientosRedisService` con un `eventoId` de prueba.
    - Loguea el JSON crudo y el DTO parseado.

- **`CatServiceClientTestRunner`**
  - `CommandLineRunner` en perfil `dev`.
  - Al levantar el proxy, ejecuta una batería de pruebas contra la cátedra:
    - Lista eventos resumidos y completos.
    - Obtiene un evento por ID.
    - Dispara `forzar-actualizacion`.
  - Muestra en logs la URL, `bodyLength` y posible error → sirve como smoke test de la integración HTTP/Feign + JWT.

---

### Paquete `messaging`

- **`EventoKafkaListener`**
  - Listener de Kafka para el topic `eventos-actualizacion`.
  - Anotado con `@KafkaListener` y usando el groupId del `.env` (`martin-nt-proxy`).
  - Por ahora solo loguea el mensaje crudo recibido.
  - En el futuro se usará para disparar sincronizaciones de eventos hacia el backend.

---

### Paquete `service.dto`

- **`AsientoRemotoDTO`**
  - Representa un asiento tal como viene desde el Redis remoto de la cátedra.
  - Campos: `fila`, `columna`, `estado`, `expira`.
  - Se usa para mapear el JSON externo a un objeto Java.

- **`EstadoAsientosRemotoDTO`**
  - Representa el estado completo de asientos de un evento.
  - Campos: `eventoId` y `List<AsientoRemotoDTO> asientos`.
  - Es el formato que el proxy devuelve actualmente en `/api/proxy/eventos/{id}/estado-asientos`.

---

### Paquete `service`

- **`CatServiceClient`**
  - Fachada de servicio sobre el cliente Feign.
  - Métodos:
    - `listarEventosResumidos()`
    - `listarEventosCompletos()`
    - `obtenerEventoPorId(Long id)`
    - `forzarActualizacion()`
  - Llama internamente a `CatServiceFeignClient`.
  - Centraliza:
    - Logs de “Llamando a … vía Feign”.
    - Manejo de errores (try/catch, null seguro, logs claros).
    - Cálculo y log de `bodyLength` de las respuestas.

- **`EstadoAsientosRedisService`**
  - Encapsula el acceso al Redis remoto de la cátedra.
  - Métodos:
    - `obtenerEstadoAsientosRaw(Long eventoId)` → JSON crudo.
    - `obtenerEstadoAsientos(Long eventoId)` → DTO `EstadoAsientosRemotoDTO`.
  - Asegura que:
    - Si no hay datos → devuelve lista vacía sin romper el proxy.
    - Si el JSON está mal → loguea error y devuelve DTO seguro.

---

### Paquete `web.rest`

- **`ProxyEventosResource`**
  - Controlador REST del proxy.
  - Endpoints públicos para el backend / Postman:
    - `GET /api/proxy/eventos-resumidos`
    - `GET /api/proxy/eventos`
    - `GET /api/proxy/eventos/{id}`
    - `GET /api/proxy/eventos/forzar-actualizacion`
    - `GET /api/proxy/eventos/{id}/estado-asientos`
  - Delegan en:
    - `CatServiceClient` para todo lo que sea HTTP hacia la cátedra.
    - `EstadoAsientosRedisService` para leer estados de asientos desde Redis.
  - Manejo de errores controlado (502 con JSON de error cuando algo falla).

---

### `src/main/resources/application.yml`

- Define la configuración base del proxy:
  - Nombre de la aplicación y puerto.
  - Integración con Redis remoto.
  - Integración con Kafka remoto.
  - URL base de la cátedra (`catservice.url`).
  - Propiedad `catedra.jwt-token` para el token JWT usado por Feign.
- Usa variables de entorno para no hardcodear IPs ni secretos.

---

### Otros archivos importantes en la raíz del proyecto

- **`.env`**
  - Centraliza la configuración del entorno de desarrollo del proxy:
    - IP y puerto del Redis de la cátedra.
    - Broker Kafka remoto.
    - URL HTTP del servidor de la cátedra.
    - `PROXY_GROUP_ID` (groupId único).
    - `CATEDRA_JWT_TOKEN` (token admin de la cátedra).

- **`boot.sh`**
  - Script de conveniencia para levantar el proxy:
    - Carga automáticamente el `.env`.
    - Muestra por consola los valores clave que se van a usar.
    - Ejecuta `./mvnw spring-boot:run` con `SPRING_PROFILES_ACTIVE=dev`.

---

## 4. Mini guion para explicárselo al profesor

1. **Arquitectura general**  
   - “Tengo un microservicio proxy independiente del backend. Todo lo que sea cátedra (HTTP, Kafka, Redis) pasa por el proxy.”

2. **Integraciones externas**  
   - Kafka: listener `EventoKafkaListener` con `groupId` propio, probado disparando `forzar-actualizacion`.
   - Redis: servicio `EstadoAsientosRedisService` leyendo `evento_{id}`, con DTOs remotos y manejo seguro de errores.
   - HTTP: cliente Feign `CatServiceFeignClient` + fachada `CatServiceClient`.

3. **API del proxy**  
   - Endpoints `/api/proxy/eventos*` para eventos.
   - Endpoint `/api/proxy/eventos/{id}/estado-asientos` que devuelve el estado de asientos leído desde Redis remoto.

4. **Seguridad hacia la cátedra**  
   - Feign agrega automáticamente `Authorization: Bearer <CATEDRA_JWT_TOKEN>` a todas las requests.
   - Configuración mínima de `SecurityConfig` solo para dev; la seguridad entre backend y proxy se terminará de definir en un issue aparte.

Con esto quedás listo para contarle al profesor **qué hace el proxy hoy**, qué integra y qué endpoints ya están disponibles para la siguiente etapa de sincronización con el backend.


Mostrale este pedacito del log:

Discovered group coordinator kafka:9092
Error connecting to node kafka:9092
java.net.UnknownHostException: kafka


Y explicale:

“Profe, me conecto bien por la IP 192.168.194.250:9092 pero el broker se anuncia internamente como kafka:9092. Como ese hostname no existe en mi máquina, me tira ese WARN. Por eso le consulto si es normal o si hay que agregar una entrada en /etc/hosts.”