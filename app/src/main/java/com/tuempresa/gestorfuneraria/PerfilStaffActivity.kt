package com.tuempresa.gestorfuneraria

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilStaffActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etUrl: EditText
    private lateinit var imgPreview: ImageView
    private lateinit var db: FirebaseFirestore
    private lateinit var emailActual: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_staff)

        // Referencias
        etNombre = findViewById(R.id.etNombreStaff)
        etUrl = findViewById(R.id.etUrlFoto)
        imgPreview = findViewById(R.id.imgPerfilPreview)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            emailActual = user.email ?: ""
            cargarDatosActuales()
        } else {
            finish() // Si no hay usuario, cierra
        }

        // Botón Guardar
        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        // Botón Volver
        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun cargarDatosActuales() {
        db.collection("usuarios").document(emailActual).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // 1. Cargar Nombre
                    val nombre = document.getString("nombre") ?: ""
                    etNombre.setText(nombre)

                    // 2. Cargar URL Foto
                    val url = document.getString("fotoUrl") ?: ""
                    etUrl.setText(url)

                    // Mostrar foto si hay URL
                    if (url.isNotEmpty()) {
                        Glide.with(this).load(url).circleCrop().into(imgPreview)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarCambios() {
        val nuevoNombre = etNombre.text.toString().trim()
        val nuevaUrl = etUrl.text.toString().trim()

        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        // Preparamos los datos a actualizar
        val datosActualizados = hashMapOf<String, Any>(
            "nombre" to nuevoNombre,
            "fotoUrl" to nuevaUrl
        )

        db.collection("usuarios").document(emailActual)
            .update(datosActualizados)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Perfil actualizado! ✅", Toast.LENGTH_SHORT).show()
                finish() // Volvemos a la pantalla anterior
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }
}