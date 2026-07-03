# Tested Versions — WaypointRPGExtension

> **Versión:** `v0.1.0-dev`  
> **Descripción:** historial del entorno probado, resultados verificables y cambios mayores.  
> **Modificado:** viernes, 3 de julio de 2026, 12:22 (America/Lima).

## Reglas de registro

- Registrar día, fecha, hora y zona horaria.
- Separar pruebas `BUILD` y `MANUAL`.
- Solo declarar estable lo confirmado explícitamente por el usuario.
- Cada versión estable debe corresponder a un commit y push.
- Describir ampliamente únicamente cambios grandes, como V2.

## Estado actual

| Versión | Momento | Verificación | Estado |
|---|---|---|---|
| `v0.1.0-dev` | viernes, 3 de julio de 2026, 12:22 `-05:00` | Documentación alineada con el source V2 | Desarrollo |
| build `1.0.0` | martes, 30 de junio de 2026; hora no registrada | `BUILD SUCCESSFUL` histórico | Build aprobado; pruebas manuales no certificadas |

## Cambio mayor — V2

Implementado alrededor del martes, 30 de junio de 2026. La hora exacta no quedó registrada.

V2 reemplazó el constructor plano por las secciones `general`, `target`, `label`, `symbol`, `beam`, `bob`, `trail`, `routes`, `integrations` y `performance`.

Fue un breaking change para JSON V1 avanzados. `simple_tracked_waypoint` quedó como adaptador deprecado. Además incorporó:

- materiales nativos para el beam;
- frecuencia independiente mediante `beamTickRate`;
- múltiples objectives simultáneos;
- separación lateral de labels y symbols;
- `snapPosition` configurable;
- smoothing lateral mejorado;
- IDs BetterHUD reforzados con el ID del entry.

## Entorno de referencia

| Componente | Versión |
|---|---|
| Paper/Minecraft objetivo | `1.21.1`, según documentación histórica |
| Java | `21` |
| Kotlin | `2.2.10` |
| Typewriter | `0.9.0-beta-173` |
| Typewriter module plugin | `2.1.0` |
| PacketEvents | `2.9.4` |
| BetterHUD compile API | `1.14.1` |

## Pruebas funcionales confirmadas

No existe todavía una versión estable certificada en este historial. Las pruebas antiguas estaban pendientes o fueron descritas informalmente; no se convierten retroactivamente en resultados aprobados.

Formato para próximos resultados:

| Versión | Fecha y hora | Tipo | Resultado | Commit |
|---|---|---|---|---|
| `v0.1.0` | `día, DD de mes de AAAA, HH:mm TZ` | `BUILD + MANUAL` | Función concreta probada | SHA |

## Compatibilidad pendiente de prueba

- Paper y Typewriter reales del servidor con manifests V2.
- PacketEvents junto con CraftEngine.
- BetterHUD runtime frente a la API de compilación `1.14.1`.
- Varias quests activas y cleanup de slots.
- Rutas reactivadas dentro de la misma sesión.
- Glow combinado con otros shared entity flags.
- Manifests V1 mediante `simple_tracked_waypoint`.
