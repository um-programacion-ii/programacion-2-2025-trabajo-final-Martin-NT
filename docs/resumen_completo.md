# Resumen Completo del Avance del Proyecto

## 1. Infraestructura Base Configurada y Funcionando
El backend fue generado utilizando **JHipster en modo monolítico**, lo cual proporciona una arquitectura robusta basada en Spring Boot. La aplicación incluye:

- **Autenticación JWT** lista y probada.
- **Base de datos PostgreSQL** configurada mediante Docker Compose.
- **Cache Ehcache** habilitado por defecto.
- **Redis** configurado como store externo para sesiones.
- **Liquibase** generando y controlando las migraciones.
- **Frontend Angular** generado automáticamente.

La aplicación se ejecuta correctamente con:
```
./mvnw
```
Y los servicios de base se levantan con:
```
docker compose -f src/main/docker/postgresql.yml up -d
```

Se verificó exitosamente:
- Acceso a la UI administrativa.
- Autenticación en Postman usando `/api/authenticate`.
- Recepción del token JWT.

---

## 2. Conexión Exitosa con la Cátedra (ZeroTier + Servicios Remotos)
Se configuró el archivo `.env` con los valores reales proporcionados para conectar el backend a los servicios remotos de la cátedra.

### Estado actual:
- **ZeroTier** correctamente unido a la red: `93afae59630d1f1d`.
- IP asignada en la red: `192.168.194.64`.
- Conectividad validada hacia:
  - **Servidor HTTP** (`curl http://192.168.194.250:8080` → 200 OK)
  - **Redis remoto** (`redis-cli -h 192.168.194.250 ping` → PONG)
  - **Kafka remoto** (variables configuradas, conexión validada al iniciar Spring Boot).

### Variables de entorno activas:
- PostgreSQL local
- Redis remoto → `192.168.194.250:6379`
- Servicio de la cátedra → `http://192.168.194.250:8080/api`
- Kafka → `192.168.194.250:9092`

Con esto se completó el objetivo final de la etapa de infraestructura.

---

## 3. Modelo de Dominio Generado con JHipster
Se generaron las entidades definidas por la consigna del trabajo:

### ✔ Entidad **Evento**
Atributos principales:
- título, descripción, fecha, hora
- filasAsientos, columnaAsientos, cantidadAsientosTotales

Relaciones:
- **OneToMany** con Asiento
- **OneToMany** con Venta

### ✔ Entidad **Asiento**
Atributos principales:
- fila, columna, estado, personaActual

Relaciones:
- **ManyToOne** con Evento
- **ManyToMany** con Venta

Enum asociado:
- `AsientoEstado`: LIBRE, BLOQUEADO, OCUPADO

### ✔ Entidad **Venta**
Atributos principales:
- fechaVenta, estado, descripcion
- precioVenta, cantidadAsientos

Relaciones:
- **ManyToOne** con Evento
- **ManyToMany** con Asiento

Enum asociado:
- `VentaEstado`: PENDIENTE, EXITOSA, FALLIDA

### ✔ Validación en Base de Datos
Liquibase generó correctamente las tablas:
- evento
- asiento
- venta
- rel_venta__asiento

Probado con PostgreSQL vía CLI.

---

## 4. Lógica de Dominio Implementada
Se implementaron reglas de negocio reales según la consigna.

### 🟣 Lógica de **Evento**
- Recalcula **cantidadAsientosTotales = fila × columna**.
- Ignora cualquier valor enviado desde el frontend en ese campo.
- Validación obligatoria en: save(), update(), partialUpdate().
- Nuevas consultas ordenadas:
  - Por fecha/hora
  - Por título

### 🟣 Lógica de **Asiento**
Reglas implementadas:
- Validación de fila y columna (> 0).
- Validación de existencia obligatoria de un Evento.
- Inmutabilidad parcial: un Asiento pertenece a un único Evento.
- Nueva consulta: obtener asientos ordenados por fila y columna dentro de un Evento.
- Excepción personalizada: `AsientoInvalidoException`.

