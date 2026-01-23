package com.tuempresa.gestorfuneraria

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CrearChoferActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_chofer)

        val etEmail = findViewById<EditText>(R.id.etEmailChofer)
        val etPass = findViewById<EditText>(R.id.etPassChofer)
        val btnCrear = findViewById<Button>(R.id.btnCrearCuenta)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        btnCrear.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPass.text.toString().trim()

            if (email.isNotEmpty() && password.length >= 6) {
                crearUsuarioSinCerrarSesion(email, password)
            } else {
                Toast.makeText(this, "Revisa los datos (Pass min 6 letras)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun crearUsuarioSinCerrarSesion(email: String, pass: String) {
        // 1. Obtenemos la configuración de tu app actual
        val opciones = FirebaseApp.getInstance().options

        // 2. Creamos una conexión "PARALELA" llamada "VentanaRegistro"
        // Esto evita que el Admin se desconecte al crear otro usuario
        val appSecundaria = try {
            FirebaseApp.getInstance("VentanaRegistro")
        } catch (e: Exception) {
            FirebaseApp.initializeApp(this, opciones, "VentanaRegistro")
        }

        val authSecundario = FirebaseAuth.getInstance(appSecundaria)

        // 3. Creamos el usuario en esa conexión paralela
        authSecundario.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                // El usuario se creó en Auth. Ahora lo guardamos en Firestore
                guardarDatosEnFirestore(email)

                // Cerramos la sesión secundaria para limpiar
                authSecundario.signOut()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun guardarDatosEnFirestore(email: String) {
        val db = FirebaseFirestore.getInstance() // Usamos la BD normal

        val nuevoUsuario = hashMapOf(
            "rol" to "STAFF",
            "disponible" to true,
            "fotoUrl" to "" // Lo dejamos vacío para que puedan poner foto después
        )

        db.collection("usuarios").document(email).set(nuevoUsuario)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Chofer Creado con Éxito! 🎉", Toast.LENGTH_LONG).show()
                finish() // Volvemos al menú
            }
            .addOnFailureListener {
                Toast.makeText(this, "Cuenta creada pero falló la BD", Toast.LENGTH_SHORT).show()
            }
    }
}