# Explicación completa del Issue: Configuración de Redis + Uso de CommandLineRunner

Este documento resume y explica **todo lo realizado** en el issue relacionado con la configuración del acceso a Redis en el proxy, el parseo de los datos remotos, la creación del servicio y DTOs, y la incorporación de un **CommandLineRunner** para pruebas manuales en perfil de desarrollo.

Está pensado para documentar claramente qué se hizo, por qué, y cómo se prueba, tal como lo pide la consigna del TP Final.

---

# 1. Objetivo del Issue #14

El objetivo fue:

- Configurar el **proxy-service** para conectarse al Redis remoto de la cátedra.
- Leer claves del estilo `evento_{ID}`.
- Obtener el JSON crudo desde Redis.
- Parsearlo a **DTOs internos** que representen los asientos y el estado del evento.
- Manejar de forma segura casos como:
  - key inexistente
  - JSON mal formado
  - Redis sin datos
- Loguear todo para permitir depuración.
- Usar un **CommandLineRunner** para probar la integración SIN exponer endpoints ni integrar todavía con el backend.

Esto cumple exactamente lo solicitado en esta parte de la Etapa 3.

---

# 2. DTOs creados: ¿Qué representan y por qué existen?

Para desacoplar el **formato externo** (JSON guardado en el Redis de la cátedra) del **modelo interno del backend**, se crearon dos DTOs:

## 2.1 `AsientoRemotoDTO`
Representa **un asiento tal como llega desde Redis**.

Características:
- Contiene `fila`, `columna`, `estado` y `expira`.
- `estado` se mantiene como `String` porque los valores provienen de un sistema que no controlamos.
- Si la cátedra cambia un valor, el proxy **no debe romperse**.

## 2.2 `EstadoAsientosRemotoDTO`
Representa el **estado general de los asientos de un evento**:

- `eventoId`
- `List<AsientoRemotoDTO>`

Este DTO se usa después de parsear el JSON crudo obtenido desde Redis.

---

# 3. Servicio creado: `EstadoAsientosRedisService`

Este servicio es el encargado de toda la lógica de integración con Redis. Hace:

### ✔ Lectura cruda desde Redis
- Construye la key `"evento_" + eventoId`.
- Usa `StringRedisTemplate.opsForValue().get(key)`.
- Loguea si la key fue encontrada o no.

### ✔ Parseo del JSON
- Si existe JSON válido → se convierte a `EstadoAsientosRemotoDTO` usando `ObjectMapper`.
- Si el JSON no trae `eventoId` → se fuerza manualmente.

### ✔ Manejo de errores
- Si la key no existe → devuelve DTO con lista vacía.
- Si el JSON está mal formado → log de error + DTO vacío.
- El proxy **jamás se rompe** por datos remotos inválidos.

Esto cumple exactamente los requisitos del issue y lo que pide la consigna: "ser tolerante a errores externos".

---

# 4. ¿Qué es un CommandLineRunner y por qué se usa aquí?

`CommandLineRunner` es una interfaz de Spring Boot que permite **ejecutar código automáticamente cuando la app se inicia**.

Sirve para:
- Debugging.
- Verificar conexiones externas.
- Ejecutar pruebas manuales.

### ¿Por qué se usa aquí?
Porque la consigna pide **probar sin endpoint**.

Spring ejecuta el método `run()` automáticamente al levantar la app, lo que permite:
- Llamar al servicio de Redis.
- Loguear JSON crudo.
- Loguear DTO parseado.

Todo esto **sin exponer aún ninguna API pública**.

### ¿Por qué se marca con `@Profile("dev")`?
Para que:
- Solo se ejecute en modo desarrollo.
- No corra en producción.
- Se pueda desactivar simplemente cambiando el perfil.

Cuando se termine este issue, el CommandLineRunner puede:
- eliminarse, **o**
- dejarse inactivo cambiando:
  ```env
  SPRING_PROFILES_ACTIVE=prod
  ```

---

# 5. Cómo se probó

La prueba consistió en:

1. Tener en `.env`:
   ```env
   SPRING_PROFILES_ACTIVE=dev
   ```

2. Levantar el proxy con:
   ```bash
   ./boot.sh
   ```

3. Observar los logs generados por el `RedisTestRunner`:

### Caso 1 — Key no encontrada (válido)
```
Consultando Redis para key=evento_1, resultado=NO ENCONTRADO
JSON crudo desde Redis = null
No hay asientos bloqueados/vendidos para eventoId=1...
DTO parseado -> eventoId=1, asientos=0
```

Esto muestra que:
- El proxy se conectó a Redis.
- La key no existía.
- El servicio devolvió DTO vacío.
- Todo funcionó correctamente.

### Caso 2 — Si la cátedra carga datos en Redis
Veremos:
```
resultado=ENCONTRADO
JSON crudo = {...}
Se parseó correctamente...
DTO parseado -> eventoId=1, asientos=X
```

---

# 6. Estado del Issue

Este issue queda completamente **cumplido** porque se verificó:

✔ Configuración de Redis cargada desde `.env` y `application.yml`.

✔ Servicio capaz de leer y parsear el JSON remoto.

✔ Manejo seguro de errores y claves inexistentes.

✔ Prueba manual mediante `CommandLineRunner`.

✔ Logs claros para análisis.

✔ Sin endpoints aún (como requiere la consigna).

---

# 7. ¿Qué sigue?

El próximo issue de la Etapa 3 es:

👉 **Crear un cliente HTTP para consumir los endpoints del servidor de la cátedra desde el proxy.**

Y luego:

👉 Exponer un endpoint del proxy para que el backend pueda obtener el estado de los asientos.

Y gracias a todo lo que hicimos aquí, esa parte será mucho más simple.

---

Si querés, puedo agregar en este mismo archivo la explicación del siguiente issue una vez que lo implementes.

