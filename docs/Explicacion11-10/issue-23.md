# Issue #23 – Integración con Redis para Estado de Asientos en Tiempo Real

## 🎯 Objetivo

Obtener el **estado REAL** de los asientos desde **Redis** (bloqueos vigentes / expirados) y combinarlo con los asientos persistidos en **PostgreSQL**, generando un **mapa de asientos unificado y en tiempo real** para el frontend.

Este issue implementa totalmente lo pedido por el profesor:
- manejo de **expiración** de bloqueos,
- validación de **rangos de fila/columna**,
- Redis como **fuente dinámica**,
- BD como estado **persistente** (ventas).

---

## 📌 1. Cambios en `ProxyService` (backend)

### ✔️ Nuevo método

```java
public String listarEstadoAsientosRedis(Long externalId) {
    return doGet("/eventos/" + externalId + "/estado-asientos");
}
```

### ✔️ ¿Qué hace este archivo?

`ProxyService.java` es el componente responsable de la **comunicación HTTP backend → proxy**.  
El método `listarEstadoAsientosRedis`:

- Llama al proxy-service: `GET /api/proxy/eventos/{id}/estado-asientos`.
- El proxy consulta Redis remoto de la cátedra.
- Devuelve el JSON con **el estado dinámico de cada asiento** (bloqueos / expiración).

### 📄 Ejemplo de JSON recibido

```json
{
  "eventoId": 1,
  "asientos": [
    {
      "fila": 10,
      "columna": 5,
      "estado": "BLOQUEADO",
      "expira": "2025-12-10T14:30:00Z"
    }
  ]
}
```

Este JSON se deserializa mediante los DTO existentes:
- `ProxyEstadoAsientosResponse`
- `ProxyAsientoDTO`

---

## 📌 2. Servicio `AsientoEstadoService`

**Archivo:** `AsientoEstadoService.java`  

### ✔️ ¿Qué es este archivo?

Es el servicio **central** que combina la información de:

1. **Asientos persistidos en BD** (`asiento` en PostgreSQL).
2. **Bloqueos y estado temporal** de los asientos en Redis (vía proxy).

Su responsabilidad es producir el **estado final correcto** para cada asiento de un evento, respetando las reglas de negocio (venta prioritaria, expiración de bloqueos, validación de rangos).

---

### 🔧 Flujo general del método principal

```java
public List<AsientoEstadoDTO> obtenerEstadoActualDeAsientos(Long eventoId)
```

1. Busca el `Evento` en la BD (para conocer filas/columnas).
2. Recupera todos los asientos de ese evento desde PostgreSQL.
3. Llama a `ProxyService.listarEstadoAsientosRedis(externalId)` para traer el mapa desde Redis.
4. Parsea el JSON a `ProxyEstadoAsientosResponse` y arma un mapa `(fila-columna) → ProxyAsientoDTO`.
5. Recorre **cada asiento de BD** y decide el estado final combinando:
   - Estado persistido (venta).
   - Estado temporal de Redis (bloqueo vigente/expirado).
6. Devuelve una lista de `AsientoEstadoDTO` lista para el frontend.

---

### 🔧 Reglas de combinación (explicadas)

#### 1️⃣ Si un asiento está **VENDIDO en la BD → manda la BD**

```java
if (asientoDB.getEstado() == AsientoEstado.VENDIDO) {
    resultado.add(new AsientoEstadoDTO(
        asientoDB.getFila(),
        asientoDB.getColumna(),
        "VENDIDO",
        null
    ));
    continue;
}
```

- **Qué valida:** si el asiento fue vendido en la base local.
- **Regla de negocio:** una venta es definitiva, por lo tanto Redis se ignora completamente aunque tenga un bloqueo viejo o datos contradictorios.
- **Estado final:** siempre **VENDIDO**.


#### 2️⃣ Si no está vendido, Redis define el estado temporal

```java
if (redis != null && redis.getExpira() != null) {
    Instant expira = redis.getExpira();

    if (expira.isAfter(ahora)) {
        // BLOQUEADO_VIGENTE
    } else {
        // bloqueo expirado → LIBRE
    }
}
```

- **Qué hace:** solo entra si hay un asiento correspondiente en Redis y tiene campo `expira`.
- Compara `expira` con el tiempo actual (`Instant.now()`):
  - Si `expira > ahora` → el bloqueo sigue vigente.
  - Si `expira < ahora` → el bloqueo expiró y el asiento vuelve a considerarse libre.

#### 3️⃣ Estados finales posibles

- **VENDIDO** → si la BD marca el asiento como vendido.
- **BLOQUEADO_VIGENTE** → bloqueo activo en Redis, no vendido en BD.
- **LIBRE** (por dos motivos):
  - Nunca estuvo en Redis.
  - Estuvo bloqueado pero `expira < ahora` (bloqueo expirado).
- (Opcionalmente se puede considerar el concepto *BLOQUEADO_EXPIRADO* a nivel de logs, pero hacia el frontend se devuelve LIBRE).

---

### 🔎 Validaciones de integridad (rango y datos válidos)

Antes de confiar en los datos de Redis, se valida que el asiento:

- Tenga fila/columna presentes.
- No tenga valores negativos o cero.
- No se salga del rango de asientos configurado en el evento.

Código real:

