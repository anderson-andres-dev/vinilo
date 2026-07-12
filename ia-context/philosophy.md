# Filosofía y visión de producto

Ver también: [`index.md`](index.md) · [`architecture.md`](architecture.md)

## Qué es Vinilo

Un reproductor de música para Android inspirado en la simplicidad de los dispositivos Pixel y
en Material You. El objetivo es una experiencia enfocada completamente en la música: portadas,
canciones, álbumes, artistas, playlists. Nada más.

Toda la app funciona localmente en el dispositivo. No hay backend propio, no hay servidores
intermedios, no hay APIs propias. Cuando la app habla con servicios externos (YouTube para
extraer audio, MusicBrainz/Cover Art Archive para metadatos), lo hace directo desde el
teléfono — no hay ningún servidor de Vinilo en el medio.

## Qué NO es Vinilo

No es un cliente de YouTube. Aunque en el futuro usará YouTube como fuente de audio, el
usuario nunca debe sentir que está usando YouTube. Por eso la app **nunca** muestra:

- Videos ni reproduce video en ningún momento (solo se extrae y reproduce audio)
- Shorts
- Comentarios
- Likes / contadores de likes
- Canales ni suscriptores
- Videos relacionados o recomendados al estilo YouTube
- Cualquier otro elemento de interfaz propio de un cliente de video

Solo se muestra lo que pertenece a un reproductor de música: portadas, canciones, álbumes,
artistas, playlists, biblioteca.

## Por qué se construye sobre Auxio en vez de desde cero

La intención explícita del proyecto **no es reinventar un reproductor de música**. Escanear
una biblioteca local, leer tags de audio correctamente, cachear, organizar álbumes/artistas,
manejar Material You e integrar Media3 son problemas ya resueltos muy bien por
[Auxio](https://github.com/OxygenCobalt/Auxio). No tiene sentido rehacer eso.

Pero tampoco es "un fork con otro nombre". La idea es tomar la base sólida que resuelve esos
problemas (concretamente el módulo `musikr` de Auxio — ver
[`architecture.md`](architecture.md#musikr-el-motor-vendorizado)), eliminar/reemplazar/
modificar lo que no encaje con la visión de Vinilo, y construir encima una identidad y
funcionalidad propias que Auxio no tiene — sobre todo, el flujo de búsqueda + extracción de
audio desde YouTube y el enriquecimiento de metadatos vía MusicBrainz.

Todo esto se hace **respetando la licencia de Auxio (GPLv3) y dando crédito explícito** a sus
autores — ver `NOTICE` y `README.md` en la raíz del repo, y [`decisions.md`](decisions.md) para
el razonamiento completo sobre la licencia.

## La funcionalidad diferencial: buscar música vía YouTube, escuchar solo audio

El flujo pensado (todavía no implementado, ver [`roadmap.md`](roadmap.md)):

```
Buscar canción → Mostrar resultados → Escuchar solo audio → Descargar audio
  → Agregar automáticamente a la biblioteca local
```

En ningún punto de ese flujo se reproduce o muestra video. Una vez descargada, la canción debe
comportarse exactamente igual que cualquier archivo de música ya presente en el dispositivo:
mismos metadatos, misma biblioteca, mismo reproductor, funciona completamente offline después.

## Portadas: prioridad visual

La experiencia visual es prioridad. El orden de resolución de portadas pensado es:

1. Portada incrustada en el archivo (durante la descarga, o ya presente en archivos locales)
2. Cover Art Archive vía MusicBrainz
3. Miniatura de YouTube (si la canción vino de ahí)
4. Imagen generada automáticamente como último recurso

El objetivo es que nunca haya una biblioteca con portadas vacías. Ver
[`roadmap.md`](roadmap.md) — hoy (fase 0) solo está resuelto el nivel 1 (portada incrustada,
vía `musikr`).

## Interfaz

Debe sentirse moderna, rápida, minimalista. Inspiración: Pixel, Material You, Auxio, Apple
Music. La interfaz debería adaptarse dinámicamente a los colores predominantes de la portada
(Palette) — esto todavía no está implementado (ver [`roadmap.md`](roadmap.md)).

## Por qué existe este proyecto

Es un proyecto personal para aprender, experimentar y divertirse desarrollando — no busca
atribuirse el trabajo de otros proyectos open source. El mérito de resolver escaneo/tagging/
reproducción/Material You es de Auxio; el de yt-dlp/NewPipeExtractor, MusicBrainz y Cover Art
Archive es de esos proyectos. El aporte de Vinilo es integrar estas piezas, extender sus
capacidades, y diseñar una experiencia de usuario y funcionalidades nuevas sobre esa base.
