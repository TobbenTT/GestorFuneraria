package com.tuempresa.gestorfuneraria

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class VerServiciosAdminActivity : AppCompatActivity() {

    // Variable para guardar la lista original y poder filtrar
    private var listaCompleta: List<DocumentSnapshot> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_servicios_admin)

        val btnVolver = findViewById<ImageButton>(R.id.btnVolverAdmin)
        val contenedor = findViewById<LinearLayout>(R.id.contenedorGlobal)
        val etBuscador = findViewById<EditText>(R.id.etBuscador) // <--- El buscador nuevo

        btnVolver.setOnClickListener { finish() }

        // 1. Cargar datos
        cargarYEscucharServicios(contenedor)

        // 2. Configurar el Buscador (Escucha lo que escribes)
        etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarLista(s.toString(), contenedor)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 3. Limpieza automática
        verificarLimpiezaAutomatica()
    }

    private fun cargarYEscucharServicios(contenedor: LinearLayout) {
        val db = FirebaseFirestore.getInstance()

        db.collection("servicios")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    // Guardamos la lista original en memoria
                    listaCompleta = snapshots.documents

                    // Mostramos todo al principio
                    renderizarLista(listaCompleta, contenedor)
                }
            }
    }

    // Función que filtra la lista en memoria
    private fun filtrarLista(texto: String, contenedor: LinearLayout) {
        val textoBusqueda = texto.lowercase().trim()

        val listaFiltrada = listaCompleta.filter { doc ->
            val difunto = (doc.getString("difunto") ?: "").lowercase()
            val cementerio = (doc.getString("cementerio") ?: "").lowercase()

            // Si el nombre O el cementerio contienen el texto, lo mostramos
            difunto.contains(textoBusqueda) || cementerio.contains(textoBusqueda)
        }

        renderizarLista(listaFiltrada, contenedor)
    }

    // Función encargada de "Dibujar" las tarjetas
    private fun renderizarLista(lista: List<DocumentSnapshot>, contenedor: LinearLayout) {
        contenedor.removeAllViews()

        if (lista.isEmpty()) {
            val mensaje = TextView(this)
            mensaje.text = "No se encontraron resultados"
            mensaje.setPadding(20, 20, 20, 20)
            contenedor.addView(mensaje)
            return
        }

        for (doc in lista) {
            val vista = LayoutInflater.from(this).inflate(R.layout.item_servicio, contenedor, false)

            // Llenar datos
            vista.findViewById<TextView>(R.id.tvDifunto).text = doc.getString("difunto")
            vista.findViewById<TextView>(R.id.tvDatos).text = "${doc.getString("fecha")} - ${doc.getString("hora")}\n${doc.getString("cementerio")}"

            // Colores según estado
            val tvEstado = vista.findViewById<TextView>(R.id.tvEstado)
            val estado = doc.getString("estado") ?: "PENDIENTE"
            tvEstado.text = estado

            if (estado.contains("FINALIZADO")) {
                tvEstado.setTextColor(android.graphics.Color.parseColor("#00C853")) // Verde
            } else if (estado.contains("EN LUGAR")) {
                tvEstado.setTextColor(android.graphics.Color.parseColor("#2962FF")) // Azul
            } else {
                tvEstado.setTextColor(android.graphics.Color.RED) // Rojo
            }

            // Botón WhatsApp
            val btnWsp = vista.findViewById<ImageButton>(R.id.btnWhatsapp)
            val telefono = doc.getString("telefonoContacto") ?: ""
            if (telefono.isNotEmpty()) {
                btnWsp.setOnClickListener {
                    try {
                        val num = telefono.replace(Regex("[^0-9]"), "")
                        val finalNum = if (num.startsWith("569")) num else "569$num"
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$finalNum"))
                        startActivity(i)
                    } catch (e: Exception) { Toast.makeText(this, "Error WhatsApp", Toast.LENGTH_SHORT).show() }
                }
            } else {
                btnWsp.alpha = 0.3f
                btnWsp.isEnabled = false
            }

            // Botón Mapa
            val btnMapa = vista.findViewById<Button>(R.id.btnMapa)
            btnMapa.setOnClickListener {
                val dir = doc.getString("cementerio") ?: ""
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(dir)))
                i.setPackage("com.google.android.apps.maps")
                try { startActivity(i) } catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(dir)))) }
            }

            // Ocultar finalizar (es admin)
            try { vista.findViewById<Button>(R.id.btnFinalizar).visibility = android.view.View.GONE } catch (e: Exception) {}

            contenedor.addView(vista)
        }
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