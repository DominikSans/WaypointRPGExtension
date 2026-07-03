# Documentación Completa de Typewriter

Plugin de Minecraft Paper para crear experiencias interactivas: NPCs, diálogos, quests, cinemáticas y más.
Toda la configuración se gestiona desde un panel web visual.

---

## Estructura de la Documentación

| # | Archivo | Contenido |
|---|---------|-----------|
| 01 | [Instalación y Configuración](01-INSTALACION.md) | Requisitos, instalación, panel web, puertos, troubleshooting |
| 02 | [Conceptos Core](02-CONCEPTOS-CORE.md) | Pages, Entries, Facts, Criteria/Modifiers, Audiences, Variables |
| 03 | [Panel Web y UI](03-PANEL-WEB-UI.md) | Layout, inspector, atajos de teclado, búsqueda, snippets |
| 04 | [Interacciones y Diálogos](04-INTERACCIONES-DIALOGOS.md) | Secuencias, opciones, diálogos condicionales, contexto |
| 05 | [Sistema de Quests](05-QUESTS.md) | Quest entries, objectives, tracking, status, placeholders |
| 06 | [Cinemáticas](06-CINEMATICAS.md) | Camera paths, diálogos, entidades, skippable, world-specific |
| 07 | [Entidades y NPCs](07-ENTIDADES-NPCS.md) | Definiciones, instancias, skins, actividades, road networks |
| 08 | [Referencia de Comandos](08-COMANDOS.md) | Todos los comandos con permisos y ejemplos |
| 09 | [Referencia de Adapters](09-ADAPTERS-REFERENCIA.md) | Todos los adapters con sus entries, campos y tipos |
| 10 | [Arquitectura del Engine](10-ARQUITECTURA-ENGINE.md) | Módulos, sistemas internos, ciclo de vida, patrones de diseño |
| 11 | [Guía de Desarrollo de Extensiones](11-DESARROLLO-EXTENSIONES.md) | Setup, anotaciones, entry types, DI, build, deploy |
| 12 | [Patrones de Código](12-PATRONES-CODIGO.md) | Ejemplos reales de cada tipo de entry con código Kotlin |
| 13 | [PlaceholderAPI](13-PLACEHOLDERAPI.md) | Placeholders disponibles, parámetros, custom placeholders |

---

## Información General

- **Plataforma**: Solo Paper (NO Spigot/Bukkit)
- **Lenguaje de extensiones**: Kotlin (NO Java)
- **JDK requerido**: 21+
- **Dependencia obligatoria**: PacketEvents
- **Extensión mínima**: BasicExtension
- **Panel web**: Puertos 8080 (panel) + 9092 (websocket)
- **Repositorio**: github.com/gabber235/TypeWriter
# Instalación y Configuración

## Requisitos

- Servidor **Paper** (NO Spigot, NO Bukkit)
- Plugin **PacketEvents** (v2.5.x para Typewriter v0.5.1)
- **NO tener instalados**: InteractiveChat, CustomNamePlates (v3.0.15 o inferior)

## Instalación del Plugin

1. Descargar la última versión del plugin Typewriter
2. Colocar el JAR en la carpeta `plugins/`
3. Descargar PacketEvents y colocarlo en `plugins/`
4. Descargar BasicExtension y colocarlo en `plugins/Typewriter/extensions/`
5. Reiniciar el servidor

### Estructura de carpetas

```
plugins/
├── Typewriter.jar
├── PacketEvents.jar
├── Typewriter/
│   ├── config.yml
│   ├── snippets.yml
│   ├── extensions/
│   │   ├── BasicExtension.jar
│   │   ├── EntityExtension.jar
│   │   └── (otras extensiones)
│   └── pages/
│       └── (archivos JSON de páginas)
```

> **IMPORTANTE**: Las extensiones van en `plugins/Typewriter/extensions/`, NO en `plugins/`.
> Plugin y extensiones DEBEN ser de la misma versión.

---

## Configuración del Panel Web

El panel web necesita **dos puertos abiertos**: 8080 (panel) y 9092 (websocket).

### config.yml

```yaml
# plugins/Typewriter/config.yml

# Habilitar panel web y websockets
enabled: true

# IP del servidor. NO incluir puerto aquí.
hostname: "127.0.0.1"

websocket:
  # Puerto del websocket. Debe estar abierto.
  port: 9092
  # Tipo de autenticación. No cambiar si no sabes qué haces.
  auth: "session"

panel:
  # Habilitar/deshabilitar panel (requiere websocket habilitado)
  enabled: true
  # Puerto del panel web. Debe estar abierto.
  port: 8080
```

### Verificar puertos

