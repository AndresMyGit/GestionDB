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

## Como abrirlo

1. Abre `index.html` en el navegador.
2. Usa cualquier usuario y contrasena o presiona `Cargar demo`.
3. Entra al sistema y navega entre los modulos desde la barra lateral.

## Nota tecnica

La demo usa `localStorage` para mantener sincronizados los datos entre paginas.
Eso permite que una venta hecha en `ventas.html` aparezca luego en `facturas.html` o que un ajuste de inventario se vea en `inventario.html` y `resumen.html`.

Si luego quieres, el siguiente paso puede ser conectar esta interfaz con la logica Java y la base de datos real.