### 🟣 Lógica de **Venta**
Reglas implementadas:
1. PrecioVenta > 0.
2. cantidadAsientos > 0.
3. La venta debe contener asientos.
4. cantidadAsientos debe coincidir con `asientos.size()`.
5. Todos los asientos deben pertenecer al mismo Evento.
6. No se pueden incluir asientos con estado **VENDIDO**.
7. Si la venta queda en estado **EXITOSA**:
   - Todos los asientos pasan a estado **VENDIDO** automáticamente.

### Excepción personalizada:
- `VentaInvalidaException`.

Estas reglas garantizan integridad del dominio y preparan el backend para el flujo de compra.

---

## 5. Manejo de Sesiones de Usuario con Redis
El sistema de sesiones implementado cumple el rol de **almacenar el estado del proceso de compra del usuario** de forma persistente y accesible desde múltiples dispositivos. Esta funcionalidad era un requisito explícito del enunciado, ya que la app móvil debe poder continuar el flujo aunque el backend se reinicie o el usuario cambie de dispositivo.

### ¿Qué es cada componente y qué hace?

### **1. UserSessionDTO (Data Transfer Object)**
Este objeto representa **el estado actual del proceso de compra del usuario**. Es lo que guardamos dentro de Redis.
Incluye:
- **pasoActual** → en qué parte del flujo está el usuario (ej: "seleccion-evento", "confirmacion", etc.)
- **idEventoSeleccionado** → qué evento eligió para comprar.
- **asientosSeleccionados** → lista de IDs de los asientos que el usuario seleccionó.

Es un objeto pequeño, serializable a JSON, ideal para almacenarse en Redis.

---

### **2. UserSessionService**
Servicio responsable de guardar y recuperar la sesión desde Redis.
Incluye dos métodos principales:

#### **saveSession(usuario, dto)**
- Convierte el DTO en JSON.
- Genera una clave única para el usuario → `user:session:{username}`.
- Guarda el JSON en Redis.
- Aplica TTL (tiempo de expiración) automáticamente.

#### **loadSession(usuario)**
- Busca la clave `user:session:{username}` en Redis.
- Si existe → recupera el JSON, lo deserializa y devuelve el DTO.
- Si no existe → devuelve `null`, indicando que el usuario no tiene sesión iniciada.

Es el corazón del sistema de persistencia del estado.

---

### **3. Redis como almacén de sesión**
Redis se utiliza porque:
- Es extremadamente rápido (memoria RAM).
- Ideal para sesiones temporales.
- Permite TTL nativos.
- Permite almacenamiento compartido entre múltiples instancias o dispositivos.

El backend NO guarda nada del estado de compra en memoria → todo está en Redis.
Esto permite sobrevivir reinicios del backend.

---

### **4. TTL configurado externamente**
El tiempo de expiración no está hardcodeado.
Se maneja mediante:
```
app.session-timeout-minutes: 30
```
El servicio toma este valor en tiempo de ejecución.
Si el profesor cambia el TTL, NO hace falta recompilar.

Redis borra automáticamente la clave tras 30 minutos sin actividad.

---

### **5. Clave por usuario: `user:session:{username}`**
Cada usuario tiene su propia entrada en Redis.
Ejemplos reales:
- `user:session:admin`
- `user:session:juan`
- `user:session:martin`

Esto permite:
- Diferenciar sesiones por usuario.
- Permitir que múltiples usuarios avancen en la compra simultáneamente.
- Sin colisiones entre claves.

---

### **Características comprobadas en pruebas**
✔ **La sesión sobrevive reinicios del backend**, porque Redis la almacena externamente.  
✔ **TTL funciona** → si pasan 30 min sin requests, la sesión desaparece sola.  
✔ **La sesión se comparte entre dispositivos** → Postman y navegador usando la misma cuenta ven el mismo estado.  
✔ **Redis remoto funciona** → probado por ZeroTier con PING y por el backend al guardar/leer.

Este sistema es totalmente compatible con la app móvil y con la arquitectura de la cátedra.

---

## 6. Estado Actual del Proyecto
### ✔ Backend JHipster completamente configurado
### ✔ Conectividad completa con la cátedra
### ✔ Modelo de dominio creado y consistente
### ✔ Lógica de negocio implementada para Evento, Asiento y Venta
### ✔ Sesiones de usuario con Redis funcionando
### ✔ Preparado para comenzar **Etapa 3: Proxy del alumno**

El backend está sólido, validado y listo para avanzar hacia la fase de integración móvil y servicios externos.

