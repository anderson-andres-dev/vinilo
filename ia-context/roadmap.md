# Roadmap

Ver también: [`index.md`](index.md) · [`decisions.md`](decisions.md) · [`architecture.md`](architecture.md)

Convención: al terminar una fase, mové su contenido de "Pendiente" a "Hecho" con la fecha, y
enlazá el commit o los archivos clave si ayuda a quien retome esto después.

## Hecho

### Fase 0 — Bootstrap (completada 2026-07-12)

Plan original: `/home/anderson/.claude/plans/wise-sprouting-bear.md`.

- Repo git inicializado, commit del esqueleto de Android Studio (Empty Compose Activity)
- `LICENSE` (GPL-3.0), `NOTICE`, sección de créditos en `README.md`
- `musikr` vendorizado desde `OxygenCobalt/Auxio@d47d786` (dev) + submodules anidados
  (`taglib@ee1931b`, `utfcpp@df857ef`) — ver `musikr/VENDORED.md`
- Hilt wireado (`ViniloApplication`, `MusicRepository` inyectable)
- UI Compose mínima: picker de carpeta SAF, spinner de indexado, lista de canciones
  (título/artista/portada incrustada)
- Reproducción básica: `PlaybackService` (MediaSessionService + ExoPlayer estándar), tap para
  reproducir
- Build verificado: `./gradlew :app:assembleDebug` exitoso, incluyendo compilación nativa de
  TagLib (4 ABIs)

**Gap conocido de esta fase**: nunca se probó en un dispositivo/emulador real (el entorno de
build no tenía `adb` ni emulador — ver [`build-and-environment.md`](build-and-environment.md)).
Antes de dar la Fase 0 por "verificada en producto" de verdad, falta:
- Correr la app en un device/emulador real
- Confirmar que el picker de carpeta SAF funciona y persiste el permiso
- Confirmar que el escaneo encuentra música real y los metadatos se ven bien
- Confirmar que tocar una canción realmente reproduce audio

## Pendiente (en orden sugerido, no obligatorio)

### Fase 1 — Persistencia de biblioteca entre reinicios

Hoy `MusicRepository` no recuerda la carpeta elegida ni el resultado del último escaneo entre
reinicios del proceso — cada apertura de la app empieza vacía hasta tocar "Elegir carpeta" de
nuevo. Antes de construir features nuevas arriba, probablemente conviene:
- Persistir el URI de la carpeta elegida (SharedPreferences o DataStore)
- Re-lanzar el escaneo automáticamente al abrir la app si ya hay una carpeta elegida
- Usar el `cache` de `musikr` (ya está wireado) para que los re-escaneos sean incrementales,
  no un re-parseo completo de tags

### Fase 2 — UI real de biblioteca (álbumes, artistas, detalle)

Hoy solo existe una lista plana de canciones. Falta:
- Pestañas o navegación Álbumes / Artistas / Canciones / Playlists (los datos ya están en
  `MutableLibrary` vía `musikr`, es 100% trabajo de UI)
- Pantalla de detalle de álbum / artista
- Cola de reproducción real (hoy tocar una canción reemplaza el media item actual, sin cola)
- Notificación de reproducción rica (hoy `PlaybackService` es mínimo, sin metadata visible)

### Fase 3 — Theming dinámico por portada (Palette)

Extraer colores dominantes de la portada de la canción/álbum actual y adaptar el esquema de
color Material You en tiempo real. Depende de tener ya portadas cargadas (Fase 0 ya resuelve
esto para portada incrustada).

### Fase 4 — Búsqueda + extracción de audio de YouTube (NewPipeExtractor)

El diferencial central del producto (ver [`philosophy.md`](philosophy.md)). Flujo:
```
Buscar canción → mostrar resultados → escuchar solo audio → descargar audio
  → agregar automáticamente a la biblioteca local
```
Decisión ya tomada: NewPipeExtractor, no yt-dlp directo (ver [`decisions.md`](decisions.md)).
Nunca se debe reproducir ni mostrar video en ningún punto de este flujo.

### Fase 5 — Descarga a biblioteca local

Al terminar una descarga se debe obtener audio + portada + metadatos disponibles, y la canción
debe comportarse exactamente igual que un archivo local ya escaneado por `musikr` (mismo
modelo `Song`, misma biblioteca, funciona offline después). Probablemente implica escribir el
archivo descargado dentro de la carpeta SAF que el usuario ya eligió como biblioteca, y
disparar un re-escaneo incremental.

### Fase 6 — Enriquecimiento de metadatos vía MusicBrainz + cadena de portadas completa

Una vez descargada una canción, completar sus metadatos con MusicBrainz cuando sea posible.
Implementar el resto de la cadena de portadas que la Fase 0 dejó pendiente (ver
[`decisions.md`](decisions.md)):
```
1. Portada incrustada (ya resuelto en Fase 0, vía musikr EmbeddedCovers)
2. Cover Art Archive (vía MusicBrainz)
3. Miniatura de YouTube (si la canción vino de ahí)
4. Imagen generada automáticamente (último recurso)
```
`musikr` ya trae las piezas para encadenar fuentes de portada (`ChainedCovers`,
`MutableChainedCovers`, `FSCovers`) — es la misma pieza que usa Auxio en su propio
`SettingCovers` (ver `architecture.md`), solo falta conectarlas en `app/`.

### Fase 7 — Favoritos, historial, géneros, playlists (UI)

`musikr` ya modela `Genre` y `Playlist` (`StoredPlaylists` ya está wireado en
`MusicRepository`) — falta toda la UI y la lógica de mutación (crear/renombrar/borrar
playlists, marcar favoritos, registrar historial de reproducción).

### Fase 8 — Widgets, integración Tasker/shortcuts

Explícitamente de baja prioridad, sin diseño todavía.

### Sin fecha / evaluar solo si hace falta

- Vendorizar el fork de Media3 de Auxio (submodule `media`) en vez de Media3 estándar — solo
  si aparece una limitación real (ver [`decisions.md`](decisions.md))
- Migrar de escaneo SAF puro a soportar también MediaStore como alternativa