Usar [portchecker.co](https://portchecker.co/) para verificar que los puertos están abiertos.

### Conectar al panel

1. Ejecutar `/typewriter connect` en el juego
2. Se genera un link para acceder al panel
3. Formato URL: `ip:8080`

---

## Compatibilidad con Hosting

### NO Compatible (sin configuración extra)
- **Minehut**
- **Aternos**
- **Apex Hosting**

Estos proveedores no soportan múltiples puertos por defecto. Contactar soporte del hosting para solicitar puertos adicionales.

### Playit.gg

Para usar con playit.gg:
1. Configurar agente y conectar servidor Minecraft
2. Crear nuevo túnel: Add tunnel → Seleccionar región → Protocolo TCP
3. Configurar puerto local a `8080` (o el puerto configurado del panel)
4. Usar la URL generada del túnel para acceder al panel
5. Puede necesitar túneles separados para panel (8080) y websocket (9092)

---

## Troubleshooting

### Panel no carga
- Verificar que el servidor está corriendo y accesible
- Comprobar conectividad de red
- Verificar que el firewall no bloquea los puertos
- Comprobar puertos abiertos con portchecker.co
- URL correcta: `ip:puerto` (NO incluir puerto en hostname del config)

### No aparecen entries
- No hay extensiones instaladas
- Instalar BasicExtension en `plugins/Typewriter/extensions/`
- Verificar que la extensión está en la carpeta correcta

### Error "plugin.yml not found"
- La extensión está en la carpeta equivocada
- Mover de `plugins/` a `plugins/Typewriter/extensions/`

### Error NoClassDefFoundError (PacketEvents)
- Versión incompatible de PacketEvents
- Typewriter v0.5.1 requiere PacketEvents v2.5.x
- Eliminar versión actual, descargar v2.5.0 desde Modrinth, reiniciar

### PlaceholderAPI no parsea
1. Identificar la extensión de placeholder necesaria
2. Instalar con: `/papi ecloud download [extension]`
3. Probar con: `/papi parse me <placeholder>`
4. Si funciona separado pero no en Typewriter, reiniciar servidor
# Conceptos Core de Typewriter

## Pages (Páginas)

Las páginas son contenedores de entries. Hay 4 tipos:

| Tipo | Descripción | Uso |
|------|-------------|-----|
| **Sequence** | Interacciones lineales que se ejecutan en orden | Diálogos, secuencias de acciones |
| **Static** | Datos de referencia que no cambian | Speakers, Road Networks, Sounds |
| **Cinematic** | Secuencias temporizadas por frames | Cutscenes, cámaras, animaciones |
| **Manifest** | Estado calculado en runtime | NPCs condicionales, audiences, quests |

Cada página tiene una **prioridad** que los entries heredan (salvo override).

---

## Entries

Los entries son las unidades básicas de Typewriter. Cada entry tiene:
- **ID**: Identificador único
- **Name**: Nombre descriptivo
- **Type**: Tipo determinado por la clase

### Categorías principales de entries

| Categoría | Color | Descripción |
|-----------|-------|-------------|
| **Action** | Rojo | Ejecutan efectos inmediatos |
| **Dialogue** | Azul | Muestran contenido al jugador |
| **Event** | Amarillo | Escuchan eventos de Bukkit |
| **Cinematic** | Magenta | Secuencias frame-based |
| **Audience** | Verde/Cyan | Filtran/muestran a grupos de jugadores |
| **Fact** | Púrpura | Variables numéricas de estado |
| **Entity** | Naranja | Definiciones/instancias de entidades |
| **Quest** | Varios | Misiones y objetivos |
| **Speaker** | Naranja | Identidad de NPC/personaje |

---

## Facts (Variables)

Los facts son **variables numéricas** que almacenan información del juego. Son el sistema de estado de Typewriter.

### Tipos de Facts

| Tipo | Persistencia | Uso |
|------|-------------|-----|
| **Permanent** | Para siempre | Progresión de quests, logros |
| **Session** | Hasta logout | Estado temporal de sesión |
| **Timed** | Duración configurable | Cooldowns, buffs temporales |
| **Countdown** | Decrementa cada segundo | Temporizadores |
| **Cron** | Hasta expresión cron | Eventos programados |
| **Inventory Item Count** | Solo lectura | Contar items en inventario |
| **Item Holding** | Solo lectura | Item en mano |
| **Quest Status** | Solo lectura | Estado de quest (0=inactivo, 1=activo, 2=tracked, -1=completado) |
| **Number Placeholder** | Solo lectura | Valor de placeholder numérico |
| **In Audience** | Solo lectura | Si jugador está en audiencia |
| **In Cinematic** | Solo lectura | Si jugador está en cinemática |

### Grupos de Facts

| Grupo | Alcance |
|-------|---------|
| **Player** | Específico de cada jugador |
| **World** | Todos los jugadores en un mundo |
| **Global** | Todos los jugadores online |

---

## Criteria (Condiciones)

Los criteria controlan si un entry se ejecuta. Evalúan un fact contra un valor.

### Operadores disponibles

| Operador | Significado |
|----------|-------------|
| `==` | Igual a |
| `!=` | Diferente de |
| `>` | Mayor que |
| `<` | Menor que |
| `>=` | Mayor o igual que |
| `<=` | Menor o igual que |

### Ejemplo
```
Si Fact "quest_progress" == 1 → Ejecutar diálogo
Si Fact "quest_progress" != 1 → No ejecutar
```

---

## Modifiers (Modificadores)

Los modifiers cambian valores de facts cuando un entry se ejecuta.

### Operadores

| Operador | Acción |
|----------|--------|
| `=` | Establecer valor |
| `+` | Sumar al valor actual |

> **IMPORTANTE**: Los modifiers SOLO se ejecutan si el entry se ejecuta (los criteria pasan).

### Ejemplo
```
Cuando se completa diálogo → Modifier: quest_progress = 2
Cuando mata enemigo → Modifier: kills + 1
```

---

## Audiences (Audiencias)

Las audiences condicionan qué jugadores ven determinado contenido.

### Tipos de Audience

1. **Audience Display Entry**: Muestra contenido (boss bars, efectos) a jugadores en la audiencia
2. **Audience Filter Entry**: Filtra jugadores o muestra contenido con entries hijos

### Lógica de filtrado

- **Entries raíz** (sin padres): Capturan todos los jugadores al unirse
- **Filtros encadenados**: Crean condiciones AND
- **Múltiples padres**: Crean condiciones OR

### Ejemplos de audiencias

| Audience | Filtra por |
|----------|-----------|
| Criteria Audience | Condiciones (facts) |
| Game Time Audience | Hora del juego Minecraft |
| Cron Audience | Expresión cron |
| Holding Item Audience | Item en mano |
| Item In Inventory Audience | Item en inventario |
| Cinematic Audience | Si está en cinemática |
| Region Audience | Región de WorldGuard |

---

## Variables Dinámicas (Var System)

El sistema de variables permite que los campos de entries sean dinámicos:

- **ConstVar**: Valor constante fijo
- **Variable Entry**: Valor calculado en runtime según contexto

### Tipos de variables

| Variable | Descripción |
|----------|-------------|
| Random Variable | Valor aleatorio de una lista |
| Interaction Context Variable | Valor del contexto de interacción |
| Player World Position Variable | Posición según mundo del jugador |
| String Builder Variable | Construir string con partes dinámicas |

---

## Lógica "Then" vs "When"

Typewriter tiene dos paradigmas de diseño:

### "Then" (Secuencias)
Lógica lineal, imperativa: "Haz esto, luego aquello"
```
Mostrar boss bar → Actualizar texto → Ocultar al salir
```
Usa páginas **Sequence**.

### "When" (Manifests)
Lógica declarativa, basada en condiciones: "Cuando se cumple X, muestra Y"
```
Cuando en aldea → Mostrar "Bienvenido"
Cuando en herrería → Mostrar "Visita al herrero"
```
Usa páginas **Manifest**. Más flexible para múltiples condiciones simultáneas.
# Panel Web y UI

## Componentes del Layout

### Barra de Acciones (Action Bar)

| Componente | Descripción |
|-----------|-------------|
| **Staging Indicator** | Naranja = cambios sin publicar, Verde = publicado |
| **Botón Publish** | Publica cambios al servidor |
| **Barra de búsqueda** | Buscar entries |
| **Botón +** | Crear nuevo entry |

### Inspector

Panel lateral para editar propiedades del entry seleccionado:
- **Entry Information**: ID y nombre
- **Fields**: Campos editables del entry
- **Operations**: Acciones disponibles (Add Segment, etc.)

---

## Layouts por Tipo de Página

### Sequence / Static / Manifest

- Vista central muestra todos los entries
- **Scroll** del mouse para zoom in/out
- **Click** en entry para abrir inspector
- **Mantener click 3 segundos** para arrastrar y conectar entries

### Cinematic

- **Segments**: Acciones temporizadas dentro del entry (no pueden solaparse)
- **Track**: Muestra todos los segments, editar duración
- **Track Duration**: Duración total en ticks de Minecraft (20 ticks = 1 segundo)

---

## Atajos de Teclado

### Windows/Linux

| Atajo | Acción |
|-------|--------|
| `Ctrl+K` o `Ctrl+Space` | Abrir búsqueda |
| `Tab` o `Ctrl+N` | Siguiente item |
| `Shift+Tab` o `Ctrl+P` | Item anterior |
| `Ctrl+Shift+P` | Publicar páginas |

### macOS

| Atajo | Acción |
|-------|--------|
| `⌘+K` o `⌘+Space` | Abrir búsqueda |
| `Tab` o `⌘+N` | Siguiente item |
| `Shift+Tab` o `⌘+P` | Item anterior |
| `⌘+Shift+P` | Publicar páginas |

### Filtros de Búsqueda

| Filtro | Muestra |
|--------|---------|
| `!ae` o `!ne` | Solo entries NUEVOS |
| `!ee` o `!a` | Solo entries EXISTENTES |

---

## Chapters (Capítulos)

Organizar páginas por capítulos:

1. Click derecho en página → Change Chapter
2. Usar notación de puntos para subcapítulos: `chapter.subchapter.subsubchapter`
3. Ejemplo: `hello.test` crea estructura de subcapítulos automáticamente

---

## Snippets (Personalización de formato)

Personalizar formato de display via `plugins/Typewriter/snippets.yml`:

```yaml
dialogue:
  message:
    format: |2

      <gray> | <bold><speaker></bold><reset><gray> |
      <reset><white> <message>
```

- Snippets solo se escriben en el primer uso
- Usar `|2` para strings multi-línea con 2 newlines al final
- Tags disponibles: `<speaker>`, `<message>`, etc.
# Interacciones y Diálogos

## Primera Interacción (Workflow básico)

### 1. Crear evento trigger
1. Crear entry "On Interact with Block" (buscar `on_interact_with_block`)
2. Configurar: Seleccionar tipo de bloque, poner nombre

### 2. Añadir diálogo
1. Click derecho en entry → Link with → Buscar "Add Spoken"
2. Configurar spoken: Texto, duración, speaker

### 3. Configurar speaker
1. Click en "Select speaker" → Crear página static
2. Añadir "Simple Speaker" → Configurar Display Name

---

## Tipos de Diálogo

| Tipo | Descripción |
|------|-------------|
| **Message** | Mensaje simple en chat |
| **Spoken** | Mensaje animado con efecto de escritura |
| **Option** | Lista de opciones para elegir |
| **Random Message** | Mensaje aleatorio de una lista |
| **Random Spoken** | Spoken aleatorio de una lista |

### Option Dialogue (Opciones)

1. Click derecho en entry → Link with → Buscar "Add Option"
2. Configurar: Texto de pregunta, seleccionar speaker
3. Click + junto a Options → Añadir respuestas
4. Para cada opción: Texto + Click + junto a Triggers → Crear entry siguiente

---

## Diálogos Condicionales

Usar **Criteria** para mostrar diálogos diferentes según estado:

1. Crear Spoken con texto para estado A
2. Click + junto a Criteria → Seleccionar fact
3. Configurar: Operador (==) y valor (ej: 0)
4. Añadir Modifier para cambiar fact al completar
5. Crear segundo Spoken con criteria diferente (ej: fact == 1)

### Flujo ejemplo
```
Jugador habla primera vez → fact "talked" == 0 → "¡Hola, aventurero!"
                          → Modifier: talked = 1
Jugador habla segunda vez → fact "talked" == 1 → "¡Bienvenido de vuelta!"
```

---

## Variables Dinámicas en Diálogos

### Random Variable
1. Click en botón dynamic-variable sobre campo Text
2. Seleccionar "Add Random Variable"
3. Configurar: Añadir múltiples valores de texto
4. Cada trigger muestra mensaje aleatorio de la lista

### Interaction Context Variable
Pasar valores entre entries de una secuencia:
1. Click en botón dynamic-variable sobre campo
2. Buscar "Add Interaction Context Variable"
3. Seleccionar entry fuente del valor

---

## Items en Diálogos

### Custom Item
Personalizar componentes:
- Amount (cantidad)
- Flags
- Lore
- Material
- Item Name

**NO soporta NBT data.**

### Serialized Item
1. Usar Item Capturer (click en icono de captura junto al campo item)
2. Copia item del inventario del jugador
3. **NO editable** excepto nombre

---

## Input Dialogues

### Integer Input
1. Añadir "Integer Input Dialogue"
2. Configurar texto del prompt (ej: "¿Cuántas flores?")
3. Opcionalmente configurar rango numérico
4. Conectar con acción siguiente (ej: Give Item)
5. En Give Item, usar dynamic-variable → "Interaction Context Variable" → Seleccionar input
6. Resultado: Input del jugador se usa como cantidad

---

## Custom Commands

Crear comandos personalizados con argumentos:

1. Añadir "Custom Command"
2. Configurar Arguments → Add Word Argument (texto)
3. Click último argumento → Add child → Number Argument (numérico)
4. En Number Argument: Añadir trigger → "Send Message"
5. En Send Message: Dynamic-variable → "String Builder Variable"
6. Para cada parte: "Interaction Context String Variable" → Seleccionar argumento
7. Construir mensaje: `"Hola <nombre>, tienes <edad> años"`

---

## Interaction Bounds (Límites de interacción)

Condiciones que confinan o interrumpen interacciones.

### Dos modos

| Modo | Comportamiento | Prioridad |
|------|---------------|-----------|
| **Interruption** | Cancela interacción si se rompe la condición | Prioridad interacción ≤ prioridad bound |
| **Blocking** | Jugador NO puede romper la condición | Prioridad interacción > prioridad bound |

### Ejemplo: Player Radius Bound

**Modo Interruption** (se aleja → se cancela):
1. Crear "On Interact With Block"
2. Link with → "Player Radius Interaction Bound"
3. Configurar: Radio (distancia) y zoom (efecto cámara opcional)

**Modo Blocking** (no puede alejarse):
1. Misma configuración
2. Crear nueva página Sequence con **prioridad más alta**
3. Mover bound a página de prioridad baja
4. Mover Spoken a página de prioridad alta
5. Ahora el jugador no puede alejarse durante el diálogo importante
# Sistema de Quests

## Filosofía Core

> Las quests son **representaciones visuales** del estado del juego, NO drivers del estado.
> Usar Permanent Facts para trackear progresión real; las quests muestran ese progreso.

---

## Quest Entry

### Estados de Quest

Determinados por criteria:

| Estado | Condición |
|--------|-----------|
| **Inactive** | Ningún criteria se cumple |
| **Active** | Active Criteria se cumple (pero no Completed) |
| **Completed** | Completed Criteria se cumple |

### Campos del Quest Entry

| Campo | Descripción |
|-------|-------------|
| Display Name | Nombre en el quest log del jugador |
| Active Criteria | Condición para que la quest sea visible (ej: Fact >= 1) |
| Completed Criteria | Condición de completado (ej: Fact == 5) |

---

## Tipos de Objectives

### Simple Objective
Objetivo básico con criterio y texto de display.

### Completable Objective
Muestra estado de completado (activo/completado).

| Campo | Descripción |
|-------|-------------|
| Quest | Quest padre |
| Show Criteria | Cuándo mostrar el objetivo |
| Completed Criteria | Cuándo marcarlo como completado |
| Display | Texto del objetivo |

### Location Objective
Objetivo con ubicación target (muestra path stream).

| Campo | Descripción |
|-------|-------------|
| Quest | Quest padre |
| Criteria | Cuándo mostrar |
| Display | Texto del objetivo |
| Target Location | Ubicación destino |

---

## Ejemplo: Talk Objective

**Setup:**
1. **Manifest page**: Quest entry + "Interact Entity Objective"
2. Configurar objective: Quest, Criteria (cuando quest activa), Entity con la que interactuar, texto
3. **Sequence page**: "On Entity Interact Event" para quest giver
4. Option con modifier: fact = 1 (dar quest)
5. "On Entity Interact Event" para quest completer
6. Spoken con criteria (fact == 1) y modifier (fact == 2)

---

## Ejemplo: Kill Objective

**Setup:**
1. **Manifest**: Quest + "Completable Objective"
2. Configurar: Quest, Show Criteria (fact == 1), Completed Criteria (fact == 1 AND kills >= 2)
3. **Sequence**: "On Entity Interact Event" → Option con modifier (fact = 1)
4. "On Player Kill Entity" event
5. "Simple Action" con Criteria (quest activa) y Modifier (kills + 1)

---

## Quest Tracking

- Solo **UNA quest** tracked a la vez (efecto spotlight)
- Auto-track cuando quest se activa
- Manual: `/tw quest track <questentry>`
- O usar entry "Track Quest Action"

---

## Placeholders de Quests

| Placeholder | Valor |
|-------------|-------|
| `%typewriter_tracked_quest%` | Nombre de quest tracked |
| `%typewriter_tracked_objectives%` | Objetivos activos (separados por coma) |
| `%typewriter_tracked_objectives_locations%` | Ubicaciones de objetivos (formato personalizable) |

---

## Display de Quests

### Sidebar
Usar "Objective Lines Entry" para mostrar objetivos tracked en sidebar.

### Quest Status Events

| Evento | Descripción |
|--------|-------------|
| Quest Start Event | Quest cambia a activa |
| Quest Complete Event | Quest se completa |
| Quest Status Update Event | Cualquier cambio de estado |

Usar para feedback visual: títulos, partículas, sonidos, mensajes en chat.
# Cinemáticas

## Conceptos Base

- Las cinemáticas son **secuencias temporizadas** basadas en frames
- **20 frames = 1 segundo** (ticks de Minecraft)
- Se crean en páginas tipo **Cinematic**
- Los segments dentro de un entry NO pueden solaparse

---

## Camera Cinematic

### Crear cinemática de cámara

1. Crear página tipo Cinematic
2. Click + → Buscar "Add Camera Cinematic"
3. Seleccionar → Operations → Add Segment
4. Click en segment para abrir inspector

### Campos del Path

Cada segmento contiene un path (lista de ubicaciones):

| Campo | Descripción |
|-------|-------------|
| World | Mundo donde ocurre |
| X, Y, Z | Coordenadas |
| Pitch | Ángulo vertical de cámara |
| Yaw | Ángulo horizontal de cámara |
| Duration | Ticks hasta siguiente ubicación (auto-calculado si no se especifica) |

- **Múltiples ubicaciones** = movimiento de cámara
- **Una ubicación** = cámara estática
- **Content mode** permite obtener ubicación actual del jugador automáticamente

### Ejecutar cinemática

```
/tw cinematic start <nombre_pagina>
```

O añadir "Cinematic Action" a una secuencia.

---

## Diálogos en Cinemáticas

### Tipos disponibles

| Tipo | Descripción |
|------|-------------|
| Spoken Dialogue Cinematic | Diálogo animado en chat |
| Actionbar Dialogue Cinematic | Diálogo en action bar |
| Subtitle Dialogue Cinematic | Subtítulos animados |
| Title Cinematic | Títulos durante cinemática |

### Configuración

1. Click + → Buscar "Add [Tipo] Dialogue Cinematic"
2. Añadir speaker al diálogo
3. Operations → Add Segment
4. Configurar segment: Texto en campo Text

### Variantes Random

- Random Actionbar Dialogue Cinematic
- Random Spoken Dialogue Cinematic
- Random Subtitle Dialogue Cinematic

---

## Entidades en Cinemáticas

**Requisito**: Entity Extension instalada

### Setup

1. Click + → Buscar "Add Entity Cinematic"
2. Click "Select entity_definition" → Elegir NPC o crear "Add Npc Definition"
3. Operations → Add Segment
4. Click "Select entity_cinematic_artifact" → Crear artifact en página static
5. Click icono de captura → Abrir content mode
6. Grabar movimientos e items en Minecraft
7. Salir con items de playback para guardar

---

## Otros Cinematics

### Blinding Cinematic
Pantalla negra (cegado) durante la cinemática.

### Screen Shake Cinematic
Sacude la pantalla del jugador.

### Pumpkin Hat Cinematic
Muestra overlay de calabaza (para barras cinematográficas).

### Particle Cinematic
Spawn de partículas durante cinemática.

### Potion Effect Cinematic
Aplica efectos de poción durante cinemática.

### Sound Cinematic
Reproduce sonido durante cinemática.

### Set Fake Block Cinematic
Coloca bloque falso durante cinemática.

### Cinematic Console/Player Command
Ejecuta comando de consola/jugador en frame específico.

### Trigger Sequence Cinematic
Ejecuta secuencia de triggers. **CUIDADO**: No usar para triggear diálogos.

---

## Cinemáticas Skippable

1. Click + → Buscar "Add Skippable Cinematic"
2. Configurar tecla de confirmación: `SNEAK` o `SWAP_HANDS`
3. Operations → Add Segment
4. Configurar Start y End frames (en ticks) del período donde se puede skipear

---

## World-Specific Cinematics

Para que una cinemática funcione en múltiples mundos:

1. Click en icono dynamic-variable del path
2. Buscar "Add Player World Position Variable"
3. Repetir para todos los paths
4. El valor del mundo se determina por el mundo actual del jugador

---

## First Join Cinematic

Cinemática que se reproduce solo la primera vez que un jugador entra:

1. Añadir "On Player Join" event
2. Configurar criteria en option: Fact == 0
3. Configurar modifier en option: Fact = 1
4. Añadir "Cinematic Action" → Conectar
5. La cinemática solo se reproduce una vez por jugador (fact cambia a 1)
# Entidades y NPCs

## Requisito

Instalar **Entity Extension** en `plugins/Typewriter/extensions/`.

---

## NPC Definition (Definición)

Define el tipo de entidad (skin, nombre, datos).

### Crear NPC Definition

1. Crear página tipo **Manifest**
2. Click + → Buscar "Add NPC Definition"
3. Configurar:

| Campo | Descripción |
|-------|-------------|
| Entry Name | Nombre organizativo (no visible en juego) |
| Display Name | Nombre sobre la cabeza del NPC. Soporta colores: `<red><b>Oliver</b></red>` |
| Sound | Sonido asociado |
| Skin | Textura y firma de Minecraft |

### Configurar Skin

1. Click en icono de cadena junto al campo Skin
2. Seleccionar "Fetch from url"
3. Introducir URL de imagen de skin → Click Fetch

---

## NPC Instance (Instancia)

Coloca un NPC definido en el mundo.

### Crear NPC Instance

1. Buscar "Add NPC Instance"
2. Seleccionar Definition → Elegir NPC Definition creado
3. Configurar ubicación de spawn:
   - Click en icono de captura junto al campo location
   - La posición actual del jugador se auto-rellena
   - El NPC aparece en esa ubicación

---

## Interacción con Entidades

1. Crear página **Sequence**
2. Click + → Buscar "Add On Entity Interact Event"
3. Seleccionar entity definition en inspector
4. Click derecho → Link with → Añadir evento (ej: Spoken)
5. Configurar speaker y contenido

---

## Entidades Condicionales (Audiences)

Mostrar/ocultar NPCs según condiciones:

1. Crear Audience entry (ej: "Game Time Audience")
2. Arrastrar entity instance sobre audience entry para enlazar
3. Configurar:

| Campo | Descripción |
|-------|-------------|
| Children | Entidades a mostrar |
| Active Times | Condición de visibilidad |
| Inverted | Invertir condición |

---

## Entity Activities

Comportamientos para entidades.

### Actividades Disponibles

| Actividad | Descripción |
|-----------|-------------|
| **Look Close** | Mira al jugador más cercano |
| **Random Look** | Mira en direcciones aleatorias |
| **Patrol** | Recorre ubicaciones definidas |
| **Path** | Se mueve por camino predefinido |
| **Target Location** | Se mueve hacia ubicación target |
| **Player Close By** | Se activa cuando jugador está cerca |
| **In Dialogue** | Comportamiento diferente hablando vs idle |
| **Audience Activity** | Selecciona actividad según audiencia |
| **Game Time Activity** | Activa según hora del juego |
| **Timed Activity** | Limita duración de actividad hijo |
| **Trigger Activity** | Triggerea secuencia al activar/desactivar |

### In Dialogue Activity

Controla dos comportamientos:
- **Talking Activity**: Comportamiento cuando habla (ej: "Look Close Activity")
- **Idle Activity**: Comportamiento cuando idle (ej: "Patrol Activity")

---

## Tipos de Entidades Disponibles

| Entidad | Descripción |
|---------|-------------|
| NPC (Player) | Jugador falso con skin |
| Armor Stand | Soporte de armadura |
| Villager | Aldeano |
| Zombie / Husk | Zombie / Husk |
| Skeleton | Esqueleto |
| Iron Golem | Golem de hierro |
| Cat | Gato |
| Cow / Chicken | Vaca / Pollo |
| Allay | Allay |
| Enderman | Enderman |
| Frog | Rana |
| Slime / Magma Cube | Slime / Cubo de magma |
| Piglin / Piglin Brute | Piglin |
| Hoglin | Hoglin |
| Warden | Warden |
| Witch | Bruja |
| Item Display | Display de item |
| Text Display | Display de texto |
| Hit Box | Hit box personalizado |
| Interaction Indicator | Indicador de interacción |
| Stacked Entity | Entidades apiladas |

### Entity Data (Propiedades)

Se pueden configurar propiedades: Ageable, Arrow Count, Cat Variant, Collar Color, Custom Name, Dancing, Glowing, Horse Variant, Llama Variant, On Fire, Pose, Potion Effect Color, Saddled, Size, Skin, Small, Villager Type, etc.

---

## Road Networks

Sistema de pathfinding eficiente para movimiento de NPCs.

### Crear Road Network

1. Crear página **Static**
2. Click + → Buscar "Base Road Network"
3. Click icono de captura → Abre content mode

### Items en Content Mode

| Item | Acción |
|------|--------|
| **Diamond** | Añadir/editar nodo (click derecho) |
| **End Crystal** | Salir del editor de nodos |
| **Redstone** | Crear/recalcular paths (click derecho) |
| **Sculk Sensor** | Cambiar radio de nodo (click derecho, usar scroll) |
| **Emerald** | Añadir conexión de fast travel (click derecho) |
| **Netherite Ingot** | Añadir nodo negativo (click derecho) |
| **Glowstone** | Resaltar nodos conectados (click derecho) |

### Conceptos

- **Nodos**: Puntos en el mundo conectados por edges
- **Radio**: 30 bloques por defecto (pathfinding busca nodos dentro del radio)
- **Nodos negativos**: Previenen pathfinding por áreas específicas
- **Fast travel**: Teletransporte entre nodos (shift+click derecho para one-way)

### Uso

- NPCs siguen paths entre nodos (con Patrol Activity)
- Visualización de caminos para quest objectives (Path Streams)
# Referencia de Comandos

## Comandos Principales

Alias: `/typewriter` o `/tw`

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tw connect` | Obtener link al panel web | `typewriter.connect` |
| `/tw clearChat` | Limpiar chat estilo Typewriter | `typewriter.clearChat` |
| `/tw reload` | Recargar plugin | `typewriter.reload` |

---

## Cinemáticas

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tw cinematic start <pageName> [player]` | Iniciar cinemática | `typewriter.cinematic.start` |
| `/tw cinematic stop [player]` | Detener cinemática | `typewriter.cinematic.stop` |

---

## Facts

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tw facts [player]` | Ver todos los facts del jugador | `typewriter.facts` |
| `/tw facts set <factEntry> <value> [player]` | Establecer valor de fact | `typewriter.facts.set` |
| `/tw facts reset` | Resetear todos los facts | `typewriter.facts.reset` |

---

## Triggers

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tw trigger <entry> [player]` | Triggear entry | `typewriter.trigger` |
| `/tw fire <entry> [player]` | Disparar Fire Trigger Event | `typewriter.fire` |

---

## Quests

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tw quest track <questEntry> [player]` | Trackear quest | `typewriter.quest.track` |
| `/tw untrack [player]` | Dejar de trackear quest | `typewriter.quest.untrack` |

---

## Manifest

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tw manifest inspect [player]` | Inspeccionar manifests activos | `typewriter.manifest.inspect` |

---

## Assets

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tw assets clean` | Limpiar assets no usados | `typewriter.assets.clean` |

---

## Road Network

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tw roadNetwork edit <roadNetworkEntry>` | Editar road network | `typewriter.roadNetwork.edit` |

---

## Notas

- `[player]` es opcional. Si no se especifica, se aplica al jugador que ejecuta el comando.
- Los nombres de entry/page se autocompletan con Tab.
- `/tw fire` dispara entries de tipo "Fire Trigger Event" específicamente.
- `/tw trigger` triggea cualquier entry directamente.
# Referencia Completa de Adapters

## Resumen de Adapters

| Adapter | Estado | Descripción |
|---------|--------|-------------|
| **BasicAdapter** | Core | Entries esenciales. Instalar siempre. |
| **EntityAdapter** | Core | Entidades dinámicas, NPCs, hologramas |
| **CitizensAdapter** | ⚠️ Unsupported | Citizens NPCs (usar EntityAdapter) |
| **FancyNpcsAdapter** | ⚠️ Deprecated | FancyNpcs (usar EntityAdapter) |
| **ZNPCsPlusAdapter** | ⚠️ Deprecated | ZNPCsPlus (usar EntityAdapter) |
| **MythicMobsAdapter** | Integración | MythicMobs (no testeado oficialmente) |
| **VaultAdapter** | Integración | Economía y permisos Vault |
| **WorldGuardAdapter** | Integración | Regiones WorldGuard |
| **RPGRegionsAdapter** | Integración | RPGRegions (no testeado) |
| **CombatLogXAdapter** | Integración | Estado de combate (no testeado) |
| **SuperiorSkyblockAdapter** | Integración | Superior Skyblock (no testeado) |

---

## 1. BasicAdapter

### Actions (23)

| Entry | Descripción | Campos clave |
|-------|-------------|-------------|
| Add Potion Effect | Efecto de poción al jugador | Potion Effect, Duration, Amplifier |
| Apply Velocity | Aplicar velocidad/fuerza | Force |
| Cinematic | Iniciar cinemática | Page |
| Console Command | Comando desde consola | Command (placeholder) |
| Delayed Action | Retrasar trigger | Duration |
| Drop Item | Soltar item en ubicación | Item, Location (opcional) |
| Firework | Spawn de fuego artificial | Location, Effects, Power |
| Give Item | Dar item al jugador | Item |
| Group Trigger | Triggear para grupo de jugadores | Group |
| Play Sound | Reproducir sonido | Sound |
| Player Command | Comando como jugador | Command (placeholder) |
| Random Trigger Gate | Selección aleatoria de triggers | Amount |
| Remove Item | Quitar item del inventario | Item |
| Send Message | Enviar mensaje | Speaker, Message |
| Set Block | Colocar bloque (todos los jugadores) | Material, Location |
| Set Item | Item en slot específico | Item, Slot |
| Show Title | Mostrar título/subtítulo | Title, Subtitle, Durations |
| Simple Action | Acción vacía (solo criteria/modifiers) | — |
| Spawn Particle | Partículas en ubicación | Location, Particle, Count, Offset, Speed |
| Stop Sound | Detener sonido | Sound (opcional) |
| Switch Server | Cambiar servidor | Server |
| Teleport | Teletransportar | Location (con rotación) |
| Track Quest | Forzar tracking de quest | Quest |

### Audiences (17)

| Entry | Descripción | Campos clave |
|-------|-------------|-------------|
| Boss Bar | Barra de progreso superior | Title, Progress, Color, Style |
| Cinematic Audience | Filtro por cinemática activa | Cinematic, Inverted |
| Closest Group Member Path Stream | Path al miembro más cercano | Road, Group |
| Criteria Audience | Filtro por criteria | Criteria, Inverted |
| Cron Audience | Filtro por expresión cron | Cron Expression |
| Direct Location Path Stream | Path a ubicación | Road, Target Location |
| Game Time Audience | Filtro por hora del juego | World, Active Times, Inverted |
| Group Members Path Stream | Paths a miembros del grupo | Road, Group |
| Holding Item Audience | Filtro por item en mano | Item, Inverted |
| Item In Inventory Audience | Filtro por item en inventario | Item, Inverted |
| Item In Slot Audience | Filtro por item en slot | Item, Slot, Inverted |
| Location Objectives Path Stream | Paths a objetivos tracked | Road |
| Looping Cinematic Audience | Cinemática en loop | Cinematic Id |
| Simple Sidebar | Sidebar para jugadores | Title, Priority Override |
| Simple Lines | Líneas de texto en sidebar | Lines, Priority Override |
| Tab List Header Footer | Header/footer del tab | Header, Footer |
| Timer Audience | Trigger periódico | Duration, On Timer |
| Trigger Audience | Trigger al entrar/salir | On Enter, On Exit |

### Cinematics (18)

| Entry | Descripción |
|-------|-------------|
| Action Bar Dialogue | Diálogo en action bar |
| Blinding | Pantalla negra |
| Camera | Path de cámara con waypoints |
| Console Command | Comando de consola en frame |
| Player Command | Comando de jugador en frame |
| Particle | Partículas durante cinemática |
| Potion Effect | Efectos de poción |
| Pumpkin Hat | Overlay de calabaza |
| Random Action Bar Dialogue | Action bar aleatorio |
| Random Spoken Dialogue | Spoken aleatorio |
| Random Subtitle Dialogue | Subtítulo aleatorio |
| Screen Shake | Sacudir pantalla |
| Set Fake Block | Bloque falso temporal |
| Sound | Sonido durante cinemática |
| Spoken Dialogue | Diálogo animado en chat |
| Subtitle Dialogue | Subtítulos animados |
| Title | Títulos durante cinemática |
| Trigger Sequence | Secuencia de triggers |

### Dialogues (5)

| Entry | Descripción | Campos clave |
|-------|-------------|-------------|
| Message | Mensaje simple | Speaker, Text |
| Option | Opciones para elegir | Speaker, Text, Options, Duration |
| Random Message | Mensaje aleatorio | Speaker, Messages |
| Random Spoken | Spoken aleatorio | Speaker, Messages, Duration |
| Spoken | Mensaje animado | Speaker, Text, Duration |

### Events (17)

| Entry | Descripción | Campos clave |
|-------|-------------|-------------|
| Block Break | Romper bloque | Block, Location, Item In Hand |
| Block Place | Colocar bloque | Location, Block |
| Chat Contains Text | Chat contiene texto | Text (regex), Exact Same |
| Craft Item | Craftear item | Crafted Item |
| Detect Command Ran | Detectar comando | Command (regex), Cancel |
| Fire Trigger | Trigger por `/tw fire` | — |
| Fish | Pescar | Item In Hand, Caught |
| Interact Block | Click derecho en bloque | Block, Location, Cancel, Interaction Type, Shift Type |
| Pickup Item | Recoger item | Item |
| Player Death | Muerte del jugador | Death Cause |
| Player Hit Entity | Golpear entidad | Entity Type |
| Player Join | Unirse al servidor | — |
| Player Kill Entity | Matar entidad | Entity Type |
| Player Kill Player | Matar jugador | Killed Triggers |
| Player Near Location | Cerca de ubicación | Location, Range |
| Player Quit | Salir del servidor | — |
| Run Command | Comando custom | Command |

### Facts (13)

| Entry | Descripción | Persistencia |
|-------|-------------|-------------|
| Permanent | Nunca expira | ∞ |
| Session | Expira al logout | Sesión |
| Timed | Expira tras duración | Configurable |
| Countdown | Decrementa cada segundo | Auto-decremento |
| Cron | Expira en hora cron | Cron |
| In Audience | 1 si en audiencia | Solo lectura |
| In Cinematic | 1 si en cinemática | Solo lectura |
| Inventory Item Count | Cantidad en inventario | Solo lectura |
| Item Holding | Cantidad en mano | Solo lectura |
| Item In Slot | Cantidad en slot | Solo lectura |
| Number Placeholder | Valor de placeholder | Solo lectura |
| Quest Status | Estado de quest | Solo lectura |
| Value Placeholder | Mapeo placeholder→fact | Solo lectura |

### Groups (3)

| Entry | Alcance |
|-------|---------|
| Global Group | Todos los jugadores online |
| Player Group | Jugador individual |
| World Group | Jugadores en un mundo |

### Quests (10)

| Entry | Descripción |
|-------|-------------|
| Simple Quest | Colección de tareas/objetivos |
| Simple Objective | Objetivo básico |
| Completable Objective | Objetivo con estado de completado |
| Location Objective | Objetivo con ubicación target |
| Objective Lines | Display de objetivos actuales |
| Quest Complete Event | Trigger cuando quest se completa |
| Quest Start Event | Trigger cuando quest inicia |
| Quest Status Update Event | Trigger al cambiar estado |
| Tracked Objective Audience | Filtro por objetivo tracked |
| Tracked Quest Audience | Filtro por quest tracked |

### Statics (3)

| Entry | Descripción |
|-------|-------------|
| Base Road Network | Definición de road network |
| Self Speaker | El jugador como speaker |
| Simple Speaker | Speaker básico (NPC) |

### Sounds (1)

| Entry | Descripción |
|-------|-------------|
| Custom Sound | Sonido custom del resource pack |

---

## 2. EntityAdapter

### Activities (11)

| Entry | Descripción |
|-------|-------------|
| Audience Activity | Actividad según audiencia |
| Game Time Activity | Actividad según hora del juego |
| In Dialogue Activity | Actividad hablando vs idle |
| Look Close Activity | Mirar al jugador cercano |
| Path Activity | Moverse por camino |
| Patrol Activity | Patrullar ubicaciones |
| Player Close By Activity | Activar cerca de jugador |
| Random Look Activity | Mirar direcciones aleatorias |
| Target Location Activity | Moverse a ubicación |
| Timed Activity | Limitar duración |
| Trigger Activity | Trigger al activar/desactivar |

### Entity Types (28+)

Allay, Cat, Chicken, Cow, Enderman, Frog, Hoglin, Husk, Iron Golem, Item Display, Magma Cube, Named Entity, NPC Instance, Piglin, Piglin Brute, Player, Skeleton, Slime, Text Display, Villager, Warden, Witch, Zombie, Armor Stand, Hit Box, Interaction Indicator, Self NPC, Stacked Entity.

### Entity Data (18+)

Ageable, Arrow Count, Cat Variant, Chested Horse Chest, Collar Color, Custom Name, Dancing, Glowing Effect, Horse Variant, Llama Carpet/Variant, Marker, On Fire, Parrot Color, Pose, Potion Effect Color, Puff State, Rabbit Type, Saddled, Size, Skin, Small, Villager.

---

## 3. VaultAdapter

| Tipo | Entry | Descripción |
|------|-------|-------------|
| Action | Deposit Balance | Depositar dinero |
| Action | Withdraw Balance | Retirar dinero |
| Action | Set Prefix | Establecer prefijo chat |
| Fact | Balance Fact | Balance del jugador |
| Fact | Permission Fact | Verificar permiso |
| Group | Balance Group | Agrupar por balance |
| Group | Permission Group | Agrupar por permiso |

---

## 4. WorldGuardAdapter

| Tipo | Entry | Descripción |
|------|-------|-------------|
| Audience | Region Audience | Filtrar por región |
| Event | Enter Region | Al entrar en región |
| Event | Exit Region | Al salir de región |
| Fact | In Region Fact | 1 si en región |
| Group | Region Group | Agrupar por regiones |

---

## 5. MythicMobsAdapter

| Tipo | Entry | Descripción |
|------|-------|-------------|
| Action | Spawn Mob | Spawnear MythicMob |
| Action | Despawn Mob | Despawnear MythicMob |
| Action | Execute Skill | Ejecutar MythicMobs skill |
| Cinematic | Mythic Mob Cinematic | Mob en cinemática |
| Cinematic | Mythic Skill Cinematic | Skill en cinemática |
| Event | Mythic Mob Death | Muerte de MythicMob |
| Event | Mythic Mob Interact | Interacción con MythicMob |
| Fact | Mob Count Fact | Contar mobs activos |

---

## 6. RPGRegionsAdapter

| Tipo | Entry | Descripción |
|------|-------|-------------|
| Action | Discover Region | Descubrir región |
| Event | Discover Region | Al descubrir región |
| Event | Enter Region | Al entrar en región |
| Fact | In Region Fact | 1 si en RPGRegion |

---

## 7. CombatLogXAdapter

| Tipo | Entry | Descripción |
|------|-------|-------------|
| Event | Player Enter Combat | Al entrar en combate |
| Event | Player Exit Combat | Al salir de combate |
| Fact | Combat Fact | 1 si en combate |

---

## 8. SuperiorSkyblockAdapter

### Actions (6)
Island Bank Deposit/Withdraw, Island Disband, Set Biome, Set Border Size, Set Member Limit.

### Events (6)
Island Create/Disband/Invite/Join/Upgrade, Mission Complete.

### Facts (1)
Island Fact — Información de la isla.

### Groups (1)
Island Group — Todos los jugadores en misma isla.

---

## 9. Adapters Deprecados

| Adapter | Estado | Alternativa |
|---------|--------|-------------|
| CitizensAdapter | Unsupported | EntityAdapter |
| FancyNpcsAdapter | Deprecated | EntityAdapter |
| ZNPCsPlusAdapter | Deprecated | EntityAdapter |

Estos adapters permiten referenciar NPCs de plugins externos pero se recomienda usar EntityAdapter con entidades nativas de Typewriter.
# Arquitectura del Engine

## Módulos

```
engine/
├── engine-core/     # Estructuras de datos e interfaces (platform-agnostic)
├── engine-loader/   # Carga de extensiones y class loading
└── engine-paper/    # Implementación para Paper server

module-plugin/
├── api/                    # Configuración y setup
├── extension-processor/    # Procesadores de anotaciones (KSP)
└── processor/              # Serialización, editors, blueprints
```

---

## Sistema de Entries

### Entry Interface

```kotlin
Entry (interface)
├── id: String              // Identificador único
├── name: String            // Nombre descriptivo
└── formattedName: String   // Nombre formateado (computed)

PriorityEntry (interface)
└── priorityOverride: Optional<Int>  // Override de prioridad de página
```

### Entry Reference System (Ref)

```kotlin
Ref<E>  // Referencia type-safe a entries
```

- Las entries **nunca** contienen otras entries directamente
- Siempre usan `Ref<E>` para referencias forward
- Soporta lazy loading y referencias circulares
- `Query<E>` — DSL para buscar entries con filtros, por nombre, por ID

### Library

Carga todas las entries desde archivos JSON en `/pages`:
- `entriesById`: Map<String, Entry>
- `entryPriority`: Map<Ref<Entry>, Int>
- `pages`: List<Page>

---

## Sistema de Pages

```kotlin
Page {
  id: String
  name: String
  entries: List<Entry>
  type: PageType         // SEQUENCE, STATIC, CINEMATIC, MANIFEST
  priority: Int
}
```

Las entries heredan prioridad de su página a menos que sean `PriorityEntry` con override.

---

## Sistema de Interacciones

### InteractionContext

```kotlin
InteractionContext {
  data: Map<InteractionContextKey, Any>

  get<T>(key): T?
  get<T>(ref, key): T?
  combine(context): InteractionContext
  expand(builder): InteractionContext
}
```

**Tipos de context keys:**
- `GlobalContextKey<T>` — Accesible por cualquier entry
- `EntryInteractionContextKey<T>` — Específico de una entry
- `RandomSeedContextKey` — Seed random consistente por interacción

### Interaction Lifecycle

```kotlin
Interaction {
  priority: Int
  context: InteractionContext

  suspend initialize(): Result<Unit>
  suspend tick(deltaTime: Duration)
  suspend teardown()
}
```

### InteractionScope

```kotlin
InteractionScope {
  interaction: Interaction
  bound: InteractionBound
  boundState: BLOCKING | INTERRUPTING | IGNORING
}
```

**Lógica de estado:**
- Si `interaction.priority > bound.priority` → **BLOCKING**
- Si no → **INTERRUPTING**

### InteractionBound

```kotlin
InteractionBound {
  priority: Int
  suspend initialize()
  suspend tick()
  suspend transitionTo(bound): Boolean
  suspend transitionFrom(bound)
  suspend teardown()
}
```

---

## Sistema de Eventos y Triggers

### PlayerSessionManager

- Gestiona `PlayerSession` por jugador
- Crea sesiones al join, destruye al quit
- `triggerEvent(Event)` — Encolar evento para procesamiento async
- `triggerActions(player, context, triggers)` — Triggear lista de acciones

### Event System

```kotlin
Event {
  player: Player
  context: InteractionContext
  triggers: List<EventTrigger>
}

EventTrigger (interface) {
  id: String
  canTriggerFor(player, context): Boolean
}
```

### TriggerHandler

```kotlin
TriggerHandler (interface) {
  priority: Int
  suspend trigger(event, currentInteraction): TriggerContinuation
}

TriggerContinuation (sealed):
- Nothing                          // No-op
- Append(events)                   // Encolar eventos adicionales
- StartInteraction(interaction)    // Iniciar interacción
- KeepInteraction                  // Mantener actual
- EndInteraction                   // Terminar interacción
- StartInteractionBound(bound)    // Iniciar bound
- EndInteractionBound              // Terminar bound
- Multi(continuations)             // Combinar múltiples
```

---

## Sistema de Audiences

```kotlin
AudienceEntry (interface) {
  suspend display(): AudienceDisplay
}

AudienceDisplay (abstract) {
  isActive: Boolean
  players: List<Player>

  initialize()
  dispose()
  addPlayer(player)
  removePlayer(player)
  abstract onPlayerAdd(player)
  abstract onPlayerRemove(player)
}

AudienceFilter (abstract extends AudienceDisplay) {
  abstract filter(player): Boolean
  // Soporta filtros invertidos, updates en cascada a hijos
}

AudienceDisplayState:
- IN_AUDIENCE        // Pasa el filtro
- BLOCKED            // No pasa el filtro
- NOT_CONSIDERED     // Filtros padre bloquean
```

**AudienceManager:** Construye jerarquía, mantiene displays, tickea `TickableDisplay` instances.

---

## Sistema de Cinemáticas

```kotlin
CinematicEntry (interface) {
  criteria: List<Criteria>

  createRecording(player): CinematicAction?
  createSimulating(player): CinematicAction?
  create(player): CinematicAction
}

Segment (interface) {
  startFrame: Int
  endFrame: Int
}

CinematicAction {
  suspend setup()
  suspend tick(frame: Int)     // Puede saltar frames (editor)
  suspend teardown()
  canFinish(frame): Boolean
}
```

**IMPORTANTE:** `tick()` muestra estado en frame dado, NO progresión. El jugador puede scroll/rewind en editor.

---

## Sistema de Carga de Extensiones

### Proceso de carga

```
1. Escanear JARs en /extensions
2. Cargar extension.json de cada JAR
3. Validar compatibilidad de versión del engine
4. Verificar dependencias externas (plugins Paper)
5. Resolver dependencias entre extensiones
6. Crear URLClassLoader con extensiones válidas
7. Cachear clases de entry por blueprintId
```

### Extension Info

```kotlin
Extension {
  extension: ExtensionInfo    // nombre, versión, namespace
  entries: List<EntryInfo>    // entries registradas
  entryListeners: List<EntryListenerInfo>
  typewriterCommands: List<TypewriterCommandInfo>
  dependencyInjections: List<DependencyInjectionInfo>
}
```

---

## Secuencia de Carga (TypewriterCore)

```kotlin
suspend fun load() {
  1. extensionLoader.load(extensionJars)    // Cargar extensiones
  2. library.load()                          // Deserializar entries
  3. dependencyInject.load()                 // Registrar beans DI
  4. initializableManager.load()             // Inicializar componentes
}
```

### Library.load()

1. Escanear `*.json` en `/pages`
2. Parsear cada página JSON
3. Para cada entry: obtener blueprintId → cargar clase → deserializar → validar
4. Construir mapa `entriesById`
5. Computar mapa `entryPriority`

---

## Patrones de Diseño Clave

### 1. Reference System
- Entries nunca contienen otras entries directamente
- `Ref<E>` para referencias forward con lazy loading

### 2. Context-Based Architecture
- Todo el estado fluye por `InteractionContext`
- Soporta contextos anidados y builders

### 3. Annotation-Driven Registration
- Sin código de registro manual
- Procesadores KSP en compile-time extraen metadata
- `extension.json` generado controla carga en runtime

### 4. Hierarchical Priority System
- Páginas tienen prioridad → entries heredan
- Interacciones tienen prioridad relativa a bounds
- Audiencias determinadas por jerarquía de filtros

### 5. Event-Driven Triggering
- Eventos encolan triggers para procesamiento async
- Cadena de TriggerHandlers
- Handlers pueden swap interacciones/bounds

### 6. Display Pattern
- Entries producen `Display` instances para rendering
- Displays son Listeners (reciben eventos Bukkit)
- TickableDisplay se tickea a intervalos regulares

### 7. Criteria/Modifier Pattern
- Pre-condiciones: `List<Criteria>` (leer facts)
- Post-acciones: `List<Modifier>` (escribir facts)
- Ambos soportan operadores y variables dinámicas
# Guía de Desarrollo de Extensiones

## Requisitos

- **JDK 21** o superior
- **IDE**: IntelliJ IDEA o Eclipse
- **Lenguaje**: Kotlin (Java NO soportado)
- Conocimiento de Gradle y Spigot API

---

## Setup del Proyecto

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.typewritermc.com/releases")  // Releases
        maven("https://maven.typewritermc.com/beta")      // Beta
    }
}
```

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.0.0"
}

group = "me.yourusername"
version = "0.0.1"

typewriter {
    namespace = "<nombre de empresa>"  // 5-20 chars, lowercase alfanumérico

    extension {
        name = "<Nombre Extensión>"           // 5-25 chars, sin "Adapter"/"Extension"
        shortDescription = "<Descripción>"    // 10-80 chars
        description = "<Descripción larga>"   // 100-2000 chars
        engineVersion = "<versión typewriter>"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA  // Opcional

        paper {
            dependency("<nombre plugin>")  // Dependencias de plugins Paper
        }
    }
}

kotlin {
    jvmToolchain(21)
}
```

