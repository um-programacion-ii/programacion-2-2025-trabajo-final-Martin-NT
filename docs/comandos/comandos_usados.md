# Comandos Utilizados 

Esta sección reúne todos los comandos utilizados desde el inicio hasta el final del proyecto

---

# Backend

---

# Node.js, NPM y NVM

## Instalar NVM
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
```

## Cargar NVM sin reiniciar
```bash
. "$HOME/.nvm/nvm.sh"
```

## Instalar Node.js
```bash
nvm install 24
```

## Verificar versiones
```bash
node -v
npm -v
```

---

# JHipster

## Instalar JHipster
```bash
npm install -g generator-jhipster
```

## Verificar versión
```bash
jhipster --version
```

## Generar proyecto
```bash
jhipster
```

---

# ZeroTier (red de la cátedra)

## Instalar ZeroTier
```bash
curl -s https://install.zerotier.com | sudo bash
```

## Ver estado
```bash
sudo systemctl status zerotier-one
```

## Unirse a la red
```bash
sudo zerotier-cli join 93afae59630d1f1d
```

## Info del nodo
```bash
sudo zerotier-cli info
```

---

# Validaciones tras habilitación del profesor #4

## 0. Ver que Docker y tus servicios locales están vivos
```bash
docker ps
```
- Respuesta:
```bash
CONTAINER ID   IMAGE           COMMAND                  CREATED      STATUS                 PORTS                      NAMES
db5464b7b9d6   postgres:17.4   "docker-entrypoint.s…"   4 days ago   Up 7 hours (healthy)   127.0.0.1:5432->5432/tcp   backendcatedra-postgresql-1
```
## 1. Ver si estoy habilitado en ZeroTier
```bash
sudo zerotier-cli listnetworks
```
- Respuesta:
```bash
200 listnetworks <nwid> <name> <mac> <status> <type> <dev> <ZT assigned ips>
200 listnetworks 93afae59630d1f1d programacion2 1e:60:d7:74:ea:a5 OK PRIVATE ztzlgkgrt6 192.168.194.64/24
```
## 2. Probar HTTP contra el servidor de la cátedra
```bash
curl -v http://192.168.194.250:8080
curl -v http://192.168.194.250:8080/actuator/health
```
- Ambos deben dar:
```bash
HTTP/1.1 200 OK
```

## 3. Probar Redis remoto de la cátedra
```bash
redis-cli -h 192.168.194.250 -p 6379 ping
```
Respuesta:
```bash
PONG
```

## 4. Levantar backend con la config remota
```bash
./mvnw
```

## 5. Probar autenticación /api/authenticate
Con el backend corriendo (no cierres la terminal del ./mvnw), en postman o en otra terminal:
```bash
curl -X POST http://localhost:8080/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin","rememberMe":true}'
```
Esperado:
```bash
{
  "id_token" : "eyJhbGciOiJIUzUxMiJ9...."
}
```

---

# Backend JHipster

## Levantar PostgreSQL y Redis (contenedores generados por JHipster)
```bash
docker compose   -f src/main/docker/postgresql.yml   -f src/main/docker/redis.yml   up -d
```

## Levantar backend
```bash
./mvnw
```

---

# Probar autenticación JWT (Postman o curl)

## Postman
- POST
```bash
http://localhost:8080/api/authenticate
```
- Body JSON
```bash
{
  "username": "user",
  "password": "user",
  "rememberMe": false
}
```
## Curl
```bash
curl -X POST http://localhost:8080/api/authenticate   -H "Content-Type: application/json"   -d '{"username":"user","password":"user"}'
```

---

# Redis

## Ver si Redis está ejecutándose en tu máquina
```bash
sudo lsof -i :6379
```

## Probar conexión Redis
```bash
redis-cli -h localhost -p 6379 ping
```
---

# PostgreSQL Manual (opcional / no requerido)

Solo si deseas instalar PostgreSQL fuera de Docker.

## Instalar PostgreSQL
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib -y
```

## Habilitar y verificar servicio
```bash
sudo systemctl enable postgresql
sudo systemctl start postgresql
sudo systemctl status postgresql
```

## Crear base de datos y usuario
```bash
sudo -u postgres psql

CREATE DATABASE "backendCatedra";
CREATE USER "backendCatedra" WITH PASSWORD 'backendCatedra';
GRANT ALL PRIVILEGES ON DATABASE "backendCatedra" TO "backendCatedra";

\q
```

## Probar conexión
```bash
psql -U backendCatedra -d backendCatedra
```

---

# Crear entidades
```bash
jhipster entity Evento
jhipster entity Asiento
jhipster entity Venta
```

