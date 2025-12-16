# Issue #19 – Seguridad del Proxy + DNS Kafka  
## Resumen Explicativo Completo

Este issue resolvió **dos problemas críticos** del proxy-service:

1. **Seguridad real del proxy**, agregando un filtro personalizado que obliga a enviar un `Authorization: Bearer <token>` para acceder a cualquier endpoint bajo `/api/proxy/**`.
2. **Arreglo del DNS para Kafka**, corrigiendo el error `UnknownHostException: kafka` que impedía que el consumidor Kafka del proxy funcione correctamente.

Este Issues Cierra la Etapa 3 - Proxy (Integración con Kafka y Redis externo)

---

# 1. Archivos creados / modificados en el PROXY

## ✅ `ProxyTokenAuthFilter.java`
Filtro de seguridad **hecho a mano** que:
- Intercepta TODAS las requests a `/api/proxy/**`
- Exige un token Bearer presente en el header
- No valida JWT → solo chequea que exista
- Si falta → responde 403 con log `[Seguridad] Token ausente`
- Si está → genera autenticación simulada `proxy-user`

### Ejemplo de log real:
```
🛡️  [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
```

---

## ✅ `SecurityConfig.java`
Configuración de seguridad REAL del proxy:

Reglas aplicadas:
- `/api/proxy/**` → **authenticated()**
- `/actuator/**` → **permitAll()**
- Todo el resto → **denyAll()**
- CSRF desactivado
- Sin sesiones → `SessionCreationPolicy.STATELESS`
- Sin BasicAuth, sin FormLogin

### Resultado:
✔ Ya no aparece el mensaje de Spring Boot:  
**“Using generated security password”**

---

## ✅ `application-dev.yml`
Se ajustaron los niveles de log:

```yml
logging:
  level:
    root: INFO
    org.springframework.security: INFO
    ar.edu.um.proxyservice.config.ProxyTokenAuthFilter: DEBUG
    org.apache.kafka: WARN
```

Esto limpia la terminal y deja solo logs útiles.

---

## ✅ `/etc/hosts`
Se agregó:

```
192.168.194.250   kafka
```

Esto solucionó el problema donde el proxy intentaba contactar a `kafka:9092` y fallaba con:

```
UnknownHostException: kafka
coordinator unavailable
```

Tras corregirlo, Kafka se conecta sin errores.

---

# 2. Logs reales para mostrar al profesor

## 🟦 PROXY – Arranque limpio
```
DEBUG ProxyTokenAuthFilter : Filter 'proxyTokenAuthFilter' configured for use
INFO ProxyApplication : Started ProxyApplication in 2.367 seconds (process running for 2.703)
```

## 🟧 PROXY – Conexión Kafka correcta
```
INFO KafkaMessageListenerContainer : martin-nt-proxy: partitions assigned: [eventos-actualizacion-0]
```

## Comportamiento del Proxy con Kafka

Si Kafka recibe una notificación de la cátedra, el proxy registra:

```
📡 [Kafka] Mensaje recibido en eventos-actualizacion
```
- Esto viene del listener de Kafka del proxy.
- No tiene que ver directamente con la request de Postman, simplemente cayó un mensaje de Kafka en ese momento.
- puede aparecer intercalado cuando Kafka entrega mensajes.
- Después llama al backend (implementado en issue #20).
- No aparecen más errores de DNS.

---
---

# 3. Funcionamiento comprobado en Postman

## 🔐 **Probar seguridad del proxy**

### 1) Llamada CON token
```
En Postman: Proxy/Eventos/listar-eventos-completos
GET http://localhost:8081/api/proxy/eventos
En Header desactivar: Authorization: Bearer <token>
```
→ Respuesta: **200 OK**

### Logs reales:
```
📡 [Kafka] Mensaje recibido en eventos-actualizacion
🛡️ [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
🌐 [Proxy] GET /api/proxy/eventos
🎓 [Cátedra] Llamando a listarEventosCompletos vía Feign
🎓 [Cátedra] Respuesta listarEventosCompletos -> bodyLength=3584
```
“Con el token presente, el filtro deja pasar la request, el controller del proxy la recibe y luego el proxy llama a la cátedra vía Feign, devolviendo la lista real de eventos.”

### Explicación Logs
Filtro de seguridad ✅ → Controller del proxy ✅ → Llamada Feign a cátedra ✅ → Respuesta OK ✅

```
🛡️ [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
```
- Lo escribe ProxyTokenAuthFilter.
- Significa:
  - Llegó una request a /api/proxy/**.
  - Traía header Authorization: Bearer changeme.
  - Entonces el filtro autoriza la request y permite que siga.

```
🌐 [Proxy] GET /api/proxy/eventos
```
- Log del ProxyEventosResource.
- Muestra que el request ya pasó la seguridad y llegó al controller.
- Indica qué endpoint se está llamando.

```
🎓 [Cátedra] Llamando a listarEventosCompletos vía Feign
```
- Log de CatServiceClient / servicio que llama a la cátedra.
- Significa que el proxy, a su vez, está llamando al servidor remoto de la cátedra, reenviando la request.

```
🎓 [Cátedra] Respuesta listarEventosCompletos -> bodyLength=3584
```
- Llegó la respuesta de la cátedra.
- bodyLength=3584 te muestra cuántos bytes devolvió (sirve para ver que vino contenido real y no un error vacío).

### 2) Llamada SIN token  
```
En Postman: Proxy/Eventos/listar-eventos-completos
GET http://localhost:8081/api/proxy/eventos
En Header desactivar: Authorization: Bearer <token>
```
→ Respuesta: **403 Forbidden**

### Logs reales:
```
🛡️  [Seguridad] Acceso bloqueado a GET /api/proxy/eventos: falta header Authorization Bearer
```
“El filtro de seguridad del proxy detecta que falta el header Authorization Bearer y bloquea el acceso antes de llegar al controlador.”

- Lo escribe ProxyTokenAuthFilter.
- Detecta que NO está el header Authorization: Bearer ....
- Loguea que bloqueará el acceso a ese endpoint.
- Devuelve 403 Forbidden y no se llama a:
  - ProxyEventosResource
  - ni a Feign/cátedra
  - ni aparece ningún log [Proxy] ni [Cátedra].

---