```java
if (redis != null) {
    boolean invalido =
        redis.getFila() == null || redis.getColumna() == null ||   // ❗ Falta fila o columna
        redis.getFila() <= 0 || redis.getColumna() <= 0 ||         // ❗ Fila/columna <= 0 (inválido)
        redis.getFila() > evento.getFilaAsientos() ||              // ❗ Fila mayor al máximo del evento
        redis.getColumna() > evento.getColumnaAsientos();          // ❗ Columna mayor al máximo del evento

    if (invalido) {
        log.warn(
            "⚠️  [Redis] Asiento remoto inválido ({}, {}): fuera de rango para evento idLocal={} (filas 1-{}, columnas 1-{})",
            redis.getFila(),
            redis.getColumna(),
            evento.getId(),
            evento.getFilaAsientos(),
            evento.getColumnaAsientos()
        );
        redis = null; // se ignora Redis para este asiento
    }
}
```

#### Explicación condición por condición:

- `redis.getFila() == null || redis.getColumna() == null`  
  👉 Detecta asientos incompletos (sin fila o sin columna).

- `redis.getFila() <= 0 || redis.getColumna() <= 0`  
  👉 Detecta valores inválidos (no pueden haber filas/columnas 0 o negativas).

- `redis.getFila() > evento.getFilaAsientos()`  
  👉 La fila que viene de Redis se pasa del máximo configurado en el evento.  
  Ejemplo: evento tiene 10 filas y en Redis aparece fila 20.

- `redis.getColumna() > evento.getColumnaAsientos()`  
  👉 Igual que lo anterior, pero para columna.  
  Ejemplo: evento tiene 6 columnas y Redis manda columna 11.

Cuando alguna de estas condiciones se cumple:

- Se registra el log:
  ```
  ⚠️  [Redis] Asiento remoto inválido (F,C): fuera de rango para evento idLocal=...
  ```
- Y se descarta Redis para ese asiento (`redis = null`).

---

### 🧠 Lógica de expiración y logs

```java
if (expira.isAfter(ahora)) {
    log.info("🔒 [Redis] Asiento ({},{}) bloqueado vigente",
        asientoDB.getFila(), asientoDB.getColumna());

    resultado.add(new AsientoEstadoDTO(
        asientoDB.getFila(),
        asientoDB.getColumna(),
        "BLOQUEADO_VIGENTE",
        expira
    ));
} else {
    log.info("🕒 [Redis] Asiento ({},{}) bloqueo expirado",
        asientoDB.getFila(), asientoDB.getColumna());

    resultado.add(new AsientoEstadoDTO(
        asientoDB.getFila(),
        asientoDB.getColumna(),
        "LIBRE",
        null
    ));
}
```

- Si `expira > ahora`:
  - Log: `🔒 [Redis] Asiento (F,C) bloqueado vigente`
  - Estado final: **BLOQUEADO_VIGENTE**.

- Si `expira <= ahora`:
  - Log: `🕒 [Redis] Asiento (F,C) bloqueo expirado`
  - Estado final: **LIBRE** (bloqueo expirado).

---

## 📌 3. DTO final: `AsientoEstadoDTO`

**Archivo:** `AsientoEstadoDTO.java`

Este DTO es lo que finalmente se le devuelve al frontend, ya con el estado combinado y limpio.

```json
{
  "fila": 10,
  "columna": 5,
  "estado": "BLOQUEADO_VIGENTE",
  "expiraEn": "2025-12-10T14:30:00Z"
}
```

Incluye:
- `fila`
- `columna`
- `estado` (LIBRE / BLOQUEADO_VIGENTE / VENDIDO)
- `expiraEn` (solo si hay bloqueo vigente)

---

## 📌 4. Endpoint actualizado en backend – `EventoResource`

### ✔️ Nuevo endpoint

```java
@GetMapping("/{id}/asientos")
public ResponseEntity<List<AsientoEstadoDTO>> obtenerEstadoActualAsientos(@PathVariable Long id) {
    log.info("[EventoResource] GET /api/eventos/{}/asientos (mapa en tiempo real)", id);

    List<AsientoEstadoDTO> mapa = asientoEstadoService.obtenerEstadoActualDeAsientos(id);

    return ResponseEntity.ok(mapa);
}
```

### ✔️ ¿Qué hace?

1. Recibe el `id` del evento local (PostgreSQL).
2. Llama a `AsientoEstadoService.obtenerEstadoActualDeAsientos(id)`.
3. Obtiene el mapa de asientos combinado (BD + Redis).
4. Devuelve la lista final de `AsientoEstadoDTO` para el frontend.

---

## ✔️ Criterios de aceptación cumplidos

- El backend distingue correctamente **LIBRE**, **BLOQUEADO_VIGENTE** y **VENDIDO**.
- Redis tiene prioridad para bloqueos, excepto cuando el asiento ya fue vendido.
- Los bloqueos expirados se interpretan correctamente y el asiento vuelve a ser tratado como **LIBRE**.
- Validación estricta de filas y columnas:
  - > 0
  - dentro del rango declarado por el `Evento`.
- Los logs muestran claramente:
  - bloqueos vigentes,
  - bloqueos expirados,
  - asientos inválidos provenientes de Redis.
- Redis se usa como **fuente de estado en tiempo real**, combinada con la información persistida en PostgreSQL.
- El frontend recibe un mapa de asientos **confiable, consistente y actualizado**.

---
