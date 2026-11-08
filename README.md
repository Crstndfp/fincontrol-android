# FinControl Android — Captura Express de Gastos, Split Expenses & Mini Dashboard

Aplicación nativa Android desarrollada en **Kotlin + Jetpack Compose (Material 3)** bajo arquitectura **Offline-First (Room + WorkManager)**, sincronizada en tiempo real con [`fincontrol-web`](https://github.com/Crstndfp/fincontrol-web).

## Módulos Implementados
1. **Registro Express de Gastos (< 5 segundos — Módulo 3.1):**
   - Entrada numérica directa con selector bimonetario (`GTQ` por defecto / `USD`).
   - Autocompletado predictivo de comercios que pre-selecciona automáticamente la categoría.
   - Fecha automática del sistema y **Cero Bloqueo Tributario (`RN-03`)**: nunca exige NIT ni número de factura.
2. **Gestión de Gastos Compartidos / Split Expenses (`RN-01` — Módulo 3.2):**
   - Switch rápido con cálculo por **Porcentaje de Participación (`50%`, `33%`, `25%`)** o **Monto Asumido Propio**.
   - Contabiliza únicamente la parte propia en el presupuesto mensual y envía el resto a **Cuentas por Cobrar a Terceros**.
3. **Mini Dashboard Financiero (Módulo 3.3):**
   - Tarjeta de Liquidez Consolidada, Semáforo del Mes (Regla 50/30/20) y Alerta de fecha de corte/pago de contado para mantener condición de **Totalero (0% intereses)**.

## Estructura del Proyecto
- `app/src/main/java/com/fincontrol/mobile/domain/SplitExpenseCalculator.kt`: Reglas de negocio `RN-01`, `RN-05`, predicción de categorías y semáforo 50/30/20.
- `app/src/main/java/com/fincontrol/mobile/data/local/LocalExpenseDatabase.kt`: Entidades y DAO de Room SQLite.
- `app/src/main/java/com/fincontrol/mobile/data/sync/ExpenseSyncWorker.kt`: Sincronización en segundo plano con `fincontrol-web`.
- `app/src/main/java/com/fincontrol/mobile/MainActivity.kt`: UI en Jetpack Compose.
- `app/src/test/java/com/fincontrol/mobile/domain/SplitExpenseCalculatorTest.kt`: Pruebas unitarias JUnit 4.
- `docs/ARCHITECTURE.md`: Documentación interna de arquitectura y flujo de conciliación.
