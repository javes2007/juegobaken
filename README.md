# Aprendiendo Música — Android

Aplicación educativa musical nativa para Android Studio, escrita en Kotlin con Jetpack Compose.

## Funciones implementadas
- Piano interactivo de dos octavas con teclas blancas y negras.
- Síntesis de sonido local con Android AudioTrack; no requiere servicios pagos ni conexión.
- Cinco instrumentos seleccionables: piano, bajo, acordeón, batería y congas (timbres sintetizados).
- Retos de reconocimiento de notas, puntuación, racha y niveles.
- Teclado físico: A S D F G H J K L Ñ y W E T Y U I O.
- Importación y reproducción de audio compatible con Android.
- Análisis automático de notas para archivos WAV PCM de 16 bits, mono o estéreo.
- Perfil local, tema claro/oscuro y estadísticas persistentes.

## Abrir en Android Studio
1. Descarga el repositorio o usa Git > Clone.
2. Abre la carpeta raíz que contiene settings.gradle.kts.
3. Configura JDK 17 y Android SDK Platform 35.
4. Sincroniza el proyecto con Gradle y ejecútalo en un emulador o dispositivo.

## Compilar APK
En Android Studio usa Build > Build Bundle(s) / APK(s) > Build APK(s).
El APK de depuración se genera en app/build/outputs/apk/debug/app-debug.apk.

El proyecto usa Android Gradle Plugin 8.7.3 y Gradle 8.9. Si Android Studio solicita crear el Gradle Wrapper, genera el wrapper desde la ventana Gradle o desde una instalación local de Gradle 8.9.

## Límites actuales
- La detección de notas está implementada para WAV PCM 16-bit mono/estéreo. MP3 y otros formatos pueden reproducirse si Android los admite, pero todavía no se analizan para extraer notas.
- Los instrumentos se sintetizan mediante ondas; no son grabaciones de instrumentos reales.
- El perfil y el progreso se guardan localmente en el dispositivo; no hay registro ni sincronización en la nube.
- La primera versión ofrece entrenamiento de notas y teclado; los cursos completos, canciones cristianas licenciadas y evaluación avanzada de ritmo/precisión se pueden ampliar en siguientes versiones.

## Tecnologías
Kotlin · Jetpack Compose · Material 3 · Android AudioTrack · MediaPlayer · Storage Access Framework
