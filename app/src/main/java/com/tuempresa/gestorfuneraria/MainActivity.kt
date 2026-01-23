package com.tuempresa.gestorfuneraria // <-- Asegúrate que esto sea tu paquete real

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    // Declaramos la variable de autenticación
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Asegúrate que coincida con tu XML

        // 1. Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 2. Referencias a los elementos visuales
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // 3. Acción del botón
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val pass = etPassword.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {

                // INTENTAMOS ENTRAR CON FIREBASE AUTH
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener {
                        // ¡Login técnico exitoso! Pero... ¿Sigue contratado?
                        val user = FirebaseAuth.getInstance().currentUser
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                        // VERIFICACIÓN DE SEGURIDAD (¿Existe en la base de datos?)
                        if (user?.email != null) {
                            db.collection("usuarios").document(user.email!!).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        // ✅ SÍ EXISTE: El empleado está activo. Vemos su rol.
                                        val rol = document.getString("rol") ?: "STAFF"

                                        if (rol == "ADMIN") {
                                            val intent = android.content.Intent(this, AdminActivity::class.java)
                                            startActivity(intent)
                                        } else {
                                            val intent = android.content.Intent(this, StaffActivity::class.java)
                                            startActivity(intent)
                                        }
                                        finish() // Cerramos el login para que no vuelva atrás

                                    } else {
                                        // 🚫 NO EXISTE: Fue eliminado por el jefe.
                                        Toast.makeText(this, "Acceso denegado: Cuenta desactivada.", Toast.LENGTH_LONG).show()
                                        FirebaseAuth.getInstance().signOut() // Lo sacamos inmediatamente
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }

            } else {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        val tvOlvide = findViewById<android.widget.TextView>(R.id.tvOlvidePass)

        tvOlvide.setOnClickListener {
            val correo = etEmail.text.toString()
            if (correo.isNotEmpty()) {
                auth.sendPasswordResetEmail(correo)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Correo de recuperación enviado 📧", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: Verifica el correo", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Escribe tu correo primero", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 1. El usuario y contraseña son correctos.
                    // Ahora preguntamos a la base de datos: "¿Qué rol tiene este usuario?"
                    checkUserRole(email)
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserRole(email: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // Buscamos en la colección "usuarios" el documento con el ID igual al email
        db.collection("usuarios").document(email).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val rol = document.getString("rol")

                    if (rol == "ADMIN") {
                        // Es el JEFE -> Vamos a la pantalla de Admin
                        val intent = Intent(this, AdminActivity::class.java)
                        startActivity(intent)
                        finish() // Cierra el login para que no pueda volver atrás
                    } else {
                        // Es EMPLEADO -> Vamos a la pantalla de Staff
                        val intent = Intent(this, StaffActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // El usuario existe en Auth pero NO en la base de datos (raro, pero posible)
                    Toast.makeText(this, "Usuario sin rol asignado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al conectar con la BD", Toast.LENGTH_SHORT).show()
            }
    }
}