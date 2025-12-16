# Issue #21 — Baja lógica de eventos eliminados en la cátedra

## 🎯 Objetivo
Implementar en el backend la lógica que detecta **cuando un evento deja de existir en la cátedra** y marcarlo como **inactivo** en la base local.  
No se elimina físicamente: se preserva el historial y se cumple la consigna del TP Final.

---

## 🧩 Cambios realizados

### 1️⃣ Campo `activo` en la entidad Evento
Se agregó:
```java
private Boolean activo = true;
```
Permite inactivar eventos que ya no figuran en la cátedra.

---

### 2️⃣ Extensión del EventoSyncService
Durante la sincronización:
- Se obtiene el conjunto de externalIds vigentes desde el proxy.
- Se recorren los eventos locales:
  - Si un evento local tiene `externalId ≠ null` **y no aparece en la lista remota**,  
    entonces se marca:
```java
evento.setActivo(false);
```

📌 Ejemplo real de log:
```
🗑️ [Sync] Evento externalId=1005 marcado como inactivo (idLocal=12)
```

---

### 3️⃣ Ajustes en EventoRepository
Se añadió:
```java
List<Evento> findByActivoTrue();
```
Permite que el backend solo devuelva eventos válidos.

---

### 4️⃣ Ajustes en EventoResource
`GET /api/eventos` ahora retorna **solo eventos activos**, cumpliendo la sincronización requerida.

---

## ✔️ Criterios de aceptación verificados
- Evento eliminado en la cátedra aparece como inactivo en la base local.  
- Backend no borra el evento: mantiene histórico (`activo = false`).  
- Endpoints exponen solamente eventos activos.  
- Logs de sincronización muestran correctamente las bajas lógicas.  

---

## 📄 Archivos modificados

| Archivo | Cambio |
|--------|--------|
| **Evento.java** | Nuevo campo `activo` |
| **EventoRepository.java** | Agregado filtro para activos |
| **EventoSyncService.java** | Lógica de baja lógica |
| **EventoResource.java** | Devuelve solo eventos activos |

---
