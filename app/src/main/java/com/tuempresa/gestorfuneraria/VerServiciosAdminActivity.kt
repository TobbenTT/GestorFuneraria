package com.tuempresa.gestorfuneraria

import android.content.Intent
import android.graphics.Color
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class VerServiciosAdminActivity : AppCompatActivity() {

    private var listaCompleta: List<DocumentSnapshot> = ArrayList()
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_servicios_admin)

        db = FirebaseFirestore.getInstance()

        val btnVolver = findViewById<ImageButton>(R.id.btnVolverAdmin)
        val contenedor = findViewById<LinearLayout>(R.id.contenedorGlobal)
        val etBuscador = findViewById<EditText>(R.id.etBuscador)

        btnVolver.setOnClickListener { finish() }

        cargarYEscucharServicios(contenedor)

        etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarLista(s.toString(), contenedor)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun cargarYEscucharServicios(contenedor: LinearLayout) {
        db.collection("servicios")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    listaCompleta = snapshots.documents
                    renderizarLista(listaCompleta, contenedor)
                }
            }
    }

    private fun filtrarLista(texto: String, contenedor: LinearLayout) {
        val busqueda = texto.lowercase().trim()
        val filtrada = listaCompleta.filter { doc ->
            val difunto = (doc.getString("difunto") ?: "").lowercase()
            val chofer = (doc.getString("staff_nombre") ?: "").lowercase()
            difunto.contains(busqueda) || chofer.contains(busqueda)
        }
        renderizarLista(filtrada, contenedor)
    }

    private fun renderizarLista(lista: List<DocumentSnapshot>, contenedor: LinearLayout) {
        contenedor.removeAllViews()

        if (lista.isEmpty()) {
            val msj = TextView(this)
            msj.text = "No hay servicios registrados."
            msj.setPadding(30, 30, 30, 30)
            contenedor.addView(msj)
            return
        }

        for (doc in lista) {
            val vista = LayoutInflater.from(this).inflate(R.layout.item_servicio, contenedor, false)

            // 1. DATOS BÁSICOS
            val difunto = doc.getString("difunto") ?: "Sin Nombre"
            val tipo = doc.getString("tipo_registro")

            // Título: Si es respaldo, agregamos etiqueta
            val tvDifunto = vista.findViewById<TextView>(R.id.tvDifunto)
            if (tipo == "RESPALDO_MANUAL") {
                tvDifunto.text = "$difunto [HISTORIAL] 📂"
            } else {
                tvDifunto.text = difunto
            }

            // Fecha
            vista.findViewById<TextView>(R.id.tvDatos).text =
                "📅 ${doc.getString("fecha")} - ⏰ ${doc.getString("hora")}"

            // 2. PERSONAL (Chofer + Apoyo)
            val chofer = doc.getString("staff_nombre") ?: "Sin Asignar"
            val acompanante = doc.getString("acompanante") ?: ""
            val tvPersonal = vista.findViewById<TextView>(R.id.tvPersonal)

            if (acompanante.isNotEmpty()) {
                tvPersonal.text = "👮 $chofer\n🤝 Apoyo: $acompanante"
            } else {
                tvPersonal.text = "👮 $chofer"
            }

            // 3. RUTA
            val retiro = doc.getString("direccion_retiro") ?: "---"
            val cementerio = doc.getString("cementerio") ?: "---"
            vista.findViewById<TextView>(R.id.tvDirecciones).text =
                "🏠 Retiro: $retiro\n✝️ Destino: $cementerio"

            // 4. ESTADO
            val tvEstado = vista.findViewById<TextView>(R.id.tvEstado)
            val estado = doc.getString("estado") ?: "PENDIENTE"
            tvEstado.text = estado
            when {
                estado.contains("FINALIZADO") -> tvEstado.setTextColor(Color.parseColor("#388E3C")) // Verde
                else -> tvEstado.setTextColor(Color.parseColor("#D32F2F")) // Rojo
            }

            // --- 5. FUNCIONES DE BOTONES ---

            // A. ELIMINAR (Papelera)
            val btnEliminar = vista.findViewById<ImageButton>(R.id.btnEliminar)
            btnEliminar.setOnClickListener {
                confirmarEliminacion(doc.id)
            }

            // B. EDITAR (Lápiz) - LÓGICA INTELIGENTE
            val btnEditar = vista.findViewById<ImageButton>(R.id.btnEditar)
            val esRespaldoManual = (tipo == "RESPALDO_MANUAL")

            // Solo ocultamos el lápiz si es un servicio NORMAL que ya finalizó.
            // Si es un RESPALDO MANUAL, permitimos editar siempre.
            if (estado.contains("FINALIZADO") && !esRespaldoManual) {
                btnEditar.visibility = View.GONE
            } else {
                btnEditar.visibility = View.VISIBLE
                btnEditar.setOnClickListener {

                    // Si es respaldo, vamos a la pantalla de RESPALDO (la que tiene campo acompañante)
                    // Si es normal, vamos a la pantalla de CREAR SERVICIO
                    val intent = if (esRespaldoManual) {
                        Intent(this, RespaldoServicioActivity::class.java)
                    } else {
                        Intent(this, CrearServicioActivity::class.java)
                    }

                    // Empaquetamos TODOS los datos
                    intent.putExtra("ID_DOCUMENTO", doc.id)
                    intent.putExtra("difunto", doc.getString("difunto"))
                    intent.putExtra("telefono", doc.getString("telefonoContacto"))
                    intent.putExtra("retiro", doc.getString("direccion_retiro"))
                    intent.putExtra("cementerio", doc.getString("cementerio"))
                    intent.putExtra("fecha", doc.getString("fecha"))
                    intent.putExtra("hora", doc.getString("hora"))
                    intent.putExtra("obs", doc.getString("observaciones"))
                    intent.putExtra("staff_email", doc.getString("staff_email"))
                    intent.putExtra("acompanante", doc.getString("acompanante")) // Dato extra para respaldo

                    startActivity(intent)
                }
            }

            // C. MAPA
            val btnMapa = vista.findViewById<Button>(R.id.btnMapa)
            btnMapa.setOnClickListener {
                val direccion = if(estado.contains("CEMENTERIO") || estado.contains("FINALIZADO")) cementerio else retiro
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(direccion)))
                i.setPackage("com.google.android.apps.maps")
                try { startActivity(i) } catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(direccion)))) }
            }

            // D. WHATSAPP
            val btnWsp = vista.findViewById<ImageButton>(R.id.btnWhatsapp)
            val tel = doc.getString("telefonoContacto") ?: ""
            if (tel.isNotEmpty()) {
                btnWsp.setOnClickListener {
                    val url = "https://api.whatsapp.com/send?phone=${tel.replace("+", "")}"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            } else {
                btnWsp.visibility = View.GONE
            }

            // Ocultar botón de chofer (no necesario para el admin)
            vista.findViewById<Button>(R.id.btnAccionPrincipal).visibility = View.GONE

            contenedor.addView(vista)
        }
    }

    private fun confirmarEliminacion(docId: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar este servicio?")
            .setMessage("Esta acción no se puede deshacer. ¿Estás seguro de borrarlo?")
            .setPositiveButton("ELIMINAR") { _, _ ->
                db.collection("servicios").document(docId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Servicio eliminado 🗑️", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}