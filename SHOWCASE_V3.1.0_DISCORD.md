# WaypointRPGExtension v3.1.0

¡La v3.1.0 ya está lista! Esta actualización amplía la integración con Typewriter,
mejora las rutas y los objetivos de entidad, y renueva el render de waypoints a través
de paredes sin convertir la configuración en algo complicado.

## Puntos clave

- Un solo `static_waypoint` puede renderizar todos los objetivos activos del jugador.
- Waypoints diferentes por misión mediante presets o themes reutilizables.
- Rutas con puntos intermedios y progreso compartido entre el waypoint, zonas y BetterHUD.
- Objetivos sobre entidades Bukkit y NPC de Typewriter.
- Glow privado por jugador para entidades y NPC packet-only de Typewriter.
- Iconos de BetterHUD seleccionables desde cada theme.
- BetterHUD continúa siendo una integración completamente opcional.
- Efecto de adquisición del símbolo al comenzar a trackear un objetivo.
- Beam de dos capas con núcleo giratorio opcional, full bright y materiales configurables.
- Movimiento V3 estable, bob suave, escalado por distancia y símbolo con snap cercano.
- Label y symbol permanecen verticales: siguen el yaw del jugador sin inclinarse con el pitch.

## Nuevos entries y configuración principal

### `quest_waypoint` — estilo por misión

Conecta directamente una Quest de Typewriter con el aspecto de sus waypoints. Solo se
elige la misión y uno de los presets `PURPLE`, `GREEN`, `RED`, `GOLD`, `BLUE`, `INHERIT`
o `CUSTOM`. Todas las location objectives de esa Quest heredan el resultado.

### `waypoint_path` — rutas guiadas

Asigna una lista ordenada de puntos intermedios a una location objective seleccionable.
El panel solo muestra `objective`, `loop` y `points`. El avance, salto de puntos ya
alcanzados y reinicio del progreso se administran internamente.

### `entity_waypoint` — entidades y NPC

Convierte una entidad en objetivo mientras su Audience esté activa. Admite UUID, nombre,
scoreboard tag y NPC compartidos de Typewriter. Puede heredar el theme de una Quest o
utilizar uno propio. El glow privado funciona tanto con entidades Bukkit como con los
IDs de paquete utilizados por los NPC de Typewriter.

### `waypoint_theme` — personalización reutilizable

Permite sobrescribir materiales y rotación del beam, textos, símbolos, sombras e icono
de BetterHUD. Es la opción avanzada; los configuradores normales pueden quedarse con
los presets simples de `quest_waypoint`.

### `waypoint_betterhud_bridge` — brújula opcional

Sincroniza objetivos, entidades o ambos con los puntos de brújula de BetterHUD. El icono
definido por el theme tiene prioridad sobre el icono fallback del bridge. La extensión
carga y funciona aunque BetterHUD no esté instalado.

### `waypoint_zone_trigger` — eventos de proximidad

Ejecuta triggers de entrada y salida al acercarse a objetivos, entidades o al punto
activo de una ruta. Puede dispararse una vez, rearmarse al salir o trabajar por target.

### `static_waypoint` — renderer global

Continúa siendo el renderer visual principal y recomendado. Controla label, symbol,
beam, bob, escalado, distancia de llegada y selección de múltiples targets. Normalmente
solo hace falta una instancia global bajo una `world_audience`.

## Nuevo sistema Smart See-Through

- Label y symbol se evalúan de manera independiente.
- El label comprueba centro, lados, parte superior e inferior.
- El symbol comprueba centro y lados.
- Los bloques opacos activan la protección inmediatamente.
- Hojas, puertas, trampillas y formas parciales usan cobertura ponderada e histéresis
  para evitar flashes.
- Cristal, cristal tintado y paneles se consideran transparentes. El rayo continúa y
  todavía puede detectar una pared situada detrás del cristal.
- Los core shaders protegen el texto completo, incluidas líneas y sombras, y mantienen
  el beam detrás del label y del symbol.
- Todos estos valores son internos para evitar que una mala configuración rompa el
  waypoint. El debug está temporalmente desactivado.

## Compatibilidad

- Typewriter `0.9.0-beta-173`
- Typewriter `0.9.0-beta-174`
- Typewriter `0.9.0-beta-175`
- BetterHUD: opcional
- Cliente: no requiere mods; debe aceptar el resource pack del servidor

Los core shaders se generan en:

```text
plugins/WaypointRPGExtension/resourcepack/
```

Esa carpeta puede fusionarse con el resource pack final mediante CraftEngine. Después
de actualizar la extensión o los shaders, es necesario reconstruir y reenviar el pack.

> `tracked_locatable_waypoint` permanece archivado únicamente como referencia histórica
> y no aparece en el panel de Typewriter.
