# Guía de la Barra de Herramientas de Android Studio vs Comandos Gradle

Esta guía resume el significado de los botones de la barra superior de Android Studio y sus comandos equivalentes en la terminal Gradle / React Native.

---

### 🔍 Explicación Botón por Botón (de izquierda a derecha)

#### 1. 📱 Selector de Dispositivo (`motorola moto g30`)
* **Comando equivalente:** `adb devices`
* **Descripción:** Muestra y selecciona el dispositivo físico conectado por USB/Wi-Fi o el emulador de Android donde se enviará e instalará la aplicación.

#### 2. 🤖 Selector de Configuración (`app`)
* **Descripción:** Muestra el módulo del proyecto activo a compilar (por defecto `:app`). Si seleccionas *Edit Configurations...*, puedes ajustar qué Activity se lanza o pasar parámetros de inicio.

#### 3. 🟢 Botón Ejecutar (`Run 'app'`)
* **Comando equivalente:** `./gradlew :app:installDebug` *(o `npx react-native run-android`)*
* **Descripción:** Compila el APK en modo **Debug**, lo instala en el teléfono mediante `adb install -r` y lo inicia automáticamente. *(Ver sección `installRelease` para cambiar a modo Release)*.

#### 4. 🔄 Aplicar Cambios (`Apply Code Changes and Restart Activity`)
* **Comando equivalente:** Similar al *Fast Refresh* / *Hot Reload* en React Native.
* **Descripción:** Inyecta los cambios de código recién guardados directamente en la app en ejecución sin necesidad de reinstalar todo el APK ni perder el estado actual.

#### 5. 🐛 Botón Depurar (`Debug 'app'`)
* **Comando equivalente:** `./gradlew :app:installDebug` + adjuntar depurador JDWP.
* **Descripción:** Compila, instala y conecta el **Debugger de Android Studio**. Te permite colocar puntos de interrupción (*breakpoints*) en el código Kotlin para pausar la ejecución y revisar variables.

#### 6. 🟥 Botón Detener (`Stop 'app'`)
* **Comando equivalente:** `adb shell am force-stop com.frankgp.appinfo`
* **Descripción:** Fuerza el cierre del proceso de la aplicación en el dispositivo seleccionado.

#### 7. 🔨 Compilar Proyecto (`Make Project`)
* **Comando equivalente:** `./gradlew :app:assembleDebug`
* **Descripción:** Compila todos los módulos del proyecto para verificar errores de código y sintaxis sin instalar la app en ningún dispositivo.

#### 8. 🐘 Sincronizar Gradle (`Sync Project with Gradle Files`)
* **Comando equivalente:** `./gradlew build --refresh-dependencies`
* **Descripción:** Lee los archivos de configuración de Gradle (`build.gradle.kts`, `settings.gradle.kts`) y actualiza el árbol de dependencias e índices del IDE.

#### 9. 📈 Profiler (Analizador de Rendimiento)
* **Descripción:** Abre el **Android Profiler**, una herramienta de diagnóstico en tiempo real para inspeccionar el consumo de **CPU, Memoria RAM, Red y Batería** del proceso de la app.

---

### 🚀 ¿Cómo ejecutar `./gradlew installRelease` en Android Studio?

Por defecto, el botón 🟢 **Run** ejecuta la variante de compilación `debug` (`installDebug`). Para ejecutar `installRelease` directamente en Android Studio existen 2 métodos:

1. **Vía la pestaña "Build Variants" (Barra Lateral Izquierda - Método Principal):**
   * Abre la pestaña lateral **Build Variants** (esquina inferior izquierda de Android Studio).
   * En la columna **Active Build Variant** para el módulo `:app`, cambia de `debug` a `release`.
   * Al presionar ahora el botón 🟢 **Run (Play verde)**, Android Studio ejecutará automáticamente `./gradlew installRelease` e instalará la versión Release firmada en tu dispositivo.

2. **Vía la ventana "Gradle" (Barra Lateral Derecha):**
   * **Para abrir el panel de Gradle:** Menú superior `View > Tool Windows > Gradle`.
   * **Para mostrar la lista de tareas en el panel de Gradle:**
     * Opción A: Haz clic directamente en el texto azul **`Task list not built...`** en la parte superior del panel Gradle.
     * Opción B: Menú `File > Settings > Experimental` y desmarcar *"Do not build Gradle task list during Gradle sync"*.
   * Una vez activado y sincronizado, navega a: `FGP App Info > app > Tasks > install > installRelease`.

---

### 🛠️ Tabla de Equivalencias (Terminal Gradle vs Android Studio)

| Acción en Terminal | Comando Gradle / ADB | Equivalente en Android Studio |
| :--- | :--- | :--- |
| **Instalar y Ejecutar Debug** | `./gradlew installDebug` | 🟢 **Botón Run (Play verde)** en variante `debug` |
| **Instalar y Ejecutar Release** | `./gradlew installRelease` | Cambiar variante a `release` en **Build Variants** y presionar 🟢 **Run** |
| **Compilar APK Debug** | `./gradlew assembleDebug` | 🔨 **Martillo (Make Project)** |
| **Compilar APK Release** | `./gradlew assembleRelease` | Menú: `Build > Build Bundle(s) / APK(s) > Build APK(s)` |
| **Generar Bundle Release (.aab)** | `./gradlew bundleRelease` | Menú: `Build > Generate Signed Bundle / APK` |
| **Forzar Cierre de App** | `adb shell am force-stop` | 🟥 **Cuadrado Rojo (Stop)** |
| **Listar Dispositivos** | `adb devices` | 📱 **Selector de Dispositivos** |
| **Refrescar Gradle** | `./gradlew --refresh-dependencies` | 🐘 **Elefante Gradle Sync** |
