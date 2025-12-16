
# Explicación del Listener Kafka en el Proxy - Issue #13

## ¿Qué hicimos?

Implementamos la primera funcionalidad real del **proxy-service**:  
un *Kafka Listener* que se conecta al servidor Kafka de la cátedra y escucha el tópico:

```
eventos-actualizacion
```

Cada vez que la cátedra genera un cambio en un evento (automático cada 2 horas o manual con el endpoint `/forzar-actualizacion`), Kafka produce un mensaje.  
Nuestro proxy ahora es capaz de **escuchar** esos mensajes.

---

## ¿Por qué hay que hacer esto?

Porque la consigna del TP indica que el backend del alumno **no debe consultar constantemente** el servidor de la cátedra.  
En cambio:  
✔ solo debe sincronizar eventos cuando haya cambios reales  
✔ esos cambios se notifican vía Kafka

Es decir:

1. La cátedra actualiza el evento  
2. Kafka envía un mensaje  
3. El proxy lo recibe  
4. (Más adelante) el proxy avisará al backend del alumno para sincronizar

Este mecanismo evita sobrecargar el servidor y simula un entorno real de sistemas distribuidos.

---

## ¿Cómo funciona lo que implementamos?

1. **Creamos la carpeta `messaging/`**  
   Para mantener organizado el código.  
   Ahí irán todos los listeners y productores de Kafka más adelante.

2. **Creamos `EventoKafkaListener.java`**  
   Esta clase está marcada con:

   ```java
   @Service
   public class EventoKafkaListener { ... }
   ```

   Y tiene un método:

   ```java
   @KafkaListener(topics = "eventos-actualizacion", groupId = "${PROXY_GROUP_ID:grupo-alumno}")
   public void onMessage(String message) {
       log.info("📩 Nuevo mensaje recibido desde Kafka: {}", message);
   }
   ```

3. **KafkaListener se queda escuchando el tópico**  
   Cuando el servidor cátedra publica un mensaje, nuestro proxy lo captura inmediatamente.

4. **El groupId es crucial**  
   Cada alumno debe tener un `groupId` único, si no todos comparten la misma posición del consumer y no funcionaría correctamente.

---

## ¿Qué falta por hacer más adelante?

Esto recién fue el primer paso.  
Luego deberás implementar:

- convertir el JSON del mensaje en un objeto Java  
- llamar al backend del alumno para sincronizar los eventos  
- consultar Redis para actualizar el estado de los asientos

Pero por ahora era 100% necesario:

✔ crear el proxy  
✔ configurarlo con Kafka  
✔ demostrar que escucha el tópico  
✔ usar un groupId único  

---

## Pregunta recomendada para el profesor

> **Hola Profe, ¿Kafka está actualmente levantado y aceptando conexiones para los alumnos?  
> Lo pregunto para verificar que el listener pueda recibir eventos.**

---