# Decisiones tomadas (y por qué)

Ver también: [`index.md`](index.md) · [`architecture.md`](architecture.md) · [`roadmap.md`](roadmap.md)

Estas decisiones fueron confirmadas explícitamente con el usuario durante la planificación de
la Fase 0 (2026-07-12). Tratarlas como asentadas — no volver a plantearlas desde cero salvo que
cambien los hechos que las motivaron o el usuario pida explícitamente revisarlas.

## Licencia: GPL-3.0, aceptado con conocimiento de causa

Vendorizar `musikr` de Auxio (GPLv3) obliga por copyleft a que todo Vinilo sea GPL-3.0. El
usuario confirmó explícitamente que acepta ese trade-off. `LICENSE`, `NOTICE` y la sección de
créditos del `README.md` ya reflejan esto. No relicenciar sin re-confirmar con el usuario.

## Cómo se trajo Auxio: copia de código fuente, no submodule, no fork completo

Se evaluaron 3 opciones: copiar código fuente, git submodule, fork completo del repo. Se eligió
**copiar el código fuente** (`musikr/` vendorizado como directorio plano dentro del repo,
paquete `org.oxycblt.musikr` sin renombrar) porque da control total para modificar/eliminar
partes sin la rigidez de un submodule, y no arrastra el historial completo de un fork que no
se necesita.

Consecuencia: **no hay sincronización automática con upstream**. Actualizar `musikr` más
adelante implica re-descargar desde un commit más nuevo de Auxio y re-diffear a mano contra
`musikr/VENDORED.md` (que documenta el commit exacto vendorizado y las adaptaciones hechas).

## UI: Compose fresco, no se copia el módulo `app` de Auxio

Auxio usa Fragments/Views/Navigation clásico, no Compose. La visión de Vinilo pide Material 3 +
Compose. Se decidió construir la UI completamente desde cero sobre los datos que expone
`musikr`, sin tocar ni copiar el código de UI de Auxio. Esto es justamente lo que separa a
Vinilo de "un fork con otro nombre" (ver [`philosophy.md`](philosophy.md)).

## Reproducción: Media3 estándar, no el fork de Auxio

Auxio mantiene su propio fork de AndroidX Media3 (submodule `media` en su repo) con parches
custom. Se decidió usar **Media3/ExoPlayer estándar de Maven** para el bootstrap: mucho más
simple de integrar, cubre reproducción básica sin problema. Solo se debería evaluar vendorizar
el fork de Auxio si aparece una limitación real y concreta que Media3 estándar no resuelva —
no adelantarse a esa necesidad.

## Extracción de audio de YouTube: NewPipeExtractor, no yt-dlp directo

El documento de visión original del usuario menciona "yt-dlp". yt-dlp es Python y no corre
nativo en Android sin un intérprete embebido pesado (Chaquopy, o el wrapper
`youtubedl-android`). Se decidió usar **NewPipeExtractor** en su lugar: librería Kotlin/Java
pura, sin runtime extra, ya probada en producción por NewPipe. Esto todavía no está
implementado (fase futura, ver [`roadmap.md`](roadmap.md)) — pero si el usuario vuelve a decir
"yt-dlp", entender que se refiere al *concepto* (extracción de audio desde YouTube), y que la
implementación acordada es NewPipeExtractor, salvo que pida explícitamente lo contrario.

## Escaneo: SAF, no MediaStore

`musikr` soporta ambos modos (`SAF.from` y `fs.mediastore.MediaStore.from`), pero se usa SAF
porque es la filosofía completa de `musikr` (bypasear MediaStore para tagging más rico) y es lo
que da control total sobre qué carpeta se indexa. Contrapartida: requiere que el usuario elija
la carpeta manualmente vía picker (`ACTION_OPEN_DOCUMENT_TREE`) — no hay "escanear todo el
dispositivo" automático. En Fase 0 tampoco se persiste la carpeta elegida entre reinicios de
la app (pendiente, ver [`roadmap.md`](roadmap.md)).

## Portadas en Fase 0: solo la incrustada, sin la cadena de fallback completa

La visión de producto pide una cadena de 4 niveles (incrustada → Cover Art Archive → miniatura
YouTube → generada). En Fase 0 solo se implementó el primer nivel (`EmbeddedCovers` de
`musikr`), deliberadamente, para no adelantar trabajo que depende de integraciones que todavía
no existen (MusicBrainz, YouTube). Ver [`roadmap.md`](roadmap.md).

## Versiones de dependencias: se sigue lo que usa Auxio `dev` cuando `musikr` lo exige

Kotlin, Room, Hilt, KSP, NDK, CMake se pinnearon para que coincidan con lo que la rama `dev` de
Auxio usa en su propio `musikr`, salvo cuando eso chocó con limitaciones reales del entorno de
build (ver [`build-and-environment.md`](build-and-environment.md) para el caso concreto de
`hilt-navigation-compose`, bajado de 1.4.0 a 1.3.0 por una exigencia de compileSdk 37 que este
entorno no tenía instalada).
