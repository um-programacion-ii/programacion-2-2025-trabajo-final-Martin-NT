# Etapa 3 – Proxy-Service COMPLETA

Este documento resume la **implementación completa de la Etapa 3** del TP Final 2025, correspondiente al servicio `proxy-service`. Incluye:

- Estructura de paquetes y clases.
- Seguridad aplicada al proxy.
- Integración con Redis remoto de la cátedra.
- Integración con Kafka (topic `eventos-actualizacion`).
- Integración HTTP con el servidor de la cátedra vía Feign.
- Scripts y configuración externa (.env, application.yml, boot.sh).

---

## 1. Estructura general del módulo `proxy`

Ruta base (resumida):

```text
proxy/
├── src/main/java/ar/edu/um/proxyservice
│   ├── client/
│   │   └── CatServiceFeignClient
│   ├── config/
│   │   ├── CatServiceFeignConfig
│   │   ├── ProxyTokenAuthFilter
│   │   └── SecurityConfig
│   ├── devtests/
│   │   ├── CatServiceClientTestRunner
│   │   └── RedisTestRunner
│   ├── messaging/
│   │   └── EventoKafkaListener
│   ├── service/
│   │   ├── dto/
│   │   │   └── EstadoAsientosRemotoDTO (DTO usado para Redis remoto)
│   │   ├── CatServiceClient
│   │   └── EstadoAsientosRedisService
│   ├── web/rest/
│   │   └── ProxyEventosResource
│   └── ProxyApplication
├── src/main/resources/
│   └── application.yml
├── .env
├── boot.sh
└── pom.xml
```

### 1.1. `ProxyApplication`

Clase principal de Spring Boot:

- Arranca el contexto de Spring Boot.
- Usa el perfil `dev` (configurado en `application.yml` / entorno).
- Levanta el servidor embebido Tomcat en el puerto `8081`.

---

## 2. Paquete `config` – Configuración de seguridad y Feign

### 2.1. `SecurityConfig`

Responsable de la **configuración de seguridad de Spring Security** para el proxy.

Puntos clave:

- Desactiva características que no se usan en una API:
  - CSRF (`csrf.disable()`).
  - Autenticación básica HTTP (`httpBasic().disable()`).
  - Form login (`formLogin().disable()`).
- Configura el modo de sesión:
  - `SessionCreationPolicy.STATELESS` → el proxy **no** guarda estado de sesión; cada request debe traer su token.
- Reglas de autorización:
  - `/actuator/**` → `permitAll()` → abierto para health-checks/diagnóstico.
  - `/api/proxy/**` → `authenticated()` → **requieren** un header `Authorization: Bearer <token>`.
  - `anyRequest().denyAll()` → cualquier otra ruta se bloquea explícitamente.
- Registra el filtro personalizado `ProxyTokenAuthFilter` **antes** del `UsernamePasswordAuthenticationFilter`.

Resultado: las rutas del proxy quedan protegidas y controladas; la seguridad por defecto de Spring (con usuario inMemory y password generada) deja de utilizarse.

---

### 2.2. `ProxyTokenAuthFilter`

Filtro de seguridad personalizado que extiende `OncePerRequestFilter`.

Responsabilidad:

- Intercepta todas las requests y decide si deben exigir un **Bearer token**.
- Excluye explícitamente `/actuator/**` mediante `shouldNotFilter`.
- Solo aplica lógica de seguridad a rutas que comiencen con `/api/proxy`.

Lógica principal:

1. Obtiene `path` y el header `Authorization`.
2. Si la ruta **no** empieza con `/api/proxy`, simplemente deja pasar la request (no aplica lógica de seguridad).
3. Si la ruta sí es `/api/proxy/**`:
   - Verifica que exista `Authorization` que comience con `Bearer `.
   - Si falta o es inválido:
     - Loguea: `"[Seguridad] Acceso bloqueado a …: falta header Authorization Bearer"`.
     - Devuelve `403 FORBIDDEN` con mensaje `"Debe enviar un token Bearer"`.
   - Si el header está presente:
     - Extrae el token (sin validarlo).
     - Loguea en DEBUG un preview del token y su longitud.
     - Crea un `UsernamePasswordAuthenticationToken` con usuario simbólico `"proxy-user"`.
     - Setea este authentication en el `SecurityContextHolder`.
     - Deja continuar la request.

Importante:

- **No valida el contenido del JWT**; solo verifica presencia. Esto es intencional para la etapa: la validación real se haría en el backend del alumno.
- Marca la request como “autenticada” para que `SecurityConfig` pueda aplicar `.authenticated()` correctamente.

---

### 2.3. `CatServiceFeignConfig`

Configuración de Feign para las llamadas HTTP hacia el servidor de la cátedra.

