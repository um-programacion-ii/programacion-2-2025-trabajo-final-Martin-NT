# Seguridad del Backend y Proxy

## 1. Seguridad en el Backend (JHipster)

El backend funciona como una **API REST stateless protegida con JWT**. Utiliza `JwtEncoder` y `JwtDecoder` para emitir y validar tokens firmados con una clave Base64 configurada en `application-dev.yml`.

### Características principales:
- Autenticación y autorización mediante **JWT**.
- Política **stateless** (no guarda sesiones en memoria).
- Endpoints públicos: `/api/authenticate`, `register`, `reset-password`, estáticos.
- Endpoints protegidos: `/api/**`.
- Endpoints administrativos: requieren rol `ADMIN`.

Los usuarios deben enviar:
```
Authorization: Bearer <jwt>
```
para acceder a la API protegida.

---

## 2. Opciones de Seguridad para el Proxy

La consigna establece que el **proxy también debe manejar seguridad**, es decir, no puede quedar completamente abierto.

Se identifican dos enfoques posibles:

---

## 2.1. Opción A – Proxy como Resource Server JWT

El proxy valida el **mismo JWT que emite el backend**.  
El backend envía el JWT del usuario al llamar al proxy, y el proxy lo valida utilizando la misma clave secreta.

### Pros
- Arquitectura coherente con JWT end‑to‑end.
- El proxy puede conocer identidad y roles del usuario.

### Contras
- Es necesario compartir la secret del backend (más sensible).
- Más complejo de configurar.

---

## 2.2. Opción B – API Key Interna (Recomendada)

El proxy exige un header interno compartido **solo entre backend y proxy**:

```
X-PROXY-KEY: valor-secreto
```

El valor se configura por `.env` y se valida en un filtro antes de permitir acceso a `/api/proxy/**`.

### Pros
- Simplicidad de implementación.
- No requiere compartir secretos de JWT del backend.
- Muy seguro si se mantiene dentro de la red backend ↔ proxy.

### Contras
- No transmite identidad del usuario (solo autenticación técnica entre servicios).

---

## 3. Conclusión

Ambas alternativas cumplen con la consigna y permiten que el proxy maneje seguridad.  
En general:

- **JWT Resource Server** es más elegante si se busca un flujo completamente unificado JWT.
- **API Key interna** es más simple, menos propensa a errores y evita exponer secretos del backend.

Para el TP Final, la API Key suele ser más práctica y suficiente salvo que el profesor especifique lo contrario.
