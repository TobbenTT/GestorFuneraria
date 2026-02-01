package com.tuempresa.gestorfuneraria

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CrearChoferActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_chofer)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Referencias a los campos
        val etNombre = findViewById<EditText>(R.id.etNombreNuevoStaff) // EL NUEVO
        val etEmail = findViewById<EditText>(R.id.etEmailNuevoStaff)
        val etPass = findViewById<EditText>(R.id.etPassNuevoStaff)
        val btnCrear = findViewById<Button>(R.id.btnCrearCuenta)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolverCrear)

        btnVolver.setOnClickListener { finish() }

        btnCrear.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            // 1. Validaciones
            if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Crear usuario en Authentication
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    // 3. Guardar el NOMBRE y datos en Firestore
                    guardarEnBaseDeDatos(email, nombre)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun guardarEnBaseDeDatos(email: String, nombre: String) {
        val nuevoUsuario = hashMapOf(
            "nombre" to nombre,     // AQUÍ GUARDAMOS EL NOMBRE INGRESADO
            "email" to email,
            "rol" to "STAFF",       // Por defecto es Staff
            "disponible" to true,   // Empieza disponible
            "fotoUrl" to ""         // Sin foto por ahora
        )

        db.collection("usuarios").document(email).set(nuevoUsuario)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Personal creado con éxito! ✅", Toast.LENGTH_LONG).show()
                finish() // Cierra la pantalla
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar datos del perfil", Toast.LENGTH_SHORT).show()
            }
    }
}