### Build y Deploy

1. Ejecutar tarea Gradle: `build`
2. JAR generado en: `build/libs/`
3. Colocar JAR en: `plugins/Typewriter/extensions/`
4. Recargar con: `/typewriter reload` o reiniciar servidor

---

## Anotaciones

### @Entry — Definir entry

```kotlin
@Entry(
    name = "example_entry",           // ID único
    description = "An example entry", // Descripción
    color = Colors.RED,               // Color (clase Colors o hex "#FF5733")
    icon = "mdi:star"                 // Icono de https://icones.js.org/
)
```

> **CRÍTICO**: Las entries son **STATELESS**. No pueden tener campos mutables. Todos los campos deben ser `val` (inmutables).

### @Tags — Categorizar entries

```kotlin
@Tags("entry", "action")
```

### @EntryListener — Listener de eventos Bukkit

```kotlin
@EntryListener(MyEventEntry::class)
fun onSomeEvent(event: BukkitEvent, query: Query<MyEventEntry>) {
    // Procesar evento
}
```

### @Singleton / @Factory — Inyección de dependencias

```kotlin
@Singleton
class MyService { }     // Una sola instancia compartida

@Factory
class MyDependency { }  // Nueva instancia cada vez
```

### @Named / @Inject — Bindings nombrados

