# ia-context — mapa mental de Vinilo

Este directorio es la memoria de arquitectura del proyecto: para qué existe Vinilo, qué
decisiones se tomaron y por qué, qué está construido y qué falta. Está pensado para que
cualquier instancia de IA (o persona) que entre a este repo sin contexto previo pueda
orientarse leyendo estos documentos en vez de releer todo el código o repreguntar decisiones
ya tomadas.

**Si solo vas a leer uno, lee este.** Los demás documentos profundizan cada sección.

## Mapa de documentos

| Documento | Para qué sirve |
|---|---|
| [`philosophy.md`](philosophy.md) | Qué es Vinilo, qué NO es, filosofía de producto, inspiración |
| [`architecture.md`](architecture.md) | Stack técnico, estructura de módulos, cómo encaja `musikr`, flujo de datos |
| [`decisions.md`](decisions.md) | Decisiones clave ya tomadas, con el porqué (no re-litigar sin motivo) |
| [`roadmap.md`](roadmap.md) | Fases completadas y fases pendientes, en orden |
| [`build-and-environment.md`](build-and-environment.md) | Versiones de toolchain, cómo compilar, problemas conocidos del entorno |

## Resumen de una línea

Vinilo es un reproductor de música local para Android (Compose + Material3) construido sobre
el motor de escaneo/tagging vendorizado de [Auxio](https://github.com/OxygenCobalt/Auxio)
(`musikr`, GPLv3), que en el futuro permitirá buscar música y traer solo el audio desde
YouTube (vía NewPipeExtractor) para agregarla a la biblioteca local — sin que la app se sienta
nunca como un cliente de video.

## Estado actual (2026-07-12)

**Fase 0 (bootstrap) completa.** La app compila (`./gradlew :app:assembleDebug` exitoso,
incluyendo compilación nativa de TagLib), escanea música local vía SAF + `musikr`, muestra una
lista simple de canciones en Compose, y reproduce audio con Media3/ExoPlayer estándar.
**No verificado todavía en un dispositivo/emulador real** (el entorno donde se construyó esto
no tenía `adb` ni emulador — ver [`build-and-environment.md`](build-and-environment.md)).

Todo lo demás del documento de visión original (búsqueda YouTube, MusicBrainz, Cover Art
Archive, theming dinámico, playlists, etc.) **todavía no existe** — ver
[`roadmap.md`](roadmap.md) para el detalle de qué falta y en qué orden tiene sentido
construirlo.

## Cómo usar esto al retomar el proyecto

1. Lee este índice y `roadmap.md` para saber en qué fase está el proyecto.
2. Antes de proponer una decisión de arquitectura nueva, revisa `decisions.md` — si ya se
   decidió algo y no cambiaron los hechos que lo motivaron, no lo vuelvas a plantear desde
   cero.
3. Si vas a tocar `musikr/`, lee primero `musikr/VENDORED.md` (en la raíz del módulo, no acá)
   y la sección correspondiente en `architecture.md`: es código vendorizado de Auxio, no se
   edita como si fuera código propio salvo para adaptarlo al catálogo de versiones.
4. Actualiza `roadmap.md` (mové el ítem de "pendiente" a "hecho", con fecha) cada vez que
   termines una fase, para que el próximo que entre no tenga que adivinar el estado leyendo
   commits.
