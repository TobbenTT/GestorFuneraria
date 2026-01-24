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
    private lateinit var switchDisponibilidad: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val usuario = auth.currentUser

        if (usuario == null) { finish(); return }
        emailActual = usuario.email ?: ""

        // Inicializar Vistas (Asegúrate de tener estos IDs en activity_staff.xml)
        tvNombre = findViewById(R.id.tvNombreStaff)
        imgPerfil = findViewById(R.id.imgPerfilStaff)
        tvEstado = findViewById(R.id.tvEstadoStaff)
        switchDisponibilidad = findViewById(R.id.switchDisponibilidad)
        val contenedor = findViewById<LinearLayout>(R.id.contenedorServiciosStaff)

        try {
            findViewById<View>(R.id.btnIrPerfil).setOnClickListener { startActivity(Intent(this, PerfilStaffActivity::class.java)) }
            findViewById<ImageButton>(R.id.btnCerrarSesion).setOnClickListener { auth.signOut(); finish() }
        } catch (e: Exception) {}
        // Configurar Botón Cerrar Sesión
        val btnCerrar = findViewById<ImageButton>(R.id.btnCerrarSesion)
        btnCerrar.setOnClickListener {
            // 1. Cerrar sesión en Firebase
            auth.signOut()

            // 2. Crear el viaje de vuelta al Login (MainActivity)
            val intent = Intent(this, MainActivity::class.java)

            // 3. Limpiar la pila (Para que al dar "Atrás" no vuelva a entrar)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // 4. Iniciar el Login y matar esta pantalla
            startActivity(intent)
            finish()
        }

        cargarDatosPerfil()
        cargarMisServicios(contenedor)
    }

    private fun cargarDatosPerfil() {
        db.collection("usuarios").document(emailActual).addSnapshotListener { document, _ ->
            if (document == null || !document.exists()) return@addSnapshotListener
            tvNombre.text = document.getString("nombre") ?: "Chofer"
            val url = document.getString("fotoUrl")
            if (!url.isNullOrEmpty()) {
                Glide.with(this).load(url).circleCrop().into(imgPerfil)
                imgPerfil.colorFilter = null
            } else {
                imgPerfil.setImageResource(R.drawable.baseline_account_circle_24)
                imgPerfil.setColorFilter(Color.parseColor("#B0BEC5"))
            }
            val disp = document.getBoolean("disponible") ?: false
            if (disp) {
                tvEstado.text = "Disponible"
                tvEstado.setTextColor(Color.parseColor("#00C853"))
            } else {
                tvEstado.text = "Ocupado"
                tvEstado.setTextColor(Color.parseColor("#D32F2F"))
            }
            switchDisponibilidad.setOnCheckedChangeListener(null)
            switchDisponibilidad.isChecked = disp
            switchDisponibilidad.setOnCheckedChangeListener { _, isChecked ->
                db.collection("usuarios").document(emailActual).update("disponible", isChecked)
            }
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

                        // 1. DATOS
                        val estadoActual = doc.getString("estado") ?: "PENDIENTE"
                        val retiro = doc.getString("direccion_retiro") ?: "Sin datos"
                        val destino = doc.getString("cementerio") ?: "Sin datos"

                        vista.findViewById<TextView>(R.id.tvDifunto).text = doc.getString("difunto")
                        vista.findViewById<TextView>(R.id.tvDatos).text = "${doc.getString("fecha")} - ${doc.getString("hora")}"
                        vista.findViewById<TextView>(R.id.tvDirecciones).text = "🏠 Retiro: $retiro\n✝️ Destino: $destino"
                        vista.findViewById<TextView>(R.id.tvEstado).text = estadoActual

                        val btnAccion = vista.findViewById<Button>(R.id.btnAccionPrincipal)
                        val btnMapa = vista.findViewById<Button>(R.id.btnMapa)

                        // 2. LÓGICA DE BOTONES Y MAPA SEGÚN ESTADO
                        when (estadoActual) {
                            "PENDIENTE" -> {
                                // Paso 1: Ir a buscar al fallecido
                                btnAccion.text = "LLEGUÉ A RETIRO 🏠"
                                btnAccion.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2962FF")) // Azul
                                btnAccion.setOnClickListener { actualizarEstadoServicio(doc.id, "EN RETIRO") }

                                // Mapa lleva al Retiro
                                btnMapa.setOnClickListener { abrirMapa(retiro) }
                            }
                            "EN RETIRO" -> {
                                // Paso 2: Ir al cementerio
                                btnAccion.text = "LLEGUÉ A CEMENTERIO ✝️"
                                btnAccion.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF6D00")) // Naranja
                                btnAccion.setOnClickListener { actualizarEstadoServicio(doc.id, "EN CEMENTERIO") }

                                // Mapa lleva al Cementerio
                                btnMapa.setOnClickListener { abrirMapa(destino) }
                            }
                            "EN CEMENTERIO" -> {
                                // Paso 3: Finalizar
                                btnAccion.text = "FINALIZAR ✅"
                                btnAccion.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00C853")) // Verde
                                btnAccion.setOnClickListener { confirmarFinalizar(doc.id) }

                                // Mapa sigue en Cementerio
                                btnMapa.setOnClickListener { abrirMapa(destino) }
                            }
                            else -> {
                                btnAccion.text = "FINALIZAR"
                                btnAccion.setOnClickListener { confirmarFinalizar(doc.id) }
                            }
                        }

                        // WhatsApp (Opcional)
                        val btnWsp = vista.findViewById<ImageButton>(R.id.btnWhatsapp)
                        val telefono = doc.getString("telefonoContacto") ?: ""
                        if(telefono.isNotEmpty()){
                            btnWsp.setOnClickListener {
                                val url = "https://api.whatsapp.com/send?phone=${telefono.replace("+", "")}"
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        } else {
                            btnWsp.visibility = View.GONE
                        }

                        contenedor.addView(vista)
                    }
                } else {
                    val msj = TextView(this)
                    msj.text = "No tienes servicios activos."
                    msj.setTextColor(Color.GRAY)
                    msj.setPadding(50,50,50,50)
                    contenedor.addView(msj)
                }
            }
    }

    private fun actualizarEstadoServicio(docId: String, nuevoEstado: String) {
        db.collection("servicios").document(docId).update("estado", nuevoEstado)
            .addOnSuccessListener { Toast.makeText(this, "Estado: $nuevoEstado", Toast.LENGTH_SHORT).show() }
    }

    private fun confirmarFinalizar(docId: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Finalizar Servicio?")
            .setMessage("Se marcará como completado y saldrá de tu lista.")
            .setPositiveButton("SÍ, FINALIZAR") { _, _ ->
                actualizarEstadoServicio(docId, "FINALIZADO ✅")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirMapa(direccion: String) {
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(direccion))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try { startActivity(intent) } catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}