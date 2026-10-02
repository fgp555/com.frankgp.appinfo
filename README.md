# FGP App Info 📱🔍

**FGP App Info** es una aplicación avanzada para Android desarrollada en **Kotlin** y **Jetpack Compose (Material 3)** que permite explorar y analizar todas las aplicaciones instaladas en el dispositivo, detectando automáticamente su lenguaje de programación, framework y librerías nativas.

---

## 🚀 Características Principales

- **Listado Completo de Apps**: Muestra todas las aplicaciones de usuario y del sistema instaladas en el teléfono.
- **Detección Automática de Lenguaje y Framework**:
  - **Java / Kotlin**: Apps nativas estándar (filtrando infraestructura AndroidX/Google).
  - **Híbrida**: Detecta automáticamente frameworks multiplataforma como **Flutter**, **React Native**, **Ionic / Cordova / Capacitor**, **Xamarin / .NET**, etc., analizando el APK.
  - **Nativa (C++)**: Detecta apps con alto contenido de código nativo o motores de juegos (*Unity*, *Unreal Engine*, *Cocos2d*).
- **Detalles Técnicos Completos**:
  - Arquitecturas soportadas (`ARM64`, `ARMv7`, `x86`, `x86_64`).
  - Listado de librerías nativas (`.so`).
  - Fechas de instalación y última actualización.
  - Copia rápida del *Package Name* al portapapeles.
- **Acciones Rápidas (Slidable)**:
  - Desliza cualquier tarjeta hacia la izquierda para revelar accesos directos con iconos puros para **Abrir la app**, **Ir a Ajustes / Forzar Detención** y **Desinstalar**.
- **Filtros Avanzados**:
  - Filtrar por origen (*Todas*, *Usuario*, *Sistema*).
  - Filtrar por tipo de lenguaje (*Java*, *Nativa*, *Híbrida*).
  - Búsqueda en tiempo real por nombre o package name.

---

## 🛠️ Tecnologías Utilizadas

- **Lenguaje**: Kotlin (Coroutines, Flow)
- **UI**: Jetpack Compose, Material 3
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Inspección de APKs**: `java.util.zip.ZipFile` para análisis local de DEX, `.so` y Assets sin requerir root.

---

## 🏗️ Cómo Compilar y Ejecutar

1. Clona el repositorio o abre el proyecto en **Android Studio**.
2. Sincroniza el proyecto con Gradle.
3. Conecta un dispositivo físico o emulador Android (API 24+).
4. Ejecuta la tarea de Gradle o presiona **Run** (` ▶ `).
