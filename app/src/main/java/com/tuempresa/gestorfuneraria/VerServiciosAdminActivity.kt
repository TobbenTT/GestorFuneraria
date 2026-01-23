package com.tuempresa.gestorfuneraria

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class VerServiciosAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_servicios_admin)

        val btnVolver = findViewById<ImageButton>(R.id.btnVolverAdmin)
        val contenedor = findViewById<LinearLayout>(R.id.contenedorGlobal)

        btnVolver.setOnClickListener { finish() }

        // 1. Iniciamos la escucha en tiempo real
        cargarTodosLosServicios(contenedor)

        // 2. Ejecutamos la limpieza en segundo plano
        verificarLimpiezaAutomatica()
    }

    // --- FUNCIÓN DE LIMPIEZA AUTOMÁTICA ---
    private fun verificarLimpiezaAutomatica() {
        val db = FirebaseFirestore.getInstance()
        val sdf = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())

        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
        val fechaLimite = cal.time

        db.collection("servicios")
            .whereEqualTo("estado", "FINALIZADO ✅")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val fechaStr = doc.getString("fecha") ?: continue
                    try {
                        val fechaServicio = sdf.parse(fechaStr)
                        if (fechaServicio != null && fechaServicio.before(fechaLimite)) {
                            archivarServicio(db, doc)
                        }
                    } catch (e: Exception) {
                        // Fecha inválida, ignorar
                    }
                }
            }
    }

    private fun archivarServicio(db: FirebaseFirestore, doc: com.google.firebase.firestore.DocumentSnapshot) {
        val datos = doc.data ?: return

        // 1. Copiamos al archivo muerto
        db.collection("historial_archivado").document(doc.id).set(datos)
            .addOnSuccessListener {
                // 2. Borramos de la lista principal
                db.collection("servicios").document(doc.id).delete()
                // NO LLAMAMOS A CARGAR DE NUEVO, EL SNAPSHOT LISTENER LO HARÁ AUTOMÁTICAMENTE
            }
    }

    private fun cargarTodosLosServicios(contenedor: LinearLayout) {
        val db = FirebaseFirestore.getInstance()

        // CONSULTA EN TIEMPO REAL DIRECTA
        db.collection("servicios")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                contenedor.removeAllViews()

                if (snapshots != null && !snapshots.isEmpty) {
                    // Usamos .documents para iterar sin ambigüedad
                    for (doc in snapshots.documents) {

                        val vistaTarjeta = LayoutInflater.from(this).inflate(R.layout.item_servicio, contenedor, false)

                        // 1. Llenar Textos
                        vistaTarjeta.findViewById<TextView>(R.id.tvDifunto).text = doc.getString("difunto")
                        vistaTarjeta.findViewById<TextView>(R.id.tvDatos).text = "${doc.getString("fecha")} - ${doc.getString("hora")}\n${doc.getString("cementerio")}"
                        vistaTarjeta.findViewById<TextView>(R.id.tvEstado).text = doc.getString("estado") ?: "PENDIENTE"

                        // 2. Lógica WhatsApp
                        val btnWsp = vistaTarjeta.findViewById<android.widget.ImageButton>(R.id.btnWhatsapp)
                        val telefono = doc.getString("telefonoContacto") ?: ""

                        if (telefono.isEmpty()) {
                            btnWsp.alpha = 0.3f
                            btnWsp.isEnabled = false
                        } else {
                            btnWsp.alpha = 1.0f
                            btnWsp.isEnabled = true
                            btnWsp.setOnClickListener {
                                try {
                                    val numeroLimpio = telefono.replace(" ", "").replace("-", "")
                                    val numeroFinal = if (numeroLimpio.startsWith("+")) numeroLimpio else "+569$numeroLimpio"
                                    val url = "https://api.whatsapp.com/send?phone=$numeroFinal"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.data = android.net.Uri.parse(url)
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // 3. Lógica MAPA
                        val btnMapa = vistaTarjeta.findViewById<android.widget.Button>(R.id.btnMapa)
                        val direccionCementerio = doc.getString("cementerio") ?: ""

                        btnMapa.setOnClickListener {
                            if (direccionCementerio.isNotEmpty()) {
                                val uri = android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(direccionCementerio))
                                val intentMapa = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                intentMapa.setPackage("com.google.android.apps.maps")
                                try {
                                    startActivity(intentMapa)
                                } catch (e: Exception) {
                                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                }
                            } else {
                                Toast.makeText(this, "Sin dirección registrada", Toast.LENGTH_SHORT).show()
                            }
                        }

                        // 4. Ocultar botón Finalizar para el Admin
                        try {
                            val btnFinalizar = vistaTarjeta.findViewById<android.widget.Button>(R.id.btnFinalizar)
                            btnFinalizar.visibility = android.view.View.GONE
                        } catch (e: Exception) {}

                        contenedor.addView(vistaTarjeta)
                    }
                } else {
                    // Mensaje si no hay nada
                    val mensaje = TextView(this)
                    mensaje.text = "No hay servicios registrados"
                    contenedor.addView(mensaje)
                }
            }
    }
}