# Issue #20 — Integración del Backend con el Proxy y Sincronización de Eventos  
**Issues:** #20  
Integración del backend con el proxy y sincronización local de eventos
**Etapa:** 4- Sincronización de datos (eventos y transacciones)

---

## 🎯 Objetivo

Integrar el **backend del alumno** con el **proxy-service**, para que el backend deje de usar datos locales y pase a consumir **eventos reales** provenientes de la cátedra, utilizando:

- llamadas HTTP al proxy,  
- sincronización completa de eventos,  
- actualización automática vía notificaciones Kafka → proxy → backend.

Este issue sienta las bases de toda la arquitectura real del proyecto.

---

# 📁 Archivos creados / modificados en el BACKEND

A continuación se listan uno por uno los archivos involucrados, explicando qué hace cada uno.

---

## 1️⃣ `ProxyWebClientConfig.java`  
**Ubicación:** `src/main/java/ar/edu/um/backend/config/`

### ✔️ ¿Qué hace?
- Configura un **WebClient** prearmado para llamar al proxy.
- Incluye automáticamente:
  - `baseUrl = PROXY_BASE_URL`
  - Header: `Authorization: Bearer <PROXY_TOKEN>`
- Evita repetir configuración en cada request.

### 🧩 Fragmento clave
```java
@Bean
public WebClient proxyWebClient() {
    return WebClient.builder()
        .baseUrl(proxyBaseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + proxyToken)
        .build();
}
```

---

## 2️⃣ `ProxyService.java`  
**Ubicación:** `src/main/java/.../service/`

### ✔️ ¿Qué hace?
Es el **cliente HTTP del backend** para comunicarse con el proxy.

Implementa métodos como:

| Método | URL consumida en el proxy | Para qué sirve |
|-------|-----------------------------|----------------|
| `listarEventosResumidos()` | `/eventos-resumidos` | Lista liviana de eventos |
| `listarEventosCompletos()` | `/eventos` | Lista completa para sincronizar |
| `obtenerEventoPorId(id)` | `/eventos/{id}` | Detalle de un evento |
| `forzarActualizacion()` | `/eventos/forzar-actualizacion` | Obligará al proxy a refrescar cache |

### ✔️ Logs agregados
```
🌐 [Proxy-Backend] GET /eventos
📩 [Proxy-Backend] Respuesta OK /eventos (bytes=3585)
```

Con esto se ve claramente que el backend sí está llamando al proxy.

---

## 3️⃣ `EventoSyncService.java`  
**Ubicación:** `src/main/java/.../service/`

### ✔️ ¿Qué hace?
Es el **corazón del issue**. Implementa:

- Obtener lista real de eventos desde el proxy.
- Convertir JSON → DTO → entidad local.
- Crear eventos nuevos.
- Actualizar eventos existentes.
- Guardar `externalId`.
- Mantener sincronizado el modelo local.

### ✔️ Logs importantes
```
🔄 [Sync-Eventos] Iniciando sincronización de eventos...
📥 [Sync-Eventos] Eventos recibidos: 5
🆕 [Sync-Eventos] Creando evento nuevo (externalId=1) → Conferencia Nerd!
♻️ [Sync-Eventos] Actualizando evento existente (id=1002, externalId=2)
💾 [DB] Evento guardado → idLocal=1001, externalId=1
```

---

## 4️⃣ `AdminSyncResource.java`  
**Endpoint expuesto:**  
```
POST /api/admin/sync-eventos
```

### ✔️ ¿Qué hace?
- Permite forzar la sincronización manualmente.
- Es accesible solo para ADMIN.
- Llama internamente a `EventoSyncService.sincronizarEventosDesdeProxy()`.

### ✔️ Ejemplo de request en Postman
```
POST http://localhost:8080/api/admin/sync-eventos
Authorization: Bearer <token-admin>
```

### ✔️ Log correspondiente
```
[Admin-Sync] Solicitud manual de sincronización de eventos.
```

---

## 5️⃣ Endpoint para recibir notificaciones desde el proxy  
Archivo: ProxyNotificationResource.java
Método: recibirNotificacionDesdeProxy()
Ruta: POST /api/proxy/notificacion-evento

### 📌 ¿Qué es este archivo?
ProxyNotificationResource es un controlador REST del backend del alumno cuyo único propósito es recibir las notificaciones que envía el proxy-service cada vez que llega un mensaje Kafka desde la cátedra.

Es decir: Cátedra → Kafka → Proxy → Backend

Este archivo representa el punto de entrada oficial para que el proxy le avise al backend:
“¡Los eventos cambiaron, actualizate!”.

### 📌 ¿Por qué existe? (Consigna oficial)

- En el PDF del enunciado, la cátedra define que:

“Cada vez que un evento sea modificado, la cátedra enviará una notificación por Kafka.
El proxy debe reenviar esa notificación al backend del alumno.”

- Y el backend:

“Debe actualizar sus datos locales al recibir esa notificación.”

Este archivo cumple exactamente esa responsabilidad.

### 📌 Qué hace este endpoint

- Cuando el proxy recibe un mensaje Kafka (eventos-actualizacion), llama automáticamente a este endpoint:

POST /api/proxy/notificacion-evento

Al entrar este método:

- Registra un log indicando que llegó la notificación.
- Invoca a EventoSyncService para que el backend:
  - vuelva a consultar al proxy los eventos reales,
  - actualice en la base local,
  - agrege nuevos eventos si aparecieron,
  - modifique los existentes,
  - marque como inactivos los que desaparecieron.
  - Devuelve HTTP 200 al proxy confirmando que el backend recibió la notificación.

En otras palabras:
- Este endpoint mantiene sincronizada la base del alumno con la cátedra en tiempo real.

### ✔️ Log
```
[Proxy-Backend] Notificación recibida → disparando nueva sincronización
```
Interpretación:
- [Proxy-Backend] → Este prefijo indica que el log proviene de la integración backend ↔ proxy.
- Notificación recibida → El proxy te está avisando que la cátedra cambió algo.
- Disparando nueva sincronización → El backend está iniciando una sincronización completa.



# ✅ Criterios de aceptación — TODOS CUMPLIDOS

| Requisito | Estado |
|----------|--------|
| WebClient configurado | ✔️ |
| ProxyService implementado | ✔️ |
| Sincronización completa de eventos | ✔️ |
| Endpoint para admin | ✔️ |
| Endpoint de notificación del proxy | ✔️ |
| Logs profesionales | ✔️ |
| Backend ya no usa datos locales | ✔️ |

---


