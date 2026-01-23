package com.tuempresa.gestorfuneraria

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilStaffActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_staff)

        val imgMiFoto = findViewById<ImageView>(R.id.imgMiFoto)
        val tvMiEmail = findViewById<TextView>(R.id.tvMiEmail)
        val etUrl = findViewById<EditText>(R.id.etUrlFoto)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarFoto)
        val btnVolver = findViewById<Button>(R.id.btnVolverStaff)

        val usuario = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        // 1. CARGAR DATOS AL ENTRAR
        if (usuario != null) {
            val email = usuario.email
            tvMiEmail.text = email

            db.collection("usuarios").document(email!!).get()
                .addOnSuccessListener { doc ->
                    val urlActual = doc.getString("fotoUrl") ?: ""

                    if (urlActual.isNotEmpty()) {
                        etUrl.setText(urlActual) // Muestra el link actual

                        // Cargar foto visualmente (quitando filtros negros si los hubiera)
                        imgMiFoto.clearColorFilter()
                        imgMiFoto.imageTintList = null
                        Glide.with(this).load(urlActual).circleCrop().into(imgMiFoto)
                    }
                }
        }

        // 2. GUARDAR EL NUEVO LINK
        btnGuardar.setOnClickListener {
            val nuevaUrl = etUrl.text.toString().trim()

            if (nuevaUrl.isNotEmpty() && usuario != null) {
                // Actualizamos solo el campo de texto en la Base de Datos
                db.collection("usuarios").document(usuario.email!!)
                    .update("fotoUrl", nuevaUrl)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Foto Actualizada! 📸", Toast.LENGTH_SHORT).show()

                        // Refrescamos la imagen en pantalla
                        imgMiFoto.clearColorFilter()
                        imgMiFoto.imageTintList = null
                        Glide.with(this).load(nuevaUrl).circleCrop().into(imgMiFoto)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Pega un enlace primero", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }
}