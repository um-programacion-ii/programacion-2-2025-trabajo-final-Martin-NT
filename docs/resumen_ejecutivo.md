# Resumen Ejecutivo del Avance del Proyecto

## Infraestructura Base funcionando
- Proyecto generado con **JHipster monolítico**.
- Configurado con: Spring Boot, JWT, MySQL/PostgreSQL, Redis, Ehcache.
- Levantado con Docker Compose.
- UI de administración funcionando.
- Autenticación verificada vía Postman (`POST /api/authenticate`).
- JWT recibido correctamente.

## Conexión con la Cátedra
- Creación del archivo **.env** con todas las variables reales.
- Conexión exitosa a:
  - Servidor HTTP de la cátedra.
  - Redis remoto.
  - Kafka remoto.
- Unión correcta a ZeroTier.

## Modelo de Dominio
- Entidades generadas con JHipster:
  - **Evento**, **Asiento**, **Venta**.
- Relaciones completas y enums definidos.
- Liquibase generó correctamente todas las tablas.
- Validado en PostgreSQL.

## Lógica de Dominio
### Evento
- Validación automática de `cantidadAsientosTotales`.
- Nuevas consultas ordenadas.

### Asiento
- Validación estricta de fila/columna.
- Excepción personalizada.

### Venta
- Validaciones completas: precio, cantidad, estados.
- Si la venta es EXITOSA → asientos pasan a VENDIDO.

## Sesiones con Redis
- Implementado `UserSessionDTO`.
- Guardado/lectura de sesión en Redis.
- TTL configurado a 30 min.
- Sesiones sincronizadas entre dispositivos.
- Persistencia tras reinicio.

## Estado Actual
✔ Backend generado  
✔ Infraestructura conectada  
✔ Dominio definido  
✔ Lógica validada  
✔ Sesiones funcionando  
✔ Comunicación con la cátedra establecida

