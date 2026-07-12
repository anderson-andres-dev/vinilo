# Vinilo

Reproductor de música local para Android, con identidad propia inspirada en
Pixel y Material You. La experiencia está enfocada exclusivamente en música:
portadas, canciones, álbumes, artistas y playlists — nunca en videos,
comentarios, canales u otros elementos propios de un cliente de video.

Toda la biblioteca y reproducción funcionan completamente offline y en el
propio dispositivo. No existe backend propio, servidores intermedios ni APIs
propias.

Como funcionalidad adicional, Vinilo permitirá buscar canciones y obtener
únicamente su audio como fuente para descargarlas y agregarlas a la
biblioteca local — en ningún momento se reproduce ni se muestra video.

## Estado del proyecto

En desarrollo activo, fase de bootstrap (biblioteca local + reproducción
básica). Ver [`CHANGELOG`](#) y los issues del repositorio para el progreso.

## Créditos / Open Source

Este proyecto no pretende reinventar un reproductor de música desde cero.
Se apoya en el excelente trabajo de proyectos open source existentes:

- **[Auxio](https://github.com/OxygenCobalt/Auxio)** (GPL-3.0) — el módulo
  `musikr` de Vinilo es código vendorizado del proyecto Auxio, que resuelve
  el escaneo de música vía Storage Access Framework, extracción de
  metadatos, cache, portadas y playlists. Todo el mérito de ese motor
  pertenece a los autores y contribuidores de Auxio.
- **[TagLib](https://github.com/taglib/taglib)** — librería nativa usada por
  `musikr` para leer metadatos de audio.

Integraciones planeadas (próximas fases):

- **[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)**
  (GPL-3.0) — extracción de audio desde resultados de búsqueda de YouTube.
- **[MusicBrainz](https://musicbrainz.org)** — enriquecimiento de metadatos.
- **[Cover Art Archive](https://coverartarchive.org)** — portadas de
  respaldo cuando no hay una incrustada ni disponible en MusicBrainz.

Ver [`NOTICE`](NOTICE) para el detalle completo de atribuciones.

## Licencia

GNU General Public License v3.0. Ver [`LICENSE`](LICENSE).
