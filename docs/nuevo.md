Pruebas manuales – Ventas y sincronización con la cátedra (Issue #24)
0. Precondiciones generales

Antes de probar ventas:

Backend levantado en dev con Redis y proxy configurados.

Proxy-service levantado y apuntando al servidor de la cátedra.

Tenés un evento sincronizado desde la cátedra (con externalId y precioEntrada configurado).

Sabés al menos:

eventoIdLocal (ID en tu tabla evento),

eventoExternalId (ID del mismo evento en la cátedra).

Tip: podés obtenerlos con:

GET Backend / Evento / eventos-locales

y mirando la columna externalId.

1. Pruebas existentes que se reutilizan
1.1 Login backend (JWT)

Carpeta: Backend / Autenticación y Sesiones

Request: login-backend-obtener-jwt

Método/URL: POST /api/authenticate

Objetivo: obtener el token JWT del backend para usar en el header Authorization: Bearer <token> en todas las requests siguientes.

1.2 Sync inicial de eventos

Carpeta: Backend / Admin

Request: admin-sync-evento

Método/URL: POST /api/admin/sync-eventos-desde-proxy (o el que tengas configurado)

Objetivo: traer eventos y asientos desde la cátedra al backend antes de probar ventas.

1.3 Estado de asientos tiempo real (para verificar)

Carpeta: Backend / Estado de Asientos (Tiempo Real)

Request: estado-asientos-tiempo-real

Método/URL: GET /api/eventos/{eventoIdLocal}/estado-asientos

Objetivo: ver el estado combinado DB + Redis (LIBRE / BLOQUEADO_VIGENTE / VENDIDO) antes y después de las pruebas de venta.

2. Nuevas pruebas – Flujo de venta desde el backend
2.1 Bloquear asientos en la cátedra (preparar el escenario feliz)

Esto todavía no está en el backend (#25), así que por ahora se hace directo contra la cátedra.

Carpeta: Servidor Cátedra

Request sugerida: P6 - bloquear-asientos-evento

Método/URL: POST P6 – Bloqueo de asientos en un evento

Body ejemplo:

{
  "eventoId": 1,
  "asientos": [
    { "fila": 2, "columna": 3 },
    { "fila": 2, "columna": 4 }
  ]
}


Objetivo: dejar en Redis remoto los asientos (2,3) y (2,4) bloqueados vigentes.

2.2 Verificar que el backend ve los bloqueos

Carpeta: Backend / Estado de Asientos (Tiempo Real)

Request: estado-asientos-tiempo-real

Método/URL: GET /api/eventos/{eventoIdLocal}/estado-asientos

Objetivo: confirmar que esos asientos aparecen como BLOQUEADO_VIGENTE.

Esperado para (2,3) y (2,4):

{
  "fila": 2,
  "columna": 3,
  "estado": "BLOQUEADO_VIGENTE",
  "expira": "..."
}

2.3 Venta OK con bloqueos vigentes (flujo feliz)

Carpeta sugerida: Backend / Ventas

Nombre de request: venta-evento-bloqueo-vigente-OK

Método/URL: POST /api/ventas/eventos/{eventoIdLocal}/venta

Headers:

Authorization: Bearer <jwt-backend>

Content-Type: application/json

Body ejemplo:

{
  "eventoIdLocal": 1001,
  "asientos": [
    { "fila": 2, "columna": 3 },
    { "fila": 2, "columna": 4 }
  ]
}


Nota: eventoIdLocal en el body se pisará con el que viene en el path, pero dejalo coherente.

Resultado esperado:

HTTP 200 (o 201 si lo cambiaste) con un VentaDTO:

estado = "CONFIRMADA"

cantidadAsientos = 2

precioVenta = precioEntrada * 2

En logs:

💸 [Sync-Venta] Venta confirmada por cátedra. Persistiendo venta local...

💾 [Sync-Venta] Venta id=X guardada correctamente...

Si consultás:

GET /api/ventas → aparece la venta id = X.

GET /api/eventos/{eventoIdLocal}/estado-asientos → esos asientos deben aparecer como VENDIDO.

2.4 Venta con asiento sin bloqueo (rechazo por backend)

Carpeta: Backend / Ventas

Nombre: venta-evento-sin-bloqueo

Método/URL: POST /api/ventas/eventos/{eventoIdLocal}/venta

Body ejemplo: usar un asiento no bloqueado:

{
  "eventoIdLocal": 1001,
  "asientos": [
    { "fila": 3, "columna": 1 }
  ]
}


Resultado esperado:

HTTP 4xx (seguramente 400/409 según cómo lo manejes).

Mensaje de error legible, algo tipo:

"no está bloqueado vigente" o similar.

En logs:

⛔ [Sync-Venta] Bloqueo vencido o inexistente ...

No se crea ninguna nueva fila en venta (ver con GET /api/ventas).

2.5 Venta con asiento ya vendido (rechazo por backend)

Primero usá el flujo feliz (2.3) para vender un asiento concreto, por ejemplo (2,3).

Luego:

Carpeta: Backend / Ventas

Nombre: venta-evento-asiento-ya-vendido

Método/URL: POST /api/ventas/eventos/{eventoIdLocal}/venta

Body:

{
  "eventoIdLocal": 1001,
  "asientos": [
    { "fila": 2, "columna": 3 }
  ]
}


Resultado esperado:

HTTP 4xx

Mensaje de error tipo:

"ya está vendido"

En logs:

⛔ [Sync-Venta] Asiento (2,3) ya está vendido. Venta rechazada.

La cantidad de ventas en GET /api/ventas no aumenta.

2.6 Venta rechazada por la cátedra (resultado=false)

Para probar este flujo necesitás provocar un resultado=false (como en tu Payload 7). Por ejemplo, intentar vender asientos no bloqueados, pero dejando que tu código llegue a la cátedra (eso depende de cómo ajustes las validaciones).

En Postman:

Carpeta: Backend / Ventas

Nombre: venta-evento-rechazada-catedra

Método/URL: POST /api/ventas/eventos/{eventoIdLocal}/venta

Body: asientos que provoquen resultado=false en el P7.

Resultado esperado:

Backend lance IllegalStateException con mensaje tipo:

"La cátedra no confirmó la venta. Venta rechazada."

HTTP 4xx.

En logs:

⛔ [Sync-Venta] La cátedra no confirmó la venta. Respuesta: { ... resultado:false ... }

No se persiste la venta local.

3. Pruebas de notificación de ventas desde el proxy

Estas pruebas simulan la llegada de Kafka → proxy → backend usando el endpoint /api/proxy/notificacion-venta.

3.1 Notificación de venta rechazada (Payload 7)

Carpeta: Backend / Notificaciones Proxy

Nombre: proxy-notificacion-venta-rechazada

Método/URL: POST /api/proxy/notificacion-venta

Headers:

Authorization: Bearer <jwt-backend> (si tu security lo exige)

Content-Type: application/json

Body: usá tu Payload 7 tal cual:

{
  "eventoId": 1,
  "ventaId": 1583,
  "fechaVenta": "2025-12-12T15:49:54.700516753Z",
  "asientos": [
    {
      "fila": 2,
      "columna": 3,
      "persona": "Fernando Galvez",
      "estado": "Ocupado"
    },
    {
      "fila": 2,
      "columna": 4,
      "persona": "Carlos Perez",
      "estado": "Ocupado"
    }
  ],
  "resultado": false,
  "descripcion": "Venta rechazada. Alguno de los asientos no se encontraban bloqueados para la venta.",
  "precioVenta": 1400.0
}


Resultado esperado (con la lógica actual):

HTTP 200 con body:

{"status":"ok","mensaje":"Notificación de venta recibida"}

En logs:

📨 [Sync-Venta] Notificación de venta recibida desde proxy: {...}

según cómo implementamos procesarNotificacionVenta, se loguea el ventaId, resultado, etc.

Si en tu implementación actual actualizás la venta local:

Venta con externalId = ventaId debe reflejar el nuevo estado y descripción.

3.2 Notificación sin body

Carpeta: Backend / Notificaciones Proxy

Nombre: proxy-notificacion-venta-sin-body

Método/URL: POST /api/proxy/notificacion-venta

Body: vacío.

Resultado esperado:

HTTP 200.

En logs:

Notificación de venta sin body. No hay datos para procesar.

No se modifica nada en la base.

4. Pruebas auxiliares
4.1 Listado de ventas locales

Carpeta: Backend / Ventas

Nombre: ventas-locales

Método/URL: GET /api/ventas

Objetivo: verificar cuántas ventas tenés y en qué estado después de cada prueba.

4.2 Consulta de estado de asientos después de ventas

Carpeta: Backend / Estado de Asientos (Tiempo Real)

Nombre: estado-asientos-tiempo-real (la misma de antes)

Método/URL: GET /api/eventos/{eventoIdLocal}/estado-asientos

Objetivo: comprobar que los asientos vendidos quedan en VENDIDO.