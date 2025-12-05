# Explicación Técnica y Arquitectura del Sistema

## 1. Servicio de la Cátedra
Backend externo que provee:
- Endpoints propios.
- Redis público para estados de asientos.
- Kafka con notificaciones de cambios.

El alumno **consume y envía** datos (bloqueos, ventas, sincronización).

---
## 2. Redis (dos roles distintos)
### Redis Local (del alumno)
- Guarda **sesiones de usuario**.
- TTL de 30 minutos.
- Comparte estado entre dispositivos.
- Persiste tras reinicios.

### Redis de la Cátedra
- Guarda estado de los asientos.
- Acceso **solo** a través del servicio Proxy.

---
## 3. Kafka (solo la cátedra produce mensajes)
- Notifica cambios en eventos.
- El Proxy del alumno consume esos mensajes.
- Luego debe notificar al backend para sincronizar.

---
## 4. Backend del Alumno (JHipster)
- Manejo de usuarios y JWT.
- Persistencia local de eventos y ventas.
- Lógica de dominio completa.
- Exponer API al móvil.
- Comunicarse con:
  - Servicio de la cátedra (HTTP).
  - Proxy del alumno.

---
## 5. Proxy del Alumno
- El único con acceso autorizado a:
  - Redis de la cátedra.
  - Kafka.

Funciones:
- Obtener estados de asientos.
- Recibir mensajes de Kafka.
- Notificar cambios al backend.

---
## 6. Cliente Móvil (KMP)
- Se comunica **solo con el backend**.
- Flujo completo:
  1. Login
  2. Lista eventos
  3. Detalles + mapa de asientos
  4. Selección hasta 4 asientos
  5. Carga de datos de personas
  6. Compra
  7. Retoma de estado según sesión Redis del backend