## Entidad Asiento
```bash
The entity Asiento is being created.


Generating field #1

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? fila
✔ What is the type of your field? Integer
✔ Do you want to add validation rules to your field? Yes
✔ Which validation rules do you want to add? Required

================= Asiento =================
Fields
fila (Integer) required


Generating field #2

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? columna
✔ What is the type of your field? Integer
✔ Do you want to add validation rules to your field? Yes
✔ Which validation rules do you want to add? Required

================= Asiento =================
Fields
fila (Integer) required
columna (Integer) required


Generating field #3

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? estado
✔ What is the type of your field? Enumeration (Java enum type)
✔ What is the class name of your enumeration? AsientoEstado
✔ What are the values of your enumeration (separated by comma, no spaces)? LIBRE,BLOQUEADO,VENDIDO
✔ Do you want to add validation rules to your field? Yes
✔ Which validation rules do you want to add? Required

================= Asiento =================
Fields
fila (Integer) required
columna (Integer) required
estado (AsientoEstado) required


Generating field #4

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? personaActual
✔ What is the type of your field? String
✔ Do you want to add validation rules to your field? No

================= Asiento =================
Fields
fila (Integer) required
columna (Integer) required
estado (AsientoEstado) required
personaActual (String) 


Generating field #5

✔ Do you want to add a field to your entity? No

================= Asiento =================
Fields
fila (Integer) required
columna (Integer) required
estado (AsientoEstado) required
personaActual (String) 


Generating relationships to other entities

✔ Do you want to add a relationship to another entity? Yes
✔ What is the other entity? Evento
✔ What is the name of the relationship? evento
✔ What is the type of the relationship? many-to-one
✔ Do you want to generate a bidirectional relationship Yes
✔ What is the name of this relationship in the other entity? asientos
✔ When you display this relationship on client-side, which field from 'Evento' do you want to use? This field will be displayed as a String, so it cannot be a Blob titulo
✔ Do you want to add any validation rules to this relationship? Yes
✔ Which validation rules do you want to add? Required

Generating relationships to other entities

✔ Do you want to add a relationship to another entity? No

================= Asiento =================
Fields
fila (Integer) required
columna (Integer) required
estado (AsientoEstado) required
personaActual (String) 

Relationships
evento (Evento) many-to-one required



✔ Do you want to use separate service class for your business logic? Yes, generate a separate service interface and implementation
✔ Do you want to use a Data Transfer Object (DTO)? Yes, generate a DTO with MapStruct
✔ Do you want to add filtering? Not needed
✔ Is this entity read-only? No
✔ Do you want pagination and sorting on your entity? No

Everything is configured, generating the entity...

```
- A todo lo otro lo marco como yes

## Entidad Venta
```bash
INFO! 
The entity Venta is being created.


Generating field #1

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? fechaVenta
✔ What is the type of your field? LocalDate
✔ Do you want to add validation rules to your field? Yes
✔ Which validation rules do you want to add? Required

================= Venta =================
Fields
fechaVenta (LocalDate) required


Generating field #2

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? estado
✔ What is the type of your field? Enumeration (Java enum type)
✔ What is the class name of your enumeration? VentaEstado
✔ What are the values of your enumeration (separated by comma, no spaces)? PENDIENTE,EXITOSA,FALLIDA
✔ Do you want to add validation rules to your field? Yes
✔ Which validation rules do you want to add? Required

================= Venta =================
Fields
fechaVenta (LocalDate) required
estado (VentaEstado) required


Generating field #3

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? descripcion
✔ What is the type of your field? String
✔ Do you want to add validation rules to your field? No

================= Venta =================
Fields
fechaVenta (LocalDate) required
estado (VentaEstado) required
descripcion (String) 


Generating field #4

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? precioVenta
✔ What is the type of your field? BigDecimal
✔ Do you want to add validation rules to your field? Yes
✔ Which validation rules do you want to add? Required

================= Venta =================
Fields
fechaVenta (LocalDate) required
estado (VentaEstado) required
descripcion (String) 
precioVenta (BigDecimal) required


Generating field #5

✔ Do you want to add a field to your entity? Yes
✔ What is the name of your field? cantidadAsientos
✔ What is the type of your field? Integer
✔ Do you want to add validation rules to your field? Yes
✔ Which validation rules do you want to add? Required

================= Venta =================
Fields
fechaVenta (LocalDate) required
estado (VentaEstado) required
descripcion (String) 
precioVenta (BigDecimal) required
cantidadAsientos (Integer) required


Generating field #6

✔ Do you want to add a field to your entity? No

================= Venta =================
Fields
fechaVenta (LocalDate) required
estado (VentaEstado) required
descripcion (String) 
precioVenta (BigDecimal) required
cantidadAsientos (Integer) required


Generating relationships to other entities

✔ Do you want to add a relationship to another entity? Yes
✔ What is the other entity? Evento
✔ What is the name of the relationship? evento
✔ What is the type of the relationship? many-to-one
✔ Do you want to generate a bidirectional relationship Yes
✔ What is the name of this relationship in the other entity? ventas
✔ When you display this relationship on client-side, which field from 'Evento' do you want to use? This field will be displayed as a String, so it cannot be a Blob titulo
✔ Do you want to add any validation rules to this relationship? Yes
✔ Which validation rules do you want to add? Required

Generating relationships to other entities

✔ Do you want to add a relationship to another entity? Yes
✔ What is the other entity? Asiento
✔ What is the name of the relationship? asientos
✔ What is the type of the relationship? many-to-many
✔ What is the name of this relationship in the other entity? ventas
✔ When you display this relationship on client-side, which field from 'Asiento' do you want to use? This field will be displayed as a String, so it cannot be a Blob id
✔ Do you want to add any validation rules to this relationship? No

Generating relationships to other entities

✔ Do you want to add a relationship to another entity? No

================= Venta =================
Fields
fechaVenta (LocalDate) required
estado (VentaEstado) required
descripcion (String) 
precioVenta (BigDecimal) required
cantidadAsientos (Integer) required

Relationships
evento (Evento) many-to-one required
asientos (Asiento) many-to-many 



✔ Do you want to use separate service class for your business logic? Yes, generate a separate service interface and implementation
✔ Do you want to use a Data Transfer Object (DTO)? Yes, generate a DTO with MapStruct
✔ Do you want to add filtering? Not needed
✔ Is this entity read-only? No
✔ Do you want pagination and sorting on your entity? No

Everything is configured, generating the entity...
```

