package com.tuempresa.gestorfuneraria

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class RespaldoServicioActivity : AppCompatActivity() {

    private val listaNombres = ArrayList<String>()
    private val listaEmails = ArrayList<String>()
    private lateinit var db: FirebaseFirestore
    private lateinit var spinner: Spinner

    // Variable para saber si estamos editando
    private var idEditar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_respaldo_servicio)

        db = FirebaseFirestore.getInstance()
        spinner = findViewById(R.id.spinnerChoferRespaldo)

        val etDifunto = findViewById<EditText>(R.id.etDifuntoRespaldo)
        val etAcompanante = findViewById<EditText>(R.id.etAcompananteRespaldo)
        val etLugar = findViewById<EditText>(R.id.etLugarRespaldo)
        val etFecha = findViewById<EditText>(R.id.etFechaRespaldo)
        val etHora = findViewById<EditText>(R.id.etHoraRespaldo)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarRespaldo)

        val btnVolver = findViewById<ImageButton>(R.id.btnVolverRespaldo)
        btnVolver.setOnClickListener { finish() }

        // --- 1. VERIFICAR SI VENIMOS A EDITAR ---
        if (intent.hasExtra("ID_DOCUMENTO")) {
            idEditar = intent.getStringExtra("ID_DOCUMENTO")

            // Cambiamos el texto del botón
            btnGuardar.text = "CORREGIR RESPALDO ✏️"

            // Rellenamos los campos
            etDifunto.setText(intent.getStringExtra("difunto"))
            etAcompanante.setText(intent.getStringExtra("acompanante"))
            etLugar.setText(intent.getStringExtra("cementerio")) // En respaldo usamos el campo cementerio como lugar
            etFecha.setText(intent.getStringExtra("fecha"))
            etHora.setText(intent.getStringExtra("hora"))

            // Nota: El spinner se seleccionará solo cuando cargue la lista (ver abajo)
        }

        cargarChoferes(intent.getStringExtra("staff_email"))
        configurarFechaHora(etFecha, etHora)

        btnGuardar.setOnClickListener {
            if (listaEmails.isEmpty()) return@setOnClickListener

            val difunto = etDifunto.text.toString()
            val lugar = etLugar.text.toString()

            if (difunto.isEmpty() || lugar.isEmpty()) {
                Toast.makeText(this, "Falta Difunto o Lugar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val index = spinner.selectedItemPosition
            val emailChofer = listaEmails[index]
            val nombreChofer = listaNombres[index]

            val datosRespaldo = hashMapOf<String, Any>(
                "difunto" to difunto,
                "cementerio" to lugar,
                "acompanante" to etAcompanante.text.toString(),
                "fecha" to etFecha.text.toString(),
                "hora" to etHora.text.toString(),
                "staff_nombre" to nombreChofer,
                "staff_email" to emailChofer,
                "direccion_retiro" to "Registro Manual (Oficina)",
                "estado" to "FINALIZADO ✅",
                "tipo_registro" to "RESPALDO_MANUAL"
                // No tocamos el timestamp al editar
            )

            if (idEditar != null) {
                // --- MODO ACTUALIZAR ---
                db.collection("servicios").document(idEditar!!).update(datosRespaldo)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Respaldo Corregido ✅", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } else {
                // --- MODO CREAR ---
                datosRespaldo["timestamp"] = System.currentTimeMillis()
                db.collection("servicios").add(datosRespaldo)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Respaldo Guardado 📂", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
        }
    }

    private fun cargarChoferes(emailPreseleccionado: String?) {
        db.collection("usuarios").whereEqualTo("rol", "STAFF").get()
            .addOnSuccessListener { documents ->
                listaNombres.clear()
                listaEmails.clear()

                var posicionSeleccionar = 0
                var i = 0

                for (doc in documents) {
                    val nombre = doc.getString("nombre") ?: "Sin Nombre"
                    listaNombres.add(nombre)
                    listaEmails.add(doc.id)

                    if (doc.id == emailPreseleccionado) {
                        posicionSeleccionar = i
                    }
                    i++
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaNombres)
                spinner.adapter = adapter

                // Si estamos editando, seleccionamos al chofer original
                if (idEditar != null) {
                    spinner.setSelection(posicionSeleccionar)
                }
            }
    }

    private fun configurarFechaHora(etF: EditText, etH: EditText) {
        val c = Calendar.getInstance()
        etF.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d -> etF.setText("$d/${m + 1}/$y") },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        etH.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> etH.setText(String.format("%02d:%02d", h, m)) },
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }
    }
}