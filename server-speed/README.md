# 🚀 Go SpeedTest Server (`fgp-speedtest-server`)

Servidor HTTP nativo en Go (Golang) diseñado para realizar pruebas de velocidad de internet (Latencia/Ping, Bajada y Subida) consumiendo memoria RAM directa con rendimiento ultra-rápido.

---

### 📌 Endpoints HTTP

| Método | Endpoint | Parámetros | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/ping` | N/A | Retorna `pong` (Medición de Latencia / RTT). |
| `GET` | `/health` | N/A | Retorna `{"status":"ok","time":"..."}` para monitoreo de salud del servidor. |
| `GET` | `/download` | `?bytes=10000000` | Transmite flujo de ceros en RAM (por defecto 10 MB). |
| `POST` | `/upload` | Body octet-stream | Recibe y descarta el payload en RAM retornando resumen en JSON. |

---

### 🧪 Archivo de Peticiones REST Client

Se ha creado el archivo **[`requests.http`](file:///E:/dev/kotlin/com.frankgp.appinfo/server/requests.http)** para probar todos los endpoints directamente desde Android Studio, IntelliJ IDEA o VS Code (REST Client extension).

---

### 💻 Ejecutar localmente con WSL:

```bash
cd /mnt/e/dev/kotlin/com.frankgp.appinfo/server
go run main.go
```

---

### 🌐 Desplegar en la Nube (Render / Fly.io / Railway)

Este repositorio contiene un `Dockerfile` estándar listo para desplegar en plataformas de la nube:

* **Render.com:** Crear *Web Service*, seleccionar *Docker* o *Go* y puerto `8080`.
* **Fly.io:** Ejecutar `fly launch`.
* **Railway.app:** Conectar repositorio GitHub o desplegar `Dockerfile`.
