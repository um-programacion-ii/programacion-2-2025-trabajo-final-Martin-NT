# Resumen del Proxy-Service (Proxy de Integración con la Cátedra)

## ¿Qué es el Proxy?
El **proxy-service** es un microservicio independiente que actúa como **intermediario** entre:
- el **backend local** del alumno (tu JHipster)
- el **servidor de la cátedra** (eventos, Kafka, Redis, sincronización)

Su función principal es manejar TODA la comunicación externa para **proteger**, **desacoplar** y **sincronizar** tu backend.

---

## ¿Por qué existe el Proxy?
El backend local **no debe conectarse directamente** a la cátedra.  
El proxy:
- evita problemas de seguridad  
- desacopla tu backend de cambios en la API de la cátedra  
- facilita pruebas y mantenimiento  
- maneja fallas de red  
- centraliza la integración

Si mañana cambia la API de la cátedra → **solo se modifica el proxy, no tu backend**.

---

## ¿Qué hace el Proxy?

### 1. Consumir Kafka (`eventos-actualizacion`)
El servidor de la cátedra envía mensajes cuando:
- cambia un evento,
- se actualiza información,
- se modifican asientos,
- se detectan ventas.

El proxy escucha el topic:
```
eventos-actualizacion
```
Cada alumno debe tener un **groupId único** para no interferir con los demás.

---

### 2. Leer eventos desde el servidor de la cátedra (HTTP)
El proxy debe llamar a los endpoints reales del servidor:
```
GET http://192.168.194.250:8080/api/eventos
GET http://192.168.194.250:8080/api/eventos/{id}
```
El backend local luego le pide información al proxy, **no directamente a la cátedra**.

---

### 3. Leer Redis remoto de la cátedra
Redis almacena el estado de asientos:
- **BLOQUEADO**
- **VENDIDO**

Con claves como:
```
evento_1
evento_7
```

El proxy consulta esos datos para saber qué asientos están ocupados o bloqueados.

---

### 4. Forzar sincronización
Existe un endpoint:
```
/api/endpoints/v1/forzar-actualizacion
```
que el proxy podrá usar para sincronizar cambios **de inmediato** sin esperar el proceso automático de 2 horas.

---

### 5. Sincronizar con el backend local
El flujo final será:
1. Proxy detecta cambios en Kafka  
2. Proxy consulta datos actualizados (HTTP + Redis)  
3. Proxy se los envía al backend  
4. El backend actualiza sus entidades locales (Evento, Asiento, Venta)

---

## ¿Qué NO hace el Proxy?
- ❌ No guarda datos  
- ❌ No usa base de datos  
- ❌ No maneja usuarios  
- ❌ No maneja ventas  
- ❌ No maneja sesiones  
- ❌ No sirve el frontend

Su única responsabilidad es **integrar** y **sincronizar**.

---

## Explicación corta (ideal para decirle al profesor)
> “El proxy es un microservicio intermediario que conecta mi backend local con el servidor de la cátedra.  
> Escucha cambios por Kafka, consulta eventos vía HTTP, obtiene estados de asientos desde Redis y prepara la información para sincronizar el backend local.  
> El backend nunca habla directo con la cátedra; siempre lo hace a través del proxy.”

---