```kotlin
@Singleton
@Named("special")
fun providesService(): MyService = MyService()

// Usar:
@Inject("special") val service: MyService
```

### @Initializer — Inicializador de extensión

```kotlin
@Initializer
object MyInitializer : Initializable {
    override fun initialize() { /* setup */ }
    override fun shutdown() { /* cleanup - SIN resource leaks */ }
}
```

### Anotaciones de campos

| Anotación | Uso |
|-----------|-----|
| `@Help` | Tooltip de documentación |
| `@Placeholder` | Campo acepta placeholders |
| `@Colored` | Campo acepta códigos de color |
| `@MultiLine` | Campo multi-línea |
| `@OnlyTags` | Restringir a tags específicos |
| `@Regex` | Validación de patrón |
| `@WithRotation` | Posición con yaw/pitch |
| `@Segments` | Marcar lista de segments |
| `@InnerMin(n)` | Duración mínima de segment (frames) |
| `@InnerMax(n)` | Duración máxima de segment (frames) |
| `@ContentEditor` | UI custom de edición in-game |
| `@GenericConstraint` | Restricción de tipos genéricos |

---

## Jerarquía de Entry Types

```
Entry
├── StaticEntry
│   ├── AssetEntry            → Recursos externos
│   ├── ArtifactEntry         → Assets generados por servidor
│   ├── SoundIdEntry          → Sonidos custom del resource pack
│   ├── SoundSourceEntry      → Sonido en entidad/ubicación
│   ├── SpeakerEntry          → NPC con nombre y sonido
│   └── VariableEntry<T>      → Valor dinámico en runtime
│
├── TriggerEntry
│   ├── EventEntry            → Listener de eventos Bukkit
│   │   └── CustomCommandEntry → Comandos in-game
│   └── TriggerableEntry
│       ├── DialogueEntry     → Diálogos con messenger
│       ├── ActionEntry       → Acciones inmediatas
│       └── CustomTriggeringActionEntry → Trigger manual
│
├── CinematicEntry            → Secuencias frame-based
│
└── ManifestEntry
    ├── AudienceEntry         → Display a grupos
    │   ├── AudienceFilterEntry → Filtro de audiencia
    │   ├── QuestEntry        → Quests
    │   ├── ObjectiveEntry    → Objetivos de quest
    │   └── LinesEntry        → Líneas de texto
    └── EntityDefinitionEntry → Definición de entidad
```

