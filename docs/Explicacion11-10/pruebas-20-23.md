
# 🧪 Pruebas del Issues #20 al #23 (Etapa 4 - Sincronización de datos (eventos y transacciones))

A continuación se detallan las pruebas **reales** que podés mostrar en vivo desde Postman.
- Levantar Backend con ./mvnw
- Levantar Proxy con ./boot.sh

## Issues
- Integración del backend con el proxy y sincronización local de eventos #20
- Baja lógica de eventos eliminados en la cátedra #21
- Sincronización de asientos del evento #22
- Integración con Redis para estado de asientos en tiempo real #23

---

## 🔹 1. Probar sincronización manual  (#20 - #21 - #22)

### En Postman  
- Backend/Admin/admin-sync-evento
- http://localhost:8080/api/admin/sync-eventos
- Devuelve: 204 No Content

### Que hace esta prueba?
Esta prueba demuestra todo el flujo de sincronización completa:

- Que el endpoint admin del backend existe y funciona: POST /api/admin/sync-eventos (#20).
- Que el backend llama al proxy, el proxy llama a la cátedra, y el backend:
    - trae los eventos reales,
    - los guarda/actualiza en PostgreSQL (Issue #20),
    - aplica reglas como la hora por defecto cuando falta (Issue #20),
    - y maneja el campo externalId y la lógica de activos/inactivos (Issue #21).

- Que, por cada evento, se dispara la sincronización de asientos:
    - el backend llama al proxy /eventos/{id}/asientos,
    - el proxy consulta Redis remoto,
    - el backend borra los asientos locales previos y regenera toda la matriz de asientos según lo que envía la cátedra (Issue #22).

En resumen: con una sola llamada desde Postman estás probando que:
1. backend ↔ proxy ↔ cátedra están bien conectados.
2. Se sincronizan eventos.
3. Se sincronizan asientos.
4. Y que todo queda persistido en mi propia base de datos.

### Terminal del Backend
```
DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
DispatcherServlet        : Completed initialization in 1 ms
AdminSyncResource        : [Admin-Sync] Solicitud manual de sincronización de eventos.
EventoSyncService        : 🔄 [Sync-Eventos] Iniciando sincronización de eventos contra proxy...
ProxyService             : 🌐 [Proxy-Backend] GET /eventos
ProxyService             : 📩 [Proxy-Backend] Respuesta OK /eventos (bytes=3585)
EventoSyncService        : 📥 [Sync-Eventos] Eventos recibidos desde proxy: 5 evento(s).

EventoSyncService        : ♻️ [Sync-Eventos] Actualizando evento existente (id=1001, externalId=1) → Conferencia Nerd!
EventoSyncService        : ⚠️ [Sync-Eventos] El evento 1 no tiene hora en el proxy. Se asigna 00:00.
EventoSyncService        : 💾 [DB] Evento guardado → idLocal=1001, externalId=1, titulo=Conferencia Nerd!
AsientoSyncService       : 🔄 [Sync-Asientos] Iniciando sincronización de asientos para evento local id=1001 (externalId=1)
ProxyService             : 🌐 [Proxy-Backend] GET /eventos/1/asientos
ProxyService             : 📩 [Proxy-Backend] Respuesta OK /eventos/1/asientos (bytes=2177)
AsientoSyncService       : 🧹 [Sync-Asientos] Asientos previos eliminados para evento idLocal=1001 → 0 asiento(s) borrado(s).
AsientoSyncService       : ✅ [Sync-Asientos] Evento idLocal=1001 (externalId=1) → Asientos sincronizados: 38 creados, 0 actualizados.

EventoSyncService        : ✅ [Sync-Eventos] Sincronización de eventos finalizada correctamente.
AdminSyncResource        : [Admin-Sync] Sincronización manual finalizada.

```

### Explicación Logs Backend

#### Versión resumida

Cuando llamo a /api/admin/sync-eventos se dispara EventoSyncService, que llama al proxy, trae 5 eventos reales y los guarda en Postgres. 

Después, por cada evento, se dispara AsientoSyncService, que va al proxy, lee los asientos de Redis y regenera toda la matriz de asientos local. 

Si el proxy no tiene asientos para un evento, limpio los locales y lo dejo sin asientos. 

#### 🟦 Arranque del endpoint admin y cierre de de la sincronización (#20)

- **DispatcherServlet**: Spring inicializa el controlador REST que va a atender la request. (Infraestructura, no es de un issue puntual.)
- **AdminSyncResource**: Entró al endpoint POST /api/admin/sync-eventos. (prueba que el endpoint administrativo existe y está funcionando)
```
DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
DispatcherServlet        : Completed initialization in 1 ms
AdminSyncResource        : [Admin-Sync] Solicitud manual de sincronización de eventos.

EventoSyncService        : ✅ [Sync-Eventos] Sincronización de eventos finalizada correctamente.
AdminSyncResource        : [Admin-Sync] Sincronización manual finalizada.
```

#### 🟩 Inicio de la sincronización de eventos (#20)

- **🔄 [Sync-Eventos]**: El servicio de sync arranca y marca el inicio del flujo backend → proxy.
- **🌐 [Proxy-Backend]**: El backend llama al proxy (GET /api/proxy/eventos) usando el WebClient configurado.
- **📩 [Proxy-Backend]**: El proxy respondió OK con ~3.5 KB de eventos reales. Demuestra que el proxy está levantado, la URL y el token son correctos.
- **📥 [Sync-Eventos]**: Se deserializa el JSON y se cuenta cuántos eventos llegaron.
```
EventoSyncService        : 🔄 [Sync-Eventos] Iniciando sincronización de eventos contra proxy...
ProxyService             : 🌐 [Proxy-Backend] GET /eventos
ProxyService             : 📩 [Proxy-Backend] Respuesta OK /eventos (bytes=3585)
EventoSyncService        : 📥 [Sync-Eventos] Eventos recibidos desde proxy: 5 evento(s).
```

#### 🟨 Actualización de cada evento + reglas especiales (#20 - #21)
- **♻️ [Sync-Eventos]**: El evento ya existía en la BD, así que se actualiza en lugar de crearlo.
- **⚠️ [Sync-Eventos]**: Regla de negocio: si la cátedra no manda hora, se usa un valor por defecto (00:00). Sirve para mostrar que el backend no se rompe con datos incompletos.
- **💾 [DB] Evento guardado**: Confirma que el evento se guardo (persistió) en PostgreSQL.
```
EventoSyncService        : ♻️ [Sync-Eventos] Actualizando evento existente (id=1001, externalId=1) → Conferencia Nerd!
EventoSyncService        : ⚠️ [Sync-Eventos] El evento 1 no tiene hora en el proxy. Se asigna 00:00.
EventoSyncService        : 💾 [DB] Evento guardado → idLocal=1001, externalId=1, titulo=Conferencia Nerd!
```
👉 Este mismo patrón se repite para los eventos 2, 3, 4 y 5.

#### 🟧 Sincronización de asientos por cada evento (#22)
- **🔄 [Sync-Asientos]**: Al terminar de guardar el evento, se dispara la sincronización de sus asientos.
- **🌐 [Proxy-Backend]**: El backend llama al proxy para obtener el estado de asientos de ese evento.
- **📩 [Proxy-Backend]**: El proxy respondió OK y llega el JSON de asientos desde Redis (vía proxy).
- **🧹 [Sync-Asientos]**: Se borran los asientos locales previos del evento (regeneración completa). Acá fueron 0 porque era la primera vez.
- **✅ [Sync-Asientos]**: Se recreó la matriz de asientos en base a lo que devolvió la cátedra.
Demuestra:
- que el JSON remoto se parseó bien (ProxyEstadoAsientosResponse / ProxyAsientoDTO),
- que se persistieron asientos en Postgres.
```
AsientoSyncService       : 🔄 [Sync-Asientos] Iniciando sincronización de asientos para evento local id=1001 (externalId=1)
ProxyService             : 🌐 [Proxy-Backend] GET /eventos/1/asientos
ProxyService             : 📩 [Proxy-Backend] Respuesta OK /eventos/1/asientos (bytes=2177)
AsientoSyncService       : 🧹 [Sync-Asientos] Asientos previos eliminados para evento idLocal=1001 → 0 asiento(s) borrado(s).
AsientoSyncService       : ✅ [Sync-Asientos] Evento idLocal=1001 (externalId=1) → Asientos sincronizados: 38 creados, 0 actualizados.
```

#### 🟪 Casos donde el proxy devuelve lista vacía (Issue #22)
- **ℹ️ [Sync-Asientos]**: El proxy respondió correctamente, pero con lista vacía de asientos.
- **⚠️ [Sync-Asientos]**: Regla de negocio: si la cátedra no tiene asientos para ese evento, tu backend limpia los que pudiera tener.
```
AsientoSyncService : ℹ️ [Sync-Asientos] El proxy devolvió lista vacía de asientos para eventoId=X
AsientoSyncService : ⚠️ [Sync-Asientos] Lista de asientos vacía para externalId=X. Se eliminarán asientos locales del evento.
```

### Terminal del Proxy
Se repite siempre el mismo patrón para cada request:
Seguridad → Controller del proxy → Servicio que habla con cátedra o Redis.
```
ProxyTokenAuthFilter      : 🛡️ [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
ProxyEventosResource      : 🌐 [Proxy] GET /api/proxy/eventos
CatServiceClient          : 🎓 [Cátedra] Llamando a listarEventosCompletos vía Feign
CatServiceClient          : 🎓 [Cátedra] Respuesta listarEventosCompletos -> bodyLength=3585

ProxyTokenAuthFilter      : 🛡️ [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
ProxyEventosResource      : 🌐 [Proxy] GET /api/proxy/eventos/1/asientos
EstadoAsientosRedisService: Consultando Redis para key=evento_1, resultado=ENCONTRADO
EstadoAsientosRedisService: Se parseó correctamente estado de asientos para eventoId=1 (38 asientos).

ProxyTokenAuthFilter      : 🛡️ [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
ProxyEventosResource      : 🌐 [Proxy] GET /api/proxy/eventos/2/asientos
EstadoAsientosRedisService: Consultando Redis para key=evento_2, resultado=NO ENCONTRADO
EstadoAsientosRedisService: No hay asientos bloqueados/vendidos para eventoId=2 (key evento_2). Devolviendo lista vacía.
...

```
#Issue19
**🛡️ [Seguridad]** Token Bearer presente (parcial=changeme , longitud=8)
- Lo escribe ProxyTokenAuthFilter.
- Significa:
  - Llegó una request a /api/proxy/**.
  - Traía header Authorization: Bearer changeme.
  - Entonces el filtro autoriza la request y permite que siga.

**🌐 [Proxy]** GET /api/proxy/eventos o GET /api/proxy/eventos/1/asientos
- Log del ProxyEventosResource.
- Muestra que el request ya pasó la seguridad y llegó al controller.
- Indica qué endpoint se está llamando.

**EstadoAsientosRedisService**: Consultando Redis para key=evento_1, resultado=ENCONTRADO
**EstadoAsientosRedisService**: Se parseó correctamente estado de asientos para eventoId=1 (38 asientos).

- Arma la key evento_1.
- Consulta al Redis remoto de la cátedra.
- Loguea que la key fue encontrada (resultado=ENCONTRADO),
- Parsea el JSON de Redis → lista de asientos (38 asientos).

**EstadoAsientosRedisService**: Consultando Redis para key=evento_2, resultado=NO ENCONTRADO
**EstadoAsientosRedisService**: No hay asientos bloqueados/vendidos para eventoId=2 (key evento_2). Devolviendo lista vacía.

- Para evento_2 no existe key en Redis.
- El servicio lo interpreta como que no hay asientos bloqueados/vendidos para ese evento.
- Responde una lista vacía al backend.
- proxy dice “no tengo nada” → backend deja al evento sin asientos locales.

📌 Y eso enlaza perfecto con lo que viste en los logs del backend:
ℹ️ [Sync-Asientos] El proxy devolvió lista vacía...
⚠️ [Sync-Asientos] Lista vacía... Se eliminarán asientos locales...

**🎓 [Cátedra]** Llamando a listarEventosCompletos vía Feign
- Log de CatServiceClient / servicio que llama a la cátedra.
- Significa que el proxy, a su vez, está llamando al servidor remoto de la cátedra, reenviando la request.

**🎓 [Cátedra]** Respuesta listarEventosCompletos -> bodyLength=3584
- Llegó la respuesta de la cátedra.
- bodyLength=3584 te muestra cuántos bytes devolvió (sirve para ver que vino contenido real y no un error vacío).


---



## 🔹 2. Consultar eventos locales activos después de sincronizar  (#20 - #21 - #23)

### En Postman
- Backend/Evento/eventos-locales
- http://localhost:8080/api/eventos
- Devuelve: 200 OK
- JSON:
```json
[
    {
        "id": 1,
        "titulo": "but never ouch",
        "descripcion": "excepting considering brr",
        "fecha": "2025-11-13",
        "hora": "21:40",
        "organizador": "revitalise efface lounge",
        "presentadores": "bleakly culture taro",
        "cantidadAsientosTotales": 27997,
        "filaAsientos": 3968,
        "columnaAsientos": 6521,
        "activo": true
    },
    {
        "id": 1002,
        "titulo": "Otra Conferencia Nerd",
        "descripcion": "Esta es una conferencia de prueba para verificar que los datos están correctos version 2",
        "fecha": "2025-12-28",
        "hora": "00:00",
        "organizador": null,
        "presentadores": null,
        "cantidadAsientosTotales": 160,
        "filaAsientos": 20,
        "columnaAsientos": 8,
        "activo": true
    },
]
```
### Que hace esta prueba?
- Explicacion Corta: 

Acá muestro los eventos que quedaron grabados en mi DB (Postgres) después de hablar con el proxy.
Solo aparecen los que están activos, y además puede ver las filas/columnas de asientos que después uso para validar lo que llega desde Redis.

---

Esta prueba muestra cómo quedó la base local después de la sincronización de la prueba anterior:
- Lista todos los eventos activos que están guardados en PostgreSQL.
    - Esto valida:
        - Que los eventos realmente se guardan/actualizan en tu BD. (#20)
        - Que el backend solo expone eventos activo = true. (#21)

- Te permite ver para cada evento sus atributos: id local (ej: 1002), titulo, descripcion, fecha, hora, filaAsientos y columnaAsientos
👉 Estos valores son clave para el Issue #23, porque se usan para validar que los asientos que vengan de Redis estén dentro del rango permitido (1..filaAsientos, 1..columnaAsientos).

- Si en algún momento la cátedra elimina un evento:
    - la próxima sync lo marcará como activo = false (Issue #21),
    - y no aparecerá más en este listado /api/eventos.


### Terminal del Backend

- Indica que el backend está devolviendo la lista de eventos activos
```
INFO EventoResource   : [EventoResource] GET /api/eventos (devolviendo solo eventos activos) 
```


---


## 🔹 3. Simular notificación desde proxy  (#20)

### En Postman 
- Backend/Notificaciones-Proxy/proxy-notificacion-evento
- http://localhost:8080/api/proxy/notificacion-evento
- JSON que se le pasa:
👉 Se envía este JSON solo para simular lo que mandaría el proxy cuando recibe un mensaje de Kafka:
```json
{
  "eventoId": 123,
  "origen": "postman-test"
}
```
- Devuelve: 200 OK
- JSON que devuelve:
👉 Este JSON es un ACK del backend:
- Confirma que recibió la notificación, y que disparó internamente la sincronización de eventos/asientos.
```json
{
    "status": "ok",
    "mensaje": "Notificación procesada y sincronización disparada"
}
```

### Que hace esta prueba?

#### Explicación Corta

Simula la notificación que enviaría el proxy cuando Kafka detecta un cambio.
El backend recibe este JSON, lo loguea y dispara exactamente la misma sincronización que el endpoint /api/admin/sync-eventos.
La respuesta status: ok confirma que la notificación fue procesada y que la sync se ejecutó.

#### Explicación un poco más completa

1. Postman llama a /api/proxy/notificacion-evento con un JSON que representa la notificación.
2. El backend:

- Loguea que recibió la notificación y su contenido.
- Llama a EventoSyncService.sincronizarEventosDesdeProxy(),
- Que a su vez:
    - Llama al proxy (GET /api/proxy/eventos).
    - Actualiza eventos en PostgreSQL.
    - Y dispara AsientoSyncService para sincronizar los asientos de cada evento.

3. En los logs ves todo ese flujo, igual que en la sincronización manual, pero esta vez disparado por una notificación externa.

### Terminal del Backend
- **ProxyNotificationResource: [Proxy-Backend]**: Confirma que el endpoint /api/proxy/notificacion-evento existe y está activo.
- **ProxyNotificationResource: [Proxy-Backend]**: Loguea que recibió la notificación y el JSON qué mandó el proxy (o en este caso, Postman).

- **A partir de acá, el flujo es igual que en la prueba 1**:
- EventoSyncService arranca la sincronización.
- ProxyService llama al proxy para traer los eventos.
- Se actualizan eventos en la BD y luego se sincronizan los asientos para cada externalId:
     - se borran los asientos viejos,
     - se crean de nuevo según la info real que devuelve la cátedra (vía proxy y Redis).

**La diferencia clave con la prueba 1 es**:
- En la prueba 1 (Probar sincronización manual) la sync se dispara con /api/admin/sync-eventos.
- En esta prueba se dispara con una notificación de proxy (/api/proxy/notificacion-evento).

- Esto demuestra que el ciclo Kafka → Proxy → Backend está listo.

```
DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
DispatcherServlet        : Completed initialization in 1 ms

ProxyNotificationResource: [Proxy-Backend] Notificación recibida desde proxy en /api/proxy/notificacion-evento
ProxyNotificationResource: [Proxy-Backend] Body de la notificación: {_  "eventoId": 1,_  "origen": "postman-test"_}

EventoSyncService        : 🔄 [Sync-Eventos] Iniciando sincronización de eventos contra proxy...
ProxyService             : 🌐 [Proxy-Backend] GET /eventos
ProxyService             : 📩 [Proxy-Backend] Respuesta OK /eventos (bytes=3584)
EventoSyncService        : 📥 [Sync-Eventos] Eventos recibidos desde proxy: 5 evento(s).

EventoSyncService        : ♻️ [Sync-Eventos] Actualizando evento existente (id=1001, externalId=1) → Conferencia Nerd
EventoSyncService        : ⚠️ [Sync-Eventos] El evento 1 no tiene hora en el proxy. Se asigna 00:00.
EventoSyncService        : 💾 [DB] Evento guardado → idLocal=1001, externalId=1, titulo=Conferencia Nerd
AsientoSyncService       : 🔄 [Sync-Asientos] Iniciando sincronización de asientos para evento local id=1001 (externalId=1)
ProxyService             : 🌐 [Proxy-Backend] GET /eventos/1/asientos
ProxyService             : 📩 [Proxy-Backend] Respuesta OK /eventos/1/asientos (bytes=2177)
AsientoSyncService       : 🧹 [Sync-Asientos] Asientos previos eliminados para evento idLocal=1001 → 38 asiento(s) borrado(s).
AsientoSyncService       : ✅ [Sync-Asientos] Evento idLocal=1001 (externalId=1) → Asientos sincronizados: 38 creados, 0 actualizados.

... (mismo patrón para eventos 1002, 1003, 1004, 1005) ...

EventoSyncService        : ✅ [Sync-Eventos] Sincronización de eventos finalizada correctamente.

```

### Terminal del Proxy

- Cuando llega una notificación (simulada desde Postman), el backend dispara una sync completa, y el proxy vuelve a hablar con la cátedra y Redis para refrescar todo.
(misma explicacion que en la prueba 1)
```
ProxyTokenAuthFilter      : 🛡️  [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
ProxyEventosResource      : 🌐 [Proxy] GET /api/proxy/eventos
CatServiceClient          : 🎓 [Cátedra] Llamando a listarEventosCompletos vía Feign
CatServiceClient          : 🎓 [Cátedra] Respuesta listarEventosCompletos -> bodyLength=3584
ProxyTokenAuthFilter      : 🛡️  [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
ProxyEventosResource      : 🌐 [Proxy] GET /api/proxy/eventos/1/asientos
EstadoAsientosRedisService: Consultando Redis para key=evento_1, resultado=ENCONTRADO
EstadoAsientosRedisService: Se parseó correctamente estado de asientos para eventoId=1 (38 asientos).
ProxyTokenAuthFilter      : 🛡️  [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
ProxyEventosResource      : 🌐 [Proxy] GET /api/proxy/eventos/2/asientos
EstadoAsientosRedisService: Consultando Redis para key=evento_2, resultado=ENCONTRADO
EstadoAsientosRedisService: Se parseó correctamente estado de asientos para eventoId=2 (4 asientos).
... (mismo patrón para eventos 3, 4 y 5) ...
```



---



## 🔹 4. Ver los asientos remotos en el proxy  (#22)
Prueba en Proxy
### En Postman
- Proxy/Asientos/asientos-por-evento
- GET http://localhost:8081/api/proxy/eventos/1/asientos
- Devuelve: 
- JSON que devuelve:
```json
{
    "asientos": [
        {
            "columna": 3,
            "estado": "Vendido",
            "expira": null,
            "fila": 2
        }, // ... otros asientos ...
    ],
    "eventoId": 1
}
```
**👉 ¿Por qué se ve este JSON así?**
- **eventoId**: el ID REAL en la cátedra (externalId = 1).
- **asientos**: es la lista tal como la tiene Redis para ese evento:
- **fila / columna**: posición del asiento.
- **estado**: "Vendido", "Bloqueado", "Libre", etc. (formato de la cátedra).
- **expira**: fecha/hora ISO cuando vence el bloqueo, o null si está vendido o libre.

Este formato es el que después usa mi backend con los DTO:
- ProxyEstadoAsientosResponse
- ProxyAsientoDTO

### Que hace esta prueba?

### Versión corta
Llama directo al proxy para ver qué está leyendo de Redis para el evento 1.
Esto me muestra el estado de los asientos en bruto (tal cual lo entrega la cátedra vía Redis), antes de que el backend los sincronice o los combine con la base local.”
- Esto demuestra que el proxy sí está leyendo Redis.

## Versión un poco más completa

- Comprueba que:
    - el proxy está levantado,
    - la seguridad funciona (pide Bearer),
    - el proxy arma la key evento_1,
    - consulta el Redis remoto de la cátedra,
    - y logra parsear el JSON a un objeto bien formado (eventoId + asientos[]).
- Es una prueba “unitaria” del proxy:
- Todavía no entra en juego AsientoSyncService ni la combinación con PostgreSQL, eso se ve en la siguiente prueba 5.

### Terminal del Backend
Esta prueba no pasa por el backend, solo habla Postman → Proxy → Redis (cátedra).

### Terminal del Proxy
- Con esta prueba le muestro el eslabón directo Proxy ↔ Redis.
- Todavía no interviene el backend, es solo el proxy devolviendo tal cual lo que Redis dice que pasa en ese evento.

```bash
2025-12-11T16:56:38.740-03:00 DEBUG 188156 --- [proxy-service] [nio-8081-exec-2] a.e.u.p.config.ProxyTokenAuthFilter      : 🛡️  [Seguridad] Token Bearer presente (parcial=eyJhbGciOiJI... , longitud=228)
2025-12-11T16:56:38.759-03:00  INFO 188156 --- [proxy-service] [nio-8081-exec-2] a.e.u.p.web.rest.ProxyEventosResource    : 🌐 [Proxy] GET /api/proxy/eventos/1/asientos
2025-12-11T16:56:39.979-03:00  INFO 188156 --- [proxy-service] [nio-8081-exec-2] a.e.u.p.s.EstadoAsientosRedisService     : Consultando Redis para key=evento_1, resultado=ENCONTRADO
2025-12-11T16:56:40.018-03:00  INFO 188156 --- [proxy-service] [nio-8081-exec-2] a.e.u.p.s.EstadoAsientosRedisService     : Se parseó correctamente estado de asientos para eventoId=1 (38 asientos).
```



---




## 🔹 5. Ver los asientos locales ya sincronizados  (#22 - #23)

### En Postman
- Backend/Estado de Asientos (Tiempo Real)/estado-asientos-tiempo-real
- GET http://localhost:8080/api/eventos/1001/asientos
- Devuelve: 200 OK
- JSON que devuelve:
```json
[
    {
        "fila": 1,
        "columna": 1,
        "estado": "VENDIDO",
        "expiraEn": null
    }, // ... otros asientos ...
]
```

**👉 Qué muestra**:
Esta prueba devuelve el mapa final de asientos que va al frontend, ya procesado por el backend:

- Cada elemento es un AsientoEstadoDTO (uno por posición Fila/Columna).
- El estado de cada asiento ya viene combinado así:
    - Si en la DB (Postgres) está VENDIDO → se muestra VENDIDO y se ignora Redis.
    - Si NO está vendido y Redis dice que está bloqueado vigente → se muestra BLOQUEADO_VIGENTE con expiraEn completo.
    - Si Redis tiene un bloqueo PERO ya venció → se considera LIBRE en el mapa final.
    - Si Redis manda asientos inválidos o fuera de rango → no se muestran, solo quedan logueados como advertencia.

Resultado de todo el flujo:
- sync de asientos (#22),
- combinación en tiempo real con Redis (#23),
- validaciones de integridad (rangos y expiraciones).

### Que hace esta prueba?

#### Versión corta

Entra por el backend al endpoint que usa el frontend, y veo el mapa de asientos final, que combina lo que está en la DB (Postgres) con el estado en tiempo real que viene de Redis, aplicando las reglas de vendido, bloqueos vigentes, expirados y validaciones de rango.

#### Versión más detallada

1. El backend recibe la request GET /api/eventos/1001/asientos.
2. Carga los asientos persistidos en Postgres para el evento local id=1001.
3. Llama al proxy para obtener el estado dinámico en Redis:
GET /eventos/1/estado-asientos (donde 1 es el externalId de la cátedra).
4. AsientoEstadoService:
    - combina DB + Redis,
    - aplica las reglas:
        - VENDIDO manda sobre Redis,
        - bloqueo vigente / expirado,
    - aplica las validaciones de fila/columna,
    - arma la lista de AsientoEstadoDTO.
5. Te devuelve un JSON limpio, listo para que el frontend pinte la grilla.

### Terminal del Backend

- **[EventoResource]**:  Confirma que está llamando al endpoint correcto del backend: el que devuelve el mapa combinado para el evento 1001.
Este método internamente usa AsientoEstadoService.

- **🌐 [Proxy-Backend]**: El backend llama al proxy, pero ahora al endpoint nuevo de estado en tiempo real (no al de sincronización completa).
Usa el externalId=1 del evento en la cátedra.

- **📩 [Proxy-Backend]**: El proxy respondió bien. Llegó un JSON con estado de asientos desde Redis.

- **AsientoEstadoService: ⚠️ [Redis]**: Estos logs vienen de AsientoEstadoService y prueban que la validación de rangos está funcionando.

Se está evaluando cada asiento que vino en el JSON de Redis con:

boolean invalido =
    redis.getFila() == null || redis.getColumna() == null ||      // faltan datos
    redis.getFila() <= 0 || redis.getColumna() <= 0 ||            // fila/col <= 0
    redis.getFila() > evento.getFilaAsientos() ||                 // fila > filas totales del evento
    redis.getColumna() > evento.getColumnaAsientos();             // col > columnas totales del evento

En tu caso, el evento tiene columnas/filas fuera de rango, por eso se loguean con ⚠️, se descartan y no se incluyen en el resultado enviado al frontend.

**“Si Redis manda basura (asientos con fila/columna incoherente para este evento), el backend no se rompe: los detecta, los loguea como inválidos y no los mezcla en el mapa final.”**

```bash
EventoResource           : [EventoResource] GET /api/eventos/1001/asientos (mapa en tiempo real)
ProxyService             : 🌐 [Proxy-Backend] GET /eventos/1/estado-asientos
ProxyService             : 📩 [Proxy-Backend] Respuesta OK /eventos/1/estado-asientos (bytes=2177)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (2, 7): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (5, 11): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (20, 20): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (21, 21): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (22, 22): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (23, 23): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (25, 25): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (26, 26): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (27, 27): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (28, 28): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (29, 29): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
AsientoEstadoService     : ⚠️  [Redis] Asiento remoto inválido (30, 30): fuera de rango para evento idLocal=1001 (filas 1-10, columnas 1-6)
```

### Terminal del Proxy
```bash
ProxyTokenAuthFilter      : 🛡️  [Seguridad] Token Bearer presente (parcial=changeme , longitud=8)
ProxyEventosResource      : 🌐 [Proxy] GET /api/proxy/eventos/1/estado-asientos
EstadoAsientosRedisService: Consultando Redis para key=evento_1, resultado=ENCONTRADO
EstadoAsientosRedisService: Se parseó correctamente estado de asientos para eventoId=1 (38 asientos).
```



---



### 🔹 6. Ver estado de asientos desde el proxy (Redis) (#23)
Prueba en Proxy
#### En Postman
- Proxy/Asientos/estado-asientos-redis
- http://localhost:8081/api/proxy/eventos/1/estado-asientos
- Devuelve: 200 OK
- JSON que devuelve:
```json
{
    "asientos": [
        {
            "columna": 3,
            "estado": "Vendido",
            "expira": null,
            "fila": 2
        }, // ... otros asientos ...
    ], 
    "eventoId": 1
}
```
**👉 Qué muestra**:
- Es el JSON directo del estado de asientos que tiene Redis para el eventoId=1, expuesta tal cual por el proxy:
- Cada elemento es un asiento con:
    - fila, columna
    - estado (Libre / Bloqueado / Vendido / Ocupado)
    - expira (si está bloqueado, cuándo vence)
- Todavía no hay combinación con la BD local ni validaciones de rangos, esto es exactamente lo que la cátedra guarda en su Redis remoto.

Sirve para comparar con la prueba anterior (5):
- Esta prueba6 → muestra el estado sin procesar (solo Redis vía proxy).
- prueba5 anterior → muestra el estado procesado (Redis + BD + validaciones).

### Que hace esta prueba?

- Acá no pasa por el backend. Llama directamente al proxy y veo el JSON que viene de Redis.
Esto me permite demostrar que el proxy está leyendo bien el Redis remoto de la cátedra y que el endpoint /eventos/{id}/estado-asientos funciona.

Después (en la prueba5 anterior), muestra cómo el backend usa justamente este JSON para construir el mapa final de asientos.

### Terminal del Backend
En esta prueba Postman habla directo con el proxy, el backend no participa.

### Terminal del Proxy

En esta prueba pruebo directamente el eslabón Redis ↔ Proxy.
El proxy consulta Redis con la key evento_1, encuentra datos, los parsea correctamente (39 asientos) y me los devuelve como JSON.

Ese mismo JSON es el que después usa mi backend en la prueba5 anterior para armar el mapa en tiempo real.
```bash
ProxyTokenAuthFilter      : 🛡️  [Seguridad] Token Bearer presente (parcial=eyJhbGciOiJI... , longitud=187)
ProxyEventosResource      : 🌐 [Proxy] GET /api/proxy/eventos/1/estado-asientos
EstadoAsientosRedisService: Consultando Redis para key=evento_1, resultado=ENCONTRADO
EstadoAsientosRedisService: Se parseó correctamente estado de asientos para eventoId=1 (39 asientos).
```

---