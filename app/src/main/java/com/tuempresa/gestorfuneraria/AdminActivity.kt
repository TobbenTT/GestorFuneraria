package com.tuempresa.gestorfuneraria

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // IMPORTANTE: Ahora buscamos 'CardView' o 'View', NO 'Button'
        // Fíjate que usamos los nuevos IDs: cardNuevoServicio y cardLogout
        val btnNuevo = findViewById<android.view.View>(R.id.cardNuevoServicio)
        val btnLogout = findViewById<android.view.View>(R.id.cardLogout)

        btnNuevo.setOnClickListener {
            val intent = android.content.Intent(this, CrearServicioActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        val btnVerHistorial = findViewById<android.view.View>(R.id.cardVerHistorial)

        btnVerHistorial.setOnClickListener {
            val intent = android.content.Intent(this, VerServiciosAdminActivity::class.java)
            startActivity(intent)
        }
        val btnPersonal = findViewById<android.view.View>(R.id.cardVerChoferes)
        btnPersonal.setOnClickListener {
            startActivity(android.content.Intent(this, VerChoferesActivity::class.java))
        }
        val btnCrearCuentas = findViewById<android.view.View>(R.id.cardCrearCuentas)
        btnCrearCuentas.setOnClickListener {
            startActivity(android.content.Intent(this, CrearChoferActivity::class.java))
        }
    }
}