---

## Crear Entry: Action

```kotlin
@Entry("my_action", "My Custom Action", Colors.RED, "mdi:play")
class MyActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    // Campos custom:
    @Help("Mensaje a enviar")
    @Placeholder
    val message: Var<String> = ConstVar(""),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        // `player` y `context` disponibles via receiver
        player.sendMessage(message.get(player, context))
    }
}
```

---

## Crear Entry: Event

```kotlin
@Entry("my_event", "My Custom Event", Colors.YELLOW, "mdi:flash")
class MyEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    val targetBlock: Material = Material.STONE,
) : EventEntry

@EntryListener(MyEventEntry::class)
fun onBlockBreak(event: BlockBreakEvent, query: Query<MyEventEntry>) {
    query.findWhere { it.targetBlock == event.block.type }
        .triggerAllFor(event.player, context())
}
```

---

## Crear Entry: Dialogue

```kotlin
@Entry("my_dialogue", "My Dialogue", Colors.BLUE, "mdi:chat")
class MyDialogueEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    override val speaker: Ref<SpeakerEntry> = emptyRef(),
    @Placeholder @Colored @MultiLine
    val text: Var<String> = ConstVar(""),
) : DialogueEntry {
    override fun messenger(player: Player, context: InteractionContext): DialogueMessenger<*> {
        return MyMessenger(player, context, this)
    }
}

class MyMessenger(
    player: Player,
    context: InteractionContext,
    entry: MyDialogueEntry,
) : DialogueMessenger(player, context, entry) {
    var state = MessengerState.RUNNING

    override fun tick(player: Player, context: InteractionContext) {
        // Mostrar contenido, verificar input
        if (playerConfirmed) state = MessengerState.FINISHED
    }
}
```

