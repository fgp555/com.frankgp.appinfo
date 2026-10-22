package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"
)

type loggingResponseWriter struct {
	http.ResponseWriter
	statusCode int
}

func (lrw *loggingResponseWriter) WriteHeader(code int) {
	lrw.statusCode = code
	lrw.ResponseWriter.WriteHeader(code)
}

func requestLogger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		lrw := &loggingResponseWriter{ResponseWriter: w, statusCode: http.StatusOK}

		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Content-Length")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			log.Printf("[%s] %s %s | 200 OPTIONS (CORS)\n", time.Now().Format("2006-01-02 15:04:05"), r.RemoteAddr, r.URL.Path)
			return
		}

		next.ServeHTTP(lrw, r)

		clientIP := r.Header.Get("CF-Connecting-IP")
		if clientIP == "" {
			clientIP = r.Header.Get("X-Forwarded-For")
		}
		if clientIP == "" {
			clientIP = r.RemoteAddr
		}

		log.Printf("[%s] %s | %s %s | Status: %d | Duración: %v\n",
			time.Now().Format("2006-01-02 15:04:05"),
			clientIP,
			r.Method,
			r.URL.RequestURI(),
			lrw.statusCode,
			time.Since(start),
		)
	})
}

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	mux := http.NewServeMux()

	// 1. Ping / Latency test
	mux.HandleFunc("/ping", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain")
		w.WriteHeader(http.StatusOK)
		fmt.Fprint(w, "pong")
	})

	// 2. Health check (JSON status)
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, `{"status":"ok","time":"%s"}`, time.Now().Format(time.RFC3339))
	})

	// 3. Download endpoint (Streams dummy bytes from RAM)
	mux.HandleFunc("/download", func(w http.ResponseWriter, r *http.Request) {
		bytesStr := r.URL.Query().Get("bytes")
		numBytes := int64(10 * 1024 * 1024) // 10 MB default
		if bytesStr != "" {
			if parsed, err := strconv.ParseInt(bytesStr, 10, 64); err == nil && parsed > 0 {
				numBytes = parsed
			}
		}

		w.Header().Set("Content-Type", "application/octet-stream")
		w.Header().Set("Content-Length", strconv.FormatInt(numBytes, 10))
		w.WriteHeader(http.StatusOK)

		buf := make([]byte, 32*1024)
		var written int64
		for written < numBytes {
			toWrite := int64(len(buf))
			if numBytes-written < toWrite {
				toWrite = numBytes - written
			}
			n, err := w.Write(buf[:toWrite])
			if err != nil {
				break
			}
			written += int64(n)
			if flusher, ok := w.(http.Flusher); ok {
				flusher.Flush()
			}
		}
	})

	// 4. Upload endpoint (Receives and discards bytes in RAM)
	mux.HandleFunc("/upload", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost && r.Method != http.MethodPut {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}

		start := time.Now()
		written, _ := io.Copy(io.Discard, r.Body)
		defer r.Body.Close()

		elapsedSec := time.Since(start).Seconds()
		var mbps float64
		if elapsedSec > 0 {
			mbps = (float64(written) * 8.0 / 1000000.0) / elapsedSec
		}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, `{"bytes_received":%d,"elapsed_seconds":%.3f,"mbps":%.2f}`, written, elapsedSec, mbps)
	})

	log.Printf("🚀 Servidor Go SpeedTest iniciado en http://0.0.0.0:%s\n", port)
	if err := http.ListenAndServe(":"+port, requestLogger(mux)); err != nil {
		log.Fatalf("Error iniciando servidor: %v\n", err)
	}
}
