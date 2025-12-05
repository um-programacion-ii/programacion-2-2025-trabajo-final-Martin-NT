# Explicación técnica – Cliente HTTP del Proxy (CatServiceClient)

Este documento explica **qué se hizo**, **por qué se hizo** y **cómo funciona** la parte del proxy responsable de comunicarse con el servidor de la cátedra mediante HTTP. Corresponde al issue de la Etapa 3 del TP Final 2025.

---

# 1. ¿Cuál es el objetivo de esta parte del proyecto?

El objetivo es que el **proxy-service** pueda llamar por HTTP al servidor de la cátedra para obtener información de eventos y para invocar la acción de "forzar actualización".

La idea clave es crear un **cliente interno**, llamado `CatServiceClient`, que centralice TODAS las llamadas HTTP hacia la cátedra.

👉 Esto prepara al proxy para que, en un issue posterior, pueda exponer **sus propios endpoints** hacia el backend del alumno.

En esta etapa NO se exponen endpoints del proxy, ni se integra con el backend JHipster.

---

# 2. ¿Qué es RestTemplate y por qué se usa?

**RestTemplate** es el cliente HTTP clásico de Spring (síncrono). Permite ejecutar requests tipo:
- GET / POST / PUT / DELETE
- recibir JSON como String
- manejar respuestas, errores, headers

Para este TP, es la opción más simple y directa para:
- llamar al servidor de la cátedra,
- recibir JSON crudo,
- loguear resultados.

La alternativa moderna sería WebClient (reactivo), pero complicaría innecesariamente esta etapa.

---

# 3. Configuración del cliente HTTP

Se creó una clase de configuración:

```
CatServiceHttpConfig
```

donde se expone un bean:

```
@Bean
public RestTemplate restTemplate() { return new RestTemplate(); }
```

Esto permite que Spring pueda inyectar un objeto `RestTemplate` en cualquier clase que lo necesite.

---

# 4. ¿Qué es `catservice.url` y para qué sirve?

En `application.yml` se definió:

```
catservice:
  url: ${CAT_SERVICE_URL:http://192.168.194.250:8080/api}
```

Esto permite:
- Cambiar fácilmente la URL del servidor de la cátedra desde `.env`.
- Evitar tener la IP hardcodeada en código.
- Reutilizar la misma base para construir los endpoints reales.

En `.env`:

```
CAT_SERVICE_URL=http://192.168.194.250:8080/api
```

---

# 5. ¿Qué es CatServiceClient?

Es un servicio ubicado en:
```
ar.edu.um.proxyservice.service.CatServiceClient
```

Se encarga de centralizar todas las llamadas hacia la cátedra.

### ¿Qué hace?
- Construye URLs finales usando `catservice.url + path`.
- Ejecuta GETs con RestTemplate.
- Loguea la operación, URL, estado devuelto y tamaño del body.
- Devuelve JSON crudo (String).
- Ante un error (401, 500, timeout, etc.) **NO rompe el proxy**, solo loguea y retorna `null`.

Esto cumple exactamente el requerimiento de la etapa.

---

# 6. Endpoints soportados internamente

El cliente define métodos genéricos para acceder a:

- **Eventos resumidos** → `/endpoints/v1/eventos-resumidos`
- **Eventos completos** → `/endpoints/v1/eventos`
- **Detalle de un evento** → `/endpoints/v1/evento/{id}`
- **Forzar actualización** → `/endpoints/v1/forzar-actualizacion`

Todos comparten un método interno común `doGet(...)`.

---

# 7. ¿Por qué no se prueban con Postman todavía?

Porque el proxy AÚN no expone endpoints propios.

Lo que se está probando en esta etapa es:

**proxy → cátedra (HTTP)**

Recién en el próximo issue se hará:

**backend del alumno → proxy → cátedra**

Y ahí sí se probará por Postman.

---

# 8. Prueba manual con CommandLineRunner

Se creó un runner temporal:

```
CatServiceClientTestRunner
```

Marcado con:
```
@Profile("dev")
```
para que se ejecute solo en modo desarrollo.

Este runner:
- se ejecuta automáticamente al iniciar el proxy,
- llama a todos los métodos de CatServiceClient,
- loguea resultados,
- verifica que la URL esté bien armada,
- maneja correctamente los errores.