**MessengerState:** `RUNNING` (continuar), `FINISHED` (trigger siguiente), `CANCELLED` (detener cadena)

---

## Crear Entry: Cinematic

```kotlin
@Entry("my_cinematic", "My Cinematic", Colors.PURPLE, "mdi:movie")
class MyCinematicEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    @Segments
    val segments: List<MySegment> = emptyList(),
) : CinematicEntry {
    override fun create(player: Player, context: InteractionContext): CinematicAction {
        return MyCinematicAction(player, this)
    }
}

data class MySegment(
    override val startFrame: Int,
    override val endFrame: Int,
    val text: String,
) : Segment

class MyCinematicAction(val player: Player, val entry: MyCinematicEntry) : CinematicAction {
    override fun setup() { /* inicializar */ }
    override fun tick(frame: Int) { /* mostrar estado en frame */ }
    override fun teardown() { /* cleanup */ }
    override fun canFinish(frame: Int): Boolean = true
}
```

**SimpleCinematicAction** (simplificado):
```kotlin
class MySimpleAction : SimpleCinematicAction<MySegment>() {
    override fun onSegmentStart(segment: MySegment, frame: Int) {}
    override fun onSegmentEnd(segment: MySegment, frame: Int) {}
    override fun tick(frame: Int) {}
    override fun canFinish(frame: Int): Boolean = true
}
```

