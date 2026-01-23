package com.tuempresa.gestorfuneraria

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StaffActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff)

        // Referencias a la interfaz
        val btnPerfil = findViewById<Button>(R.id.btnIrPerfil)
        val btnCerrarSesion = findViewById<ImageButton>(R.id.btnCerrarSesion) // ¡Ahora sí existe! ✅
        val contenedor = findViewById<LinearLayout>(R.id.contenedorServiciosStaff)
        val tvTitulo = findViewById<TextView>(R.id.tvTituloBienvenida)

        val usuario = FirebaseAuth.getInstance().currentUser
        val emailActual = usuario?.email ?: ""
        tvTitulo.text = "Hola, ${emailActual.split("@")[0]}"

        // 1. IR AL PERFIL
        btnPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilStaffActivity::class.java))
        }

        // 2. CERRAR SESIÓN
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, com.tuempresa.gestorfuneraria.ui.login.LoginActivity::class.java)
            // Borrar historial para que no pueda volver atrás
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 3. CARGAR TAREAS
        cargarMisServicios(emailActual, contenedor)
    }

    private fun cargarMisServicios(email: String, contenedor: LinearLayout) {
        val db = FirebaseFirestore.getInstance()

        db.collection("servicios")
            .whereEqualTo("staff_email", email)
            .whereNotEqualTo("estado", "FINALIZADO ✅")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                contenedor.removeAllViews()

                if (snapshots != null && !snapshots.isEmpty) {
                    for (doc in snapshots) {
                        val vista = LayoutInflater.from(this).inflate(R.layout.item_servicio, contenedor, false)

                        // Datos
                        vista.findViewById<TextView>(R.id.tvDifunto).text = doc.getString("difunto")
                        vista.findViewById<TextView>(R.id.tvDatos).text = "${doc.getString("fecha")} - ${doc.getString("hora")}\n${doc.getString("cementerio")}"
                        vista.findViewById<TextView>(R.id.tvEstado).text = doc.getString("estado")

                        // Mapa
                        val btnMapa = vista.findViewById<Button>(R.id.btnMapa)
                        val direccion = doc.getString("cementerio") ?: ""
                        btnMapa.setOnClickListener {
                            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(direccion))
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            try { startActivity(intent) } catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }

                        // Finalizar
                        val btnFinalizar = vista.findViewById<Button>(R.id.btnFinalizar)
                        btnFinalizar.visibility = View.VISIBLE
                        btnFinalizar.setOnClickListener {
                            mostrarConfirmacion(doc.id)
                        }

                        // Ocultar WhatsApp para el chofer (opcional)
                        try { vista.findViewById<View>(R.id.btnWhatsapp).visibility = View.GONE } catch (e: Exception) {}

                        contenedor.addView(vista)
                    }
                } else {
                    val mensaje = TextView(this)
                    mensaje.text = "No tienes servicios pendientes. ¡Descansa! 😴"
                    mensaje.setTextColor(android.graphics.Color.GRAY)
                    mensaje.setPadding(30, 50, 30, 30)
                    contenedor.addView(mensaje)
                }
            }
    }

    private fun mostrarConfirmacion(idDocumento: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Finalizar Servicio?")
            .setMessage("Se marcará como completado y desaparecerá de tu lista.")
            .setPositiveButton("SÍ, FINALIZAR") { _, _ ->
                FirebaseFirestore.getInstance().collection("servicios").document(idDocumento)
                    .update("estado", "FINALIZADO ✅")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Servicio completado ✅", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}