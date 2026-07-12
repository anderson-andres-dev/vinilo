# Build y entorno

Ver también: [`index.md`](index.md) · [`architecture.md`](architecture.md) · [`decisions.md`](decisions.md)

## Cómo compilar

```bash
./gradlew :app:assembleDebug
```

Esto dispara, entre otras cosas, `:musikr:assembleTaglib` (compila TagLib nativo desde fuente
para `x86`, `x86_64`, `armeabi-v7a`, `arm64-v8a` vía el NDK) antes de compilar Kotlin. Requiere
que el NDK `28.2.13676358` y `cmake 3.22.1` estén disponibles — normalmente Android Studio los
ofrece instalar solos la primera vez que hace falta.

## Versiones pinneadas (`gradle/libs.versions.toml`)

| Componente | Versión | Motivo |
|---|---|---|
| AGP | 9.2.1 | Igual a lo que usa Auxio `dev` |
| Kotlin | 2.3.10 | Igual a Auxio `dev` (lo exige `musikr`) |
| KSP | 2.3.6 | Compatible con Kotlin 2.3.10 |
| Room | 2.8.4 | Igual a Auxio `dev` (lo exige `musikr`) |
| Hilt | 2.60.1 | Última estable al momento |
| `hilt-navigation-compose` | **1.3.0** (no 1.4.0) | 1.4.0 exige compileSdk 37, ver gotcha #2 abajo |
| NDK | 28.2.13676358 | Igual a Auxio `dev` (lo exige `musikr`) |
| CMake | 3.22.1 | Igual a Auxio `dev` |
| core-ktx | 1.18.0 | Igual a Auxio `dev` |
| Media3 | 1.10.1 | Última estable al momento (Media3 estándar, no el fork de Auxio) |
| Coil | 3.5.0 (coil3) | Última estable al momento |
| compileSdk / targetSdk | 36 / 36 | Ver gotcha #2 — 37 no estaba disponible en el entorno de build |

## Gotchas conocidos (ya resueltos en el código, pero explican por qué el build se ve como se ve)

1. **AGP 9+ tiene soporte de Kotlin incorporado.** Aplicar el plugin
   `org.jetbrains.kotlin.android` explícitamente (como hacía el `musikr/build.gradle` original
   de Auxio) ahora es un error duro: *"The 'org.jetbrains.kotlin.android' plugin is no longer
   required for Kotlin support since AGP 9.0"*. Se quitó del `build.gradle` de `musikr` y del
   catálogo de plugins.

2. **`androidx.hilt:hilt-navigation-compose:1.4.0` exige compileSdk 37 transitivamente**
   (pull de `androidx.lifecycle:lifecycle-*-compose:2.11.0`). El entorno de build donde se
   hizo Fase 0 solo tenía instalados los platforms `android-36`/`android-36.1`, sin
   `sdkmanager` disponible para bajar el 37. Se fijó `hiltNavigationCompose = "1.3.0"` (que
   trae `lifecycle-compose:2.9.1`, compatible con compileSdk 36). **Si tu entorno ya tiene
   compileSdk 37 instalado**, es seguro subir ambas versiones de nuevo.

3. **`:musikr` requiere core library desugaring** (por su propio `compileOptions` en Java 21).
   AGP obliga a que cualquier módulo consumidor también lo habilite, con un error claro que lo
   explica. `:app` tiene `isCoreLibraryDesugaringEnabled = true` +
   `coreLibraryDesugaring(libs.desugar.jdk.libs)` por esto.

4. **TagLib vendorizado tiene su propio submodule anidado, `utfcpp`**
   (`musikr/src/main/cpp/taglib/3rdparty/utfcpp`), que no aparece como submodule de primer
   nivel en el repo de Auxio — es un submodule *de TagLib*, un nivel más adentro. Si alguna vez
   se re-vendoriza `musikr`/`taglib` desde cero, hay que acordarse de traer también este
   submodule (pin usado: `nemtrif/utfcpp @ df857efc5bbc2aa84012d865f7d7e9cccdc08562`, el mismo
   que fija `taglib@ee1931b`) o el build nativo falla con *"utfcpp not found"*.

5. **`Artist.name` (y en general cualquier `Music` que no sea `Song`/`Playlist`) es `Name`
   plano, no `Name.Known`.** Puede ser `Name.Unknown`. Código que asuma `.raw` directo sobre
   `artist.name` no compila — hay que hacer `(name as? Name.Known)?.raw ?: fallback` (ver
   `Name.display()` en `MainActivity.kt`).

## Limitaciones del entorno donde se hizo la Fase 0

El entorno de desarrollo usado para construir y verificar la Fase 0 **no tenía**: `cmake`,
`ninja`, `adb`, ni ningún emulador/dispositivo conectado, ni sudo sin contraseña. El NDK sí
estaba presente.

Para poder al menos verificar que el build nativo de TagLib compilaba, se instalaron `cmake` y
`ninja` de forma portátil dentro de un venv de Python aislado (`pip install cmake ninja` en un
venv, ya que el pip del sistema está en modo "externally-managed" / PEP 668), y se antepuso ese
venv al `PATH` solo para las invocaciones de Gradle — nada se instaló a nivel de sistema, y ese
venv no es parte del repo (vive en un directorio de scratch de la sesión que lo creó).

**Consecuencia importante**: nunca se corrió la app en un dispositivo/emulador real. Todo lo
que dice "verificado" en [`roadmap.md`](roadmap.md) para la Fase 0 se refiere solo a que el
código compila y linkea, no a que el flujo de escaneo+reproducción funcione de extremo a
extremo en un teléfono real. Si tu entorno sí tiene Android Studio con SDK/NDK/cmake
completos y un emulador o device, ese es el primer paso pendiente antes de seguir con la
Fase 1 del [`roadmap.md`](roadmap.md).
