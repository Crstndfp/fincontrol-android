# Arquitectura Móvil — FinControl Android (Offline-First & Red Local LAN)

## 1. Contexto de Red & Operación Offline
La aplicación opera la mayor parte del tiempo **desconectada (Offline)** en el punto de venta (restaurantes, comercios, gasolineras). El servidor backend (`fincontrol-web`) reside en una **red interna local (LAN/On-Premise)** sin salida pública a internet.

```mermaid
flowchart TD
    subgraph Exterior["🛍️ Punto de Venta (100% Offline)"]
        U["Usuario"] --> CAP["Captura Express (< 5s)\nTeclado Numérico + Split"]
        CAP --> ROOM[("Room SQLite\nPersistencia Instantánea\n(syncStatus = PENDING_SYNC)")]
    end

    subgraph OficinaHogar["🏠 Conexión a Red Local (LAN / WiFi)"]
        ROOM --> AUTO["Detección Automática\nde Red Interna"]
        ROOM --> BTN["Botón Manual 'Sincronizar'\nen Cabecera"]
        AUTO --> WORKER["ExpenseSyncWorker (WorkManager)"]
        BTN --> WORKER
        WORKER -->|POST /api/v1/sync\n(Push Gastos + Pull Estado)| SERVER["Servidor Interno Next.js\n(HTTP o HTTPS Local)"]
        SERVER --> ROOM
    end
```

## 2. Mecanismo de Sincronización Bidireccional (`POST /api/v1/sync`)
1. **Push (Subida)**:
   - Recopila todas las transacciones guardadas localmente con `syncStatus = 'PENDING_SYNC'` y las envía en bloque al backend.
   - Tras recibir respuesta exitosa, se actualizan localmente a `SYNCED_AWAITING_PDF_RECONCILE`.
2. **Pull (Bajada & Cache)**:
   - El backend retorna los saldos actualizados de cuentas (tras procesar extractos PDF en la web), el techo presupuestario 50/30/20 y la cuenta regresiva de tarjetas Totalero.
   - Estos valores se almacenan localmente para que el Mini Dashboard funcione de forma autónoma aun cuando el usuario vuelva a salir a la calle.

## 3. Configuración de Red Local (HTTP y HTTPS)
- **`app/src/main/res/xml/network_security_config.xml`**:
  - Habilita `cleartextTrafficPermitted="true"` para permitir conexiones HTTP directas a IPs de red local (`192.168.*`, `10.*`, `172.16.*`, `10.0.2.2`).
  - Habilita certificados de usuario y de sistema para permitir conexiones seguras HTTPS con certificados autofirmados o CAs privadas internas.
- **Configurador de URL en Caliente (UI)**:
  - Icono de engrane en la cabecera que permite al usuario definir la URL base del servidor (ej. `http://192.168.1.150:3000/api/v1/`), almacenada en `SharedPreferences` sin necesidad de recompilar el código.
