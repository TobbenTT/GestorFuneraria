package com.tuempresa.gestorfuneraria

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore

class PerfilStaffActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etUrl: EditText
    private lateinit var etPass: EditText
    private lateinit var imgPreview: ImageView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var emailActual: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_staff)

        // Referencias
        etNombre = findViewById(R.id.etNombreStaff)
        etUrl = findViewById(R.id.etUrlFoto)
        etPass = findViewById(R.id.etNuevaPass)
        imgPreview = findViewById(R.id.imgPerfilPreview)

        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)
        val btnCambiarPass = findViewById<Button>(R.id.btnCambiarPass)

        // BOTÓN FLECHA ATRÁS (NUEVO)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolverAtras)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            emailActual = user.email ?: ""
            cargarDatosActuales()
        } else {
            finish()
        }

        // --- LISTENERS ---

        // 1. Guardar Datos Personales
        btnGuardar.setOnClickListener {
            guardarDatosPersonales()
        }

        // 2. Cambiar Contraseña
        btnCambiarPass.setOnClickListener {
            cambiarContrasena()
        }

        // 3. Volver (Flecha Arriba)
        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun cargarDatosActuales() {
        db.collection("usuarios").document(emailActual).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: ""
                    etNombre.setText(nombre)

                    val url = document.getString("fotoUrl") ?: ""
                    etUrl.setText(url)

                    if (url.isNotEmpty()) {
                        Glide.with(this).load(url).circleCrop().into(imgPreview)
                    }
                }
            }
    }

    private fun guardarDatosPersonales() {
        val nuevoNombre = etNombre.text.toString().trim()
        val nuevaUrl = etUrl.text.toString().trim()

        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val datosActualizados = hashMapOf<String, Any>(
            "nombre" to nuevoNombre,
            "fotoUrl" to nuevaUrl
        )

        db.collection("usuarios").document(emailActual)
            .update(datosActualizados)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Datos actualizados! ✅", Toast.LENGTH_SHORT).show()
                finish() // Volvemos automáticamente al guardar
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cambiarContrasena() {
        val nuevaPass = etPass.text.toString().trim()
        val user = auth.currentUser

        if (nuevaPass.isEmpty()) {
            Toast.makeText(this, "Escribe una contraseña", Toast.LENGTH_SHORT).show()
            return
        }
        if (nuevaPass.length < 6) {
            Toast.makeText(this, "⚠️ Mínimo 6 caracteres", Toast.LENGTH_LONG).show()
            return
        }

        if (user != null) {
            user.updatePassword(nuevaPass)
                .addOnSuccessListener {
                    Toast.makeText(this, "¡Contraseña actualizada! 🔐", Toast.LENGTH_SHORT).show()
                    etPass.setText("")
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(this, "⚠️ Cierra sesión y entra de nuevo para cambiar la clave.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}