---

## Crear Entry: Audience

```kotlin
@Entry("my_audience", "My Audience", Colors.GREEN, "mdi:people")
class MyAudienceEntry(
    override val id: String = "",
    override val name: String = "",
    override val children: List<Ref<AudienceEntry>> = emptyList(),
    val filterValue: String = "",
    override val inverted: Boolean = false,
) : AudienceFilterEntry, Invertible {
    override suspend fun display(): AudienceFilter = MyFilter(ref(), filterValue)
}

class MyFilter(
    ref: Ref<out AudienceFilterEntry>,
    private val filterValue: String,
) : AudienceFilter(ref) {
    override fun filter(player: Player): Boolean {
        return player.world.name == filterValue
    }
}
```

**TickableDisplay** para updates regulares:
```kotlin
class MyDisplay : AudienceDisplay, TickableDisplay {
    override fun tick() {
        // Llamado cada tick de Minecraft (20x/segundo)
        // Ejecuta en thread async — asegurar thread safety
    }
}
```

---

## Crear Entry: Fact

```kotlin
@Entry("my_fact", "My Fact", Colors.PURPLE, "mdi:database")
class MyFactEntry(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val value = calculateValue(player)
        return FactData(value)
    }
}
```

---

## Dependency Injection (Koin)

### Registrar

```kotlin
@Singleton
class MyService { }

@Factory
class MyDependency { }

@Singleton
@Named("special")
fun providesService(): MyService = MyService()
```

### Obtener

```kotlin
// KoinComponent
class MyClass : KoinComponent {
    val service: MyService by inject()
}

// Manual
val service = KoinJavaComponent.get<MyService>()

// Named
val service = KoinJavaComponent.get<MyService>(named("special"))

// All bindings
val all: List<MyService> = getAll<MyService>()
```

---

## Query System

```kotlin
Query.find<SomeEntryType>()                    // Todas del tipo
Query.find<SomeEntryType> { it.prop == val }   // Con filtro
Query.findById("entry-id")                      // Por ID
Query.findFromPage(pageId)                       // De una página
```

---

## Triggering System

```kotlin
// Trigger simple
triggerEntry.trigger(player)

// Con contexto
triggerEntry.trigger(player, existingContext)

// Iniciar diálogo o trigger siguiente
startDialogueWithOrNextDialogue(player, dialogueEntry)

// Trigger para todos los matches
entries.triggerAllFor(player, context())
```

---

## Interaction Context

### Flujo de datos

```
E[event] → S[spoken] → A1[action]
           ↓
           O[option] → A3[action]

Acceso:
- E: contexto inicial
- S: puede leer E
- A1: puede leer E, S
- A3: puede leer E, S, O
- A1 NO puede leer O, A3 (rama diferente)
```

### Crear y usar context keys

```kotlin
object MyContextKey : ContextKey<String>

// Escribir
context[MyContextKey] = "valor"

// Leer
val value = context[MyContextKey]
```

---

## Imports Comunes

```kotlin
// Anotaciones
import com.typewritermc.core.extension.annotations.*
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.books.pages.Colors

// Entry types
import com.typewritermc.engine.paper.entry.entries.*
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.core.interaction.InteractionContext

// Utilities
import com.typewritermc.engine.paper.entry.startDialogueWithOrNextDialogue
import com.typewritermc.engine.paper.entry.triggerAllFor
import com.typewritermc.core.entries.Query
```
# Patrones de Código (Ejemplos Reales)

Patrones extraídos del código fuente de extensiones existentes de Typewriter.

---

## Patrón: Action Entry Simple

Basado en `MessageActionEntry` (BasicExtension):

```kotlin
@Entry("send_message", "Send Message", Colors.RED, "flowbite:message-dots-outline")
class MessageActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("El speaker del mensaje")
    val speaker: Ref<SpeakerEntry> = emptyRef(),
    @Placeholder @Colored @MultiLine
    @Help("El mensaje a enviar")
    val message: Var<String> = ConstVar(""),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val msg = message.get(player, context)
        player.sendMessage(msg)
    }
}
```

---

## Patrón: Action con Delay (Coroutines)

Basado en `DelayedActionEntry`:

```kotlin
@Entry("delayed_action", "Delayed Action", Colors.RED, "mdi:timer")
class DelayedActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    val duration: Var<Duration> = ConstVar(Duration.ZERO),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        // Deshabilitamos auto-trigger
        disableAutomaticTriggering()

        // Delay async, luego trigger manual
        delay(duration.get(player, context))
        triggerManually()
    }
}
```

---

## Patrón: Event Entry con Listener

Basado en `OnInteractWithBlockEventEntry`:

```kotlin
@Entry("on_interact_with_block", "On Interact With Block", Colors.YELLOW, "mdi:cube-outline")
class OnInteractWithBlockEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    val block: Material = Material.AIR,
    @WithRotation
    val location: Optional<Position> = Optional.empty(),
    val interactionType: InteractionType = InteractionType.ALL,
    val shiftType: ShiftType = ShiftType.ANY,
    val cancel: Boolean = false,
) : EventEntry

@EntryListener(OnInteractWithBlockEventEntry::class)
fun onInteractWithBlock(event: PlayerInteractEvent, query: Query<OnInteractWithBlockEventEntry>) {
    if (event.action != Action.RIGHT_CLICK_BLOCK) return
    val block = event.clickedBlock ?: return

    query.findWhere {
        it.block == block.type &&
        (it.location.isEmpty || it.location.get() == block.location.toPosition()) &&
        it.interactionType.matches(event) &&
        it.shiftType.matches(event.player)
    }.forEach { entry ->
        if (entry.cancel) event.isCancelled = true
        entry.startDialogueWithOrNextDialogue(event.player, context())
    }
}
```

---

## Patrón: Dialogue con Messenger

Basado en `SpokenDialogueEntry`:

