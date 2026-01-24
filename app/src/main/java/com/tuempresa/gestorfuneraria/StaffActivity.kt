package com.tuempresa.gestorfuneraria

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StaffActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var emailActual: String = ""

    // UI Global
    private lateinit var tvNombre: TextView
    private lateinit var imgPerfil: ImageView
    private lateinit var tvEstado: TextView
    private lateinit var viewIndicador: View
    private lateinit var switchDisponibilidad: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val usuario = auth.currentUser

        if (usuario == null) {
            volverAlLogin()
            return
        }
        emailActual = usuario.email ?: ""

        // 1. Iniciar Vistas
        tvNombre = findViewById(R.id.tvNombreStaff)
        imgPerfil = findViewById(R.id.imgPerfilStaff)
        tvEstado = findViewById(R.id.tvEstadoStaff)
        viewIndicador = findViewById(R.id.viewIndicadorEstado)
        switchDisponibilidad = findViewById(R.id.switchDisponibilidad)
        val contenedor = findViewById<LinearLayout>(R.id.contenedorServiciosStaff)

        // 2. Configurar Botones Superiores
        findViewById<View>(R.id.btnIrPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilStaffActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnCerrarSesion).setOnClickListener {
            auth.signOut()
            volverAlLogin()
        }

        // 3. Cargar Datos del Chofer (Nombre, Foto, Switch)
        cargarDatosPerfil()

        // 4. Cargar la Lista de Tareas (Con botón "Llegada" y "Finalizar")
        cargarMisServicios(contenedor)
    }

    private fun cargarDatosPerfil() {
        db.collection("usuarios").document(emailActual)
            .addSnapshotListener { document, e ->
                if (e != null || document == null || !document.exists()) return@addSnapshotListener

                // Nombre
                val nombre = document.getString("nombre") ?: "Chofer"
                tvNombre.text = nombre

                // Foto
                val urlFoto = document.getString("fotoUrl")
                if (!urlFoto.isNullOrEmpty()) {
                    Glide.with(this).load(urlFoto).circleCrop().into(imgPerfil)
                    imgPerfil.clearColorFilter()
                }

                // Estado (Disponible/Ocupado)
                val disponible = document.getBoolean("disponible") ?: false
                actualizarUIEstado(disponible)

                // Switch (evitando bucle infinito)
                switchDisponibilidad.setOnCheckedChangeListener(null)
                switchDisponibilidad.isChecked = disponible
                switchDisponibilidad.setOnCheckedChangeListener { _, isChecked ->
                    actualizarEstadoEnFirebase(isChecked)
                }
            }
    }

    private fun actualizarUIEstado(disponible: Boolean) {
        if (disponible) {
            tvEstado.text = "Disponible"
            tvEstado.setTextColor(Color.parseColor("#00C853")) // Verde
            viewIndicador.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00C853"))
        } else {
            tvEstado.text = "Ocupado"
            tvEstado.setTextColor(Color.parseColor("#D32F2F")) // Rojo
            viewIndicador.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D32F2F"))
        }
    }

    private fun actualizarEstadoEnFirebase(disponible: Boolean) {
        db.collection("usuarios").document(emailActual)
            .update("disponible", disponible)
            .addOnFailureListener {
                switchDisponibilidad.isChecked = !disponible // Revertir si falla
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarMisServicios(contenedor: LinearLayout) {
        db.collection("servicios")
            .whereEqualTo("staff_email", emailActual)
            .whereNotEqualTo("estado", "FINALIZADO ✅")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                contenedor.removeAllViews()

                if (snapshots != null && !snapshots.isEmpty) {
                    for (doc in snapshots.documents) {
                        val vista = LayoutInflater.from(this).inflate(R.layout.item_servicio, contenedor, false)

                        // A. Datos Básicos
                        vista.findViewById<TextView>(R.id.tvDifunto).text = doc.getString("difunto")
                        vista.findViewById<TextView>(R.id.tvDatos).text = "${doc.getString("fecha")} - ${doc.getString("hora")}\n${doc.getString("cementerio")}"
                        vista.findViewById<TextView>(R.id.tvEstado).text = doc.getString("estado")

                        // B. Botón Mapa
                        val btnMapa = vista.findViewById<Button>(R.id.btnMapa)
                        val direccion = doc.getString("cementerio") ?: ""
                        btnMapa.setOnClickListener {
                            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(direccion))
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            try { startActivity(intent) } catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }

                        // C. BOTÓN INTELIGENTE (LLEGADA / FINALIZAR)
                        val btnAccion = vista.findViewById<Button>(R.id.btnFinalizar)
                        btnAccion.visibility = View.VISIBLE
                        val estadoActual = doc.getString("estado") ?: "PENDIENTE"

                        if (estadoActual == "PENDIENTE") {
                            // MODO 1: En camino -> Marcar Llegada
                            btnAccion.text = "📍 MARCAR LLEGADA"
                            btnAccion.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2962FF")) // Azul
                            btnAccion.setOnClickListener {
                                db.collection("servicios").document(doc.id).update("estado", "EN LUGAR 🏁")
                            }
                        } else {
                            // MODO 2: En lugar -> Finalizar
                            btnAccion.text = "✅ FINALIZAR SERVICIO"
                            btnAccion.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00C853")) // Verde
                            btnAccion.setOnClickListener {
                                mostrarConfirmacion(doc.id)
                            }
                        }

                        // Ocultar WhatsApp (innecesario para chofer si ya tiene hoja de ruta)
                        try { vista.findViewById<View>(R.id.btnWhatsapp).visibility = View.GONE } catch (e: Exception) {}

                        contenedor.addView(vista)
                    }
                } else {
                    val mensaje = TextView(this)
                    mensaje.text = "No tienes servicios pendientes.\n¡Buen trabajo! 😴"
                    mensaje.gravity = android.view.Gravity.CENTER
                    mensaje.setPadding(50, 100, 50, 50)
                    mensaje.setTextColor(Color.DKGRAY)
                    contenedor.addView(mensaje)
                }
            }
    }

    private fun mostrarConfirmacion(idDocumento: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Finalizar Servicio?")
            .setMessage("El servicio se marcará como completado y saldrá de tu lista.")
            .setPositiveButton("FINALIZAR") { _, _ ->
                db.collection("servicios").document(idDocumento).update("estado", "FINALIZADO ✅")
                Toast.makeText(this, "Servicio Completado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun volverAlLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}