# Issues #5
docker compose -f src/main/docker/postgresql.yml -f src/main/docker/redis.yml up -d

./mvnw

docker exec -it backendcatedra-postgresql-1 psql -U backendCatedra

\dt

psql (17.4 (Debian 17.4-1.pgdg120+2))
Type "help" for help.

backendCatedra=# \dt
                    List of relations
 Schema |         Name          | Type  |     Owner      
--------+-----------------------+-------+----------------
 public | asiento               | table | backendCatedra
 public | databasechangelog     | table | backendCatedra
 public | databasechangeloglock | table | backendCatedra
 public | evento                | table | backendCatedra
 public | jhi_authority         | table | backendCatedra
 public | jhi_user              | table | backendCatedra
 public | jhi_user_authority    | table | backendCatedra
 public | rel_venta__asientos   | table | backendCatedra
 public | venta                 | table | backendCatedra
(9 rows)

# Sesiones de Redis (Persistencia y Concurrencia) #6

1. Ejecutar el Backend
Abrir terminal en la raíz del proyecto y correr:
```bash
./mvnw
```
2. Pruebas en Postman (Carpeta: Sesiones de Redis)

Paso 1: Login (Obtener Token)
- Método: POST
- URL: http://localhost:8080/api/authenticate
- Body (JSON):
```bash
{
    "username": "user",
    "password": "user",
    "rememberMe": true
}
```
Respuesta: el valor de id_token. 
- Lo copiamos para el siguiente paso
```bash
{
    "id_token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

Paso 2: Guardar Estado
- Método: POST
- URL: http://localhost:8080/api/session/evento
- Auth: Seleccionar Bearer Token y pegar el token del Paso 1.
- Body (JSON):
```bash
{ 
    "eventoId": 555 
}
```
Resultado esperado: Status 200 OK.

Paso 3: Verificar antes de apagar
- Método: GET
- URL: http://localhost:8080/api/session/evento
- Auth: Bearer Token (mismo token).
- Respuesta esperada:
```bash
{ 
    "eventoId": 555 
}
```

3. Prueba de Persistencia (Reinicio)

Paso 4: Apagar y Reiniciar
- Ir a la terminal donde corre el backend.
- Presionar Ctrl + C para detener el proceso.
- Volver a ejecutar ./mvnw y esperar a que arranque completamente.

Paso 5: Verificar después de prender
- Vuelve a Postman y repite la petición del Paso 3 (GET).
- Auth: Bearer Token (mismo token anterior).
- Resultado: Si devuelve {"eventoId": 555}, ¡la persistencia funciona! * Redis guardó el dato de sesión mientras el servidor Java estaba apagado.

---

# Proxy

---

## Crear Proxy con Spring Initializr
```bash
curl https://start.spring.io/starter.zip \
  -d dependencies=web,actuator,kafka,data-redis,security \
  -d language=java \
  -d type=maven-project \
  -d groupId=ar.edu.um \
  -d artifactId=proxy \
  -d name=proxy \
  -d packageName=ar.edu.um.proxyservice \
  -d javaVersion=17 \
  -o proxy.zip
```
```bash
unzip proxy.zip
rm proxy.zip
```

## Correrlo
Ubicarse en el proxy
```bash
./mvnw spring-boot:run
```
- El proxy está corriendo en: http://localhost:8081

#

chmod +x install.sh
chmod +x boot.sh


./install.sh
./boot.sh


## GET – Forzar actualización de eventos (cátedra)
1. Abre postman y ejecuta Forzar actualización de eventos (cátedra)
- URL: http://192.168.194.250:8080/api/endpoints/v1/forzar-actualizacion
- en Headers:
| Key           | Value                         |
| ------------- | ----------------------------- |
| Authorization | Bearer TU_TOKEN_DE_LA_CATEDRA |
- enviar 
- 200 OK


Kafka

sudo nano /etc/hosts
agregar: 192.168.194.250    kafka
Guardar y salir en nano:
Ctrl + O → Enter
Ctrl + X

para probar desde consola:
ping -c 3 kafka

deberia ver: PING kafka (192.168.194.250) ...


http://192.168.194.250:8080/