```kotlin
@Entry("spoken", "Spoken Dialogue", "#1E88E5", "mdi:chat-processing")
class SpokenDialogueEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    override val speaker: Ref<SpeakerEntry> = emptyRef(),
    @Placeholder @Colored @MultiLine
    val text: Var<String> = ConstVar(""),
    val duration: Var<Duration> = ConstVar(Duration.ofSeconds(3)),
) : DialogueEntry {
    override fun messenger(player: Player, context: InteractionContext): DialogueMessenger<*> {
        return SpokenDialogueMessenger(player, context, this)
    }
}

class SpokenDialogueMessenger(
    player: Player,
    context: InteractionContext,
    entry: SpokenDialogueEntry,
) : DialogueMessenger<SpokenDialogueEntry>(player, context, entry) {
    private var startTime = System.currentTimeMillis()

    override var state: MessengerState = MessengerState.RUNNING

    override fun tick(player: Player, context: InteractionContext) {
        val elapsed = System.currentTimeMillis() - startTime
        val text = entry.text.get(player, context)
        val duration = entry.duration.get(player, context).toMillis()

        // Calcular cuántos caracteres mostrar (efecto escritura)
        val charsToShow = (text.length * (elapsed.toDouble() / duration)).toInt()
            .coerceAtMost(text.length)

        val displayText = text.substring(0, charsToShow)
        player.sendMessage(displayText)

        // Verificar si jugador confirma o duración termina
        if (elapsed >= duration && playerConfirmed) {
            state = MessengerState.FINISHED
        }
    }
}
```

---

## Patrón: Integración con Plugin Externo (Vault)

Basado en `VaultInitializer` + `DepositBalanceActionEntry`:

### Inicializador

```kotlin
@Singleton
class VaultInitializer : Initializable {
    var economy: Economy? = null
        private set
    var permission: Permission? = null
        private set
    var chat: Chat? = null
        private set

    override suspend fun initialize() {
        val server = Bukkit.getServer()

        // Obtener servicio de Vault
        val rspEconomy = server.servicesManager.getRegistration(Economy::class.java)
        economy = rspEconomy?.provider

        val rspPermission = server.servicesManager.getRegistration(Permission::class.java)
        permission = rspPermission?.provider

        val rspChat = server.servicesManager.getRegistration(Chat::class.java)
        chat = rspChat?.provider
    }

    override suspend fun shutdown() {
        economy = null
        permission = null
        chat = null
    }
}
```

### Entry usando plugin externo

```kotlin
@Entry("deposit_balance", "Deposit Balance", Colors.RED, "mdi:cash-plus")
class DepositBalanceActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Placeholder
    val amount: Var<Double> = ConstVar(0.0),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val vault = KoinJavaComponent.get<VaultInitializer>(VaultInitializer::class.java)
        val economy = vault.economy ?: return

        val depositAmount = amount.get(player, context)
        economy.depositPlayer(player, depositAmount)
    }
}
```

---

## Patrón: Audience Filter con Tick

Basado en `PermissionAudienceEntry` (VaultExtension):

```kotlin
@Entry("permission_audience", "Permission Audience", Colors.GREEN, "mdi:shield-account")
class PermissionAudienceEntry(
    override val id: String = "",
    override val name: String = "",
    override val children: List<Ref<AudienceEntry>> = emptyList(),
    val permission: String = "",
    override val inverted: Boolean = false,
) : AudienceFilterEntry, Invertible {
    override suspend fun display(): AudienceFilter {
        return PermissionAudienceFilter(ref(), permission)
    }
}

class PermissionAudienceFilter(
    ref: Ref<out AudienceFilterEntry>,
    private val permission: String,
) : AudienceFilter(ref), TickableDisplay {

    override fun filter(player: Player): Boolean {
        val vault = KoinJavaComponent.get<VaultInitializer>(VaultInitializer::class.java)
        return vault.permission?.playerHas(null, player, permission) ?: false
    }

    override fun tick() {
        // Refresh filtro periódicamente
        players.forEach { player ->
            player.refresh()  // Re-evalúa filter() para cada jugador
        }
    }
}
```

---

## Patrón: Fact (Solo Lectura)

Basado en `InRegionFact` (WorldGuardExtension):

```kotlin
@Entry("in_region", "In Region Fact", Colors.PURPLE, "mdi:map-marker")
class InRegionFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    val region: String = "",
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val container = WorldGuard.getInstance()
            .platform.regionContainer
        val regions = container.get(BukkitAdapter.adapt(player.world))
            ?.getApplicableRegions(BukkitAdapter.asBlockVector(player.location))

        val isInRegion = regions?.regions?.any { it.id == region } ?: false
        return FactData(if (isInRegion) 1 else 0)
    }
}
```

---

## Patrón: Entity Definition + Instance

Basado en EntityExtension:

```kotlin
@Entry("npc_definition", "NPC Definition", Colors.ORANGE, "mdi:account")
@Tags("npc_definition")
class NpcDefinition(
    override val id: String = "",
    override val name: String = "",
    override val displayName: Var<String> = ConstVar(""),
    override val sound: Var<Sound> = ConstVar(Sound.EMPTY),
    @OnlyTags("generic_player_data", "npc_data")
    override val data: List<Ref<EntityData<*>>> = emptyList(),
) : SimpleEntityDefinition {
    override fun create(player: Player): FakeEntity = NpcEntity(player)
}

@Entry("npc_instance", "NPC Instance", Colors.YELLOW, "mdi:account-check")
class NpcInstance(
    override val id: String = "",
    override val name: String = "",
    override val definition: Ref<NpcDefinition> = emptyRef(),
    @WithRotation
    override val spawnLocation: Position = Position.ORIGIN,
    @OnlyTags("generic_player_data", "npc_data")
    override val data: List<Ref<EntityData<*>>> = emptyList(),
    override val activity: Ref<out SharedEntityActivityEntry> = emptyRef(),
) : SimpleEntityInstance
```

---

## Patrón: build.gradle.kts de Extensión

```kotlin
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    // Repos del plugin externo si aplica
}

dependencies {
    compileOnly("com.external:plugin-api:1.0.0")  // SIEMPRE compileOnly
}

typewriter {
    namespace = "mycompany"

    extension {
        name = "MyExtension"
        shortDescription = "Mi extensión custom"
        description = """
            Descripción detallada de lo que hace la extensión.
            Mínimo 100 caracteres, máximo 2000.
        """
        engineVersion = file("../../version.txt").readText().trim()
        channel = com.typewritermc.moduleplugin.ReleaseChannel.NONE

        paper {
            dependency("ExternalPlugin")  // Si depende de plugin Paper
        }
    }
}

kotlin {
    jvmToolchain(21)
}
```

**Notas:**
- Dependencias externas SIEMPRE como `compileOnly`
- `paper()` declara plataforma Paper/Bukkit
- `dependency()` en paper block para plugins requeridos
- `flag(ExtensionFlag.Deprecated)` para marcar como deprecado
# PlaceholderAPI

## Requisito

Instalar plugin **PlaceholderAPI** en el servidor.

---

## Placeholders de Typewriter

### Placeholders Base

| Placeholder | Valor devuelto |
|-------------|---------------|
| `%typewriter_<entryid>%` | Valor según tipo de entry |
| `%typewriter_<entryname>%` | Valor según tipo de entry |

### Valores según tipo de entry

| Tipo de Entry | Valor devuelto |
|---------------|---------------|
| Speaker | displayName |
| Fact | Valor del fact |
| Sound | ID del sonido del resource pack |
| Entity | displayName |
| Lines entry | Contenido |
| Quest | displayName |
| Objective | displayName formateado |
| Sidebar | title |

---

## Parámetros de Facts

| Placeholder | Descripción |
|-------------|-------------|
| `%typewriter_<id>:remaining:<number>%` | `number - valor del fact` |
| `%typewriter_<id>:time:lastUpdated%` | Tiempo de última actualización |
| `%typewriter_<id>:time:lastUpdated:relative%` | Tiempo relativo (ej: "3m 20s") |
| `%typewriter_<id>:time:expires%` | Tiempo de expiración (facts expirables) |
| `%typewriter_<id>:time:expires:relative%` | Tiempo hasta expiración |

---

## Placeholders Custom

| Placeholder | Valor |
|-------------|-------|
| `%typewriter_tracked_quest%` | Nombre de la quest tracked |
| `%typewriter_tracked_objectives%` | Nombres de objetivos activos (separados por coma) |
| `%typewriter_tracked_objectives_locations%` | Ubicaciones de objetivos tracked |
| `%typewriter_in_dialogue%` | `0` (no) o `1` (sí en diálogo) |
| `%typewriter_in_cinematic%` | `0` (no) o `1` (sí en cinemática) |

---

## Crear Placeholders en Extensiones

### Placeholder básico

```kotlin
@Entry("my_entry", "My Entry", Colors.BLUE, "mdi:tag")
class MyEntry(...) : SomeEntry, PlaceholderEntry {
    override fun placeholder(player: Player): String {
        return "Hello ${player.name}!"
    }
}
// Uso: %typewriter_<entry-id>%
```

### Sub-placeholders (literales)

```kotlin
class MyEntry(...) : SomeEntry, PlaceholderEntry {
    override fun subPlaceholders(): Map<String, (Player) -> String> {
        return mapOf(
            "greet" to { player -> "Hello, ${player.name}!" },
            "greet:enthusiastic" to { player -> "HEY ${player.name}!" },
        )
    }
}
// Uso: %typewriter_<entry-id>:greet%
// Uso: %typewriter_<entry-id>:greet:enthusiastic%
```

### Sub-placeholders (variables)

```kotlin
class MyEntry(...) : SomeEntry, PlaceholderEntry {
    override fun subPlaceholder(key: String): (Player) -> String {
        return { player -> "Hello, $key!" }
    }
}
// Uso: %typewriter_<entry-id>:bob%  → "Hello, bob!"
// Uso: %typewriter_<entry-id>:alice% → "Hello, alice!"
```

---

## Troubleshooting

### Placeholder no parsea

1. Verificar extensión requerida instalada en PlaceholderAPI:
   ```
   /papi ecloud download [extension]
   ```
   Ejemplo: `/papi ecloud download player` para `%player_name%`

2. Probar placeholder aislado:
   ```
   /papi parse me <placeholder>
   ```

3. Si funciona aislado pero no en Typewriter: reiniciar servidor