Responsabilidad:

- Inyecta el token JWT interno de la cátedra en todas las requests Feign.
- Usa la property `catedra.jwt-token` (configurada en `application.yml` y tomada de la variable de entorno `CATEDRA_JWT_TOKEN`).

Elementos:

- Define un `RequestInterceptor` de Feign:
  - Si `catedra.jwt-token` está vacío:
    - Loguea un warning (`[CatServiceFeignConfig] catedra.jwt-token está vacío. Se llamará sin Authorization.`).
    - No agrega el header.
  - Si tiene valor y la request **no** trae ya un `Authorization`:
    - Agrega `Authorization: Bearer <token>` a los headers.

De esta forma:

- El backend del alumno jamás ve el token real de la cátedra.
- El proxy lo usa internamente al llamar al servidor remoto.

---

## 3. Paquete `client` – Cliente Feign hacia la cátedra

### 3.1. `CatServiceFeignClient`

Interfaz Feign que modela los endpoints HTTP expuestos por el servidor de la cátedra.

Métodos típicos (ejemplo conceptual):

- `GET /endpoints/v1/eventos-resumidos`
- `GET /endpoints/v1/eventos`
- `GET /endpoints/v1/evento/{id}`
- `GET /endpoints/v1/forzar-actualizacion`

La URL base se toma desde `catservice.url`, que a su vez se configura con `CAT_SERVICE_URL` en `.env`:

```env
CAT_SERVICE_URL=http://192.168.194.250:8080
```

---

## 4. Paquete `service` – Lógica de integración

### 4.1. `CatServiceClient`

Servicio fachada que envuelve a `CatServiceFeignClient` y centraliza:

- Logs con etiqueta `[Cátedra]`.
- Manejo de excepciones.

Responsabilidad:

- Delegar cada operación al Feign client.
- Loguear:
  - Antes de llamar: `"[Cátedra] Llamando a ... vía Feign"`.
  - Después de llamar: `"[Cátedra] Respuesta ... -> bodyLength=..."`.
- En caso de error:
  - Capturar la excepción.
  - Loguear el error con stacktrace.
  - Devolver `null` para que el controlador pueda responder `502 BAD_GATEWAY`.

Métodos principales:

- `listarEventosResumidos()`
- `listarEventosCompletos()`
- `obtenerEventoPorId(Long id)`
- `forzarActualizacion()`

---

### 4.2. `EstadoAsientosRedisService`

Servicio dedicado a leer el estado de los asientos desde el **Redis remoto de la cátedra**.

Dependencias:

- `StringRedisTemplate` → para leer valores String desde Redis.
- `ObjectMapper` (versión sombreada de JHipster) → para parsear JSON a DTO.

Responsabilidades:

1. **Construcción de la key**:
   - Utiliza el patrón `evento_{id}`, por ejemplo: `evento_1`, `evento_42`.
2. **Método `obtenerEstadoAsientosRaw(Long eventoId)`**:
   - Usa `stringRedisTemplate.opsForValue().get(key)` para leer el JSON crudo.
   - Loguea algo como: `"[Redis] Consultando key=evento_1, resultado=ENCONTRADO/NO ENCONTRADO"`.
   - Devuelve el String crudo o `null`.
3. **Método `obtenerEstadoAsientos(Long eventoId)`**:
   - Llama a `obtenerEstadoAsientosRaw`.
   - Si el JSON es `null`:
     - Crea un `EstadoAsientosRemotoDTO` con:
       - `eventoId` = id solicitado.
       - `asientos` = lista vacía.
     - Loguea que no hay información y devuelve el DTO seguro.
   - Si hay JSON:
     - Intenta parsearlo con `objectMapper.readValue(...)`.
     - Si el JSON no trae `eventoId` dentro, lo setea explícitamente.
     - Loguea: `"Se parseó correctamente estado de asientos para eventoId=X (N asientos)."`.
     - Devuelve el DTO.
   - Si ocurre un error de parseo:
     - Loguea el error y el JSON problemático.
     - Devuelve un DTO seguro con lista vacía.

**Idea central**:  
El proxy **nunca** debe caerse por culpa de datos inconsistentes en Redis; siempre devuelve un objeto válido y controlado.

---

## 5. Paquete `web.rest` – Endpoints del proxy

### 5.1. `ProxyEventosResource`

Controlador REST que expone los endpoints del proxy hacia el backend del alumno (o Postman).

Base path de la clase:

```java
@RestController
@RequestMapping("/api/proxy")
public class ProxyEventosResource { ... }
```

Dependencias:

- `CatServiceClient` → para llamar a la cátedra vía HTTP.
- `EstadoAsientosRedisService` → para leer el estado de asientos desde Redis remoto.

