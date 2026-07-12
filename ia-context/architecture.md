# Arquitectura técnica

Ver también: [`index.md`](index.md) · [`philosophy.md`](philosophy.md) · [`decisions.md`](decisions.md) · [`build-and-environment.md`](build-and-environment.md)

## Stack

Kotlin, Jetpack Compose, Material 3, Media3 (ExoPlayer estándar), Room, Coroutines/Flow, Coil,
Hilt. `musikr` (vendorizado) usa además TagLib nativo vía JNI/NDK/CMake.

Ver [`build-and-environment.md`](build-and-environment.md) para versiones exactas pinneadas.

## Estructura de módulos

```
Vinilo/
├── app/            módulo Android app — TODO el código propio de Vinilo
│   └── com.anderson.vinilo/
│       ├── ViniloApplication.kt      @HiltAndroidApp
│       ├── MainActivity.kt           Compose UI (pantalla única de biblioteca por ahora)
│       ├── music/MusicRepository.kt  wrapper delgado sobre Musikr (musikr)
│       ├── library/LibraryViewModel.kt
│       ├── playback/PlaybackService.kt  MediaSessionService + ExoPlayer estándar
│       └── ui/theme/                 tema Compose (default de Android Studio, sin adaptar aún)
├── musikr/         módulo vendorizado de Auxio — motor de biblioteca local (ver abajo)
└── ia-context/      este directorio
```

**Regla importante**: `app/` es 100% código de Vinilo, escrito desde cero. `musikr/` es
código ajeno vendorizado — no se edita como si fuera propio (ver más abajo y
`musikr/VENDORED.md`).

## `musikr`: el motor vendorizado

Vendorizado desde `OxygenCobalt/Auxio` (rama `dev`, commit
`d47d78664c7f74dc7be466d6569fba5158a600a4`), paquete `org.oxycblt.musikr` sin renombrar.
Detalle de la vendorización, incluyendo submodules anidados (`taglib`, y el `utfcpp` que
`taglib` a su vez requiere) en `musikr/VENDORED.md`.

Qué resuelve `musikr` (no hay que reimplementar nada de esto):

- **Escaneo**: vía Storage Access Framework (no MediaStore), el usuario elige una carpeta y
  el sistema mantiene permiso persistente sobre ese URI.
- **Extracción de metadatos**: JNI hacia TagLib nativo (compilado en build-time desde fuente,
  no binarios prebuilt) — soporta ID3v1/v2, MP4, Xiph, etc.
- **Cache**: Room, para no re-extraer tags de archivos ya indexados.
- **Portadas**: `musikr.covers.*` — `EmbeddedCovers` (extrae portada incrustada en el archivo),
  `MutableStoredCovers` (persiste en storage interno de la app), y piezas para encadenar
  fuentes (`ChainedCovers`) que Vinilo todavía no usa (ver [`roadmap.md`](roadmap.md)).
- **Playlists guardadas**: `musikr.playlist.db.StoredPlaylists`.
- **Modelo de música**: `Song`, `Album`, `Artist`, `Genre`, `Playlist`, todos con `Music.UID`
  que soporta IDs de MusicBrainz — pensado para el enriquecimiento futuro.

Qué **NO** resuelve `musikr` (hay que construirlo en `app/`):

- Reproducción de audio (Vinilo usa Media3/ExoPlayer estándar directo, no via musikr)
- Cualquier UI
- Ajustes/settings persistentes de usuario (ubicación de la biblioteca recordada, etc. — hoy
  el picker de carpeta no persiste entre reinicios de la app)

### Pipeline de carga (dentro de `musikr`, para entender logs/errores)

```
ExploreStep (encuentra archivos vía SAF) 
  → ExtractStep (JNI a TagLib, extrae tags + portada incrustada)
    → EvaluateStep (arma el grafo canciones/álbumes/artistas/géneros)
```

Se invoca así desde `app` (ver `MusicRepository.scan()`):

```kotlin
val result = Musikr.new(context, Config(fs, storage, interpretation)).run()
// result.library: MutableLibrary (songs, albums, artists, genres, playlists)
// result.cleanup(): libera portadas huérfanas
```

## Cómo arma Vinilo la config de `musikr` (`MusicRepository.kt`)

- `fs`: `SAF.from(context, SAF.Query(source = [carpeta elegida], ...))`
- `storage.cache`: `MutableDBCache.from(context)` (Room, provisto por musikr)
- `storage.covers`: `MutableStoredCovers(EmbeddedCovers(...), CoverStorage.at(...), NoTranscoding)`
  — **solo portada incrustada por ahora**, sin fallback a Cover Art Archive/YouTube/generada
  (eso es fase futura, ver [`roadmap.md`](roadmap.md))
- `storage.storedPlaylists`: `StoredPlaylists.from(context)` (provisto por musikr, sin UI propia
  todavía)
- `interpretation`: `Naming.simple()` + `Separators.from(";")`

Nada de esto usa las clases de Auxio-app (`SettingCovers`, `MusicSettings`, etc.) — esas nunca
se vendorizaron, son parte del módulo `app` de Auxio que decidimos no copiar (ver
[`decisions.md`](decisions.md)).

## UI (Compose)

Una sola pantalla por ahora (`MainActivity.kt` → `LibraryScreen`):

- Botón para elegir carpeta (`ActivityResultContracts.OpenDocumentTree`)
- Spinner mientras `MusicRepository.indexing` es `true`
- `LazyColumn` de canciones: portada (decodificada on-the-fly desde `Song.cover` con
  `BitmapFactory`, mostrada con Coil `Image`), título, artista(s)
- Tap en una canción → `MediaController` (conectado a `PlaybackService` vía `SessionToken`) →
  `setMediaItem` + `prepare()` + `play()`

No hay theming dinámico por portada (Palette), no hay pantallas de álbum/artista/detalle, no
hay cola de reproducción, no hay notificación de reproducción rica — todo eso es fase futura.

## Reproducción (`PlaybackService.kt`)

`MediaSessionService` mínimo: un `ExoPlayer` detrás de un `MediaSession`, sin cola, sin
persistencia de estado de reproducción entre reinicios del proceso. Deliberadamente **no** se
usa el fork de Media3 que mantiene Auxio (submodule `media` en su repo) — se evaluará solo si
aparece una limitación real de Media3 estándar (ver [`decisions.md`](decisions.md)).

## Data flow de extremo a extremo (estado actual)

```
Usuario toca "Elegir carpeta" 
  → SAF picker → Uri
    → LibraryViewModel.onFolderChosen(uri) 
      → MusicRepository.scan(uri) [suspend, corre en viewModelScope]
        → arma Config/Storage/Interpretation
        → Musikr.new(...).run() 
          → StateFlow<MutableLibrary?> se actualiza
            → Compose recompone LazyColumn con result.library.songs
              → tap en canción → MediaController.play()
```
