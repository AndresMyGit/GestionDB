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
.\run-server.ps1
```

3. Abre `http://localhost:8081/`.
4. Inicia sesion con un empleado registrado en la base de datos.

## Nota tecnica

La web ahora usa un backend Java/JDBC:

- `backend/Conexion.java`: conexion Oracle configurable por entorno (servicio `freepdb1` por defecto).
- `backend/Login.java`, `Resumen.java`, `Ventas.java`, `Productos.java`, `Clientes.java`, `Inventario.java`, `Credito.java`, `Facturas.java`, `Cortes.java`: un endpoint por ventana HTML.
- `app.js`: consume `/api/*` y deja `localStorage` solo para la sesion local y el carrito temporal.

Puedes cambiar la conexion sin editar codigo usando variables de entorno:

```powershell
$env:GESTIONDB_DB_URL="jdbc:oracle:thin:@//localhost:1521/freepdb1"
$env:GESTIONDB_DB_USER="TU_USUARIO_ORACLE"
$env:GESTIONDB_DB_PASSWORD="TU_PASSWORD_ORACLE"
```