Endpoints implementados:

1. **`GET /api/proxy/eventos-resumidos`**
   - Llama a `catServiceClient.listarEventosResumidos()`.
   - Si la respuesta es `null` → responde `502 BAD_GATEWAY` con JSON `"error"`.
   - Si hay body → lo devuelve directamente (JSON crudo).
   - Log etiqueta: `[Proxy] GET /api/proxy/eventos-resumidos`.

2. **`GET /api/proxy/eventos`**
   - Devuelve eventos completos desde la cátedra.
   - Comportamiento de error similar al anterior.

3. **`GET /api/proxy/eventos/{id}`**
   - Devuelve el detalle de un evento por ID.
   - Si falla la cátedra → `502 BAD_GATEWAY` con mensaje de error.

4. **`GET /api/proxy/eventos/forzar-actualizacion`**
   - Dispara el endpoint de la cátedra `forzar-actualizacion`.
   - Devuelve el body crudo (o un error 502 en caso de fallo).
   - Log: `[Proxy] GET /api/proxy/eventos/forzar-actualizacion`.

5. **`GET /api/proxy/eventos/{id}/estado-asientos`**
   - Llama a `estadoAsientosRedisService.obtenerEstadoAsientos(id)`.
   - Devuelve el DTO `EstadoAsientosRemotoDTO` como JSON.
   - Maneja cualquier excepción devolviendo `502 BAD_GATEWAY` con JSON de error e incluye el `eventoId` en el mensaje.
   - Log: `[Proxy] GET /api/proxy/eventos/{}/estado-asientos`.

Todos estos endpoints:

- Están protegidos por el filtro `ProxyTokenAuthFilter`.
- Requieren header `Authorization: Bearer ...`.
- Exponen una interfaz limpia para que el backend del alumno nunca llame directamente al Redis o al servidor de la cátedra.

---

## 6. Paquete `messaging` – Kafka

### 6.1. `EventoKafkaListener`

Componente `@Component` que actúa como **consumidor Kafka** para el topic `eventos-actualizacion`.

Configuración:

```java
@KafkaListener(
    topics = "eventos-actualizacion",
    groupId = "${spring.kafka.consumer.group-id}"
)
public void onEventoActualizado(String mensaje) {
    log.info("[Kafka] Mensaje recibido en eventos-actualizacion");
    log.debug("[Kafka] Payload recibido: {}", mensaje);
}
```

Características:

- Escucha el topic `eventos-actualizacion` indicado por la cátedra.
- Usa un `groupId` configurable vía `spring.kafka.consumer.group-id`, que a su vez se carga desde `.env` con `PROXY_GROUP_ID`.
- Loguea cuando llega un mensaje real (INFO) y opcionalmente su payload (DEBUG).

Configuración asociada en `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BROKER:192.168.194.250:9092}
    consumer:
      group-id: ${PROXY_GROUP_ID:grupo-alumno}
      auto-offset-reset: earliest
    listener:
      missing-topics-fatal: false
```

Además, a nivel de sistema operativo se añadió una entrada en `/etc/hosts` para resolver el nombre `kafka` cuando sea necesario:

```text
192.168.194.250   kafka
```

Con esto se resolvieron los errores `UnknownHostException: kafka` y `coordinator unavailable`.

---

## 7. Paquete `devtests` – Runners de pruebas manuales (deshabilitados)

### 7.1. `CatServiceClientTestRunner`
### 7.2. `RedisTestRunner`

Ambos implementan `CommandLineRunner` y estaban diseñados para:

- Probar llamadas HTTP a la cátedra (`CatServiceClient`).
- Probar lectura y parseo desde Redis (`EstadoAsientosRedisService`).

Actualmente:

- Están en el paquete `devtests`.
- Tienen comentadas las anotaciones `@Component` y `@Profile("dev")`, por lo que **no se registran** como beans y **no se ejecutan** al arrancar el proxy.
- El arranque queda limpio, pero los runners siguen disponibles para pruebas manuales futuras si se vuelven a habilitar.

---

## 8. `application.yml` – Configuración central del proxy

Archivo: `src/main/resources/application.yml`.

Responsable de:

- Configurar el puerto del proxy:
  ```yaml
  server:
    port: 8081
  ```

- Configurar Redis remoto:
  ```yaml
  spring:
    data:
      redis:
        host: ${REDIS_HOST:192.168.194.250}
        port: ${REDIS_PORT:6379}
  ```

- Configurar Kafka:
  ```yaml
  spring:
    kafka:
      bootstrap-servers: ${KAFKA_BROKER:192.168.194.250:9092}
      consumer:
        group-id: ${PROXY_GROUP_ID:grupo-alumno}
        auto-offset-reset: earliest
      listener:
        missing-topics-fatal: false
  ```

