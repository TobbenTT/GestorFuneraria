package com.tuempresa.gestorfuneraria

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ocultar la barra de arriba (ActionBar) para que se vea pantalla completa
        supportActionBar?.hide()

        // Esperar 3000 milisegundos (3 segundos)
        Handler(Looper.getMainLooper()).postDelayed({
            // Abrir el Login (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Matamos la Splash para que no puedan volver atrás
        }, 3000)
    }
}