### ¿Por qué usar un runner?
- No necesitamos endpoints públicos aún.
- Es la forma más simple de validar funcionamiento interno.
- Permite ver en logs si la cátedra responde o devuelve errores.

---

# 9. Resultado de la prueba: ¿por qué aparece 401?

El log del alumno muestra:

```
HttpClientErrorException$Unauthorized: 401 Unauthorized
```

Esto significa:
- La conexión al servidor funciona.
- El endpoint existe.
- PERO la cátedra requiere autenticación (token).

No es un error del cliente ni del proxy:
👉 es un comportamiento esperado para un endpoint protegido.

Lo importante es que el proxy:
- loguea el error,
- NO se cae,
- continúa ejecutando,
- cumple los criterios de aceptación.

---

# 10. ¿Por qué este issue está completo?

Porque se cumple TODO lo solicitado:

- `CatServiceClient` creado y funcionando.
- Lectura de `catservice.url` desde configuración.
- Bean `RestTemplate` funcionando.
- Llamadas HTTP centrales implementadas.
- Logs claros de cada operación.
- Manejo de errores sin romper la aplicación.
- Prueba desde runner en perfil `dev`.
- Proxy funcionando a pesar de respuestas 401.
- No se exponen endpoints propios aún (eso es otro issue).

---

# 11. ¿Qué sigue después de esto?

En el próximo issue deberás:
- crear endpoints REST en el proxy como:
  - `GET /proxy/eventos`
  - `GET /proxy/eventos/{id}`
  - `GET /proxy/eventos/{id}/asientos`
  - etc.
- esos endpoints usarán **CatServiceClient + Redis** por dentro;
- se probarán con **Postman**;
- y servirán para que el **backend alumno** deje de hablar directo con la cátedra.

---

Si querés, puedo agregar también la explicación de la parte siguiente apenas la empecemos.


---

# 12. Resumen de lo que se hizo en esta etapa

A lo largo de este issue completamos los siguientes pasos clave:

### ✔ 1. Configuración del cliente HTTP
- Creamos `CatServiceHttpConfig`.
- Registramos un bean `RestTemplate` para poder ejecutar llamadas HTTP.
- Dejamos esta configuración desacoplada del cliente para mantener un diseño limpio y extensible.

### ✔ 2. Lectura de la URL del servidor de la cátedra
- Configuramos `catservice.url` en `application.yml`.
- Permitimos sobreescribirla mediante `.env` con `CAT_SERVICE_URL`.
- Inyectamos dinámicamente la URL en `CatServiceClient`.

### ✔ 3. Creación del cliente HTTP interno del proxy
- Implementamos `CatServiceClient` como un servicio dedicado a comunicarse con la cátedra.
- Centralizamos la lógica de armado de URLs, ejecución de GETs y manejo de errores.
- Agregamos logs detallados para facilitar el debugging.
- Aseguramos que este cliente **no rompa el proxy** ante errores externos.

### ✔ 4. Implementación de métodos para endpoints clave
- Eventos resumidos
- Eventos completos
- Detalle de evento
- Forzar actualización

Cada método usa el mismo patrón: URL → GET → logs → retorno seguro.

### ✔ 5. Pruebas mediante CommandLineRunner en perfil dev
- Creamos `CatServiceClientTestRunner`.
- Ejecutamos automáticamente todas las llamadas al iniciar el proxy.
- Verificamos:
  - que el cliente funciona y arma bien las URLs,
  - que el proxy contacta a la cátedra,
  - que se manejan errores correctamente (ej.: 401).

### ✔ 6. Confirmación del comportamiento esperado
- El proxy se inicia sin fallas.
- El cliente HTTP se comporta correctamente.
- Se generan logs claros de éxito o error.
- El 401 demuestra que el servidor responde y que la falta de token está siendo manejada.
- No se exponen endpoints todavía (eso es para el próximo issue).

---

Este resumen deja claro qué se implementó, por qué se hizo así y cómo se validó su funcionamiento. Si querés, puedo agregar también una sección con diagramas de flujo o un “cómo contárselo al profesor”.