- Configurar URL base de la cátedra:
  ```yaml
  catservice:
    url: ${CAT_SERVICE_URL:http://192.168.194.250:8080}
  ```

- Configurar JWT interno de la cátedra:
  ```yaml
  catedra:
    jwt-token: ${CATEDRA_JWT_TOKEN:}
  ```

- Configuración de logs:
  ```yaml
  logging:
    level:
      root: INFO
      org.springframework.security: INFO
      ar.edu.um.proxyservice.config.ProxyTokenAuthFilter: DEBUG
      ar.edu.um.proxyservice.messaging: INFO
      org.apache.kafka: WARN
  ```

Con esto se controla:

- Nivel de ruido de logs.
- Etiquetas conceptuales `[Seguridad]`, `[Proxy]`, `[Cátedra]`, `[Kafka]`, `[Redis]` desde el código.
- Uso de variables de entorno (`.env`) para diferenciar entornos sin tocar el código.

---

## 9. `.env` – Variables de entorno de la cátedra

Archivo `.env` en la raíz del proyecto, utilizado por `boot.sh` y por Spring al arrancar.

Ejemplo de contenido:

```env
REDIS_HOST=192.168.194.250
REDIS_PORT=6379

CAT_SERVICE_URL=http://192.168.194.250:8080

KAFKA_BROKER=192.168.194.250:9092
PROXY_GROUP_ID=martin-nt-proxy

CATEDRA_JWT_TOKEN=eyJhbGciOi...
```

Rol:

- Centralizar la configuración específica del alumno y de la red de la cátedra.
- Evitar hardcodear hosts y tokens dentro del código.

---

## 10. `boot.sh` – Script de arranque del proxy

Script en la raíz del módulo, responsable de:

1. Posicionarse en la carpeta del proyecto.
2. Cargar variables desde `.env` (si existe):
   - Usa `set -a` / `source .env` / `set +a`.
3. Mostrar un resumen de la configuración activa:
   - `REDIS_HOST`, `REDIS_PORT`
   - `KAFKA_BROKER`
   - `CAT_SERVICE_URL`
   - `PROXY_GROUP_ID`
4. Ejecutar el comando:
   ```bash
   ./mvnw spring-boot:run
   ```

Beneficios:

- Simplifica el arranque (solo hay que ejecutar `./boot.sh`).
- Deja explícito qué valores se están usando (útil para debugging y para mostrar al profesor).
- Separa claramente **código** de **configuración de entorno**.

---

## 11. Resumen conceptual de la Etapa 3

1. **Seguridad**:
   - Proxy totalmente stateless.
   - Rutas `/api/proxy/**` protegidas con `Authorization: Bearer`.
   - Filtro propio `ProxyTokenAuthFilter` con logs `[Seguridad]`.
   - Autoconfiguración por defecto de Spring Security desactivada en la práctica (no se usa el usuario/password generados).

2. **HTTP hacia la cátedra (Feign)**:
   - Cliente tipado `CatServiceFeignClient`.
   - Configuración `CatServiceFeignConfig` que aplica el `JWT` interno desde `.env`.
   - Fachada `CatServiceClient` con logs `[Cátedra]` y manejo de errores.

3. **Redis remoto**:
   - Acceso controlado mediante `EstadoAsientosRedisService`.
   - Uso de `StringRedisTemplate` y `ObjectMapper`.
   - DTO seguro `EstadoAsientosRemotoDTO`.
   - Tolerancia a errores: nunca rompe el proxy por datos malos.

4. **Kafka**:
   - Listener `EventoKafkaListener` suscripto a `eventos-actualizacion`.
   - GroupId único del alumno configurado por `.env`.
   - Logs `[Kafka]` para mensajes reales recibidos.
   - Problemas de DNS corregidos (`kafka` → `192.168.194.250`).

5. **Endpoints del proxy**:
   - Ruta base `/api/proxy`.
   - Endpoints para eventos (resumen, completos, por id, forzar actualización).
   - Endpoint específico para estado de asientos que encapsula Redis.
   - Manejo de errores con `502 BAD_GATEWAY` cuando la cátedra o Redis fallan.

6. **Calidad y limpieza**:
   - Runners de pruebas manuales movidos a `devtests` y deshabilitados.
   - Logs unificados y legibles.
   - Script de arranque `boot.sh` y configuración `.env` bien organizados.

Con todo esto, la **Etapa 3 queda completamente implementada**, dejando al `proxy-service` listo para:

- Servir de capa de integración única con la cátedra.
- Proteger el acceso con un esquema simple de Bearer token.
- Proveer información de eventos y estado de asientos al backend del alumno.
