# Camion Android

Kotlin mobile app migrated from the legacy WPF Camion2023 HMI.

## Features

- **HMI screen**: real-time temperature, humidity, voltage, process state, start/stop controls
- **Parameters screen**: equipment parameters with min/max validation and Modbus write hook
- **Role-based auth**: profile levels (1=Admin … 4=Operario) matching the legacy `Config` login rules
- **SQLite database**: `Furgones.db` schema aligned with the SQL Server EDMX model

## Database

The app ships with a pre-built SQLite database in `app/src/main/assets/Furgones.db`.
Regenerate it from the repo root:

```bash
python scripts/create_furgones_db.py
```

Default users (password in parentheses):

| IdUsuario | Perfil | Password |
|-----------|--------|----------|
| DESARROLLADOR | 0  | dev      |
| ADMIN     | 1      | admin    |
| SUPERVISOR| 2      | super    |
| TECNICO   | 3      | tec      |
| OPERARIO  | 4      | oper     |

## Build

Open `camion-android` in Android Studio, or:

```bash
cd camion-android
./gradlew assembleDebug
```

## Modbus

`SimulatedModbusClient` provides demo PLC values until a Modbus TCP client is connected to the truck PLC IP (`192.168.1.10:502` by default, from legacy settings).

## Legacy mapping

| WPF | Android |
|-----|---------|
| `Camion2020View` | `HmiScreen` |
| `EquiposParametrosHMI` | `ParametrosScreen` |
| `Config` | `ConfigScreen` |
| `PerfilesParametros` | Room + `ParametrosRepository.canEdit` |
| `UserButton.NivelAcceso` | `AppDestination.accessLevel` |
