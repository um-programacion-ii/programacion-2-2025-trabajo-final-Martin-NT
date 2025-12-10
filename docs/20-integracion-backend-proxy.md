# Integración del backend con el proxy y sincronización local de eventos  
### Issue #20 – Backend JHipster

## 📌 Objetivo  
Implementar en el backend del alumno la **integración completa con el proxy-service**, permitiendo:

- Consultar los eventos reales provenientes de la cátedra (vía proxy).
- Sincronizar periódicamente la base de datos local con los datos remotos.
- Recibir notificaciones del proxy cuando Kafka detecte cambios en los eventos.
- Eliminar usos de datos “mock” y comenzar a trabajar únicamente con datos reales.

---

## 🧩 Componentes creados / modificados

Este issue introdujo **todas las piezas necesarias** para que el backend pueda consumir el proxy y mantener la base local actualizada.

---

# 1️⃣ Configuración del cliente HTTP (WebClient)

### **Archivo:** `ProxyProperties.java`  
Define dos propiedades externas:

- `proxy.base-url` → URL del proxy (`http://localhost:8081/api/proxy`)
- `proxy.token` → Token JWT para autenticar llamadas desde el backend al proxy

Estas variables se obtienen desde `.env` mediante:

```env
PROXY_BASE_URL=http://localhost:8081/api/proxy
PROXY_TOKEN=<jwt-backend>
```

---

### **Archivo:** `ProxyWebClientConfig.java`

Crea un **WebClient preconfigurado** con:

- Base URL del proxy
- Header: `Authorization: Bearer <PROXY_TOKEN>`
- Logs informativos al inicializarse

Es el cliente HTTP oficial del backend para hablar con el proxy.

---

# 2️⃣ Servicio de acceso al proxy

### **Archivo:** `ProxyService.java`

Engloba todas las llamadas HTTP al proxy mediante un método privado `doGet()`.

Expone:

- `listarEventosResumidos()`
- `listarEventosCompletos()`
- `obtenerEventoPorId(Long id)`
- `forzarActualizacion()`

Incluye logs claros:

- ✔️ `🌐 [Proxy-Backend] GET /eventos`
- ✔️ `📩 [Proxy-Backend] Respuesta OK /eventos (bytes=1833)`
- ❌ Manejo de errores con logs

Este servicio NO transforma datos, solo obtiene JSON crudo.

---

# 3️⃣ DTO para representar la data que llega desde el proxy

### **Archivo:** `ProxyEventoDTO.java`

Modelo interno usado exclusivamente para **deserializar el JSON remoto**:

Incluye:  
`id`, `titulo`, `descripcion`, `fecha`, `hora`, `filaAsientos`, `columnaAsientos`, etc.

Permite convertir el JSON remoto en objetos Java manejables.

---

# 4️⃣ Sincronización de eventos locales

### **Archivo:** `EventoSyncService.java`

Es el core del issue.

Realiza:

### ✔️ **Obtención de datos reales**
Llama a `proxyService.listarEventosCompletos()` → recibe JSON de eventos.

### ✔️ **Conversión a DTOs remotos**
```java
ProxyEventoDTO[] remotos = objectMapper.readValue(json, ProxyEventoDTO[].class);
```

### ✔️ **Crear o actualizar eventos locales**
- Busca un evento mediante `externalId`
- Si no existe → lo crea
- Si existe → actualiza campos
- Corrige valores faltantes (ej. hora nula → 00:00)
- Calcula `cantidadAsientosTotales = filas * columnas`

Ejemplo de logs agregados:

- `🆕 [Sync] Creando evento nuevo`
- `♻️ [Sync] Actualizando evento existente`
- `⚠️ [Sync] Evento sin información completa`
- `💾 [DB] Evento guardado`
- `✅ [Sync] Sincronización finalizada`

---

# 5️⃣ Repositorio para trabajo con la BD

### **Archivo:** `EventoRepository.java`

Método clave añadido:

```java
Optional<Evento> findByExternalId(Long externalId);
```

Permite vincular el evento local con el ID real de la cátedra.

---

# 6️⃣ Endpoint administrativo para forzar sincronización

### **Archivo:** `AdminSyncResource.java`

Expone:

```
POST /api/admin/sync-eventos
```

✔️ Protegido por rol `ADMIN`  
✔️ Llama internamente a `EventoSyncService.sincronizarEventosDesdeProxy()`  
✔️ Se usa desde Postman para disparar sincronización manual  

Logs:

- `[Admin-Sync] Solicitud manual de sincronización de eventos.`
- `[Admin-Sync] Sincronización manual finalizada.`

---

# 7️⃣ Endpoint para notificaciones desde el proxy (Kafka → proxy → backend)

### **Archivo:** `ProxyNotificationResource.java`

Expone:

```
POST /api/proxy/notificacion-evento
```

✔️ Llamado exclusivamente por el proxy cuando Kafka detecta cambios  
✔️ Recibe body opcional  
✔️ Loguea la notificación  
✔️ Vuelve a llamar a `EventoSyncService.sincronizarEventosDesdeProxy()`

Ejemplo real en logs:

```
[Proxy-Backend] Notificación recibida desde proxy
[Proxy-Backend] Body: { "eventoId": 1, "origen": "postman-test" }
```

---

# 8️⃣ Loggers mejorados en todos los componentes

Todos los logs del issue siguen un esquema consistente:

- `[Proxy-Backend]` → llamadas al proxy  
- `[Sync]` → parte de sincronización  
- `[DB]` → guardado de datos locales  
- `[Admin-Sync]` → endpoint administrativo  

Además incluyen:
- Emojis para mejorar lectura técnica  
- Mensajes claros y didácticos (útiles para el profe ✨)

---

# ✔️ Criterios de aceptación cumplidos

| Requisito | Estado |
|----------|--------|
| Backend obtiene lista real de eventos desde el proxy | ✔️ |
| Servicio de sincronización creado | ✔️ |
| Crear/actualizar eventos locales | ✔️ |
| Usar externalId como clave remota | ✔️ |
| Endpoint admin para sincronizar | ✔️ |
| Endpoint para notificaciones desde proxy/Kafka | ✔️ |
| Logs claros y profesionales | ✔️ |
| Backend deja de usar mocks | ✔️ |

---

## 🎯 Resultado final

El backend quedó totalmente integrado con:

- **El proxy-service**
- **El servidor real de la cátedra**
- **Kafka (vía notificaciones del proxy)**

Tu base local ahora se mantiene:

- Sincronizada  
- Actualizada automáticamente  
- Cohesiva con los datos reales del servidor cátedra  

Este issue marca el final de toda la infraestructura de integración.
