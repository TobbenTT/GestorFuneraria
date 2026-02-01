package com.tuempresa.gestorfuneraria

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
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

    private var listaCompleta: List<DocumentSnapshot> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_servicios_admin)

        val btnVolver = findViewById<ImageButton>(R.id.btnVolverAdmin)
        val contenedor = findViewById<LinearLayout>(R.id.contenedorGlobal)
        val etBuscador = findViewById<EditText>(R.id.etBuscador)

        btnVolver.setOnClickListener { finish() }

        // 1. Cargar y escuchar cambios en tiempo real
        cargarYEscucharServicios(contenedor)

        // 2. Configurar el Buscador
        etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarLista(s.toString(), contenedor)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun cargarYEscucharServicios(contenedor: LinearLayout) {
        val db = FirebaseFirestore.getInstance()

        // Ordenamos por timestamp para ver los más recientes primero
        db.collection("servicios")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    listaCompleta = snapshots.documents
                    renderizarLista(listaCompleta, contenedor)
                }
            }
    }

    private fun filtrarLista(texto: String, contenedor: LinearLayout) {
        val textoBusqueda = texto.lowercase().trim()

        val listaFiltrada = listaCompleta.filter { doc ->
            val difunto = (doc.getString("difunto") ?: "").lowercase()
            val cementerio = (doc.getString("cementerio") ?: "").lowercase()
            val retiro = (doc.getString("direccion_retiro") ?: "").lowercase()
            val chofer = (doc.getString("staff_nombre") ?: "").lowercase()

            // Buscamos por nombre, cementerio, retiro o nombre del chofer
            difunto.contains(textoBusqueda) || cementerio.contains(textoBusqueda) ||
                    retiro.contains(textoBusqueda) || chofer.contains(textoBusqueda)
        }

        renderizarLista(listaFiltrada, contenedor)
    }

    private fun renderizarLista(lista: List<DocumentSnapshot>, contenedor: LinearLayout) {
        contenedor.removeAllViews()

        if (lista.isEmpty()) {
            val mensaje = TextView(this)
            mensaje.text = "No se encontraron servicios."
            mensaje.setPadding(30, 30, 30, 30)
            contenedor.addView(mensaje)
            return
        }

        for (doc in lista) {
            val vista = LayoutInflater.from(this).inflate(R.layout.item_servicio, contenedor, false)

            // --- 1. LLENADO DE DATOS (Actualizado para mostrar Acompañante) ---
            val retiro = doc.getString("direccion_retiro") ?: "Sin datos"
            val cementerio = doc.getString("cementerio") ?: "Sin datos"
            val chofer = doc.getString("staff_nombre") ?: "Sin Asignar"

            // NUEVO: Leemos si hay un acompañante guardado
            val acompanante = doc.getString("acompanante") ?: ""

            // Si hay acompañante, lo mostramos entre paréntesis al lado del chofer
            val textoChofer = if (acompanante.isNotEmpty()) {
                "$chofer (Apoyo: $acompanante) 👥"
            } else {
                chofer
            }

            vista.findViewById<TextView>(R.id.tvDifunto).text = "${doc.getString("difunto")}"

            // Usamos un color distinto si es un RESPALDO MANUAL para diferenciarlo
            val tipoRegistro = doc.getString("tipo_registro")
            if (tipoRegistro == "RESPALDO_MANUAL") {
                vista.findViewById<TextView>(R.id.tvDifunto).append(" [HISTORIAL] 📂")
            }

            vista.findViewById<TextView>(R.id.tvDatos).text = "📅 ${doc.getString("fecha")} - ⏰ ${doc.getString("hora")}"

            // Mostramos el Chofer + Acompañante aquí abajo o en el título
            // Para que se vea ordenado, agreguémoslo en una línea nueva en la dirección o datos:
            vista.findViewById<TextView>(R.id.tvDirecciones).text = "👮 Encargados: $textoChofer\n🏠 Retiro: $retiro\n✝️ Destino: $cementerio"
            // Aquí mostramos la ruta completa
            vista.findViewById<TextView>(R.id.tvDirecciones).text = "🏠 Retiro: $retiro\n✝️ Destino: $cementerio"

            // --- 2. ESTADO Y COLORES ---
            val tvEstado = vista.findViewById<TextView>(R.id.tvEstado)
            val estado = doc.getString("estado") ?: "PENDIENTE"
            tvEstado.text = estado

            when {
                estado.contains("FINALIZADO") -> tvEstado.setTextColor(android.graphics.Color.parseColor("#00C853")) // Verde
                estado.contains("CEMENTERIO") -> tvEstado.setTextColor(android.graphics.Color.parseColor("#FF6D00")) // Naranja
                estado.contains("RETIRO") -> tvEstado.setTextColor(android.graphics.Color.parseColor("#2962FF")) // Azul
                else -> tvEstado.setTextColor(android.graphics.Color.RED) // Rojo (Pendiente)
            }

            // --- 3. BOTONES ---

            // Botón WhatsApp (Para llamar al familiar)
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
                btnWsp.visibility = View.GONE
            }

            // Botón Mapa (El admin puede ver dónde es el retiro)
            val btnMapa = vista.findViewById<Button>(R.id.btnMapa)
            btnMapa.setOnClickListener {
                // Por defecto el admin ve el retiro, o el cementerio si ya pasaron el retiro
                val direccionMapa = if (estado == "EN CEMENTERIO") cementerio else retiro
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(direccionMapa)))
                i.setPackage("com.google.android.apps.maps")
                try { startActivity(i) } catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(direccionMapa)))) }
            }

            // --- 4. CORRECCIÓN DEL ERROR ---
            // Ocultamos el botón de acción principal porque el Admin NO marca llegadas
            // Aquí es donde cambiamos 'btnFinalizar' por 'btnAccionPrincipal'
            val btnAccion = vista.findViewById<Button>(R.id.btnAccionPrincipal)
            btnAccion.visibility = View.GONE

            contenedor.addView(vista)
        }
    }
}