# Issue #22 — Sincronización de asientos del evento

## 🎯 Objetivo

### "Sincronización de Asientos desde la Cátedra"

Este issue introduce toda la infraestructura necesaria para que el backend del alumno pueda reflejar en su propia base de datos el estado real de los asientos que la cátedra mantiene en Redis.

La sincronización es obligatoria porque:

- El backend necesita persistir una copia local de los asientos, ya que el frontend y las operaciones internas dependen de esta estructura.
- Los asientos remotos incluyen:
  - disponibilidad
  bloqueos
  ventas
  expiraciones
  persona asignada

- El proxy obtiene esta información en tiempo real, pero el backend debe reconstruir su propio mapa de asientos cada vez que se sincroniza un evento.
- Además, si la cátedra cambia filas/columnas, la asignación de butacas o el estado general, el backend debe regenerar todo para mantenerse consistente.
---

## 🧩 Archivos creados / modificados en el Backend

### 1️⃣ `ProxyAsientoDTO`

#### ¿Qué es?
- Es el DTO que representa un asiento individual tal como llega desde la cátedra vía proxy.

#### Campos incluidos:
- fila
- columna
- estado (string crudo: "Libre", "Bloqueado", "Vendido", “Ocupado”)
- personaActual
- expira (fecha/hora de expiración del bloqueo)

#### ¿Por qué es importante?
- Permite convertir el JSON del proxy en un objeto Java usable.
- Se utiliza para mapear correctamente cada asiento a la entidad Asiento en la base local.
- El backend debe interpretar correctamente estados especiales:
- OCUPADO → VENDIDO (regla de la cátedra)

---

### 2️⃣ `ProxyEstadoAsientosResponse`

- El proxy no devuelve directamente una lista de asientos, sino un objeto raíz con esta estructura:
```json
  {
    "eventoId": 1,
    "asientos": [ ... ]
  }
```
- Por eso este wrapper es obligatorio.
- Permite reflejar exactamente la estructura enviada por Redis/proxy.
- Facilita acceder a asientos[] y asociarlos al eventoId.
- Es necesario porque el proxy envía **un objeto** que contiene la lista, no un array suelto.

---

### 3️⃣ `AsientoRepository`

- El repositorio se amplía para permitir operaciones masivas necesarias en la sincronización.

#### Métodos agregados:
1. deleteByEventoId(Long id)

- Elimina todos los asientos anteriores del evento.
- Se usa para regenerar completamente el mapa de butacas.

2. findByEventoIdOrderByFilaAscColumnaAsc(...)

- Permite devolver los asientos ordenados para el frontend.
- También facilita comparaciones y controles posteriores.

#### ¿Por qué es importante?

- Porque la sincronización implica reemplazar completamente los asientos locales para garantizar consistencia.

---

### 4️⃣ `ProxyService`
Se agregó el método:
```java
public String listarAsientosDeEvento(Long externalId)
```
Encargado de llamar a:
```
GET /api/proxy/eventos/{id}/asientos
```
Este endpoint en el proxy obtiene el estado real desde Redis.

#### ¿Qué obtiene este método?

- Un JSON con todos los asientos del evento almacenados en Redis.
- Incluye ventas, bloqueos, expiraciones y estados temporales.

#### ¿Por qué es importante?

- Porque este es el único lugar donde el backend puede obtener el estado real y completo de la cátedra.
---

### 5️⃣ `AsientoSyncService`

- Este servicio implementa todo el proceso de sincronización.

#### Hace lo siguiente:

1) Obtiene el JSON de Redis a través del proxy
- Usa ProxyService.listarAsientosDeEvento().

2) Lo parsea a objetos Java
- Convierte el JSON a ProxyEstadoAsientosResponse.

3) Borra todos los asientos previos del evento
Esto garantiza que:
- no queden asientos viejos,
- no existan inconsistencias.

4) Regenera toda la matriz de asientos
Crea todos los registros en BD con:
- fila
- columna
- estado correcto
- vínculo al evento

5) Mapea los estados remotos a los locales
Reglas aplicadas:
| Estado remoto | Estado local |
| ------------- | ------------ |
| LIBRE         | LIBRE        |
| BLOQUEADO     | BLOQUEADO    |
| OCUPADO       | VENDIDO      |
| VENDIDO       | VENDIDO      |

6) Registra logs claros
```
[Sync-Asientos] Asientos previos eliminados: N
[Sync-Asientos] Asientos sincronizados: X creados, Y actualizados
```

#### ¿Por qué es fundamental este servicio?

- Garantiza que la estructura de asientos local sea idéntica a la de la cátedra. 
- Permite que el backend funcione de forma independiente a Redis.
- Facilita consultas, búsquedas, ventas y futuras operaciones sin depender del proxy.


---

### 6️⃣ `EventoSyncService`

- Cada vez que se sincroniza un evento, también se sincronizan sus asientos:
```java
asientoSyncService.sincronizarAsientosDeEvento(eventoGuardado, remoteId);
```
Esto asegura que **cada vez que un evento se actualiza**, también se actualiza su mapa de asientos.

---

## 📌 Criterios de aceptación — VERIFICADOS

✔ Los asientos locales coinciden con Redis/cátedra  
✔ Se eliminan los asientos anteriores  
✔ Se regeneran completamente si cambia la capacidad  
✔ Los estados remotos se mapean correctamente  
✔ El backend no tiene inconsistencias  
✔ Logs claros y en español  

---
