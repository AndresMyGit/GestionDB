# AA1 Web Redesign

Reconstruccion visual del proyecto Java Swing `AA1` en una version web hecha con:

- `index.html`
- `resumen.html`
- `ventas.html`
- `productos.html`
- `clientes.html`
- `inventario.html`
- `credito.html`
- `facturas.html`
- `cortes.html`
- `empleados.html`
- `styles.css`
- `app.js`

## Estructura actual

- `index.html`: login de acceso
- `resumen.html`: panorama general del negocio
- `ventas.html`: flujo de venta y facturacion
- `productos.html`: alta y listado del catalogo
- `clientes.html`: consulta de clientes y estado de credito
- `inventario.html`: ajustes, devoluciones e historial
- `credito.html`: cartera y abonos
- `facturas.html`: busqueda y detalle de facturas
- `cortes.html`: resumen financiero por periodo
- `empleados.html`: administracion de empleados, cargos y despidos para gerencia

## Que conserva del proyecto original

- Login de acceso
- Panel principal con los mismos modulos base del sistema Swing
- Flujo de facturacion
- Control basico de cartera y stock

## Que mejora

- Cada modulo vive en su propio archivo HTML
- Navegacion lateral persistente entre paginas
- Diseno responsive
- Paneles mas claros y legibles
- Flujo de venta mas directo

## Como abrirlo con Oracle

1. Confirma que la base Oracle tenga cargado `baseDatosCreacion_oracle (1).sql`.
2. Desde PowerShell, entra a esta carpeta y ejecuta:

```powershell
$env:GESTIONDB_DB_USER="TU_USUARIO_ORACLE"
$env:GESTIONDB_DB_PASSWORD="TU_PASSWORD_ORACLE"
# Opcional si usas un wallet fuera del repo:
# $env:GESTIONDB_TNS_ADMIN="C:\ruta\al\wallet"
# Opcional si prefieres URL JDBC directa:
# $env:GESTIONDB_DB_URL="jdbc:oracle:thin:@//localhost:1521/freepdb1"
.\run-server.ps1
```

3. Abre `http://localhost:8081/`.
4. Inicia sesion con un empleado registrado en la base de datos.

## Nota tecnica

La web ahora usa un backend Java/JDBC:

- `backend/Conexion.java`: conexion Oracle configurable por entorno con soporte para `GESTIONDB_DB_URL` o wallet/TNS.
- `backend/Login.java`, `Resumen.java`, `Ventas.java`, `Productos.java`, `Clientes.java`, `Inventario.java`, `Credito.java`, `Facturas.java`, `Cortes.java`, `Empleados.java`: un endpoint por ventana HTML.
- `backend/GestionDBServer.java`: sirve el frontend, expone `/api/*` y agrega `/api/health` para health checks.
- `app.js`: consume `/api/*` y deja `localStorage` solo para la sesion local y el carrito temporal.

## Patrones de diseno en backend

- Singleton: `backend/Conexion.java` centraliza una unica instancia de configuracion y apertura de conexiones con `getInstance()`.
- Factory Method: `backend/HandlerFactory.java` crea los `HttpHandler` usados por `GestionDBServer`.
- Strategy: `backend/AccessPolicy.java` define la politica de acceso y `backend/ManagerAccessPolicy.java` implementa la regla de gerente.

Puedes cambiar la conexion sin editar codigo usando variables de entorno:

```powershell
$env:GESTIONDB_DB_USER="TU_USUARIO_ORACLE"
$env:GESTIONDB_DB_PASSWORD="TU_PASSWORD_ORACLE"

# Opcion A: URL JDBC directa
$env:GESTIONDB_DB_URL="jdbc:oracle:thin:@//localhost:1521/freepdb1"

# Opcion B: wallet/TNS
$env:GESTIONDB_TNS_ALIAS="gestiondb2_high"
$env:GESTIONDB_TNS_ADMIN="C:\ruta\al\wallet"
```

## Despliegue en Render

El repositorio ahora incluye:

- `Dockerfile`: compila el backend Java y empaqueta los archivos estaticos.
- `render.yaml`: blueprint para crear el servicio web en Render.
- `/api/health`: endpoint de health check para Render.

### Variables de entorno en Render

Minimas:

```text
GESTIONDB_DB_USER=TU_USUARIO_ORACLE
GESTIONDB_DB_PASSWORD=TU_PASSWORD_ORACLE
```

Conexion por URL JDBC:

```text
GESTIONDB_DB_URL=jdbc:oracle:thin:@//host:1521/servicio
```

Conexion por wallet/TNS:

```text
GESTIONDB_TNS_ALIAS=gestiondb2_high
GESTIONDB_TNS_ADMIN=/app/aa1-web/wallet
```

### Pasos

1. Sube este repo a GitHub.
2. En Render, crea un nuevo servicio usando `render.yaml` o conecta el repo como servicio Docker.
3. Configura `GESTIONDB_DB_USER` y `GESTIONDB_DB_PASSWORD`.
4. Si no vas a versionar el wallet dentro del repo, monta tus archivos secretos en Render y actualiza `GESTIONDB_TNS_ADMIN`.
5. Despliega y valida `https://tu-servicio.onrender.com/api/health`.
