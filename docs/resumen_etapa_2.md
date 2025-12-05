# Resumen de la Etapa 2 – Modelo de Dominio y Generación de Entidades JPA

Este documento resume todo lo realizado durante la Etapa 2 del TP Final 2025, explicando qué se hizo, por qué se hizo y para qué sirve cada componente generado.

---

## 🎯 Objetivo General de la Etapa 2
Definir el **modelo de dominio** de la aplicación y generar las **entidades JPA** utilizando JHipster, junto con sus relaciones, enums y migraciones automáticas en PostgreSQL mediante Liquibase.

El resultado final es un backend completamente estructurado, con base de datos coherente, entidades conectadas entre sí y código generado para servicio, repositorio y controladores REST.

---

# 1. 📌 Entidades creadas
Se generaron **tres entidades principales**, basadas en el enunciado del TP.

## 🟦 1.1 Evento
Representa un evento que se puede publicar en la plataforma.

**Atributos:**
- `titulo` – Nombre del evento.
- `descripcion` – Descripción general.
- `fecha` – Día del evento.
- `hora` – Hora del evento.
- `organizador` – Quién lo organiza.
- `presentadores` – Participantes o expositores.
- `cantidadAsientosTotales` – Total de asientos disponibles.
- `filasAsientos` – Número de filas.
- `columnaAsientos` – Número de columnas.

**Relaciones:**
- `asientos` (1 a muchos) → Cada evento tiene muchos asientos.
- `ventas` (1 a muchos) → Cada evento puede tener muchas ventas.

📌 **Para qué sirve:** Unifica toda la información base del evento y actúa como entidad madre del sistema.

---

## 🟩 1.2 Asiento
Representa un asiento dentro de un evento.

**Atributos:**
- `fila` – Número de fila.
- `columna` – Número de columna.
- `estado` – Estado del asiento (enum).
- `personaActual` – Usado para mostrar quién lo reservó o compró.

**Enum:** `AsientoEstado` con los valores:
- `LIBRE`
- `BLOQUEADO`
- `VENDIDO` 

**Relación:**
- `evento` (muchos a uno) → Cada asiento pertenece a un único evento.

📌 **Para qué sirve:** Representa la grilla de asientos del evento y su estado en tiempo real.

---

## 🟥 1.3 Venta
Representa la compra de uno o varios asientos.

**Atributos:**
- `fechaVenta`
- `estado` (enum `VentaEstado`: PENDIENTE, EXITOSA, FALLIDA)
- `descripcion`
- `precioVenta`
- `cantidadAsientos`

**Relaciones:**
- `evento` (muchos a uno) → Toda venta corresponde a un único evento.
- `asientos` (muchos a muchos) → Una venta puede incluir varios asientos.

📌 **Para qué sirve:** Gestiona la compra de entradas y el historial de ventas.

---

# 2. 🔗 Relaciones del Dominio
El modelo quedó así:

- **Evento 1 ─ * Asiento** → un evento genera muchos asientos.
- **Evento 1 ─ * Venta** → un evento puede tener muchas ventas.
- **Venta * ─ * Asiento** → una venta contiene varios asientos. preguntar por esto??????

Estas relaciones se generaron automáticamente por JHipster tanto en las entidades Java como en la base de datos (incluyendo tabla intermedia `rel_venta__asiento`).

---

# 3. 🧱 Migraciones con Liquibase
JHipster creó automáticamente los changelogs en:
```
src/main/resources/config/liquibase/changelog/
```

Liquibase generó: que es ?????????????????
- Tablas `evento`, `asiento`, `venta`.
- Tabla intermedia `rel_venta__asiento`.
- Constraints, foreign keys y columnas necesarias.

📌 **Para qué sirve:** Garantiza que la base de datos se mantenga consistente, reproducible y versionada.

---

# 4. 🗄 PostgreSQL funcionando
Se levantó el entorno con Docker:
```
docker compose -f postgresql.yml -f redis.yml up -d
```

Se accedió a la base:
```
docker exec -it backendcatedra-postgresql-1 psql -U backendCatedra
```

Se verificó la existencia de tablas con `\dt`:
- evento
- asiento
- venta
- rel_venta__asientos

📌 **Para qué sirve:** Confirmar que Liquibase aplicó todo correctamente.

---

# 5. 🧩 Generación automática de JHipster
Por cada entidad se generó automáticamente:
- Clase de dominio (`.java`)
- DTO (`.java`)
- Mapper MapStruct (`.java`)
- Repositorio (`.java`)
- Servicio + implementación (`.java`)
- Controlador REST (`Resource.java`)
- Frontend Angular:
  - vistas (list, detail, update, delete)
  - rutas
  - modelos TS

📌 **Para qué sirve:** Esto evita escribir boilerplate y permite enfocarse en lógica de negocio.

---

Resumen: Implementación de Estado de Sesión en Redis (Issue #6)

🎯 Objetivo

Cumplir el requisito de que el estado del proceso de compra del usuario (ej: evento seleccionado, asientos) sea persistente, concurrente y con tiempo de expiración (TTL) de 30 minutos, a pesar de que el backend de JHipster use Tokens JWT (Stateless).

💡 El Problema y la Solución Arquitectónica

El framework JHipster usa JWT, lo que significa que el servidor no "recuerda" al usuario entre peticiones (es Stateless). Para darle memoria, usamos Redis como un almacén de estado distribuido.

Componente                  Rol en la Aplicación

Tokens JWT                 Autenticación (Quién eres).

Redis Local                Estado (Qué estás haciendo).

No utilizamos la librería Spring Session (diseñada para Cookies), sino una solución manual más limpia para arquitecturas JWT.

🛠️ Componentes Creados

Se crearon 3 componentes principales para gestionar el estado:

1. DTO (Objeto de Transferencia de Datos)

- Archivo: UserSessionDTO.java
- Función: La "caja" que transporta los datos. Define qué se guarda en Redis.
- Campos Clave: pasoActual, idEventoSeleccionado, asientosSeleccionados.

2. Servicio (UserSessionService + Impl)

- Archivo: UserSessionServiceImpl.java
- Función: La lógica de negocio y la conexión directa con Redis.
- Mecanismo: Inyecta StringRedisTemplate para enviar comandos SET y GET a Redis.
- Concurrencia: Guarda los datos con una clave basada en el username (user:session:admin), garantizando que si el usuario accede desde dos dispositivos, ambos leen la misma información.

3. Configuración del TTL (30 minutos)

- En Código: La variable tiempoExpiracionSesion se lee con @Value("${app.session-timeout-minutes:30}"). El valor :30 sirve como fallback de seguridad.
- En YAML: La propiedad app.session-timeout-minutes: 30 en application-dev.yml permite cambiar el tiempo de expiración sin recompilar.

## ✅ Verificación y Cumplimiento de Requisitos

- Requisito: Persistencia (Sobrevivir a reinicios)
- Prueba Realizada: El dato fue guardado, el backend se detuvo (Ctrl+C) y se reinició.
- Resultado: El dato se recuperó con éxito de Redis.

- Requisito: TTL (30 minutos de inactividad)
- Prueba Realizada: Se configuró a 1 minuto y se esperó la expiración.
- Resultado: El dato fue borrado automáticamente por Redis.